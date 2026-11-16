package com.rehan.monolith.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rehan.monolith.data.AppEntry
import com.rehan.monolith.data.AppRepository
import com.rehan.monolith.data.InstalledCheck
import com.rehan.monolith.data.LauncherPrefs
import com.rehan.monolith.data.MonolithSettings
import com.rehan.monolith.shizuku.ShellResult
import com.rehan.monolith.shizuku.ShizukuOps
import com.rehan.monolith.shizuku.ShizukuState
import com.rehan.monolith.ui.install.PackageInstallTracker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PackageInstallState {
    data object Idle : PackageInstallState
    data class Installing(val appLabel: String, val packageName: String) : PackageInstallState
    data class Success(val appLabel: String, val packageName: String) : PackageInstallState
    data class Error(val appLabel: String, val reason: String) : PackageInstallState
}

data class SystemTweaks(
    val grayscale: Boolean = false,
    val stayAwake: Boolean = false,
    val ambientDisplay: Boolean = false,
    val lockscreenMode: String = "FULL",
    val installersLocked: Boolean = false
)

class LauncherViewModel(
    private val prefs: LauncherPrefs,
    private val appRepository: AppRepository,
    private val shizuku: ShizukuOps,
    private val installed: InstalledCheck,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    val settings: StateFlow<MonolithSettings> = prefs.settings
    val apps: StateFlow<List<AppEntry>> = appRepository.apps
    val shizukuStatus: StateFlow<ShizukuState> = shizuku.status

    private val _tweaks = MutableStateFlow(SystemTweaks())
    val tweaks: StateFlow<SystemTweaks> = _tweaks.asStateFlow()

    val packageInstallState: StateFlow<PackageInstallState> = PackageInstallTracker.state

    private val _messages = Channel<String>(Channel.UNLIMITED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            shizuku.status.filter { it.isReady }.distinctUntilChanged().collect { readTweaks() }
        }
    }

    fun readTweaks() {
        if (!shizuku.status.value.isReady) return
        viewModelScope.launch {
            _tweaks.update {
                it.copy(
                    grayscale = shizuku.isGrayscaleEnabled(),
                    stayAwake = shizuku.getStayAwake(),
                    ambientDisplay = shizuku.isDozeAlwaysOn(),
                    lockscreenMode = shizuku.getLockscreenNotificationMode()
                )
            }
        }
    }

    private fun say(msg: String) {
        _messages.trySend(msg)
    }

    private fun failure(r: ShellResult) = "Failed: ${r.output.ifBlank { "exit ${r.exitCode}" }}"

    /** Runs [op] only when Shizuku is ready; reports the outcome and calls [onOk] on success. */
    private fun withShizuku(
        success: String?,
        op: suspend () -> ShellResult,
        onOk: () -> Unit = {}
    ) {
        if (!shizuku.status.value.isReady) {
            shizuku.requestPermission()
            say("Shizuku is not connected")
            return
        }
        viewModelScope.launch {
            val r = op()
            if (r.ok) {
                onOk()
                success?.let(::say)
            } else {
                say(failure(r))
            }
        }
    }

    private val mindfulCooldowns = java.util.concurrent.ConcurrentHashMap<String, Long>()
    @Volatile
    private var temporaryMonochromeActive = false

    fun isMindfulCooldownActive(pkg: String): Boolean {
        val expiry = mindfulCooldowns[pkg] ?: return false
        return if (System.currentTimeMillis() < expiry) {
            true
        } else {
            mindfulCooldowns.remove(pkg)
            false
        }
    }

    fun startMindfulCooldown(pkg: String, durationMinutes: Int = 15) {
        mindfulCooldowns[pkg] = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
    }

    fun onAppLaunchRequested(pkg: String) {
        val isMindful = settings.value.mindfulPauseApps.contains(pkg)
        if (isMindful && shizuku.status.value.isReady && !_tweaks.value.grayscale) {
            temporaryMonochromeActive = true
            viewModelScope.launch(ioDispatcher) {
                shizuku.setGrayscale(true)
            }
        }
    }

    fun onLauncherResumed() {
        if (temporaryMonochromeActive) {
            temporaryMonochromeActive = false
            if (!settings.value.focusModeActive && shizuku.status.value.isReady) {
                viewModelScope.launch(ioDispatcher) {
                    shizuku.setGrayscale(false)
                }
            }
        }
    }

    fun setGrayscale(on: Boolean) =
        withShizuku(null, { shizuku.setGrayscale(on) }) { _tweaks.update { it.copy(grayscale = on) } }

    fun setStayAwake(on: Boolean) =
        withShizuku(null, { shizuku.setStayAwake(on) }) { _tweaks.update { it.copy(stayAwake = on) } }

    fun setAmbientDisplay(on: Boolean) =
        withShizuku(if (on) "Always-on display enabled" else "Always-on display disabled", { shizuku.setDozeAlwaysOn(on) }) {
            _tweaks.update { it.copy(ambientDisplay = on) }
            prefs.setAmbientDisplayEnabled(on)
        }

    fun setLockscreenMode(mode: String) =
        withShizuku("Lockscreen: $mode mode", { shizuku.setLockscreenNotificationMode(mode) }) {
            _tweaks.update { it.copy(lockscreenMode = mode) }
        }

    fun setAnimationScale(scale: Float) =
        withShizuku("Animation scale ${scale}x", { shizuku.setAnimationScale(scale) })

    fun setInstallersLocked(locked: Boolean, success: String? = if (locked) "Installers locked" else "Installers unlocked") {
        val present = INSTALLERS.filter { installed.isInstalled(it) }
        withShizuku(success, { shizuku.setEnabled(present, !locked) }) {
            _tweaks.update { it.copy(installersLocked = locked) }
        }
    }

    fun setFrozen(pkg: String, freeze: Boolean) = withShizuku(
        if (freeze) "Paused" else "Resumed",
        {
            if (freeze) {
                shizuku.forceStop(pkg)
                shizuku.freezePackage(pkg)
            } else {
                shizuku.unfreezePackage(pkg)
            }
        }
    ) { appRepository.refresh() }

    fun unfreezeAll() {
        val frozen = apps.value.filter { it.isFrozen }.map { it.packageName }
        withShizuku("Unfroze ${frozen.size} app(s)", { shizuku.setEnabled(frozen, true) }) { appRepository.refresh() }
    }

    fun forceStop(pkg: String, label: String) =
        withShizuku("Force stopped $label", { shizuku.forceStop(pkg) })

    fun trimCaches() = withShizuku("All caches trimmed", { shizuku.trimAllCaches() })

    fun allowUsageStats() = withShizuku("Screen time enabled", { shizuku.allowUsageStats() })

    fun toggleFocus() {
        val next = !settings.value.focusModeActive
        val pkgs = settings.value.focusDistractions.toList()
        if (!next) {
            // Turning OFF: always flip state immediately even if Shizuku is currently offline
            prefs.setFocusModeActive(false)
            _tweaks.update { it.copy(grayscale = false) }
            if (shizuku.status.value.isReady) {
                withShizuku(
                    "Focus OFF",
                    {
                        val results = buildList {
                            add(shizuku.setGrayscale(false))
                            add(shizuku.setDnd(false))
                            pkgs.forEach { pkg ->
                                add(shizuku.unfreezePackage(pkg))
                            }
                        }
                        appRepository.refresh()
                        results.firstOrNull { !it.ok } ?: ShellResult(0, "")
                    }
                )
            } else {
                appRepository.refresh()
                say("Focus OFF (apps will restore when privileged service connects)")
            }
            return
        }

        withShizuku(
            "Focus ON (${pkgs.size} paused)",
            {
                val results = buildList {
                    add(shizuku.setGrayscale(true))
                    add(shizuku.setDnd(true))
                    pkgs.forEach { pkg ->
                        shizuku.forceStop(pkg)
                        add(shizuku.freezePackage(pkg))
                    }
                }
                prefs.setFocusModeActive(true)
                _tweaks.update { it.copy(grayscale = true) }
                appRepository.refresh()
                results.firstOrNull { !it.ok } ?: ShellResult(0, "")
            }
        )
    }

    fun installPackage(apkPath: String, appLabel: String, packageName: String, flags: List<String>) {
        if (!shizuku.status.value.isReady) {
            shizuku.requestPermission()
            say("Privileged service is not ready")
            return
        }
        PackageInstallTracker.update(PackageInstallState.Installing(appLabel, packageName))
        viewModelScope.launch(ioDispatcher) {
            val r = shizuku.installApk(apkPath, flags)
            try {
                java.io.File(apkPath).delete()
            } catch (_: Throwable) {}

            if (r.ok) {
                PackageInstallTracker.update(PackageInstallState.Success(appLabel, packageName))
                say("Installed $appLabel")
                appRepository.refresh()
            } else {
                val errorMsg = mapInstallError(r.output, r.exitCode)
                PackageInstallTracker.update(PackageInstallState.Error(appLabel, errorMsg))
                say("Failed to install $appLabel: $errorMsg")
            }
        }
    }

    fun dismissPackageInstallState() {
        PackageInstallTracker.reset()
    }

    private fun mapInstallError(output: String, exitCode: Int): String {
        return when {
            output.contains("INSTALL_FAILED_VERSION_DOWNGRADE") -> "Existing version is newer. Enable 'Allow downgrade'."
            output.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") -> "Signature mismatch with installed package."
            output.contains("INSTALL_FAILED_INSUFFICIENT_STORAGE") -> "Device storage is full."
            output.contains("INSTALL_FAILED_OLDER_SDK") -> "App requires a newer Android version."
            output.contains("INSTALL_FAILED_DEPRECATED_SDK_VERSION") -> "Target SDK blocked. Enable 'Bypass target SDK restrictions'."
            output.contains("INSTALL_PARSE_FAILED") -> "Invalid or corrupted package file."
            output.isNotBlank() -> output.lines().firstOrNull { it.contains("Failure") || it.contains("Error") } ?: output
            else -> "Shell exit code $exitCode"
        }
    }

    companion object {
        val INSTALLERS = listOf(
            "com.android.vending",
            "com.google.android.packageinstaller",
            "org.fdroid.fdroid",
            "dev.imranr.obtainium.fdroid",
            "com.rosan.installer.x.revived"
        )
    }
}

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
    private val installed: InstalledCheck
) : ViewModel() {
    val settings: StateFlow<MonolithSettings> = prefs.settings
    val apps: StateFlow<List<AppEntry>> = appRepository.apps
    val shizukuStatus: StateFlow<ShizukuState> = shizuku.status

    private val _tweaks = MutableStateFlow(SystemTweaks())
    val tweaks: StateFlow<SystemTweaks> = _tweaks.asStateFlow()

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
        withShizuku(
            if (next) "Focus ON (${pkgs.size} paused)" else "Focus OFF",
            {
                val results = buildList {
                    add(shizuku.setGrayscale(next))
                    add(shizuku.setDnd(next))
                    pkgs.forEach { pkg ->
                        if (next) {
                            shizuku.forceStop(pkg)
                            add(shizuku.freezePackage(pkg))
                        } else {
                            add(shizuku.unfreezePackage(pkg))
                        }
                    }
                }
                // Focus state flips even on partial failure so the user can always turn it back off.
                prefs.setFocusModeActive(next)
                _tweaks.update { it.copy(grayscale = next) }
                appRepository.refresh()
                results.firstOrNull { !it.ok } ?: ShellResult(0, "")
            }
        )
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

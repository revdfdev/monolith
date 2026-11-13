package com.rehan.monolith.shizuku

import com.rehan.monolith.*
import com.rehan.monolith.data.*
import com.rehan.monolith.ui.common.*
import com.rehan.monolith.ui.home.*
import com.rehan.monolith.ui.widgets.*
import com.rehan.monolith.ui.drawer.*
import com.rehan.monolith.ui.shelf.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.install.*
import com.rehan.monolith.ui.wellbeing.*

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class ShellResult(val exitCode: Int, val output: String) {
    val ok: Boolean get() = exitCode == 0
}

data class AndroidUser(val id: Int, val name: String, val isRunning: Boolean)

class ShizukuManager {

    private val _status = MutableStateFlow<ShizukuState>(ShizukuState.Unavailable)
    val status: StateFlow<ShizukuState> = _status.asStateFlow()

    @Volatile
    private var shellService: IShizukuShellService? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShizukuShellService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shizuku_shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val bound = IShizukuShellService.Stub.asInterface(service) ?: return
            shellService = bound
            val uid = try { bound.uid } catch (_: Throwable) { -1 }
            dispatch(ShizukuEvent.ServiceConnected(uid))
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
            dispatch(ShizukuEvent.ServiceDisconnected(binderAlive(), hasShizukuPermission()))
        }
    }

    private val binderListener = Shizuku.OnBinderReceivedListener { refreshAndBind() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        shellService = null
        dispatch(ShizukuEvent.BinderDead)
    }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        dispatch(ShizukuEvent.PermissionResult(grantResult == PackageManager.PERMISSION_GRANTED))
    }

    @Synchronized
    private fun dispatch(event: ShizukuEvent) {
        val t = reduceShizuku(_status.value, event)
        _status.value = t.state
        if (t.bind) bindService()
    }

    fun init() {
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        refreshAndBind()
    }

    fun destroy() {
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        try {
            if (shellService != null) {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            }
        } catch (_: Throwable) {
        }
    }

    private fun binderAlive(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun hasShizukuPermission(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestPermission() {
        if (!binderAlive()) return
        if (!hasShizukuPermission()) {
            Shizuku.requestPermission(1001)
        } else {
            refreshAndBind()
        }
    }

    fun refreshAndBind() {
        dispatch(ShizukuEvent.Refresh(binderAlive(), hasShizukuPermission()))
    }

    private fun bindService() {
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (_: Throwable) {
        }
    }

    private suspend fun call(op: IShizukuShellService.() -> OpResult): ShellResult = withContext(Dispatchers.IO) {
        val svc = shellService ?: return@withContext ShellResult(-1, "Shizuku service not bound")
        try {
            val r = svc.op()
            ShellResult(if (r.ok) 0 else r.code.takeIf { it != 0 } ?: -1, r.message.orEmpty())
        } catch (t: Throwable) {
            ShellResult(-1, t.message ?: "Remote call failed")
        }
    }

    private suspend fun putSetting(ns: String, key: String, value: String) = call { putSetting(ns, key, value) }
    private suspend fun getSetting(ns: String, key: String): String? =
        call { getSetting(ns, key) }.takeIf { it.ok }?.output?.trim()

    private fun all(vararg results: ShellResult): ShellResult =
        results.firstOrNull { !it.ok } ?: ShellResult(0, results.joinToString("\n") { it.output }.trim())

    suspend fun listUsers(): List<AndroidUser> {
        val res = call { listUsers() }
        if (!res.ok) return emptyList()
        val regex = Regex("""UserInfo\{(\d+):([^:]+):[^}]+\}(.*)""")
        return res.output.lines().mapNotNull { line ->
            val m = regex.find(line.trim()) ?: return@mapNotNull null
            val id = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            AndroidUser(id, m.groupValues[2], m.groupValues[3].contains("running"))
        }
    }

    suspend fun switchUser(userId: Int): ShellResult = call { switchUser(userId) }

    suspend fun createUser(name: String): ShellResult {
        val safe = name.replace(Regex("""[^a-zA-Z0-9_-]"""), "")
        if (safe.isBlank()) return ShellResult(-1, "Invalid user name")
        return call { createUser(safe) }
    }

    suspend fun mirrorPackageToUser(pkg: String, userId: Int): ShellResult = call { installExisting(pkg, userId) }

    suspend fun freezePackage(pkg: String): ShellResult = call { setEnabled(arrayOf(pkg), false) }

    suspend fun unfreezePackage(pkg: String): ShellResult = call { setEnabled(arrayOf(pkg), true) }

    suspend fun forceStop(pkg: String): ShellResult = call { forceStop(pkg) }

    suspend fun trimAllCaches(): ShellResult = call { trimCaches() }

    suspend fun setDnd(on: Boolean): ShellResult = call { setDnd(if (on) "alarms" else "off") }

    suspend fun allowUsageStats(): ShellResult = call { allowUsageStats(BuildConfig.APPLICATION_ID) }

    suspend fun getFrozenPackages(): Set<String> {
        val res = call { listDisabledPackages() }
        if (!res.ok) return emptySet()
        return res.output.lines()
            .mapNotNull { it.trim().removePrefix("package:").takeIf { s -> s.isNotBlank() } }
            .toSet()
    }

    suspend fun isGrayscaleEnabled(): Boolean =
        getSetting("secure", "accessibility_display_daltonizer_enabled") == "1"

    suspend fun setGrayscale(enabled: Boolean): ShellResult = if (enabled) {
        all(
            putSetting("secure", "accessibility_display_daltonizer", "0"),
            putSetting("secure", "accessibility_display_daltonizer_enabled", "1"),
            putSetting("system", "gray_scale_on", "1")
        )
    } else {
        all(
            putSetting("secure", "accessibility_display_daltonizer_enabled", "0"),
            putSetting("system", "gray_scale_on", "0")
        )
    }

    suspend fun setAnimationScale(scale: Float): ShellResult = all(
        putSetting("global", "window_animation_scale", scale.toString()),
        putSetting("global", "transition_animation_scale", scale.toString()),
        putSetting("global", "animator_duration_scale", scale.toString())
    )

    suspend fun getStayAwake(): Boolean =
        (getSetting("global", "stay_on_while_plugged_in")?.toIntOrNull() ?: 0) != 0

    suspend fun setStayAwake(enabled: Boolean): ShellResult =
        putSetting("global", "stay_on_while_plugged_in", if (enabled) "3" else "0")

    suspend fun setInstallersLocked(locked: Boolean): ShellResult {
        val installers = arrayOf(
            "com.android.vending",
            "com.google.android.packageinstaller",
            "org.fdroid.fdroid",
            "dev.imranr.obtainium.fdroid",
            "com.rosan.installer.x.revived"
        )
        return call { setEnabled(installers, !locked) }
    }

    suspend fun uninstallPackage(pkg: String): ShellResult = call { uninstall(pkg) }

    suspend fun installApk(apkPath: String, flags: List<String> = listOf("-r")): ShellResult {
        val file = File(apkPath)
        if (!file.isFile) return ShellResult(-1, "APK not found")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            call { install(pfd, file.length(), flags.toTypedArray()) }
        }
    }

    suspend fun unfreezePackages(pkgs: List<String>): ShellResult {
        if (pkgs.isEmpty()) return ShellResult(0, "Nothing to unfreeze")
        return call { setEnabled(pkgs.toTypedArray(), true) }
    }

    suspend fun getLockscreenNotificationMode(): String {
        val show = getSetting("secure", "lock_screen_show_notifications") == "1"
        val priv = getSetting("secure", "lock_screen_allow_private_notifications") == "1"
        return when {
            !show -> "CLEAN"
            !priv -> "QUIET"
            else -> "FULL"
        }
    }

    suspend fun setLockscreenNotificationMode(mode: String): ShellResult {
        val (show, priv) = when (mode) {
            "CLEAN" -> 0 to 0
            "QUIET" -> 1 to 0
            else -> 1 to 1
        }
        return all(
            putSetting("secure", "lock_screen_show_notifications", show.toString()),
            putSetting("secure", "lock_screen_allow_private_notifications", priv.toString())
        )
    }

    suspend fun isDozeAlwaysOn(): Boolean = getSetting("secure", "doze_always_on") == "1"

    suspend fun setDozeAlwaysOn(enabled: Boolean): ShellResult {
        val v = if (enabled) "1" else "0"
        // AOSP key plus the OnePlus/Oppo OEM AOD keys
        return all(
            putSetting("secure", "doze_always_on", v),
            putSetting("secure", "Setting_AodEnable", v),
            putSetting("secure", "Setting_AodSwitchEnable", v)
        )
    }
}

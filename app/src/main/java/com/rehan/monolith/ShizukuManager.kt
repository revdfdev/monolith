package com.rehan.monolith

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
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

data class ShizukuStatus(
    val binderAlive: Boolean = false,
    val hasPermission: Boolean = false,
    val serviceBound: Boolean = false,
    val remoteUid: Int = -1
)

class ShizukuManager {

    private val _status = MutableStateFlow(ShizukuStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

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
            val bound = IShizukuShellService.Stub.asInterface(service)
            shellService = bound
            val uid = try {
                bound?.uid ?: -1
            } catch (_: Throwable) {
                -1
            }
            _status.value = _status.value.copy(
                binderAlive = Shizuku.pingBinder(),
                hasPermission = hasShizukuPermission(),
                serviceBound = bound != null,
                remoteUid = uid
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
            _status.value = _status.value.copy(
                serviceBound = false,
                remoteUid = -1
            )
        }
    }

    private val binderListener = Shizuku.OnBinderReceivedListener { refreshAndBind() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        shellService = null
        _status.value = ShizukuStatus()
    }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        _status.value = _status.value.copy(hasPermission = granted)
        if (granted) bindService()
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

    fun hasShizukuPermission(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder()) return
        if (!hasShizukuPermission()) {
            Shizuku.requestPermission(1001)
        } else {
            bindService()
        }
    }

    fun refreshAndBind() {
        val alive = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
        val granted = hasShizukuPermission()
        _status.value = _status.value.copy(
            binderAlive = alive,
            hasPermission = granted,
            serviceBound = shellService != null
        )
        if (alive && granted && shellService == null) {
            bindService()
        }
    }

    private fun bindService() {
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (_: Throwable) {
        }
    }

    suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        val svc = shellService ?: return@withContext ShellResult(-1, "Shizuku service not bound")
        return@withContext try {
            val raw = svc.exec(command)
            val newline = raw.indexOf('\n')
            if (raw.startsWith("EXIT:") && newline > 5) {
                val code = raw.substring(5, newline).trim().toIntOrNull() ?: -1
                val body = raw.substring(newline + 1).trimEnd()
                ShellResult(code, body)
            } else {
                ShellResult(0, raw)
            }
        } catch (t: Throwable) {
            ShellResult(-1, t.message ?: "Remote execution failed")
        }
    }

    suspend fun listUsers(): List<AndroidUser> {
        val res = exec("pm list users")
        if (!res.ok) return emptyList()
        val regex = Regex("""UserInfo\{(\d+):([^:]+):[^}]+\}(.*)""")
        return res.output.lines().mapNotNull { line ->
            val m = regex.find(line.trim()) ?: return@mapNotNull null
            val id = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val name = m.groupValues[2]
            val running = m.groupValues[3].contains("running")
            AndroidUser(id, name, running)
        }
    }

    suspend fun switchUser(userId: Int): ShellResult = exec("am switch-user $userId")

    suspend fun createUser(name: String): ShellResult {
        val safe = name.replace(Regex("""[^a-zA-Z0-9_-]"""), "")
        if (safe.isBlank()) return ShellResult(-1, "Invalid user name")
        return exec("pm create-user $safe")
    }

    suspend fun mirrorPackageToUser(pkg: String, userId: Int): ShellResult =
        exec("pm install-existing --user $userId $pkg")

    suspend fun freezePackage(pkg: String): ShellResult =
        exec("pm disable-user --user 0 $pkg")

    suspend fun unfreezePackage(pkg: String): ShellResult =
        exec("pm enable --user 0 $pkg")

    suspend fun forceStop(pkg: String): ShellResult =
        exec("am force-stop $pkg")

    suspend fun trimAllCaches(): ShellResult =
        exec("pm trim-caches 999G")

    suspend fun getFrozenPackages(): Set<String> {
        val res = exec("pm list packages -d")
        if (!res.ok) return emptySet()
        return res.output.lines()
            .mapNotNull { it.trim().removePrefix("package:").takeIf { s -> s.isNotBlank() } }
            .toSet()
    }

    suspend fun isGrayscaleEnabled(): Boolean {
        val res = exec("settings get secure accessibility_display_daltonizer_enabled")
        return res.ok && res.output.trim() == "1"
    }

    suspend fun setGrayscale(enabled: Boolean): ShellResult {
        val flag = if (enabled) 1 else 0
        return if (enabled) {
            exec("settings put secure accessibility_display_daltonizer 0 && settings put secure accessibility_display_daltonizer_enabled 1 && settings put system gray_scale_on 1")
        } else {
            exec("settings put secure accessibility_display_daltonizer_enabled 0 && settings put system gray_scale_on 0")
        }
    }

    suspend fun setAnimationScale(scale: Float): ShellResult =
        exec("settings put global window_animation_scale $scale && settings put global transition_animation_scale $scale && settings put global animator_duration_scale $scale")

    suspend fun getStayAwake(): Boolean {
        val res = exec("settings get global stay_on_while_plugged_in")
        return res.ok && (res.output.trim().toIntOrNull() ?: 0) != 0
    }

    suspend fun setStayAwake(enabled: Boolean): ShellResult {
        val v = if (enabled) 3 else 0
        return exec("settings put global stay_on_while_plugged_in $v")
    }

    suspend fun setInstallersLocked(locked: Boolean): ShellResult {
        val installers = listOf(
            "com.android.vending",
            "com.google.android.packageinstaller",
            "org.fdroid.fdroid",
            "dev.imranr.obtainium.fdroid",
            "com.rosan.installer.x.revived"
        )
        val cmd = installers.joinToString("; ") { pkg ->
            if (locked) "pm disable-user --user 0 $pkg 2>/dev/null" else "pm enable --user 0 $pkg 2>/dev/null"
        }
        return exec("$cmd; true")
    }

    suspend fun uninstallPackage(pkg: String): ShellResult =
        exec("pm uninstall --user 0 $pkg")

    suspend fun installApk(apkPath: String): ShellResult =
        exec("pm install -r \"$apkPath\"")

    suspend fun unfreezePackages(pkgs: List<String>): ShellResult {
        if (pkgs.isEmpty()) return ShellResult(0, "Nothing to unfreeze")
        val cmd = pkgs.joinToString("; ") { "pm enable --user 0 $it 2>/dev/null" }
        return exec("$cmd; true")
    }

    suspend fun getLockscreenNotificationMode(): String {
        val showRes = exec("settings get secure lock_screen_show_notifications")
        val privRes = exec("settings get secure lock_screen_allow_private_notifications")
        val show = showRes.ok && showRes.output.trim() == "1"
        val priv = privRes.ok && privRes.output.trim() == "1"
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
        val cmd = "settings put secure lock_screen_show_notifications $show; settings put secure lock_screen_allow_private_notifications $priv"
        return exec(cmd)
    }

    suspend fun isDozeAlwaysOn(): Boolean {
        val res = exec("settings get secure doze_always_on")
        return res.ok && res.output.trim() == "1"
    }

    suspend fun setDozeAlwaysOn(enabled: Boolean): ShellResult {
        val v = if (enabled) 1 else 0
        // Universal AOSP + OnePlus/Oppo OEM AOD toggles
        val cmd = "settings put secure doze_always_on $v; settings put secure Setting_AodEnable $v 2>/dev/null; settings put secure Setting_AodSwitchEnable $v 2>/dev/null; true"
        return exec(cmd)
    }

    suspend fun getLockscreenOwnerInfo(): String {
        val res = exec("settings get secure lock_screen_owner_info")
        return if (res.ok && res.output.trim() != "null") res.output.trim() else ""
    }

    suspend fun setLockscreenOwnerInfo(enabled: Boolean, message: String): ShellResult {
        val enVal = if (enabled) 1 else 0
        val escaped = message.replace("\"", "\\\"")
        val cmd = "settings put secure lock_screen_owner_info_enabled $enVal; settings put secure lock_screen_owner_info \"$escaped\""
        return exec(cmd)
    }

    suspend fun setPitchBlackLockscreenWallpaper(): ShellResult {
        // Universal via Shizuku: generate a 1x1 black pixel or reset lockscreen
        return exec("cmd wallpaper set-empty -l 2>/dev/null || true")
    }
}

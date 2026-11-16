package com.rehan.monolith.shizuku

/** Builds argv lists for the Shizuku user service. Nothing here goes through a shell. */
object ShizukuCommands {
    private val PACKAGE = Regex("^[A-Za-z0-9_.]+$")
    private val SETTING_KEY = Regex("^[A-Za-z0-9_.]+$")
    private val USER_NAME = Regex("^[A-Za-z0-9_-]+$")
    private val NAMESPACES = setOf("system", "secure", "global")
    private val INSTALL_FLAGS = setOf("-r", "-d", "-g", "-t", "--bypass-low-target-sdk-block")
    private val DND_MODES = setOf("off", "priority", "alarms", "none")

    fun isValidPackage(pkg: String) = PACKAGE.matches(pkg)

    private fun pkg(p: String) = p.also { require(isValidPackage(it)) { "Invalid package name: $it" } }
    private fun user(id: Int) = id.also { require(it >= 0) { "Invalid user id: $it" } }.toString()
    private fun ns(n: String) = n.also { require(it in NAMESPACES) { "Invalid settings namespace: $it" } }
    private fun key(k: String) = k.also { require(SETTING_KEY.matches(it)) { "Invalid settings key: $it" } }

    fun setSuspended(p: String, suspended: Boolean) =
        listOf("pm", if (suspended) "suspend" else "unsuspend", "--user", "0", pkg(p))
    fun setEnabled(p: String, enabled: Boolean) =
        listOf("pm", if (enabled) "enable" else "disable-user", "--user", "0", pkg(p))
    fun forceStop(p: String) = listOf("am", "force-stop", pkg(p))
    fun uninstall(p: String) = listOf("pm", "uninstall", "--user", "0", pkg(p))
    fun putSetting(namespace: String, k: String, value: String) = listOf("settings", "put", ns(namespace), key(k), value)
    fun getSetting(namespace: String, k: String) = listOf("settings", "get", ns(namespace), key(k))
    fun install(flags: List<String>, size: Long): List<String> {
        require(flags.all { it in INSTALL_FLAGS }) { "Invalid install flags: $flags" }
        require(size > 0) { "Invalid APK size: $size" }
        return listOf("pm", "install") + flags + listOf("-S", size.toString())
    }
    fun listDisabledPackages() = listOf("pm", "list", "packages", "-d")
    fun trimCaches() = listOf("pm", "trim-caches", "999G")
    fun setDnd(mode: String) = listOf("cmd", "notification", "set_dnd", mode.also { require(it in DND_MODES) { "Invalid DND mode: $it" } })
    fun allowUsageStats(selfPkg: String) = listOf("appops", "set", pkg(selfPkg), "GET_USAGE_STATS", "allow")
    fun listUsers() = listOf("pm", "list", "users")
    fun switchUser(id: Int) = listOf("am", "switch-user", user(id))
    fun createUser(name: String) = listOf("pm", "create-user", name.also { require(USER_NAME.matches(it)) { "Invalid user name" } })
    fun installExisting(p: String, userId: Int) = listOf("pm", "install-existing", "--user", user(userId), pkg(p))

    fun installCreate(flags: List<String>): List<String> {
        require(flags.all { it in INSTALL_FLAGS }) { "Invalid install flags: $flags" }
        return listOf("pm", "install-create") + flags
    }

    fun installWrite(sessionId: String, name: String, size: Long): List<String> {
        require(sessionId.all { it.isDigit() }) { "Invalid session id: $sessionId" }
        require(size > 0) { "Invalid size: $size" }
        return listOf("pm", "install-write", "-S", size.toString(), sessionId, name, "-")
    }

    fun installCommit(sessionId: String): List<String> {
        require(sessionId.all { it.isDigit() }) { "Invalid session id: $sessionId" }
        return listOf("pm", "install-commit", sessionId)
    }

    fun installAbandon(sessionId: String): List<String> {
        require(sessionId.all { it.isDigit() }) { "Invalid session id: $sessionId" }
        return listOf("pm", "install-abandon", sessionId)
    }
}

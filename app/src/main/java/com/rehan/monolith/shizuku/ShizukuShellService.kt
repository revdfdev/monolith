package com.rehan.monolith.shizuku

import android.os.ParcelFileDescriptor
import android.os.Process
import com.rehan.monolith.IShizukuShellService
import com.rehan.monolith.OpResult
import java.io.InputStream
import kotlin.system.exitProcess
import com.rehan.monolith.shizuku.ShizukuCommands as C

class ShizukuShellService : IShizukuShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun getUid(): Int = Process.myUid()

    override fun setSuspended(pkgs: Array<String>, suspended: Boolean) = each(pkgs) { C.setSuspended(it, suspended) }
    override fun setEnabled(pkgs: Array<String>, enabled: Boolean) = each(pkgs) { C.setEnabled(it, enabled) }
    override fun forceStop(pkg: String) = run { C.forceStop(pkg) }
    override fun putSetting(namespace: String, key: String, value: String) = run { C.putSetting(namespace, key, value) }
    override fun getSetting(namespace: String, key: String) = run { C.getSetting(namespace, key) }
    override fun uninstall(pkg: String) = run { C.uninstall(pkg) }
    override fun install(apk: ParcelFileDescriptor, size: Long, flags: Array<String>) =
        ParcelFileDescriptor.AutoCloseInputStream(apk).use { input -> run(input) { C.install(flags.toList(), size) } }
    override fun listDisabledPackages() = run { C.listDisabledPackages() }
    override fun trimCaches() = run { C.trimCaches() }
    override fun setDnd(mode: String) = run { C.setDnd(mode) }
    override fun allowUsageStats(selfPkg: String) = run { C.allowUsageStats(selfPkg) }
    override fun listUsers() = run { C.listUsers() }
    override fun switchUser(userId: Int) = run { C.switchUser(userId) }
    override fun createUser(name: String) = run { C.createUser(name) }
    override fun installExisting(pkg: String, userId: Int) = run { C.installExisting(pkg, userId) }

    private fun each(pkgs: Array<String>, argv: (String) -> List<String>): OpResult {
        val failures = pkgs.map { it to run { argv(it) } }.filterNot { it.second.ok }
        return if (failures.isEmpty()) result(true, 0, "")
        else result(false, failures.first().second.code, failures.joinToString("\n") { "${it.first}: ${it.second.message}" })
    }

    private fun run(stdin: InputStream? = null, argv: () -> List<String>): OpResult = try {
        val process = ProcessBuilder(argv()).redirectErrorStream(true).start()
        if (stdin != null) process.outputStream.use { stdin.copyTo(it) } else process.outputStream.close()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trimEnd()
        val code = process.waitFor()
        result(code == 0, code, output)
    } catch (e: IllegalArgumentException) {
        result(false, -2, e.message ?: "Invalid argument")
    } catch (t: Throwable) {
        result(false, -1, t.message ?: t.javaClass.simpleName)
    }

    private fun result(ok: Boolean, code: Int, message: String) = OpResult().apply {
        this.ok = ok; this.code = code; this.message = message
    }
}

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

import android.os.Process
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class ShizukuShellService : IShizukuShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun getUid(): Int = Process.myUid()

    override fun exec(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readText()
            }
            val exitCode = process.waitFor()
            "EXIT:$exitCode\n$output"
        } catch (t: Throwable) {
            "EXIT:-1\n${t.message ?: t.javaClass.simpleName}"
        }
    }
}

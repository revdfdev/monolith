package com.rehan.monolith

import com.rehan.monolith.shizuku.ShizukuCommands as C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuCommandsTest {

    private fun rejects(block: () -> Unit) {
        val ok = runCatching(block).exceptionOrNull() is IllegalArgumentException
        assertTrue("expected IllegalArgumentException", ok)
    }

    @Test
    fun `valid package names pass`() {
        assertTrue(C.isValidPackage("com.example.app_2"))
        assertEquals(listOf("am", "force-stop", "com.a.b"), C.forceStop("com.a.b"))
    }

    @Test
    fun `GIVEN injection attempts as package names THEN every builder rejects them`() {
        listOf("a;rm -rf /", "\$(id)", "a b", "`id`", "a&&b", "", "a\nb", "a|b", "../x").forEach { bad ->
            assertFalse(bad, C.isValidPackage(bad))
            rejects { C.forceStop(bad) }
            rejects { C.uninstall(bad) }
            rejects { C.setEnabled(bad, true) }
            rejects { C.setSuspended(bad, true) }
            rejects { C.installExisting(bad, 0) }
        }
    }

    @Test
    fun `GIVEN a setting value with quotes and backticks THEN it is one literal argv element`() {
        val v = "it's \"quoted\" `id` \$(id); rm -rf /"
        assertEquals(listOf("settings", "put", "secure", "my_key", v), C.putSetting("secure", "my_key", v))
    }

    @Test
    fun `GIVEN bad namespace or key THEN rejected`() {
        rejects { C.putSetting("vendor", "k", "1") }
        rejects { C.getSetting("global", "k;id") }
        rejects { C.getSetting("global", "") }
        assertEquals(listOf("settings", "get", "global", "stay_on_while_plugged_in"), C.getSetting("global", "stay_on_while_plugged_in"))
    }

    @Test
    fun `suspend, enable and uninstall build argv for user 0`() {
        assertEquals(listOf("pm", "suspend", "--user", "0", "a.b"), C.setSuspended("a.b", true))
        assertEquals(listOf("pm", "unsuspend", "--user", "0", "a.b"), C.setSuspended("a.b", false))
        assertEquals(listOf("pm", "disable-user", "--user", "0", "a.b"), C.setEnabled("a.b", false))
        assertEquals(listOf("pm", "enable", "--user", "0", "a.b"), C.setEnabled("a.b", true))
        assertEquals(listOf("pm", "uninstall", "--user", "0", "a.b"), C.uninstall("a.b"))
    }

    @Test
    fun `install only accepts known flags and a positive size`() {
        assertEquals(listOf("pm", "install", "-r", "-g", "-S", "42"), C.install(listOf("-r", "-g"), 42))
        rejects { C.install(listOf("-r; id"), 42) }
        rejects { C.install(listOf("--user"), 42) }
        rejects { C.install(emptyList(), 0) }
    }

    @Test
    fun `dnd, users and create user are validated`() {
        assertEquals(listOf("cmd", "notification", "set_dnd", "alarms"), C.setDnd("alarms"))
        rejects { C.setDnd("on; id") }
        assertEquals(listOf("am", "switch-user", "10"), C.switchUser(10))
        rejects { C.switchUser(-1) }
        assertEquals(listOf("pm", "create-user", "Kids_1"), C.createUser("Kids_1"))
        rejects { C.createUser("x; id") }
        rejects { C.installExisting("a.b", -2) }
    }
}

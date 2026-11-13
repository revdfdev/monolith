package com.rehan.monolith

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rehan.monolith.data.LauncherPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParentPinTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("monolith_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @org.junit.After
    fun tearDown() = org.koin.core.context.stopKoin()

    private fun rawPrefs() = context.getSharedPreferences("monolith_prefs", Context.MODE_PRIVATE)

    @Test
    fun `GIVEN fresh install WHEN checking THEN no PIN exists and 1234 is rejected`() {
        val prefs = LauncherPrefs(context)
        assertFalse(prefs.hasParentPin())
        assertFalse(prefs.verifyParentPin("1234"))
    }

    @Test
    fun `GIVEN PIN set WHEN verifying THEN only the correct PIN passes`() {
        val prefs = LauncherPrefs(context)
        prefs.setParentPin("4821")
        assertTrue(prefs.hasParentPin())
        assertTrue(prefs.verifyParentPin("4821"))
        assertFalse(prefs.verifyParentPin("4822"))
        assertFalse(prefs.verifyParentPin(""))
    }

    @Test
    fun `GIVEN PIN set WHEN reading storage THEN the PIN is not stored in plain text`() {
        val prefs = LauncherPrefs(context)
        prefs.setParentPin("4821")
        prefs.flushForTest()
        val stored = java.io.File(context.filesDir, "datastore/monolith_settings.json").readText()
        assertTrue(stored.contains("parentPinHash"))
        assertFalse(stored.contains("4821"))
    }

    @Test
    fun `GIVEN same PIN set twice WHEN reading hash THEN salt makes hashes differ`() {
        val prefs = LauncherPrefs(context)
        prefs.setParentPin("4821")
        val first = prefs.settings.value.parentPinHash
        prefs.setParentPin("4821")
        val second = prefs.settings.value.parentPinHash
        assertNotEquals(first, second)
    }

    @Test
    fun `GIVEN PIN shorter than 4 digits WHEN setting THEN it is refused`() {
        val prefs = LauncherPrefs(context)
        assertFalse(prefs.setParentPin("12"))
        assertFalse(prefs.hasParentPin())
    }

    @Test
    fun `GIVEN legacy plain text PIN WHEN prefs load THEN it is migrated to a hash and removed`() {
        rawPrefs().edit().putString("parent_pin", "9070").commit()
        val prefs = LauncherPrefs(context)
        assertTrue(prefs.verifyParentPin("9070"))
        assertEquals(null, rawPrefs().getString("parent_pin", null))
    }
}

package com.rehan.monolith

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rehan.monolith.data.LauncherPrefs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() = stopKoin()

    @Test
    fun `GIVEN legacy SharedPreferences WHEN prefs load THEN every value is carried into the new store`() {
        context.getSharedPreferences("monolith_prefs", Context.MODE_PRIVATE).edit()
            .putString("pinned_packages", "a.b,c.d")
            .putBoolean("seeded_defaults_v1", true)
            .putStringSet("focus_distractions", setOf("x.y"))
            .putBoolean("focus_mode_active", true)
            .putBoolean("auto_launch_single", true)
            .putBoolean("show_frozen", false)
            .putStringSet("kids_apps", setOf("k.a"))
            .putBoolean("kids_mode_active", true)
            .putString("quick_note", "hi")
            .putString("home_clock_style", "LINE")
            .putBoolean("home_24h", false)
            .putString("home_amoled_accent", "AMBER")
            .putBoolean("home_show_stopwatch", false)
            .putString("center_widgets_v2", "STOPWATCH,CUSTOM_1")
            .putString("left_dock_pkg", "l.p")
            .putString("right_dock_pkg", "r.p")
            .putString("system_widget_ids", "7,9")
            .putString("custom_widgets_v1", "CUSTOM_1||Up||uptime")
            .putString("app_daily_limits", "a.b=30,c.d=15")
            .putStringSet("mindful_pause_apps", setOf("m.p"))
            .putBoolean("ambient_display_enabled", false)
            .commit()

        val p = LauncherPrefs(context)

        assertEquals(listOf("a.b", "c.d"), p.getPinnedPackages())
        assertTrue(p.hasSeededDefaults())
        assertEquals(setOf("x.y"), p.getFocusDistractions())
        assertTrue(p.isFocusModeActive())
        assertTrue(p.isAutoLaunchSingleMatch())
        assertFalse(p.isShowFrozenInDrawer())
        assertEquals(setOf("k.a"), p.getKidsApps())
        assertTrue(p.isKidsModeActive())
        assertEquals("hi", p.getQuickNote())
        assertEquals("LINE", p.getHomeConfig().clockStyle)
        assertFalse(p.getHomeConfig().is24Hour)
        assertEquals("AMBER", p.getHomeConfig().amoledAccent)
        assertFalse(p.getHomeConfig().showStopwatch)
        assertTrue(p.getHomeConfig().showClock)
        assertEquals(listOf("STOPWATCH"), p.getCenterWidgets())
        assertEquals("l.p", p.getLeftDockPkg())
        assertEquals("r.p", p.getRightDockPkg())
        assertEquals(listOf(7, 9), p.getSystemWidgetIds())
        assertEquals(mapOf("a.b" to 30, "c.d" to 15), p.getDailyLimits())
        assertEquals(setOf("m.p"), p.getMindfulPauseApps())
        assertFalse(p.isAmbientDisplayEnabled())
    }

    @Test
    fun `GIVEN fresh install WHEN prefs load THEN defaults match the old defaults`() {
        val p = LauncherPrefs(context)
        assertEquals(emptyList<String>(), p.getPinnedPackages())
        assertTrue(p.isShowFrozenInDrawer())
        assertEquals(listOf("FOCUS_5217", "STOPWATCH", "ALARM_USAGE", "APPS_GRID"), p.getCenterWidgets())
        assertEquals("STACKED", p.getHomeConfig().clockStyle)
        assertTrue(p.isAmbientDisplayEnabled())
    }

    @Test
    fun `GIVEN migration ran WHEN checking disk THEN legacy SharedPreferences file is removed`() {
        context.getSharedPreferences("monolith_prefs", Context.MODE_PRIVATE).edit().putString("quick_note", "x").commit()
        LauncherPrefs(context)
        val legacy = File(context.dataDir, "shared_prefs/monolith_prefs.xml")
        assertFalse(legacy.exists())
    }

    @Test
    fun `GIVEN values changed WHEN a new instance reads the store THEN changes persisted`() {
        val first = LauncherPrefs(context)
        first.setDailyLimit("a.b", 20)
        first.togglePinned("p.q")
        first.flushForTest()
        val raw = File(context.filesDir, "datastore/monolith_settings.json").readText()
        assertTrue(raw.contains("p.q"))
        assertTrue(raw.contains("a.b"))
    }
}

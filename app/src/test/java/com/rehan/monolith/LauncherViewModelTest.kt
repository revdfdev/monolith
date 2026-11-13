package com.rehan.monolith

import com.rehan.monolith.data.LauncherPrefs
import com.rehan.monolith.ui.home.LauncherViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = MonolithApp::class)
class LauncherViewModelTest {

    @After
    fun tearDown() = stopKoin()

    private val koin get() = GlobalContext.get()

    @Test
    fun `GIVEN view model WHEN prefs change THEN settings state reflects them immediately`() {
        val vm = koin.get<LauncherViewModel>()
        val prefs = koin.get<LauncherPrefs>()

        prefs.togglePinned("a.app")
        prefs.setShowFrozenInDrawer(false)
        prefs.setLeftDockPkg("d.app")

        assertEquals(listOf("a.app"), vm.settings.value.pinnedPackages)
        assertFalse(vm.settings.value.showFrozenInDrawer)
        assertEquals("d.app", vm.settings.value.leftDockPkg)
    }

    @Test
    fun `GIVEN view model WHEN parent PIN is set THEN state reports a PIN without exposing it`() {
        val vm = koin.get<LauncherViewModel>()
        assertFalse(vm.settings.value.parentPinHash != null)
        koin.get<LauncherPrefs>().setParentPin("5531")
        assertTrue(vm.settings.value.parentPinHash != null)
        assertFalse(vm.settings.value.parentPinHash!!.contains("5531"))
    }

    @Test
    fun `GIVEN drawer switched to list WHEN prefs are reloaded THEN list view is remembered`() {
        val prefs = koin.get<LauncherPrefs>()
        assertTrue(prefs.settings.value.drawerGrid)
        prefs.setDrawerGrid(false)
        prefs.flushForTest()
        val file = java.io.File(org.robolectric.RuntimeEnvironment.getApplication().filesDir, "datastore/monolith_settings.json")
        assertTrue(file.readText(), file.readText().contains("\"drawerGrid\":false"))
    }
}

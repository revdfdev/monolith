package com.rehan.monolith

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.rehan.monolith.data.AppEntry
import com.rehan.monolith.data.HomeConfig
import com.rehan.monolith.shizuku.ShizukuState
import com.rehan.monolith.ui.home.HomeLauncherPage
import com.rehan.monolith.ui.home.PulleyMenuItem
import com.rehan.monolith.ui.home.PulleyMenuState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeLauncherUiTest {

    @get:Rule
    val rule = createComposeRule()

    @After
    fun tearDown() = stopKoin()

    private val testApps = listOf(
        AppEntry("pkg.alpha", "Alpha", false, false, null),
        AppEntry("pkg.beta", "Beta", false, false, null),
        AppEntry("pkg.gamma", "Gamma", false, false, null),
        AppEntry("pkg.omega", "Omega", false, false, null)
    )

    @Test
    fun `GIVEN pulley menu state WHEN dragged past thresholds THEN active index updates correctly`() {
        val density = Density(density = 2f)
        val state = PulleyMenuState(density = density, itemCount = 4)

        assertEquals(-1, state.activeIndex)

        // Below minimum threshold (< 45dp)
        state.onDragDelta(delta = 40f, haptics = null)
        assertEquals(-1, state.activeIndex)

        // Drag further to reach bottom item (index 3: CUSTOMIZE)
        // targetPx = (rawDragY * 0.5f). For offsetDp = 60dp -> targetPx = 120px -> delta = 200px
        state.onDragDelta(delta = 200f, haptics = null)
        assertEquals(3, state.activeIndex)

        // Drag further to reach next item (index 2: FOCUS)
        state.onDragDelta(delta = 160f, haptics = null)
        assertEquals(2, state.activeIndex)

        // Drag further to reach next item (index 1: SHELF)
        state.onDragDelta(delta = 180f, haptics = null)
        assertEquals(1, state.activeIndex)

        // Drag further to reach topmost item (index 0: WIDGETS)
        state.onDragDelta(delta = 200f, haptics = null)
        assertEquals(0, state.activeIndex)
    }

    @Test
    fun `GIVEN pulley menu state WHEN released on an active item THEN action is executed and state resets`() = runBlocking {
        val density = Density(density = 2f)
        val state = PulleyMenuState(density = density, itemCount = 4)
        var actionExecuted = -1

        // Drag to select index 3
        state.onDragDelta(delta = 240f, haptics = null)
        assertEquals(3, state.activeIndex)

        state.release(haptics = null) { chosenIdx ->
            actionExecuted = chosenIdx
        }

        assertEquals(3, actionExecuted)
        assertEquals(-1, state.activeIndex)
        assertEquals(0f, state.currentOffsetPx)
    }

    @Test
    fun `GIVEN home screen WHEN displayed THEN calm header is visible and scrubber is absent`() {
        var openedDrawerAt: Char? = null

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val appWidgetHost = android.appwidget.AppWidgetHost(context, 1024)

        rule.setContent {
            HomeLauncherPage(
                shizukuStatus = ShizukuState.Ready(uid = 0),
                apps = testApps,
                pinnedPackages = emptyList(),
                focusActive = false,
                grayscaleOn = false,
                quickNote = "",
                homeConfig = HomeConfig(),
                centerWidgets = emptyList(),
                systemWidgetIds = emptyList(),
                appWidgetManager = appWidgetManager,
                appWidgetHost = appWidgetHost,
                leftDockPkg = null,
                rightDockPkg = null,
                onRequestShizuku = {},
                onOpenShizukuShelf = {},
                onOpenCustomizer = {},
                onOpenWidgetManager = {},
                onOpenAppPicker = {},
                onPickLeftDock = {},
                onPickRightDock = {},
                onRemoveSystemWidget = {},
                onTrimCaches = {},
                onOpenDrawer = {},
                onOpenDrawerAt = { openedDrawerAt = it },
                onKidsMode = {},
                onToggleFocus = {},
                onToggleGrayscale = {},
                onEditNote = {},
                onGrantUsageAccess = {},
                onLaunchApp = {},
                onLongPressApp = {},
                onOpenDialer = {},
                onOpenCamera = {}
            )
        }

        // Calm minimalist header
        rule.onNodeWithTag("home_status").assertIsDisplayed()
        rule.onNodeWithText("MONOLITH").assertIsDisplayed()
        rule.onNodeWithTag("home_pulley_hint", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("PULL DOWN").assertIsDisplayed()

        // Scrubber is absent in pure Sailfish Silica edge-to-edge layout
        rule.onNodeWithTag("home_scrubber").assertDoesNotExist()
    }
}

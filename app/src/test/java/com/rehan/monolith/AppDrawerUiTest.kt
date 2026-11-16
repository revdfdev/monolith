package com.rehan.monolith

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.rehan.monolith.data.AppEntry
import com.rehan.monolith.ui.drawer.AppDrawerOverlay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.After
import org.junit.Assert.assertEquals
import org.koin.core.context.stopKoin
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDrawerUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val apps = ('A'..'Z').flatMap { c ->
        (1..3).map { n -> AppEntry("p.${c.lowercase()}$n", "$c-app $n", isFrozen = false, isSystem = false, icon = null) }
    } + AppEntry("p.paused", "Paused One", isFrozen = true, isSystem = false, icon = null)

    @After
    fun tearDown() = stopKoin()

    private val launched = mutableListOf<String>()
    private var closed = 0

    private fun show(showPaused: Boolean = true) = rule.setContent {
        var grid by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
        AppDrawerOverlay(
            apps = apps, showFrozen = showPaused, gridView = grid, onToggleGridView = { grid = !grid }, autoLaunchSingle = false, grayscaleOn = false,
            pinnedPackages = emptySet(), focusDistractions = emptySet(), kidsApps = emptySet(),
            onClose = { closed++ }, onToggleShowFrozen = {}, onToggleAutoLaunch = {},
            onLaunchApp = { launched += it.packageName }, onLongPressApp = {}
        )
    }

    @Test
    fun `GIVEN drawer WHEN searching and pressing Go THEN the top match launches`() {
        show()
        rule.onNodeWithTag("drawer_search").performTextInput("m-app")
        rule.onNodeWithTag("drawer_search").performImeAction()
        assertEquals(listOf("p.m1"), launched)
    }

    @Test
    fun `GIVEN drawer WHEN rendered THEN edge scrubber is absent`() {
        show()
        rule.onNodeWithTag("drawer_scrubber").assertDoesNotExist()
        rule.onNodeWithTag("scrubber_Z").assertDoesNotExist()
    }

    @Test
    fun `GIVEN list view WHEN switched to list THEN letter headers and apps are shown`() {
        show()
        rule.onNodeWithTag("drawer_menu").performClick()
        rule.onNodeWithText("Show as list").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("header_A").assertIsDisplayed()
        rule.onNodeWithTag("app_p.a1").assertIsDisplayed()
    }

    @Test
    fun `GIVEN grid view THEN no letter headers are drawn`() {
        show()
        rule.onNodeWithTag("header_A").assertDoesNotExist()
    }

    @Test
    fun `GIVEN empty search WHEN tapping close THEN the drawer closes`() {
        show()
        rule.onNodeWithContentDescription("Close drawer").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun `GIVEN a paused app WHEN shown THEN it is listed with a paused badge, and Go skips it`() {
        show()
        rule.onNodeWithTag("drawer_search").performTextInput("paused")
        rule.onNodeWithTag("app_p.paused").assertIsDisplayed()
        rule.onNodeWithContentDescription("Paused").assertIsDisplayed()
        rule.onNodeWithTag("drawer_search").performImeAction()
        assertTrue(launched.isEmpty())
    }

    @Test
    fun `GIVEN drawer WHEN opening the menu THEN the old toggle pills are options there`() {
        show()
        rule.onNodeWithTag("drawer_menu").performClick()
        rule.onNodeWithText("Show paused apps").assertIsDisplayed()
        rule.onNodeWithText("Open single search match").assertIsDisplayed()
        rule.onNodeWithText("Show as list").assertIsDisplayed()
    }
}

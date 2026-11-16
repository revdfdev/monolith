package com.rehan.monolith

import com.rehan.monolith.data.AppEntry
import com.rehan.monolith.ui.drawer.buildDrawerSections
import com.rehan.monolith.ui.drawer.searchApps
import com.rehan.monolith.ui.drawer.sectionLetter
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawerListTest {

    private fun app(label: String, pkg: String = "p.${label.lowercase().filter { it.isLetterOrDigit() }}", paused: Boolean = false) =
        AppEntry(pkg, label, isFrozen = paused, isSystem = false, icon = null)

    @Test
    fun `section letters are uppercase A-Z and everything else is hash`() {
        assertEquals('B', sectionLetter("brave"))
        assertEquals('#', sectionLetter("1Password"))
        assertEquals('#', sectionLetter(""))
        assertEquals('#', sectionLetter("Équipe"))
    }

    @Test
    fun `GIVEN unsorted apps THEN sections are A-Z then hash, apps sorted case-insensitively`() {
        val s = buildDrawerSections(listOf(app("chess"), app("Brave"), app("2048"), app("Calculator"), app("brave Beta")), showPaused = true)
        assertEquals(listOf('B', 'C', '#'), s.map { it.letter })
        assertEquals(listOf("Brave", "brave Beta"), s[0].apps.map { it.label })
        assertEquals(listOf("Calculator", "chess"), s[1].apps.map { it.label })
        assertEquals(listOf("2048"), s[2].apps.map { it.label })
    }

    @Test
    fun `GIVEN paused apps hidden THEN they are left out, shown THEN kept`() {
        val apps = listOf(app("Alpha"), app("Beta", paused = true))
        assertEquals(listOf("Alpha"), buildDrawerSections(apps, showPaused = false).flatMap { it.apps }.map { it.label })
        assertEquals(listOf("Alpha", "Beta"), buildDrawerSections(apps, showPaused = true).flatMap { it.apps }.map { it.label })
    }

    @Test
    fun `GIVEN a query THEN prefix matches come before word and substring matches`() {
        val apps = listOf(app("Google Maps"), app("Maps Lite"), app("Bitmaps"), app("Chess"))
        assertEquals(listOf("Maps Lite", "Google Maps", "Bitmaps"), searchApps(apps, "map", showPaused = true).map { it.label })
    }

    @Test
    fun `GIVEN a query THEN package name matches are included last and blank query returns nothing`() {
        val apps = listOf(app("Camera", pkg = "com.oplus.camera"), app("Snap", pkg = "com.snapchat.android"))
        assertEquals(listOf("Camera"), searchApps(apps, "oplus", showPaused = true).map { it.label })
        assertEquals(emptyList<AppEntry>(), searchApps(apps, "  ", showPaused = true))
    }

    @Test
    fun `GIVEN paused apps hidden WHEN searching THEN paused apps are not returned`() {
        val apps = listOf(app("Chess", paused = true), app("Chrome"))
        assertEquals(listOf("Chrome"), searchApps(apps, "ch", showPaused = false).map { it.label })
    }
}

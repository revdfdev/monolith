package com.rehan.monolith.ui.drawer

import com.rehan.monolith.data.AppEntry

data class DrawerSection(val letter: Char, val apps: List<AppEntry>)

fun sectionLetter(label: String): Char =
    label.trim().firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'

private val byLabel = compareBy<AppEntry>({ it.label.lowercase() }, { it.packageName })

fun buildDrawerSections(apps: List<AppEntry>, showPaused: Boolean): List<DrawerSection> =
    apps.asSequence()
        .filter { showPaused || !it.isFrozen }
        .sortedWith(byLabel)
        .groupBy { sectionLetter(it.label) }
        .toSortedMap(compareBy { if (it == '#') ' ' else it })
        .map { (letter, list) -> DrawerSection(letter, list) }

/** Prefix match first, then a word inside the label, then any substring, then package name. */
fun searchApps(apps: List<AppEntry>, query: String, showPaused: Boolean): List<AppEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    fun rank(app: AppEntry): Int {
        val label = app.label.lowercase()
        return when {
            label.startsWith(q) -> 0
            label.split(' ', '-', '_', '.').any { it.startsWith(q) } -> 1
            label.contains(q) -> 2
            app.packageName.lowercase().contains(q) -> 3
            else -> -1
        }
    }
    return apps.asSequence()
        .filter { showPaused || !it.isFrozen }
        .map { it to rank(it) }
        .filter { it.second >= 0 }
        .sortedWith(compareBy<Pair<AppEntry, Int>> { it.second }.thenBy { it.first.label.lowercase() })
        .map { it.first }
        .toList()
}

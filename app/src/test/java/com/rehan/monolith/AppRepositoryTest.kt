package com.rehan.monolith

import com.rehan.monolith.data.AppEntry
import com.rehan.monolith.data.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppRepositoryTest {

    private fun entry(pkg: String, frozen: Boolean = false) = AppEntry(pkg, pkg, frozen, false, null)

    @Test
    fun `GIVEN loaded list WHEN a package is installed THEN the change refreshes the list`() = runTest {
        val installed = mutableListOf(entry("a.app"))
        val repo = AppRepository(
            scope = TestScope(testScheduler),
            dispatcher = StandardTestDispatcher(testScheduler),
            frozenPackages = { emptySet() },
            loader = { _, emit -> emit(installed.toList()) }
        )
        repo.refresh(); advanceUntilIdle()
        assertEquals(listOf("a.app"), repo.apps.value.map { it.packageName })

        installed += entry("b.app")
        repo.onPackagesChanged(); advanceUntilIdle()
        assertEquals(listOf("a.app", "b.app"), repo.apps.value.map { it.packageName })
    }

    @Test
    fun `GIVEN loaded list WHEN a package is removed THEN it disappears`() = runTest {
        val installed = mutableListOf(entry("a.app"), entry("b.app"))
        val repo = AppRepository(TestScope(testScheduler), StandardTestDispatcher(testScheduler), { emptySet() }) { _, emit -> emit(installed.toList()) }
        repo.refresh(); advanceUntilIdle()
        installed.removeAt(0)
        repo.onPackagesChanged(); advanceUntilIdle()
        assertEquals(listOf("b.app"), repo.apps.value.map { it.packageName })
    }

    @Test
    fun `GIVEN burst of package events WHEN debounced THEN loader runs once`() = runTest {
        var loads = 0
        val repo = AppRepository(TestScope(testScheduler), StandardTestDispatcher(testScheduler), { emptySet() }) { _, emit -> loads++; emit(emptyList()) }
        repeat(5) { repo.onPackagesChanged() }
        advanceUntilIdle()
        assertEquals(1, loads)
    }

    @Test
    fun `GIVEN frozen set WHEN refreshing THEN it is passed to the loader`() = runTest {
        var seen: Set<String>? = null
        val repo = AppRepository(TestScope(testScheduler), StandardTestDispatcher(testScheduler), { setOf("f.app") }) { frozen, emit -> seen = frozen; emit(emptyList()) }
        repo.refresh(); advanceUntilIdle()
        assertEquals(setOf("f.app"), seen)
    }
}

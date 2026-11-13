package com.rehan.monolith.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Synchronous facade over the DataStore-backed [MonolithSettings]. Reads come from an in-memory
 * snapshot loaded once at startup; writes update the snapshot immediately and persist in order.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LauncherPrefs(context: Context) {
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val store = DataStoreFactory.create(
        serializer = MonolithSettingsSerializer,
        migrations = listOf(LegacyPrefsMigration(context.applicationContext)),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.applicationContext.dataStoreFile("monolith_settings.json") }
    )
    private val state = MutableStateFlow(runBlocking { store.data.first() })
    val settings: StateFlow<MonolithSettings> = state.asStateFlow()

    private fun edit(transform: (MonolithSettings) -> MonolithSettings) {
        state.update(transform)
        writeScope.launch { store.updateData { state.value } }
    }

    internal fun flushForTest() = runBlocking { writeScope.launch { store.updateData { state.value } }.join() }

    private val s get() = state.value

    private fun <T> Set<T>.toggle(item: T): Pair<Set<T>, Boolean> =
        if (item in this) (this - item) to false else (this + item) to true

    fun getPinnedPackages(): List<String> = s.pinnedPackages
    fun setPinnedPackages(packages: List<String>) = edit { it.copy(pinnedPackages = packages.distinct()) }

    fun hasSeededDefaults(): Boolean = s.seededDefaults
    fun markDefaultsSeeded() = edit { it.copy(seededDefaults = true) }

    fun togglePinned(pkg: String): Boolean {
        val added = pkg !in s.pinnedPackages
        setPinnedPackages(if (added) s.pinnedPackages + pkg else s.pinnedPackages - pkg)
        return added
    }

    fun getFocusDistractions(): Set<String> = s.focusDistractions
    fun toggleFocusDistraction(pkg: String): Boolean {
        val (next, added) = s.focusDistractions.toggle(pkg)
        edit { it.copy(focusDistractions = next) }
        return added
    }

    fun isFocusModeActive(): Boolean = s.focusModeActive
    fun setFocusModeActive(active: Boolean) = edit { it.copy(focusModeActive = active) }

    fun isAutoLaunchSingleMatch(): Boolean = s.autoLaunchSingleMatch
    fun setAutoLaunchSingleMatch(enabled: Boolean) = edit { it.copy(autoLaunchSingleMatch = enabled) }

    fun isShowFrozenInDrawer(): Boolean = s.showFrozenInDrawer
    fun setShowFrozenInDrawer(show: Boolean) = edit { it.copy(showFrozenInDrawer = show) }

    fun getKidsApps(): Set<String> = s.kidsApps
    fun toggleKidsApp(pkg: String): Boolean {
        val (next, added) = s.kidsApps.toggle(pkg)
        edit { it.copy(kidsApps = next) }
        return added
    }

    fun isKidsModeActive(): Boolean = s.kidsModeActive
    fun setKidsModeActive(active: Boolean) = edit { it.copy(kidsModeActive = active) }

    fun hasParentPin(): Boolean = s.parentPinHash != null

    fun setParentPin(pin: String): Boolean {
        if (!PinHasher.isValid(pin)) return false
        val salt = PinHasher.newSalt()
        edit {
            it.copy(
                parentPinSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
                parentPinHash = PinHasher.hash(pin, salt)
            )
        }
        return true
    }

    fun verifyParentPin(pin: String): Boolean {
        val hash = s.parentPinHash ?: return false
        val salt = s.parentPinSalt ?: return false
        val candidate = PinHasher.hash(pin, Base64.decode(salt, Base64.NO_WRAP))
        return java.security.MessageDigest.isEqual(hash.toByteArray(), candidate.toByteArray())
    }

    fun getQuickNote(): String = s.quickNote
    fun setQuickNote(note: String) = edit { it.copy(quickNote = note) }

    fun getHomeConfig(): HomeConfig = s.home
    fun saveHomeConfig(cfg: HomeConfig) = edit { it.copy(home = cfg) }

    fun getCenterWidgets(): List<String> = s.centerWidgets
    fun saveCenterWidgets(widgets: List<String>) = edit { it.copy(centerWidgets = widgets.distinct()) }

    fun getLeftDockPkg(): String? = s.leftDockPkg
    fun setLeftDockPkg(pkg: String?) = edit { it.copy(leftDockPkg = pkg) }

    fun getRightDockPkg(): String? = s.rightDockPkg
    fun setRightDockPkg(pkg: String?) = edit { it.copy(rightDockPkg = pkg) }

    fun getSystemWidgetIds(): List<Int> = s.systemWidgetIds
    fun addSystemWidgetId(id: Int) = edit { it.copy(systemWidgetIds = (it.systemWidgetIds + id).distinct()) }
    fun removeSystemWidgetId(id: Int) = edit { it.copy(systemWidgetIds = it.systemWidgetIds - id) }

    fun getCustomWidgets(): List<CustomWidgetSpec> = s.customWidgets

    fun addCustomWidget(title: String, command: String): CustomWidgetSpec {
        val spec = CustomWidgetSpec("CUSTOM_${System.currentTimeMillis()}", title.trim(), command.trim())
        edit { it.copy(customWidgets = it.customWidgets + spec, centerWidgets = (it.centerWidgets + spec.id).distinct()) }
        return spec
    }

    fun removeCustomWidget(id: String) = edit {
        it.copy(customWidgets = it.customWidgets.filterNot { w -> w.id == id }, centerWidgets = it.centerWidgets - id)
    }

    fun getDailyLimits(): Map<String, Int> = s.dailyLimits
    fun setDailyLimit(pkg: String, minutes: Int) = edit {
        it.copy(dailyLimits = if (minutes <= 0) it.dailyLimits - pkg else it.dailyLimits + (pkg to minutes))
    }

    fun getMindfulPauseApps(): Set<String> = s.mindfulPauseApps
    fun toggleMindfulPause(pkg: String): Boolean {
        val (next, added) = s.mindfulPauseApps.toggle(pkg)
        edit { it.copy(mindfulPauseApps = next) }
        return added
    }

    fun getLockscreenMessage(): String = s.lockscreenMessage
    fun setLockscreenMessage(msg: String) = edit { it.copy(lockscreenMessage = msg) }

    fun isLockscreenMessageEnabled(): Boolean = s.lockscreenMessageEnabled
    fun setLockscreenMessageEnabled(enabled: Boolean) = edit { it.copy(lockscreenMessageEnabled = enabled) }

    fun isAmbientDisplayEnabled(): Boolean = s.ambientDisplayEnabled
    fun setAmbientDisplayEnabled(enabled: Boolean) = edit { it.copy(ambientDisplayEnabled = enabled) }
}

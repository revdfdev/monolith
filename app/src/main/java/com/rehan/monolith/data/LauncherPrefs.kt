package com.rehan.monolith.data

import com.rehan.monolith.*
import com.rehan.monolith.shizuku.*
import com.rehan.monolith.ui.common.*
import com.rehan.monolith.ui.home.*
import com.rehan.monolith.ui.widgets.*
import com.rehan.monolith.ui.drawer.*
import com.rehan.monolith.ui.shelf.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.install.*
import com.rehan.monolith.ui.wellbeing.*

import android.content.Context
import android.util.Base64

data class HomeConfig(
    val clockStyle: String = "STACKED",
    val is24Hour: Boolean = true,
    val batteryStyle: String = "RING",
    val alignCenter: Boolean = false,
    val showClock: Boolean = true,
    val showBattery: Boolean = true,
    val showModes: Boolean = true,
    val showNote: Boolean = false,
    val showHomeIcons: Boolean = true,
    val showTimer: Boolean = true,
    val showUsageAlarm: Boolean = true,
    val showStopwatch: Boolean = true,
    val showQuickActions: Boolean = true,
    val pinnedLayout: String = "GRID",
    val ambienceStyle: String = "SAILFISH",
    val tileStyle: String = "GLASS",
    val blurBackground: Boolean = true,
    val colorMode: String = "DYNAMIC",
    val amoledAccent: String = "WHITE"
)

data class CustomWidgetSpec(
    val id: String,
    val title: String,
    val command: String
)

class LauncherPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("monolith_prefs", Context.MODE_PRIVATE)

    init {
        prefs.getString("parent_pin", null)?.let { legacy ->
            if (!setParentPin(legacy)) prefs.edit().remove("parent_pin").commit()
        }
    }

    fun getPinnedPackages(): List<String> {
        val raw = prefs.getString("pinned_packages", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    fun setPinnedPackages(packages: List<String>) {
        prefs.edit().putString("pinned_packages", packages.distinct().joinToString(",")).apply()
    }

    fun hasSeededDefaults(): Boolean = prefs.getBoolean("seeded_defaults_v1", false)

    fun markDefaultsSeeded() {
        prefs.edit().putBoolean("seeded_defaults_v1", true).apply()
    }

    fun togglePinned(pkg: String): Boolean {
        val current = getPinnedPackages().toMutableList()
        val added = if (current.contains(pkg)) {
            current.remove(pkg)
            false
        } else {
            current.add(pkg)
            true
        }
        setPinnedPackages(current)
        return added
    }

    fun getFocusDistractions(): Set<String> {
        return prefs.getStringSet("focus_distractions", emptySet())?.toSet() ?: emptySet()
    }

    fun toggleFocusDistraction(pkg: String): Boolean {
        val current = getFocusDistractions().toMutableSet()
        val added = if (current.contains(pkg)) {
            current.remove(pkg)
            false
        } else {
            current.add(pkg)
            true
        }
        prefs.edit().putStringSet("focus_distractions", current).apply()
        return added
    }

    fun isFocusModeActive(): Boolean = prefs.getBoolean("focus_mode_active", false)

    fun setFocusModeActive(active: Boolean) {
        prefs.edit().putBoolean("focus_mode_active", active).apply()
    }

    fun isAutoLaunchSingleMatch(): Boolean = prefs.getBoolean("auto_launch_single", false)

    fun setAutoLaunchSingleMatch(enabled: Boolean) {
        prefs.edit().putBoolean("auto_launch_single", enabled).apply()
    }

    fun isShowFrozenInDrawer(): Boolean = prefs.getBoolean("show_frozen", true)

    fun setShowFrozenInDrawer(show: Boolean) {
        prefs.edit().putBoolean("show_frozen", show).apply()
    }

    fun getKidsApps(): Set<String> {
        return prefs.getStringSet("kids_apps", emptySet())?.toSet() ?: emptySet()
    }

    fun toggleKidsApp(pkg: String): Boolean {
        val current = getKidsApps().toMutableSet()
        val added = if (current.contains(pkg)) {
            current.remove(pkg)
            false
        } else {
            current.add(pkg)
            true
        }
        prefs.edit().putStringSet("kids_apps", current).apply()
        return added
    }

    fun isKidsModeActive(): Boolean = prefs.getBoolean("kids_mode_active", false)

    fun setKidsModeActive(active: Boolean) {
        prefs.edit().putBoolean("kids_mode_active", active).apply()
    }

    fun hasParentPin(): Boolean = prefs.getString("parent_pin_hash", null) != null

    fun setParentPin(pin: String): Boolean {
        if (pin.length < 4 || !pin.all { it.isDigit() }) return false
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString("parent_pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("parent_pin_hash", hashPin(pin, salt))
            .remove("parent_pin")
            .commit()
        return true
    }

    fun verifyParentPin(pin: String): Boolean {
        val hash = prefs.getString("parent_pin_hash", null) ?: return false
        val salt = prefs.getString("parent_pin_salt", null) ?: return false
        val candidate = hashPin(pin, Base64.decode(salt, Base64.NO_WRAP))
        return java.security.MessageDigest.isEqual(hash.toByteArray(), candidate.toByteArray())
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        val key = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }

    fun getQuickNote(): String = prefs.getString("quick_note", "") ?: ""

    fun setQuickNote(note: String) {
        prefs.edit().putString("quick_note", note).apply()
    }

    fun getHomeConfig(): HomeConfig {
        return HomeConfig(
            clockStyle = prefs.getString("home_clock_style", "STACKED") ?: "STACKED",
            is24Hour = prefs.getBoolean("home_24h", true),
            batteryStyle = prefs.getString("home_battery_style", "RING") ?: "RING",
            alignCenter = prefs.getBoolean("home_align_center", false),
            showClock = prefs.getBoolean("home_show_clock", true),
            showBattery = prefs.getBoolean("home_show_battery", true),
            showModes = prefs.getBoolean("home_show_modes", true),
            showNote = prefs.getBoolean("home_show_note_v2", false),
            showHomeIcons = prefs.getBoolean("home_show_icons", true),
            showTimer = prefs.getBoolean("home_show_timer", true),
            showUsageAlarm = prefs.getBoolean("home_show_usage_alarm", true),
            showStopwatch = prefs.getBoolean("home_show_stopwatch", true),
            showQuickActions = prefs.getBoolean("home_show_quick_actions", true),
            pinnedLayout = prefs.getString("home_pinned_layout", "GRID") ?: "GRID",
            ambienceStyle = prefs.getString("home_ambience_style", "SAILFISH") ?: "SAILFISH",
            tileStyle = prefs.getString("home_tile_style", "GLASS") ?: "GLASS",
            blurBackground = prefs.getBoolean("home_blur_bg", true),
            colorMode = prefs.getString("home_color_mode", "DYNAMIC") ?: "DYNAMIC",
            amoledAccent = prefs.getString("home_amoled_accent", "WHITE") ?: "WHITE"
        )
    }

    fun saveHomeConfig(cfg: HomeConfig) {
        prefs.edit()
            .putString("home_clock_style", cfg.clockStyle)
            .putBoolean("home_24h", cfg.is24Hour)
            .putString("home_battery_style", cfg.batteryStyle)
            .putBoolean("home_align_center", cfg.alignCenter)
            .putBoolean("home_show_clock", cfg.showClock)
            .putBoolean("home_show_battery", cfg.showBattery)
            .putBoolean("home_show_modes", cfg.showModes)
            .putBoolean("home_show_note_v2", cfg.showNote)
            .putBoolean("home_show_icons", cfg.showHomeIcons)
            .putBoolean("home_show_timer", cfg.showTimer)
            .putBoolean("home_show_usage_alarm", cfg.showUsageAlarm)
            .putBoolean("home_show_stopwatch", cfg.showStopwatch)
            .putBoolean("home_show_quick_actions", cfg.showQuickActions)
            .putString("home_pinned_layout", cfg.pinnedLayout)
            .putString("home_ambience_style", cfg.ambienceStyle)
            .putString("home_tile_style", cfg.tileStyle)
            .putBoolean("home_blur_bg", cfg.blurBackground)
            .putString("home_color_mode", cfg.colorMode)
            .putString("home_amoled_accent", cfg.amoledAccent)
            .apply()
    }

    fun getCenterWidgets(): List<String> {
        val raw = prefs.getString("center_widgets_v2", null)
            ?: return listOf("FOCUS_5217", "STOPWATCH", "ALARM_USAGE", "APPS_GRID")
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun saveCenterWidgets(widgets: List<String>) {
        prefs.edit().putString("center_widgets_v2", widgets.distinct().joinToString(",")).apply()
    }

    fun getLeftDockPkg(): String? = prefs.getString("left_dock_pkg", null)
    fun setLeftDockPkg(pkg: String?) {
        prefs.edit().putString("left_dock_pkg", pkg).apply()
    }

    fun getRightDockPkg(): String? = prefs.getString("right_dock_pkg", null)
    fun setRightDockPkg(pkg: String?) {
        prefs.edit().putString("right_dock_pkg", pkg).apply()
    }

    fun getSystemWidgetIds(): List<Int> {
        val raw = prefs.getString("system_widget_ids", "") ?: ""
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    fun addSystemWidgetId(id: Int) {
        val updated = (getSystemWidgetIds() + id).distinct()
        prefs.edit().putString("system_widget_ids", updated.joinToString(",")).apply()
    }

    fun removeSystemWidgetId(id: Int) {
        val updated = getSystemWidgetIds().filterNot { it == id }
        prefs.edit().putString("system_widget_ids", updated.joinToString(",")).apply()
    }

    fun getCustomWidgets(): List<CustomWidgetSpec> {
        val raw = prefs.getString("custom_widgets_v1", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";;").mapNotNull { entry ->
            val parts = entry.split("||")
            if (parts.size >= 3) CustomWidgetSpec(parts[0], parts[1], parts[2]) else null
        }
    }

    fun addCustomWidget(title: String, command: String): CustomWidgetSpec {
        val id = "CUSTOM_${System.currentTimeMillis()}"
        val safeTitle = title.replace("||", " ").replace(";;", " ").trim()
        val safeCmd = command.replace("||", " ").replace(";;", " ").trim()
        val spec = CustomWidgetSpec(id, safeTitle, safeCmd)
        val updated = getCustomWidgets() + spec
        saveCustomWidgets(updated)
        saveCenterWidgets(getCenterWidgets() + id)
        return spec
    }

    fun removeCustomWidget(id: String) {
        saveCustomWidgets(getCustomWidgets().filterNot { it.id == id })
        saveCenterWidgets(getCenterWidgets().filterNot { it == id })
    }

    private fun saveCustomWidgets(list: List<CustomWidgetSpec>) {
        val serialized = list.joinToString(";;") { "${it.id}||${it.title}||${it.command}" }
        prefs.edit().putString("custom_widgets_v1", serialized).apply()
    }

    fun getDailyLimits(): Map<String, Int> {
        val raw = prefs.getString("app_daily_limits", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split("=")
            val mins = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size == 2 && mins != null && mins > 0) parts[0] to mins else null
        }.toMap()
    }

    fun setDailyLimit(pkg: String, minutes: Int) {
        val map = getDailyLimits().toMutableMap()
        if (minutes <= 0) map.remove(pkg) else map[pkg] = minutes
        val serialized = map.entries.joinToString(",") { "${it.key}=${it.value}" }
        prefs.edit().putString("app_daily_limits", serialized).apply()
    }

    fun getMindfulPauseApps(): Set<String> {
        return prefs.getStringSet("mindful_pause_apps", emptySet())?.toSet() ?: emptySet()
    }

    fun toggleMindfulPause(pkg: String): Boolean {
        val current = getMindfulPauseApps().toMutableSet()
        val added = if (current.contains(pkg)) {
            current.remove(pkg)
            false
        } else {
            current.add(pkg)
            true
        }
        prefs.edit().putStringSet("mindful_pause_apps", current).apply()
        return added
    }

    fun getLockscreenMessage(): String = prefs.getString("lockscreen_owner_msg", "Stay intentional • Monolith") ?: "Stay intentional • Monolith"

    fun setLockscreenMessage(msg: String) {
        prefs.edit().putString("lockscreen_owner_msg", msg).apply()
    }

    fun isLockscreenMessageEnabled(): Boolean = prefs.getBoolean("lockscreen_owner_msg_enabled", false)

    fun setLockscreenMessageEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("lockscreen_owner_msg_enabled", enabled).apply()
    }

    fun isAmbientDisplayEnabled(): Boolean = prefs.getBoolean("ambient_display_enabled", true)

    fun setAmbientDisplayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ambient_display_enabled", enabled).apply()
    }
}

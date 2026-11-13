package com.rehan.monolith.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
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

@Serializable
data class CustomWidgetSpec(
    val id: String,
    val title: String,
    val command: String
)

@Serializable
data class MonolithSettings(
    val schemaVersion: Int = 1,
    val pinnedPackages: List<String> = emptyList(),
    val seededDefaults: Boolean = false,
    val focusDistractions: Set<String> = emptySet(),
    val focusModeActive: Boolean = false,
    val autoLaunchSingleMatch: Boolean = false,
    val showFrozenInDrawer: Boolean = true,
    val kidsApps: Set<String> = emptySet(),
    val kidsModeActive: Boolean = false,
    val parentPinHash: String? = null,
    val parentPinSalt: String? = null,
    val quickNote: String = "",
    val home: HomeConfig = HomeConfig(),
    val centerWidgets: List<String> = listOf("FOCUS_5217", "STOPWATCH", "ALARM_USAGE", "APPS_GRID"),
    val leftDockPkg: String? = null,
    val rightDockPkg: String? = null,
    val systemWidgetIds: List<Int> = emptyList(),
    val customWidgets: List<CustomWidgetSpec> = emptyList(),
    val dailyLimits: Map<String, Int> = emptyMap(),
    val mindfulPauseApps: Set<String> = emptySet(),
    val lockscreenMessage: String = "Stay intentional • Monolith",
    val lockscreenMessageEnabled: Boolean = false,
    val ambientDisplayEnabled: Boolean = true
)

internal val settingsJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal object MonolithSettingsSerializer : Serializer<MonolithSettings> {
    override val defaultValue = MonolithSettings()

    override suspend fun readFrom(input: InputStream): MonolithSettings = try {
        settingsJson.decodeFromString(MonolithSettings.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Cannot read settings", e)
    }

    override suspend fun writeTo(t: MonolithSettings, output: OutputStream) {
        output.write(settingsJson.encodeToString(MonolithSettings.serializer(), t).encodeToByteArray())
    }
}

internal object PinHasher {
    fun hash(pin: String, salt: ByteArray): String {
        val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        val key = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }

    fun newSalt(): ByteArray = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }

    fun isValid(pin: String) = pin.length >= 4 && pin.all { it.isDigit() }
}

internal class LegacyPrefsMigration(private val context: Context) : DataMigration<MonolithSettings> {
    private val legacy get() = context.getSharedPreferences(LEGACY_NAME, Context.MODE_PRIVATE)

    override suspend fun shouldMigrate(currentData: MonolithSettings) = legacy.all.isNotEmpty()

    override suspend fun migrate(currentData: MonolithSettings): MonolithSettings {
        val p = legacy
        fun str(k: String) = p.getString(k, null)
        fun csv(k: String) = str(k)?.split(",")?.filter { it.isNotBlank() }
        val d = MonolithSettings()
        val h = HomeConfig()

        var pinHash = str("parent_pin_hash")
        var pinSalt = str("parent_pin_salt")
        val plainPin = str("parent_pin")
        if (pinHash == null && plainPin != null && PinHasher.isValid(plainPin)) {
            val salt = PinHasher.newSalt()
            pinSalt = Base64.encodeToString(salt, Base64.NO_WRAP)
            pinHash = PinHasher.hash(plainPin, salt)
        }

        return d.copy(
            pinnedPackages = csv("pinned_packages") ?: d.pinnedPackages,
            seededDefaults = p.getBoolean("seeded_defaults_v1", d.seededDefaults),
            focusDistractions = p.getStringSet("focus_distractions", null)?.toSet() ?: d.focusDistractions,
            focusModeActive = p.getBoolean("focus_mode_active", d.focusModeActive),
            autoLaunchSingleMatch = p.getBoolean("auto_launch_single", d.autoLaunchSingleMatch),
            showFrozenInDrawer = p.getBoolean("show_frozen", d.showFrozenInDrawer),
            kidsApps = p.getStringSet("kids_apps", null)?.toSet() ?: d.kidsApps,
            kidsModeActive = p.getBoolean("kids_mode_active", d.kidsModeActive),
            parentPinHash = pinHash,
            parentPinSalt = pinSalt,
            quickNote = str("quick_note") ?: d.quickNote,
            home = HomeConfig(
                clockStyle = str("home_clock_style") ?: h.clockStyle,
                is24Hour = p.getBoolean("home_24h", h.is24Hour),
                batteryStyle = str("home_battery_style") ?: h.batteryStyle,
                alignCenter = p.getBoolean("home_align_center", h.alignCenter),
                showClock = p.getBoolean("home_show_clock", h.showClock),
                showBattery = p.getBoolean("home_show_battery", h.showBattery),
                showModes = p.getBoolean("home_show_modes", h.showModes),
                showNote = p.getBoolean("home_show_note_v2", h.showNote),
                showHomeIcons = p.getBoolean("home_show_icons", h.showHomeIcons),
                showTimer = p.getBoolean("home_show_timer", h.showTimer),
                showUsageAlarm = p.getBoolean("home_show_usage_alarm", h.showUsageAlarm),
                showStopwatch = p.getBoolean("home_show_stopwatch", h.showStopwatch),
                showQuickActions = p.getBoolean("home_show_quick_actions", h.showQuickActions),
                pinnedLayout = str("home_pinned_layout") ?: h.pinnedLayout,
                ambienceStyle = str("home_ambience_style") ?: h.ambienceStyle,
                tileStyle = str("home_tile_style") ?: h.tileStyle,
                blurBackground = p.getBoolean("home_blur_bg", h.blurBackground),
                colorMode = str("home_color_mode") ?: h.colorMode,
                amoledAccent = str("home_amoled_accent") ?: h.amoledAccent
            ),
            centerWidgets = csv("center_widgets_v2") ?: d.centerWidgets,
            leftDockPkg = str("left_dock_pkg"),
            rightDockPkg = str("right_dock_pkg"),
            systemWidgetIds = csv("system_widget_ids")?.mapNotNull { it.trim().toIntOrNull() } ?: d.systemWidgetIds,
            customWidgets = str("custom_widgets_v1")?.takeIf { it.isNotBlank() }?.split(";;")?.mapNotNull { e ->
                val parts = e.split("||")
                if (parts.size >= 3) CustomWidgetSpec(parts[0], parts[1], parts[2]) else null
            } ?: d.customWidgets,
            dailyLimits = csv("app_daily_limits")?.mapNotNull { e ->
                val parts = e.split("=")
                val mins = parts.getOrNull(1)?.toIntOrNull()
                if (parts.size == 2 && mins != null && mins > 0) parts[0] to mins else null
            }?.toMap() ?: d.dailyLimits,
            mindfulPauseApps = p.getStringSet("mindful_pause_apps", null)?.toSet() ?: d.mindfulPauseApps,
            lockscreenMessage = str("lockscreen_owner_msg") ?: d.lockscreenMessage,
            lockscreenMessageEnabled = p.getBoolean("lockscreen_owner_msg_enabled", d.lockscreenMessageEnabled),
            ambientDisplayEnabled = p.getBoolean("ambient_display_enabled", d.ambientDisplayEnabled)
        )
    }

    override suspend fun cleanUp() {
        context.deleteSharedPreferences(LEGACY_NAME)
    }

    companion object {
        const val LEGACY_NAME = "monolith_prefs"
    }
}

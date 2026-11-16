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

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Environment
import android.os.Process
import android.os.StatFs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class SysStats(val batteryPct: Int, val isCharging: Boolean, val freeRamGb: String, val freeStorageGb: String)

internal data class UsageAlarmInfo(
    val hasUsagePermission: Boolean,
    val screenTime: String,
    val totalMinutes: Int = 0,
    val unlockCount: Int = 0,
    val nextAlarm: String
)

internal fun readUsageAndAlarm(context: Context): UsageAlarmInfo {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val nextTrigger = am.nextAlarmClock?.triggerTime
    val alarmStr = if (nextTrigger != null) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextTrigger))
    } else {
        "—"
    }

    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    val allowed = mode == AppOpsManager.MODE_ALLOWED
    if (!allowed) {
        return UsageAlarmInfo(
            hasUsagePermission = false,
            screenTime = "",
            totalMinutes = 0,
            unlockCount = 0,
            nextAlarm = alarmStr
        )
    }

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis())
    val totalMs = stats
        ?.filter { it.lastTimeStamp >= cal.timeInMillis && it.totalTimeInForeground > 0 }
        ?.sumOf { it.totalTimeInForeground } ?: 0L
    val totalMins = (totalMs / 60_000L).toInt()
    val h = totalMins / 60
    val m = totalMins % 60
    val screenTime = if (h > 0) "${h}h ${m}m" else "${m}m"

    var unlocks = 0
    try {
        val events = usm.queryEvents(cal.timeInMillis, System.currentTimeMillis())
        val ev = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            if (ev.eventType == android.app.usage.UsageEvents.Event.KEYGUARD_HIDDEN) {
                unlocks++
            }
        }
    } catch (_: Throwable) {}

    return UsageAlarmInfo(
        hasUsagePermission = true,
        screenTime = screenTime,
        totalMinutes = totalMins,
        unlockCount = unlocks,
        nextAlarm = alarmStr
    )
}

internal fun readSystemStats(context: Context): SysStats {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val bat = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val charging = bm.isCharging
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am.getMemoryInfo(memInfo)
    val freeRam = String.format(Locale.US, "%.1f", memInfo.availMem / (1024.0 * 1024.0 * 1024.0))
    val stat = StatFs(Environment.getDataDirectory().path)
    val freeGb = (stat.availableBytes / (1024L * 1024L * 1024L)).toString()
    return SysStats(bat, charging, freeRam, freeGb)
}

internal fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap.asImageBitmap()
        } else {
            val w = drawable.intrinsicWidth.coerceIn(48, 128)
            val h = drawable.intrinsicHeight.coerceIn(48, 128)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        }
    } catch (_: Throwable) {
        null
    }
}

internal class BoundedIconCache<T>(private val maxSize: Int = 256) {
    private val lock = Any()
    private val map = object : LinkedHashMap<String, T>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, T>?): Boolean {
            return size > maxSize
        }
    }

    operator fun get(key: String): T? = synchronized(lock) { map[key] }
    operator fun set(key: String, value: T) {
        synchronized(lock) { map.put(key, value) }
    }
    fun clear() = synchronized(lock) { map.clear() }
    val size: Int get() = synchronized(lock) { map.size }
}

internal val appIconCache = BoundedIconCache<ImageBitmap>(256)

fun interface InstalledCheck {
    fun isInstalled(pkg: String): Boolean
}

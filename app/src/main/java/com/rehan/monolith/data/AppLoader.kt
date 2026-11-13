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
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.Locale

internal suspend fun loadInstalledApps(
    context: Context,
    frozenSet: Set<String>,
    onUpdate: (List<AppEntry>) -> Unit
) = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(mainIntent, 0)
    }

    val fastEntries = ArrayList<AppEntry>(resolved.size + frozenSet.size)
    val toResolve = ArrayList<ResolveInfo>()

    for (ri in resolved) {
        val pkg = ri.activityInfo.packageName
        if (pkg == context.packageName) continue
        val appInfo = ri.activityInfo.applicationInfo
        if (!appInfo.enabled && !frozenSet.contains(pkg)) continue
        if (!ri.activityInfo.isEnabled && !frozenSet.contains(pkg)) continue

        val isDisabled = !appInfo.enabled || frozenSet.contains(pkg)
        val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val label = ri.loadLabel(pm).toString().ifBlank { pkg }
        val cachedIcon = appIconCache[pkg]
        fastEntries.add(AppEntry(pkg, label, isDisabled, isSys, cachedIcon))
        if (cachedIcon == null) {
            toResolve.add(ri)
        }
    }

    for (pkg in frozenSet) {
        if (fastEntries.any { it.packageName == pkg } || pkg == context.packageName) continue
        try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!isSys) {
                val label = pm.getApplicationLabel(appInfo).toString().ifBlank { pkg }
                val cachedIcon = appIconCache[pkg]
                fastEntries.add(AppEntry(pkg, label, true, false, cachedIcon))
            }
        } catch (_: Throwable) {}
    }

    fastEntries.sortBy { it.label.lowercase(Locale.getDefault()) }

    // Phase 1: Immediately emit list (< 30ms) so app grid renders instantly
    withContext(Dispatchers.Main) {
        onUpdate(fastEntries.toList())
    }

    // Phase 2: Asynchronously decode uncached icons in parallel across CPU cores
    if (toResolve.isNotEmpty()) {
        val deferred = toResolve.map { ri ->
            async(Dispatchers.Default) {
                val pkg = ri.activityInfo.packageName
                try {
                    val icon = drawableToImageBitmap(ri.loadIcon(pm))
                        ?: drawableToImageBitmap(pm.defaultActivityIcon)
                    if (icon != null) {
                        appIconCache[pkg] = icon
                    }
                } catch (_: Throwable) {}
            }
        }
        deferred.awaitAll()

        val fullEntries = fastEntries.map { entry ->
            val icon = appIconCache[entry.packageName]
            if (entry.icon == null && icon != null) {
                entry.copy(icon = icon)
            } else {
                entry
            }
        }
        withContext(Dispatchers.Main) {
            onUpdate(fullEntries)
        }
    }
}

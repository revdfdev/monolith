package com.rehan.monolith.ui.wellbeing

import com.rehan.monolith.*
import com.rehan.monolith.data.*
import com.rehan.monolith.shizuku.*
import com.rehan.monolith.ui.common.*
import com.rehan.monolith.ui.home.*
import com.rehan.monolith.ui.widgets.*
import com.rehan.monolith.ui.drawer.*
import com.rehan.monolith.ui.shelf.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.install.*

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

internal fun readTodayAppUsageMinutes(context: Context): Map<String, Int> {
    return try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        if (mode != AppOpsManager.MODE_ALLOWED) return emptyMap()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis())
            ?: return emptyMap()
        stats.filter { it.lastTimeStamp >= cal.timeInMillis && it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .mapValues { (_, list) -> (list.sumOf { it.totalTimeInForeground } / 60_000L).toInt() }
    } catch (_: Throwable) {
        emptyMap()
    }
}

@Composable
internal fun UsageLimitsCoverWidget(
    apps: List<AppEntry>,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val topUsed = remember(apps) {
        val usage = readTodayAppUsageMinutes(context)
        val labelMap = apps.associate { it.packageName to it.label }
        usage.entries
            .filter { it.value > 0 && labelMap.containsKey(it.key) }
            .sortedByDescending { it.value }
            .take(3)
            .map { (labelMap[it.key] ?: it.key) to it.value }
    }
    Surface(
        color = LocalTileSurface.current,
        contentColor = PureWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LocalTileBorder.current),
        modifier = Modifier
            .fillMaxSize()
            .clickable { onOpenSettings() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("APP USAGE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp, color = TextDim, maxLines = 1)
            if (topUsed.isEmpty()) {
                Text("No usage data", fontSize = 12.sp, color = TextDim)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    topUsed.forEach { (name, mins) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, fontSize = 12.sp, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(6.dp))
                            Text("${mins}m", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextDim, maxLines = 1)
                        }
                    }
                }
            }
            Text("App Usage Limit", fontSize = 10.sp, color = TextDim, maxLines = 1)
        }
    }
}

@Composable
internal fun AppLimitGateDialog(
    appLabel: String,
    usedMins: Int,
    limitMins: Int,
    onAllowExtra: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBlack,
        title = { Text("$appLabel • Limit Reached", color = PureWhite) },
        text = {
            Text(
                "Used ${usedMins}m today (limit ${limitMins}m). Step back or allow a brief override.",
                fontSize = 13.sp,
                color = TextDim
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
            ) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(onClick = onAllowExtra) {
                Text("Allow Today", color = TextDim)
            }
        }
    )
}

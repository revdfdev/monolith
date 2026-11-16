package com.rehan.monolith.ui.widgets

import com.rehan.monolith.*
import com.rehan.monolith.data.*
import com.rehan.monolith.shizuku.*
import com.rehan.monolith.ui.common.*
import com.rehan.monolith.ui.home.*
import com.rehan.monolith.ui.drawer.*
import com.rehan.monolith.ui.shelf.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.install.*
import com.rehan.monolith.ui.wellbeing.*

import android.graphics.Canvas
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FocusRingWidget(
    title: String,
    workMinutes: Int,
    breakMinutes: Int,
    secondsLeft: Int,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    val totalSecs = workMinutes * 60f
    val clamped = secondsLeft.coerceIn(0, workMinutes * 60)
    val mins = clamped / 60
    val secs = clamped % 60
    val timeText = String.format(Locale.US, "%02d:%02d", mins, secs)
    val progress by animateFloatAsState(
        targetValue = clamped / totalSecs,
        animationSpec = tween(300),
        label = "focusRing"
    )

    Surface(
        color = LocalTileSurface.current,
        contentColor = PureWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isRunning) PureWhite else LocalTileBorder.current),
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(onClick = onToggle, onLongClick = onReset)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp, color = TextDim, maxLines = 1)
            Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(BorderHairline, 0f, 360f, false, style = stroke)
                    drawArc(PureWhite, -90f, 360f * progress, false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        timeText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        if (isRunning) "RUNNING" else "TAP",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim,
                        maxLines = 1
                    )
                }
            }
            Text("Break ${breakMinutes}m • Hold reset", fontSize = 10.sp, color = TextDim, maxLines = 1, softWrap = false)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StopwatchCenterWidget(
    seconds: Int,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    val mins = seconds / 60
    val secs = seconds % 60
    val text = String.format(Locale.US, "%02d:%02d", mins, secs)
    Surface(
        color = LocalTileSurface.current,
        contentColor = PureWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isRunning) PureWhite else LocalTileBorder.current),
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(onClick = onToggle, onLongClick = onReset)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("STOPWATCH", fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp, color = TextDim, maxLines = 1)
            Text(
                text,
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                color = PureWhite,
                maxLines = 1,
                softWrap = false
            )
            Text(
                if (isRunning) "Tap pause • Hold reset" else "Tap start • Hold reset",
                fontSize = 10.sp,
                color = TextDim,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
internal fun AlarmUsageCenterWidget(
    usageAlarm: UsageAlarmInfo,
    onGrantUsage: () -> Unit,
    onOpenAlarms: () -> Unit
) {
    Surface(
        color = LocalTileSurface.current,
        contentColor = PureWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, LocalTileBorder.current),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DETOX & USAGE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.4.sp,
                    color = TextDim,
                    maxLines = 1
                )
                if (usageAlarm.unlockCount > 0) {
                    Text(
                        "${usageAlarm.unlockCount} UNLOCKS",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!usageAlarm.hasUsagePermission) onGrantUsage() }
            ) {
                Text(
                    if (usageAlarm.hasUsagePermission) usageAlarm.screenTime else "Tap for stats",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1).sp,
                    color = PureWhite,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                // Minimalist hairline budget bar (based on 3-hour daily target)
                val budgetFraction = if (usageAlarm.totalMinutes > 0) {
                    (usageAlarm.totalMinutes / 180f).coerceIn(0.05f, 1f)
                } else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(BorderHairline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(budgetFraction)
                            .height(2.dp)
                            .background(PureWhite)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAlarms() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "NEXT ALARM",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = TextDim
                )
                Text(
                    usageAlarm.nextAlarm,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = PureWhite
                )
            }
        }
    }
}

@Composable
internal fun QuickActionsCenterWidget(
    onWifi: () -> Unit,
    onBluetooth: () -> Unit,
    onTrimCache: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        color = LocalTileSurface.current,
        contentColor = PureWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, LocalTileBorder.current),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onWifi() }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Wifi, contentDescription = "Wi-Fi", modifier = Modifier.size(18.dp), tint = PureWhite)
                        Spacer(Modifier.height(3.dp))
                        Text("WI-FI", fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, color = TextDim)
                    }
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxSize()
                        .background(BorderHairline)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onBluetooth() }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Bluetooth, contentDescription = "Bluetooth", modifier = Modifier.size(18.dp), tint = PureWhite)
                        Spacer(Modifier.height(3.dp))
                        Text("BT", fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, color = TextDim)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderHairline)
            )
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onTrimCache() }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = "Trim", modifier = Modifier.size(18.dp), tint = PureWhite)
                        Spacer(Modifier.height(3.dp))
                        Text("TRIM", fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, color = TextDim)
                    }
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxSize()
                        .background(BorderHairline)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onSettings() }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Prefs", modifier = Modifier.size(18.dp), tint = PureWhite)
                        Spacer(Modifier.height(3.dp))
                        Text("PREFS", fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, color = TextDim)
                    }
                }
            }
        }
    }
}


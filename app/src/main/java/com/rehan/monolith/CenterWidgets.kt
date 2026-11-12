package com.rehan.monolith

import android.graphics.Canvas
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LocalTileBorder.current),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TODAY & ALARM", fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp, color = TextDim, maxLines = 1)
            Column(modifier = Modifier.clickable { if (!usageAlarm.hasUsagePermission) onGrantUsage() }) {
                Text("SCREEN TIME", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextDim, maxLines = 1)
                Text(
                    if (usageAlarm.hasUsagePermission) usageAlarm.screenTime else "Tap to allow",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = PureWhite,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Column(modifier = Modifier.clickable { onOpenAlarms() }) {
                Text("NEXT ALARM", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextDim, maxLines = 1)
                Text(
                    usageAlarm.nextAlarm,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = PureWhite,
                    maxLines = 1,
                    softWrap = false
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LocalTileBorder.current),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("QUICK ACTIONS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp, color = TextDim, maxLines = 1)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onWifi,
                    border = BorderStroke(1.dp, BorderHairline),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null, modifier = Modifier.size(14.dp), tint = PureWhite)
                    Spacer(Modifier.width(4.dp))
                    Text("Wi-Fi", color = PureWhite, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onBluetooth,
                    border = BorderStroke(1.dp, BorderHairline),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(14.dp), tint = PureWhite)
                    Spacer(Modifier.width(4.dp))
                    Text("BT", color = PureWhite, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onTrimCache,
                    border = BorderStroke(1.dp, BorderHairline),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp), tint = PureWhite)
                    Spacer(Modifier.width(4.dp))
                    Text("Trim", color = PureWhite, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onSettings,
                    border = BorderStroke(1.dp, BorderHairline),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = PureWhite)
                    Spacer(Modifier.width(4.dp))
                    Text("Prefs", color = PureWhite, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
internal fun CustomCenterWidget(
    spec: CustomWidgetSpec,
    shizukuBound: Boolean,
    onExecShell: suspend (String) -> ShellResult
) {
    val scope = rememberCoroutineScope()
    var output by remember(spec.id) { mutableStateOf("") }

    LaunchedEffect(spec.id, shizukuBound) {
        if (shizukuBound && spec.command.isNotBlank()) {
            val r = onExecShell(spec.command)
            output = r.output.ifBlank { "(exit ${r.exitCode})" }
        }
    }

    Surface(
        color = SurfaceBlack,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderHairline),
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (shizukuBound && spec.command.isNotBlank()) {
                    scope.launch {
                        val r = onExecShell(spec.command)
                        output = r.output.ifBlank { "(exit ${r.exitCode})" }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(spec.title.uppercase(Locale.getDefault()), fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp, color = TextDim)
            Text(
                output.ifBlank { spec.command },
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = PureWhite,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
            Text(if (shizukuBound) "Tap to refresh" else "Enable Enhanced Controls in Settings", fontSize = 10.sp, color = TextDim)
        }
    }
}

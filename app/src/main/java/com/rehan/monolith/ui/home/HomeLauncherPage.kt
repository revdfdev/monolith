package com.rehan.monolith.ui.home

import com.rehan.monolith.*
import com.rehan.monolith.data.*
import com.rehan.monolith.shizuku.*
import com.rehan.monolith.ui.common.*
import com.rehan.monolith.ui.widgets.*
import com.rehan.monolith.ui.drawer.*
import com.rehan.monolith.ui.shelf.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.install.*
import com.rehan.monolith.ui.wellbeing.*

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeLauncherPage(
    shizukuStatus: ShizukuState,
    apps: List<AppEntry>,
    pinnedPackages: List<String>,
    focusActive: Boolean,
    focusDistractionCount: Int = 0,
    mindfulApps: Set<String> = emptySet(),
    usageMinutesMap: Map<String, Int> = emptyMap(),
    grayscaleOn: Boolean,
    quickNote: String,
    homeConfig: HomeConfig,
    centerWidgets: List<String>,
    systemWidgetIds: List<Int>,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: AppWidgetHost,
    leftDockPkg: String?,
    rightDockPkg: String?,
    onRequestShizuku: () -> Unit,
    onOpenShizukuShelf: () -> Unit,
    onOpenCustomizer: () -> Unit,
    onOpenWidgetManager: () -> Unit,
    onOpenAppPicker: () -> Unit,
    onPickLeftDock: () -> Unit,
    onPickRightDock: () -> Unit,
    onRemoveSystemWidget: (Int) -> Unit,
    onTrimCaches: () -> Unit,
    onOpenDrawer: () -> Unit,
    onKidsMode: () -> Unit,
    onToggleFocus: () -> Unit,
    onToggleGrayscale: () -> Unit,
    onEditNote: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onOpenDialer: () -> Unit,
    onOpenCamera: () -> Unit
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }

    val hourFmt = if (homeConfig.is24Hour) "HH" else "hh"
    val fullTimeFmt = if (homeConfig.is24Hour) "HH:mm" else "hh:mm a"
    val hourStr = remember(now, homeConfig.is24Hour) { SimpleDateFormat(hourFmt, Locale.getDefault()).format(Date(now)) }
    val minStr = remember(now) { SimpleDateFormat("mm", Locale.getDefault()).format(Date(now)) }
    val timeStr = remember(now, homeConfig.is24Hour) { SimpleDateFormat(fullTimeFmt, Locale.getDefault()).format(Date(now)) }
    val dateStr = remember(now) { SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(now)).uppercase(Locale.getDefault()) }
    val sysStats = remember(now) { readSystemStats(context) }
    val usageAlarm = remember(now) { readUsageAndAlarm(context) }

    var timerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(if ("FOCUS_5217" in centerWidgets) 52 * 60 else 25 * 60) }
    var stopwatchRunning by remember { mutableStateOf(false) }
    var stopwatchSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(timerRunning) {
        while (timerRunning && timerSeconds > 0) {
            delay(1_000)
            timerSeconds -= 1
            if (timerSeconds == 0) timerRunning = false
        }
    }

    LaunchedEffect(stopwatchRunning) {
        while (stopwatchRunning) {
            delay(1_000)
            stopwatchSeconds += 1
        }
    }

    val appMap = remember(apps) { apps.associateBy { it.packageName } }
    val pinnedEntries = remember(pinnedPackages, appMap) { pinnedPackages.mapNotNull { appMap[it] } }
    val horizontalAlign = if (homeConfig.alignCenter) Alignment.CenterHorizontally else Alignment.Start

    // The home grid claims vertical drags, so leftover (unscrollable) drag is what opens the drawer or shelf.
    val swipeThreshold = with(LocalDensity.current) { 48.dp.toPx() }
    val currentOpenDrawer by rememberUpdatedState(onOpenDrawer)
    val currentOpenShelf by rememberUpdatedState(onOpenShizukuShelf)
    val homeSwipe = remember(swipeThreshold) {
        object : NestedScrollConnection {
            var leftover = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                leftover += available.y
                when {
                    leftover < -swipeThreshold -> { leftover = 0f; currentOpenDrawer() }
                    leftover > swipeThreshold -> { leftover = 0f; currentOpenShelf() }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                leftover = 0f
                return Velocity.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(homeSwipe)
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag < -90f) {
                            totalDrag = 0f
                            onOpenDrawer()
                        } else if (totalDrag > 90f) {
                            totalDrag = 0f
                            onOpenShizukuShelf()
                        }
                    }
                )
            }
            .combinedClickable(
                onClick = {},
                onLongClick = { if (!focusActive) onOpenCustomizer() },
                interactionSource = null,
                indication = null
            )
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        // Top status bar: Settings + Customize button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = LocalTileSurface.current,
                contentColor = PureWhite,
                shape = CircleShape,
                border = BorderStroke(1.dp, LocalTileBorder.current),
                modifier = Modifier.clickable {
                    try {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (_: Throwable) {
                        onOpenShizukuShelf()
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (shizukuStatus.isReady) PureWhite else TextDim)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "SETTINGS",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite
                    )
                }
            }

            Surface(
                color = if (focusActive) PureWhite else LocalTileSurface.current,
                contentColor = if (focusActive) PureBlack else PureWhite,
                shape = CircleShape,
                border = BorderStroke(1.dp, LocalTileBorder.current),
                modifier = Modifier.clickable {
                    if (focusActive) onToggleFocus() else onOpenCustomizer()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (focusActive) Icons.Rounded.DoNotDisturbOn else Icons.Rounded.Tune,
                        contentDescription = if (focusActive) "Focus Active" else "Customize",
                        tint = if (focusActive) PureBlack else PureWhite,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (focusActive) "FOCUS ON" else "CUSTOMIZE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (focusActive) PureBlack else PureWhite
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Spacer(Modifier.height(10.dp))

        val deckItems = centerWidgets.filterNot { it == "APPS_GRID" }
        val showAppsTiles = centerWidgets.contains("APPS_GRID")

        // No stretch overscroll so leftover swipe reaches homeSwipe (opens drawer / shelf).
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (homeConfig.showClock) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = horizontalAlign
                    ) {
                        AnimatedContent(
                            targetState = homeConfig.clockStyle,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                            label = "clockStyle"
                        ) { style ->
                            when (style) {
                                "STACKED" -> Column(horizontalAlignment = horizontalAlign) {
                                    Text(hourStr, fontSize = 62.sp, lineHeight = 60.sp, fontWeight = FontWeight.Thin, fontFamily = FontFamily.Monospace, letterSpacing = (-3).sp, color = PureWhite)
                                    Text(minStr, fontSize = 62.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = (-3).sp, color = PureWhite)
                                    Spacer(Modifier.height(2.dp))
                                    Text(dateStr, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, color = TextDim)
                                }
                                "MINIMAL" -> Text("$timeStr  •  $dateStr", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = PureWhite)
                                else -> Column(horizontalAlignment = horizontalAlign) {
                                    Text(timeStr, fontSize = 54.sp, fontWeight = FontWeight.ExtraLight, fontFamily = FontFamily.Monospace, letterSpacing = (-2).sp, color = PureWhite)
                                    Text(dateStr, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.8.sp, color = TextDim)
                                }
                            }
                        }
                    }
                }
            }

            if (focusActive) {
                item(span = { GridItemSpan(2) }) {
                    Surface(
                        color = LocalTileSurface.current,
                        contentColor = PureWhite,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.28f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "FOCUS MODE ACTIVE",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.4.sp,
                                    color = PureWhite
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "DND ON • $focusDistractionCount Frozen • Edits Locked",
                                    fontSize = 12.sp,
                                    color = TextDim
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                color = PureWhite,
                                contentColor = PureBlack,
                                shape = CircleShape,
                                modifier = Modifier.clickable { onToggleFocus() }
                            ) {
                                Text(
                                    "Exit Focus",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (homeConfig.showBattery) {
                item {
                    BatteryWidget(
                        stats = sysStats,
                        style = homeConfig.batteryStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        onClick = { onOpenCustomizer() }
                    )
                }
            }

            items(deckItems.size) { idx ->
                val wid = deckItems[idx]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    when (wid) {
                        "FOCUS_5217" -> FocusRingWidget(
                            title = "52 / 17",
                            workMinutes = 52,
                            breakMinutes = 17,
                            secondsLeft = timerSeconds,
                            isRunning = timerRunning,
                            onToggle = {
                                if (timerSeconds == 0 || timerSeconds > 52 * 60) timerSeconds = 52 * 60
                                timerRunning = !timerRunning
                            },
                            onReset = {
                                timerRunning = false
                                timerSeconds = 52 * 60
                            }
                        )
                        "POMODORO" -> FocusRingWidget(
                            title = "POMODORO",
                            workMinutes = 25,
                            breakMinutes = 5,
                            secondsLeft = timerSeconds.coerceAtMost(25 * 60),
                            isRunning = timerRunning,
                            onToggle = {
                                if (timerSeconds == 0 || timerSeconds > 25 * 60) timerSeconds = 25 * 60
                                timerRunning = !timerRunning
                            },
                            onReset = {
                                timerRunning = false
                                timerSeconds = 25 * 60
                            }
                        )
                        "STOPWATCH" -> StopwatchCenterWidget(
                            seconds = stopwatchSeconds,
                            isRunning = stopwatchRunning,
                            onToggle = { stopwatchRunning = !stopwatchRunning },
                            onReset = {
                                stopwatchRunning = false
                                stopwatchSeconds = 0
                            }
                        )
                        "ALARM_USAGE" -> AlarmUsageCenterWidget(
                            usageAlarm = usageAlarm,
                            onGrantUsage = onGrantUsageAccess,
                            onOpenAlarms = {
                                try {
                                    context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Throwable) {
                                }
                            }
                        )
                        "QUICK_ACTIONS" -> QuickActionsCenterWidget(
                            onWifi = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                            onBluetooth = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                            onTrimCache = onTrimCaches,
                            onSettings = onOpenShizukuShelf
                        )
                        "USAGE_LIMITS" -> UsageLimitsCoverWidget(
                            apps = apps,
                            onOpenSettings = onOpenShizukuShelf
                        )
                        "NOTES" -> Surface(
                            color = LocalTileSurface.current,
        contentColor = PureWhite,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, LocalTileBorder.current),
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onEditNote() }
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Text("NOTE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                                Text(quickNote.ifBlank { "Tap to write..." }, fontSize = 13.sp, color = if (quickNote.isBlank()) TextDim else PureWhite, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                Text("Edit", fontSize = 10.sp, color = TextDim)
                            }
                        }
                        else -> {}
                    }
                }
            }

            // Real Android System Widgets (AppWidgetHost)
            items(
                count = systemWidgetIds.size,
                key = { i -> "sys_${systemWidgetIds[i]}" },
                span = { i ->
                    val info = appWidgetManager.getAppWidgetInfo(systemWidgetIds[i])
                    GridItemSpan(if (info != null && info.minWidth >= 180) 2 else 1)
                }
            ) { i ->
                val widgetId = systemWidgetIds[i]
                val info = remember(widgetId) { appWidgetManager.getAppWidgetInfo(widgetId) }
                if (info != null) {
                    val isWide = info.minWidth >= 180
                    Surface(
                        color = SurfaceBlack,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BorderHairline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (info.minHeight >= 180) 200.dp else 160.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { ctx ->
                                    appWidgetHost.createView(ctx, widgetId, info).apply {
                                        setAppWidget(widgetId, info)
                                        val wDp = if (isWide) 340 else 165
                                        val hDp = if (info.minHeight >= 180) 190 else 150
                                        updateAppWidgetSize(null, wDp, hDp, wDp, hDp)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            )
                            Text(
                                "✕",
                                fontSize = 11.sp,
                                color = TextDim,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clickable { onRemoveSystemWidget(widgetId) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }

            if (showAppsTiles) {
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PINNED TILES", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                        if (!focusActive) {
                            Text(
                                "+ WIDGETS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = PureWhite,
                                modifier = Modifier.clickable { onOpenWidgetManager() }
                            )
                        }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val gridEntries: List<AppEntry?> = if (focusActive) pinnedEntries else pinnedEntries + listOf<AppEntry?>(null)
                        val rows = gridEntries.chunked(4)
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { app ->
                                    if (app == null) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { onOpenAppPicker() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = "Add", tint = TextDim, modifier = Modifier.size(22.dp))
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .combinedClickable(
                                                    onClick = { if (app.isFrozen) onLongPressApp(app) else onLaunchApp(app.packageName) },
                                                    onLongClick = { onLongPressApp(app) }
                                                )
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (app.icon != null) {
                                                Box {
                                                    Image(
                                                        bitmap = app.icon,
                                                        contentDescription = null,
                                                        colorFilter = if (grayscaleOn) MonoFilter else null,
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                    )
                                                    if (mindfulApps.contains(app.packageName)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(7.dp)
                                                                .clip(CircleShape)
                                                                .background(PureWhite)
                                                                .align(Alignment.TopEnd)
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.height(5.dp))
                                            }
                                            Text(app.label, fontSize = 11.sp, color = if (app.isFrozen) TextDim else PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val usedMins = usageMinutesMap[app.packageName] ?: 0
                                            if (usedMins > 0) {
                                                Text(
                                                    "${usedMins}m",
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = TextDim,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                                repeat(4 - rowItems.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // 5. Bottom Launcher Dock
        // 5. Bottom Launcher Dock (Hold left/right icon to customize shortcut)
        val leftApp = remember(leftDockPkg, appMap) { leftDockPkg?.let { appMap[it] } }
        val rightApp = remember(rightDockPkg, appMap) { rightDockPkg?.let { appMap[it] } }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = LocalTileSurface.current,
        contentColor = PureWhite,
                shape = CircleShape,
                border = BorderStroke(1.dp, LocalTileBorder.current),
                modifier = Modifier
                    .size(46.dp)
                    .combinedClickable(
                        onClick = { if (leftApp != null) onLaunchApp(leftApp.packageName) else onOpenDialer() },
                        onLongClick = { if (!focusActive) onPickLeftDock() }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (leftApp?.icon != null) {
                        Image(
                            bitmap = leftApp.icon,
                            contentDescription = leftApp.label,
                            colorFilter = if (grayscaleOn) MonoFilter else null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Rounded.Phone, contentDescription = "Phone", tint = PureWhite, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Surface(
                color = LocalTileSurface.current,
        contentColor = PureWhite,
                shape = CircleShape,
                border = BorderStroke(1.dp, LocalTileBorder.current),
                modifier = Modifier
                    .height(46.dp)
                    .clickable { onOpenDrawer() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("APPS", fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp, color = PureWhite)
                }
            }

            Surface(
                color = LocalTileSurface.current,
        contentColor = PureWhite,
                shape = CircleShape,
                border = BorderStroke(1.dp, LocalTileBorder.current),
                modifier = Modifier
                    .size(46.dp)
                    .combinedClickable(
                        onClick = { if (rightApp != null) onLaunchApp(rightApp.packageName) else onOpenCamera() },
                        onLongClick = { if (!focusActive) onPickRightDock() }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (rightApp?.icon != null) {
                        Image(
                            bitmap = rightApp.icon,
                            contentDescription = rightApp.label,
                            colorFilter = if (grayscaleOn) MonoFilter else null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = "Camera", tint = PureWhite, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

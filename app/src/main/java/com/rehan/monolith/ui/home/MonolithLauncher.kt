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
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonolithLauncher(
    viewModel: LauncherViewModel,
    shizuku: ShizukuManager,
    appRepository: AppRepository,
    prefs: LauncherPrefs,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: AppWidgetHost,
    incomingApkUri: Uri? = null,
    onConsumeIncomingApk: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shizukuStatus by shizuku.status.collectAsState()

    val pagerState = rememberPagerState(initialPage = 1) { 2 }
    var drawerOpen by remember { mutableStateOf(false) }

    val apps by viewModel.apps.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val pinnedPackages = settings.pinnedPackages
    val focusDistractions = settings.focusDistractions
    val kidsApps = settings.kidsApps
    val kidsModeActive = settings.kidsModeActive
    val hasParentPin = settings.parentPinHash != null
    val quickNote = settings.quickNote
    var showNoteDialog by remember { mutableStateOf(false) }
    val homeConfig = settings.home
    val centerWidgets = settings.centerWidgets.filterNot { it.startsWith("CUSTOM_") }
    var showCustomizer by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showWidgetManager by remember { mutableStateOf(false) }
    val leftDockPkg = settings.leftDockPkg
    val rightDockPkg = settings.rightDockPkg
    var dockPickerTarget by remember { mutableStateOf<String?>(null) }
    val systemWidgetIds = settings.systemWidgetIds

    val widgetConfigureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == android.app.Activity.RESULT_OK && id != -1) {
            prefs.addSystemWidgetId(id)
        } else if (id != -1) {
            appWidgetHost.deleteAppWidgetId(id)
        }
    }

    val widgetBindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == android.app.Activity.RESULT_OK && id != -1) {
            val info = appWidgetManager.getAppWidgetInfo(id)
            if (info?.configure != null) {
                val cfgIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }
                try {
                    widgetConfigureLauncher.launch(cfgIntent)
                } catch (_: Throwable) {
                    prefs.addSystemWidgetId(id)
                }
            } else {
                prefs.addSystemWidgetId(id)
            }
        } else if (id != -1) {
            appWidgetHost.deleteAppWidgetId(id)
        }
    }
    var showSystemWidgetPicker by remember { mutableStateOf(false) }

    val focusActive = settings.focusModeActive
    var grayscaleOn by remember { mutableStateOf(false) }
    var stayAwakeOn by remember { mutableStateOf(false) }
    var lockscreenNotifMode by remember { mutableStateOf("QUIET") }
    var ambientDisplayOn by remember { mutableStateOf(prefs.isAmbientDisplayEnabled()) }
    var installersLocked by remember { mutableStateOf(false) }
    val showFrozen = settings.showFrozenInDrawer
    val autoLaunchSingle = settings.autoLaunchSingleMatch
    var selectedAppForSheet by remember { mutableStateOf<AppEntry?>(null) }
    var showKidsAppsPicker by remember { mutableStateOf(false) }
    var enterKidsAfterPick by remember { mutableStateOf(false) }
    val dailyLimits = settings.dailyLimits
    val mindfulPauseApps = settings.mindfulPauseApps
    var usageMinutesMap by remember { mutableStateOf(readTodayAppUsageMinutes(context)) }
    var tempUnlockedLimits by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingMindfulPkg by remember { mutableStateOf<String?>(null) }
    var pendingLimitPkg by remember { mutableStateOf<String?>(null) }
    var pendingApkInstall by remember { mutableStateOf<PendingApkInfo?>(null) }

    BackHandler(enabled = true) {
        when {
            kidsModeActive -> { /* Locked in Kids Space */ }
            drawerOpen -> drawerOpen = false
            pagerState.currentPage != 1 -> scope.launch { pagerState.animateScrollToPage(1) }
            else -> { /* Stay on Home; never exit to system launcher */ }
        }
    }

    fun reloadAll() {
        scope.launch {
            appRepository.refresh()
            usageMinutesMap = readTodayAppUsageMinutes(context)
            if (shizukuStatus.isReady) {
                grayscaleOn = shizuku.isGrayscaleEnabled()
                stayAwakeOn = shizuku.getStayAwake()
                ambientDisplayOn = shizuku.isDozeAlwaysOn()
                lockscreenNotifMode = shizuku.getLockscreenNotificationMode()
            }
        }
    }

    LaunchedEffect(shizukuStatus.isReady) {
        appRepository.refresh()
    }

    LaunchedEffect(apps) {
        val loaded = apps
        if (!prefs.hasSeededDefaults() && pinnedPackages.isEmpty() && loaded.isNotEmpty()) {
            val seeded = loaded.filter { !it.isSystem && !it.isFrozen }.take(4).map { it.packageName }
            if (seeded.isNotEmpty()) {
                prefs.setPinnedPackages(seeded)
            }
            prefs.markDefaultsSeeded()
        }
    }

    LaunchedEffect(Unit) {
        reloadAll()
    }

    fun processApkUri(uri: Uri) {
        scope.launch {
            val info = withContext(Dispatchers.IO) {
                val out = File(context.externalCacheDir ?: context.cacheDir, "install_${System.currentTimeMillis()}.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                inspectApkFile(context, out)
            }
            pendingApkInstall = info
        }
    }

    LaunchedEffect(incomingApkUri) {
        val uri = incomingApkUri
        if (uri != null) {
            processApkUri(uri)
            onConsumeIncomingApk()
        }
    }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun forceLaunchApp(pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            drawerOpen = false
        } else {
            toast("App cannot be launched")
        }
    }

    fun launchApp(pkg: String) {
        val limit = dailyLimits[pkg] ?: 0
        val used = usageMinutesMap[pkg] ?: 0
        if (limit > 0 && used >= limit && !tempUnlockedLimits.contains(pkg)) {
            pendingLimitPkg = pkg
            return
        }
        if (mindfulPauseApps.contains(pkg)) {
            pendingMindfulPkg = pkg
            return
        }
        forceLaunchApp(pkg)
    }

    fun enterKidsMode() {
        if (!prefs.hasParentPin()) {
            toast("Set a Parent PIN first")
            return
        }
        if (kidsApps.isEmpty()) {
            enterKidsAfterPick = true
            showKidsAppsPicker = true
            return
        }
        scope.launch {
            if (shizukuStatus.isReady) {
                shizuku.setInstallersLocked(true)
            }
            prefs.setKidsModeActive(true)
            drawerOpen = false
        }
    }

    fun exitKidsMode() {
        scope.launch {
            if (shizukuStatus.isReady) {
                shizuku.setInstallersLocked(false)
            }
            prefs.setKidsModeActive(false)
            reloadAll()
            toast("Kids Mode OFF")
        }
    }

    fun toggleFocusMode() {
        scope.launch {
            if (!shizukuStatus.isReady) {
                shizuku.requestPermission()
                toast("Grant Shizuku permission first")
                return@launch
            }
            val next = !focusActive
            shizuku.setGrayscale(next)
            grayscaleOn = next
            shizuku.setDnd(next)
            for (pkg in focusDistractions) {
                if (next) {
                    shizuku.forceStop(pkg)
                    shizuku.freezePackage(pkg)
                } else {
                    shizuku.unfreezePackage(pkg)
                }
            }
            prefs.setFocusModeActive(next)
            reloadAll()
            toast(if (next) "Focus ON (${focusDistractions.size} frozen + Mono)" else "Focus OFF")
        }
    }

    if (kidsModeActive) {
        val allowedKidsApps = remember(apps, kidsApps) {
            apps.filter { kidsApps.contains(it.packageName) && !it.isFrozen }
        }
        KidsModeLauncherScreen(
            kidsApps = allowedKidsApps,
            verifyPin = { prefs.verifyParentPin(it) },
            onLaunchApp = { launchApp(it) },
            onExitKidsMode = { exitKidsMode() },
            onWrongPin = { toast("Wrong Parent PIN") }
        )
        return
    }

    var wallpaperPrimaryColor by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                try {
                    val wm = android.app.WallpaperManager.getInstance(context)
                    wm.getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM)
                        ?.primaryColor?.toArgb()?.let { Color(it) }
                } catch (_: Throwable) { null }
            } else null
        )
    }

    DisposableEffect(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wm = android.app.WallpaperManager.getInstance(context)
            val listener = android.app.WallpaperManager.OnColorsChangedListener { colors, which ->
                if ((which and android.app.WallpaperManager.FLAG_SYSTEM) != 0) {
                    val c = colors?.primaryColor?.toArgb()?.let { Color(it) }
                    wallpaperPrimaryColor = c
                }
            }
            try {
                wm.addOnColorsChangedListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
            } catch (_: Throwable) {}
            onDispose {
                try {
                    wm.removeOnColorsChangedListener(listener)
                } catch (_: Throwable) {}
            }
        } else {
            onDispose {}
        }
    }

    val dynamicPrimary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.primary
    } else {
        wallpaperPrimaryColor ?: Color(0xFF4A6572)
    }

    val amoledAccentColor = when (homeConfig.amoledAccent.uppercase()) {
        "AMBER" -> Color(0xFFFFB300)
        "CYAN" -> Color(0xFF00E5FF)
        "EMERALD" -> Color(0xFF00E676)
        "ROSE" -> Color(0xFFFF4081)
        "PURPLE" -> Color(0xFFD500F9)
        "ORANGE" -> Color(0xFFFF6D00)
        "BLUE" -> Color(0xFF2979FF)
        else -> PureWhite
    }

    LaunchedEffect(homeConfig.ambienceStyle, homeConfig.blurBackground, focusActive) {
        val win = (context as? android.app.Activity)?.window ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val radius = if (focusActive || !homeConfig.blurBackground || homeConfig.ambienceStyle == "AMOLED") {
                0
            } else if (homeConfig.ambienceStyle == "MIST") {
                35
            } else {
                65
            }
            if (radius > 0) {
                win.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                win.attributes = win.attributes.apply { blurBehindRadius = radius }
                win.setBackgroundBlurRadius(radius)
            } else {
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                win.setBackgroundBlurRadius(0)
            }
        }
    }

    val isOled = focusActive || homeConfig.colorMode == "OLED" || homeConfig.ambienceStyle == "AMOLED"
    val accentBase = if (isOled) amoledAccentColor else (wallpaperPrimaryColor ?: dynamicPrimary)
    val ambienceBrush = when {
        isOled -> Brush.verticalGradient(listOf(PureBlack, PureBlack))
        homeConfig.ambienceStyle == "MIST" -> Brush.verticalGradient(
            listOf(accentBase.copy(alpha = 0.22f), Color(0x66060606), Color(0x99000000))
        )
        else -> Brush.verticalGradient(
            listOf(accentBase.copy(alpha = 0.34f), Color(0xA6050505), Color(0xD8000000))
        )
    }
    val tileSurface = when {
        isOled -> Color(0xFF0A0A0A)
        homeConfig.tileStyle == "SOLID" -> Color(0xEB0A0A0A)
        homeConfig.tileStyle == "OUTLINE" -> Color(0x1F000000)
        else -> androidx.compose.ui.graphics.lerp(Color(0x780C0C0C), accentBase.copy(alpha = 0.78f), 0.28f)
    }
    val tileBorder = when {
        isOled && homeConfig.amoledAccent != "WHITE" -> accentBase.copy(alpha = 0.38f)
        isOled -> Color(0xFF242424)
        homeConfig.tileStyle == "SOLID" -> accentBase.copy(alpha = 0.45f)
        homeConfig.tileStyle == "OUTLINE" -> accentBase.copy(alpha = 0.75f)
        else -> androidx.compose.ui.graphics.lerp(Color(0x40FFFFFF), accentBase.copy(alpha = 0.85f), 0.5f)
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalTileSurface provides tileSurface,
        LocalTileBorder provides tileBorder
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambienceBrush)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { idx ->
                val active = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .width(if (active) 16.dp else 6.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(if (active) PureWhite else BorderHairline)
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ShizukuShelfPage(
                    status = shizukuStatus,
                    apps = apps,
                    kidsAppCount = kidsApps.size,
                    hasParentPin = hasParentPin,
                    grayscaleOn = grayscaleOn,
                    stayAwakeOn = stayAwakeOn,
                    installersLocked = installersLocked,
                    ambientDisplayOn = ambientDisplayOn,
                    onToggleAmbientDisplay = { next ->
                        scope.launch {
                            shizuku.setDozeAlwaysOn(next)
                            ambientDisplayOn = next
                            prefs.setAmbientDisplayEnabled(next)
                            toast(if (next) "Doze ambient display enabled" else "Ambient display disabled")
                        }
                    },
                    lockscreenNotifMode = lockscreenNotifMode,
                    onChangeLockscreenNotifMode = { mode ->
                        scope.launch {
                            shizuku.setLockscreenNotificationMode(mode)
                            lockscreenNotifMode = mode
                            toast("Lockscreen: $mode mode")
                        }
                    },
                    onRequestPermission = { shizuku.requestPermission() },
                    onRefresh = { reloadAll() },
                    onEnterKidsMode = { enterKidsMode() },
                    onConfigureKidsApps = {
                        enterKidsAfterPick = false
                        showKidsAppsPicker = true
                    },
                    focusActive = focusActive,
                    onToggleFocus = { toggleFocusMode() },
                    onSaveParentPin = { newPin ->
                        if (prefs.setParentPin(newPin)) {
                            toast("Parent PIN saved")
                        } else {
                            toast("PIN must be at least 4 digits")
                        }
                    },
                    onSetAnimScale = { scale ->
                        scope.launch {
                            shizuku.setAnimationScale(scale)
                            toast("Animation scale -> ${scale}x")
                        }
                    },
                    onToggleGrayscale = { next ->
                        scope.launch {
                            shizuku.setGrayscale(next)
                            grayscaleOn = next
                        }
                    },
                    onToggleStayAwake = { next ->
                        scope.launch {
                            shizuku.setStayAwake(next)
                            stayAwakeOn = next
                        }
                    },
                    onToggleInstallersLock = { next ->
                        scope.launch {
                            shizuku.setInstallersLocked(next)
                            installersLocked = next
                            reloadAll()
                            toast(if (next) "Installers locked" else "Installers unlocked")
                        }
                    },
                    onFreezeToggle = { pkg, freeze ->
                        scope.launch {
                            val r = if (freeze) {
                                shizuku.forceStop(pkg)
                                shizuku.freezePackage(pkg)
                            } else {
                                shizuku.unfreezePackage(pkg)
                            }
                            if (!r.ok) toast(r.output)
                            reloadAll()
                        }
                    },
                    onUnfreezeAll = {
                        scope.launch {
                            val frozenList = apps.filter { it.isFrozen }.map { it.packageName }
                            shizuku.unfreezePackages(frozenList)
                            reloadAll()
                            toast("Unfroze ${frozenList.size} app(s)")
                        }
                    },
                    onUninstallApp = { pkg ->
                        scope.launch {
                            if (shizukuStatus.isReady) {
                                val r = shizuku.uninstallPackage(pkg)
                                toast(if (r.ok) "Uninstalled $pkg" else r.output)
                                reloadAll()
                            } else {
                                context.startActivity(
                                    Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    },
                    onInstallApkUri = { uri ->
                        processApkUri(uri)
                    },
                    onTrimCaches = {
                        scope.launch {
                            val r = shizuku.trimAllCaches()
                            toast(if (r.ok) "All caches trimmed" else r.output)
                        }
                    },
                    onSetDefaultLauncher = {
                        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    }
                )

                1 -> HomeLauncherPage(
                    shizukuStatus = shizukuStatus,
                    apps = apps,
                    pinnedPackages = pinnedPackages,
                    focusActive = focusActive,
                    focusDistractionCount = focusDistractions.size,
                    mindfulApps = mindfulPauseApps,
                    usageMinutesMap = usageMinutesMap,
                    grayscaleOn = grayscaleOn,
                    quickNote = quickNote,
                    homeConfig = homeConfig,
                    centerWidgets = centerWidgets,
                    systemWidgetIds = systemWidgetIds,
                    appWidgetManager = appWidgetManager,
                    appWidgetHost = appWidgetHost,
                    leftDockPkg = leftDockPkg,
                    rightDockPkg = rightDockPkg,
                    onRequestShizuku = { shizuku.requestPermission() },
                    onOpenShizukuShelf = { scope.launch { pagerState.animateScrollToPage(0) } },
                    onOpenCustomizer = { showCustomizer = true },
                    onOpenWidgetManager = { showWidgetManager = true },
                    onOpenAppPicker = { showAppPicker = true },
                    onPickLeftDock = { dockPickerTarget = "LEFT" },
                    onPickRightDock = { dockPickerTarget = "RIGHT" },
                    onRemoveSystemWidget = { wid ->
                        appWidgetHost.deleteAppWidgetId(wid)
                        prefs.removeSystemWidgetId(wid)
                    },
                    onTrimCaches = {
                        scope.launch {
                            if (shizukuStatus.isReady) {
                                shizuku.trimAllCaches()
                                toast("App caches trimmed")
                            } else {
                                toast("Enable Enhanced Controls in Settings")
                            }
                        }
                    },
                    onOpenDrawer = { drawerOpen = true },
                    onKidsMode = { enterKidsMode() },
                    onToggleFocus = { toggleFocusMode() },
                    onToggleGrayscale = {
                        scope.launch {
                            if (!shizukuStatus.isReady) {
                                shizuku.requestPermission()
                            } else {
                                val next = !grayscaleOn
                                shizuku.setGrayscale(next)
                                grayscaleOn = next
                            }
                        }
                    },
                    onEditNote = { showNoteDialog = true },
                    onGrantUsageAccess = {
                        scope.launch {
                            if (shizukuStatus.isReady) {
                                shizuku.allowUsageStats()
                                toast("Screen time enabled")
                            } else {
                                context.startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    },
                    onLaunchApp = { launchApp(it) },
                    onLongPressApp = { if (!focusActive) selectedAppForSheet = it },
                    onOpenDialer = {
                        context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    onOpenCamera = {
                        context.startActivity(
                            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(150))
        ) {
            AppDrawerOverlay(
                apps = apps,
                showFrozen = showFrozen,
                autoLaunchSingle = autoLaunchSingle,
                grayscaleOn = grayscaleOn,
                focusActive = focusActive,
                pinnedPackages = pinnedPackages.toSet(),
                focusDistractions = focusDistractions,
                mindfulApps = mindfulPauseApps,
                usageMinutesMap = usageMinutesMap,
                kidsApps = kidsApps,
                onClose = { drawerOpen = false },
                onToggleShowFrozen = {
                    prefs.setShowFrozenInDrawer(!showFrozen)
                },
                onToggleAutoLaunch = {
                    prefs.setAutoLaunchSingleMatch(!autoLaunchSingle)
                },
                onLaunchApp = { entry ->
                    if (entry.isFrozen) {
                        selectedAppForSheet = entry
                    } else {
                        launchApp(entry.packageName)
                    }
                },
                onLongPressApp = { selectedAppForSheet = it }
            )
        }
    }

    if (showNoteDialog) {
        var noteDraft by remember(quickNote) { mutableStateOf(quickNote) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            containerColor = SurfaceBlack,
            title = { Text("Scratchpad", color = PureWhite) },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    placeholder = { Text("Quick TODO or reminder...", color = TextDim) },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = BorderHairline
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.setQuickNote(noteDraft.trim())
                        showNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel", color = TextDim)
                }
            }
        )
    }

    val sheetApp = selectedAppForSheet
    if (sheetApp != null) {
        val isPinned = pinnedPackages.contains(sheetApp.packageName)
        val isDistraction = focusDistractions.contains(sheetApp.packageName)
        val isKids = kidsApps.contains(sheetApp.packageName)

        ModalBottomSheet(
            onDismissRequest = { selectedAppForSheet = null },
            containerColor = systemBottomSheetContainerColor(),
            contentColor = systemBottomSheetContentColor()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(sheetApp.label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val usedToday = usageMinutesMap[sheetApp.packageName] ?: 0
                val currentLimit = dailyLimits[sheetApp.packageName] ?: 0
                val isMindful = mindfulPauseApps.contains(sheetApp.packageName)
                Text(
                    "${sheetApp.packageName} • Used today: ${usedToday}m" +
                        if (currentLimit > 0) " / ${currentLimit}m" else "",
                    fontSize = 12.sp,
                    color = TextDim,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(14.dp))

                SheetAction(
                    icon = Icons.Rounded.PushPin,
                    label = if (isPinned) "Unpin from Home" else "Pin to Home"
                ) {
                    prefs.togglePinned(sheetApp.packageName)
                    selectedAppForSheet = null
                }

                SheetAction(
                    icon = Icons.Rounded.ChildCare,
                    label = if (isKids) "Remove from Kids Mode" else "Add to Kids Mode"
                ) {
                    prefs.toggleKidsApp(sheetApp.packageName)
                    selectedAppForSheet = null
                }

                SheetAction(
                    icon = Icons.Rounded.DoNotDisturbOn,
                    label = if (isDistraction) "Remove from Focus Freeze" else "Add to Focus Freeze"
                ) {
                    prefs.toggleFocusDistraction(sheetApp.packageName)
                    selectedAppForSheet = null
                }

                if (shizukuStatus.isReady) {
                    SheetAction(
                        icon = Icons.Rounded.AcUnit,
                        label = if (sheetApp.isFrozen) "Resume App" else "Pause App"
                    ) {
                        scope.launch {
                            val r = if (sheetApp.isFrozen) {
                                shizuku.unfreezePackage(sheetApp.packageName)
                            } else {
                                shizuku.forceStop(sheetApp.packageName)
                                shizuku.freezePackage(sheetApp.packageName)
                            }
                            toast(if (r.ok) "Done" else r.output)
                            reloadAll()
                            selectedAppForSheet = null
                        }
                    }

                    SheetAction(
                        icon = Icons.Rounded.StopCircle,
                        label = "Force Close"
                    ) {
                        scope.launch {
                            shizuku.forceStop(sheetApp.packageName)
                            toast("Force stopped ${sheetApp.label}")
                            selectedAppForSheet = null
                        }
                    }
                }

                SheetAction(
                    icon = Icons.Rounded.Timer,
                    label = if (isMindful) "Mindful Pause (5s): ON" else "Mindful Pause (5s): OFF"
                ) {
                    prefs.toggleMindfulPause(sheetApp.packageName)
                    selectedAppForSheet = null
                }

                // Curbox Daily Limit selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Limit:", fontSize = 13.sp, color = TextDim)
                    listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m").forEach { (mins, lbl) ->
                        val active = currentLimit == mins
                        Surface(
                            color = if (active) PureWhite else PureBlack,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (active) PureWhite else BorderHairline),
                            modifier = Modifier.clickable {
                                prefs.setDailyLimit(sheetApp.packageName, mins)
                            }
                        ) {
                            Text(
                                lbl,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (active) PureBlack else PureWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                SheetAction(
                    icon = Icons.Rounded.Add,
                    label = "Export APK"
                ) {
                    scope.launch {
                        val msg = exportAppApk(context, sheetApp.packageName, sheetApp.label)
                        toast(msg)
                        selectedAppForSheet = null
                    }
                }

                SheetAction(
                    icon = Icons.Rounded.Info,
                    label = "App Info"
                ) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${sheetApp.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    selectedAppForSheet = null
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showCustomizer) {
        HomeCustomizerSheet(
            config = homeConfig,
            onUpdate = { updated ->
                prefs.saveHomeConfig(updated)
            },
            onPickHomeApps = {
                showCustomizer = false
                showAppPicker = true
            },
            onManageWidgets = {
                showCustomizer = false
                showWidgetManager = true
            },
            onDismiss = { showCustomizer = false }
        )
    }

    if (showWidgetManager) {
        WidgetManagerSheet(
            activeWidgets = centerWidgets,
            onSaveWidgets = { updated ->
                prefs.saveCenterWidgets(updated)
            },
            onAddSystemWidget = {
                showWidgetManager = false
                showSystemWidgetPicker = true
            },
            onDismiss = { showWidgetManager = false }
        )
    }

    if (showSystemWidgetPicker) {
        SystemWidgetPickerDialog(
            appWidgetManager = appWidgetManager,
            onSelectProvider = { provider ->
                showSystemWidgetPicker = false
                val id = appWidgetHost.allocateAppWidgetId()
                val bound = try {
                    appWidgetManager.bindAppWidgetIdIfAllowed(id, provider.profile, provider.provider, null)
                } catch (_: Throwable) {
                    false
                }
                if (bound) {
                    if (provider.configure != null) {
                        val cfgIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                            component = provider.configure
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        }
                        try {
                            widgetConfigureLauncher.launch(cfgIntent)
                        } catch (_: Throwable) {
                            prefs.addSystemWidgetId(id)
                        }
                    } else {
                        prefs.addSystemWidgetId(id)
                    }
                } else {
                    val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
                    }
                    try {
                        widgetBindLauncher.launch(bindIntent)
                    } catch (_: Throwable) {
                        appWidgetHost.deleteAppWidgetId(id)
                        Toast.makeText(context, "Unable to bind system widget", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showSystemWidgetPicker = false }
        )
    }

    val dockTarget = dockPickerTarget
    if (dockTarget != null) {
        PickSingleAppDialog(
            title = if (dockTarget == "LEFT") "Left Dock Shortcut" else "Right Dock Shortcut",
            apps = apps,
            onSelect = { pkg ->
                if (dockTarget == "LEFT") {
                    prefs.setLeftDockPkg(pkg)
                } else {
                    prefs.setRightDockPkg(pkg)
                }
                dockPickerTarget = null
            },
            onResetDefault = {
                if (dockTarget == "LEFT") {
                    prefs.setLeftDockPkg(null)
                } else {
                    prefs.setRightDockPkg(null)
                }
                dockPickerTarget = null
            },
            onDismiss = { dockPickerTarget = null }
        )
    }

    if (showAppPicker) {
        PickAppsMultiDialog(
            title = "Choose Home Apps",
            confirmLabel = "Done",
            apps = apps,
            selectedPackages = pinnedPackages.toSet(),
            onTogglePackage = { pkg ->
                prefs.togglePinned(pkg)
            },
            onConfirm = { showAppPicker = false },
            onDismiss = { showAppPicker = false }
        )
    }

    if (showKidsAppsPicker) {
        PickAppsMultiDialog(
            title = "Choose Kids Apps",
            confirmLabel = if (enterKidsAfterPick && kidsApps.isNotEmpty()) "Enter Kids Mode" else "Done",
            apps = apps,
            selectedPackages = kidsApps,
            onTogglePackage = { pkg ->
                prefs.toggleKidsApp(pkg)
            },
            onConfirm = {
                showKidsAppsPicker = false
                if (enterKidsAfterPick && kidsApps.isNotEmpty()) {
                    enterKidsAfterPick = false
                    enterKidsMode()
                }
            },
            onDismiss = {
                showKidsAppsPicker = false
                enterKidsAfterPick = false
            }
        )
    }

    val limitPkg = pendingLimitPkg
    if (limitPkg != null) {
        val label = apps.firstOrNull { it.packageName == limitPkg }?.label ?: limitPkg
        AppLimitGateDialog(
            appLabel = label,
            usedMins = usageMinutesMap[limitPkg] ?: 0,
            limitMins = dailyLimits[limitPkg] ?: 0,
            onAllowExtra = {
                tempUnlockedLimits = tempUnlockedLimits + limitPkg
                pendingLimitPkg = null
                forceLaunchApp(limitPkg)
            },
            onDismiss = { pendingLimitPkg = null }
        )
    }

    val mindfulPkg = pendingMindfulPkg
    if (mindfulPkg != null) {
        val label = apps.firstOrNull { it.packageName == mindfulPkg }?.label ?: mindfulPkg
        MindfulPauseDialog(
            appLabel = label,
            onContinue = {
                pendingMindfulPkg = null
                forceLaunchApp(mindfulPkg)
            },
            onDismiss = { pendingMindfulPkg = null }
        )
    }

    val apkInfo = pendingApkInstall
    if (apkInfo != null) {
        PackageInstallDialog(
            apkInfo = apkInfo,
            shizukuBound = shizukuStatus.isReady,
            onInstall = { flags ->
                scope.launch {
                    val r = shizuku.installApk(apkInfo.path, flags)
                    toast(if (r.ok) "Installed ${apkInfo.appLabel}" else r.output.ifBlank { "Install failed" })
                    pendingApkInstall = null
                    reloadAll()
                }
            },
            onDismiss = { pendingApkInstall = null }
        )
    }
    }
}

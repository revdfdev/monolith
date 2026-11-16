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
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
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
    var drawerInitialLetter by remember { mutableStateOf<Char?>(null) }

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
    val tweaks by viewModel.tweaks.collectAsState()
    val grayscaleOn = tweaks.grayscale
    val stayAwakeOn = tweaks.stayAwake
    val lockscreenNotifMode = tweaks.lockscreenMode
    val ambientDisplayOn = tweaks.ambientDisplay
    val installersLocked = tweaks.installersLocked
    val showFrozen = settings.showFrozenInDrawer
    val autoLaunchSingle = settings.autoLaunchSingleMatch
    var showSettings by remember { mutableStateOf(false) }
    var showKidsAppsPicker by remember { mutableStateOf(false) }
    var enterKidsAfterPick by remember { mutableStateOf(false) }
    val dailyLimits = settings.dailyLimits
    val mindfulPauseApps = settings.mindfulPauseApps
    var usageMinutesMap by remember { mutableStateOf(readTodayAppUsageMinutes(context)) }
    var tempUnlockedLimits by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingMindfulPkg by remember { mutableStateOf<String?>(null) }
    var pendingLimitPkg by remember { mutableStateOf<String?>(null) }
    var pendingApkInstall by remember { mutableStateOf<PendingApkInfo?>(null) }
    var apkLoadingState by remember { mutableStateOf<ApkLoadingState?>(null) }
    var copyApkJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var currentLoadingUri by remember { mutableStateOf<Uri?>(null) }
    val packageInstallState by viewModel.packageInstallState.collectAsState()

    BackHandler(enabled = true) {
        when {
            kidsModeActive -> { /* Locked in Kids Space */ }
            drawerOpen -> {
                drawerOpen = false
                drawerInitialLetter = null
            }
            showSettings -> {
                showSettings = false
            }
            else -> { /* Stay on Home; never exit to system launcher */ }
        }
    }

    fun reloadAll() {
        scope.launch {
            appRepository.refresh()
            usageMinutesMap = readTodayAppUsageMinutes(context)
            viewModel.readTweaks()
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

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun processApkUri(uri: Uri) {
        currentLoadingUri = uri
        copyApkJob?.cancel()
        copyApkJob = scope.launch {
            var out: File? = null
            try {
                val info = withContext(Dispatchers.IO) {
                    var totalSize = -1L
                    var displayName = "package.apk"
                    try {
                        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIdx >= 0) cursor.getString(nameIdx)?.let { displayName = it }
                                val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                if (sizeIdx >= 0) totalSize = cursor.getLong(sizeIdx)
                            }
                        }
                    } catch (_: Throwable) {}

                    withContext(Dispatchers.Main) {
                        apkLoadingState = ApkLoadingState(displayName, 0L, totalSize)
                    }

                    val extension = if (displayName.contains(".")) displayName.substring(displayName.lastIndexOf('.')) else ".apk"
                    val targetFile = File(context.externalCacheDir ?: context.cacheDir, "install_${System.currentTimeMillis()}$extension")
                    out = targetFile

                    context.contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var copied = 0L
                            var lastUpdate = System.currentTimeMillis()
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                copied += read
                                val now = System.currentTimeMillis()
                                if (now - lastUpdate > 120) {
                                    withContext(Dispatchers.Main) {
                                        apkLoadingState = ApkLoadingState(displayName, copied, totalSize)
                                    }
                                    lastUpdate = now
                                }
                            }
                        }
                    }
                    inspectApkFile(context, targetFile)
                }
                apkLoadingState = null
                currentLoadingUri = null
                pendingApkInstall = info
            } catch (_: kotlinx.coroutines.CancellationException) {
                apkLoadingState = null
                try { out?.delete() } catch (_: Throwable) {}
            } catch (t: Throwable) {
                apkLoadingState = null
                currentLoadingUri = null
                try { out?.delete() } catch (_: Throwable) {}
                toast("Failed to read package: ${t.message ?: "error"}")
            }
        }
    }

    LaunchedEffect(incomingApkUri) {
        val uri = incomingApkUri
        if (uri != null) {
            processApkUri(uri)
            onConsumeIncomingApk()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { toast(it) }
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
        if (mindfulPauseApps.contains(pkg) && !viewModel.isMindfulCooldownActive(pkg)) {
            pendingMindfulPkg = pkg
            return
        }
        viewModel.onAppLaunchRequested(pkg)
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
        if (shizukuStatus.isReady) viewModel.setInstallersLocked(true, success = null)
        prefs.setKidsModeActive(true)
        drawerOpen = false
    }

    fun exitKidsMode() {
        if (shizukuStatus.isReady) viewModel.setInstallersLocked(false, success = null)
        prefs.setKidsModeActive(false)
        reloadAll()
        toast("Kids Mode OFF")
    }

    fun toggleFocusMode() = viewModel.toggleFocus()

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
        HomeLauncherPage(
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
            onOpenShizukuShelf = { showSettings = true },
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
                viewModel.trimCaches()
            },
            onOpenDrawer = {
                drawerInitialLetter = null
                drawerOpen = true
            },
            onOpenDrawerAt = { letter ->
                drawerInitialLetter = letter
                drawerOpen = true
            },
            onKidsMode = { enterKidsMode() },
            onToggleFocus = { toggleFocusMode() },
            onToggleGrayscale = {
                viewModel.setGrayscale(!grayscaleOn)
            },
            onEditNote = { showNoteDialog = true },
            onGrantUsageAccess = {
                if (shizukuStatus.isReady) {
                    viewModel.allowUsageStats()
                } else {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            },
            onLaunchApp = { launchApp(it) },
            onLongPressApp = { /* Bottom sheet shortcuts removed */ },
            onOpenDialer = {
                context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            onOpenCamera = {
                context.startActivity(
                    Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )

        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically(tween(250, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(180))
        ) {
            ShizukuShelfPage(
                status = shizukuStatus,
                apps = apps,
                kidsAppCount = kidsApps.size,
                hasParentPin = hasParentPin,
                grayscaleOn = grayscaleOn,
                stayAwakeOn = stayAwakeOn,
                installersLocked = installersLocked,
                ambientDisplayOn = ambientDisplayOn,
                onToggleAmbientDisplay = { next ->
                    viewModel.setAmbientDisplay(next)
                },
                lockscreenNotifMode = lockscreenNotifMode,
                onChangeLockscreenNotifMode = { mode ->
                    viewModel.setLockscreenMode(mode)
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
                    viewModel.setAnimationScale(scale)
                },
                onToggleGrayscale = { next ->
                    viewModel.setGrayscale(next)
                },
                onToggleStayAwake = { next ->
                    viewModel.setStayAwake(next)
                },
                onToggleInstallersLock = { next ->
                    viewModel.setInstallersLocked(next)
                },
                onFreezeToggle = { pkg, freeze ->
                    viewModel.setFrozen(pkg, freeze)
                },
                onUnfreezeAll = {
                    viewModel.unfreezeAll()
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
                    viewModel.trimCaches()
                },
                onSetDefaultLauncher = {
                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                },
                onClose = { showSettings = false }
            )
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(tween(250, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200)) { it / 3 } + fadeOut(tween(180))
        ) {
            AppDrawerOverlay(
                apps = apps,
                showFrozen = showFrozen,
                gridView = settings.drawerGrid,
                autoLaunchSingle = autoLaunchSingle,
                grayscaleOn = grayscaleOn,
                focusActive = focusActive,
                initialLetter = drawerInitialLetter,
                pinnedPackages = pinnedPackages.toSet(),
                focusDistractions = focusDistractions,
                mindfulApps = mindfulPauseApps,
                usageMinutesMap = usageMinutesMap,
                kidsApps = kidsApps,
                onClose = {
                    drawerOpen = false
                    drawerInitialLetter = null
                },
                onToggleShowFrozen = {
                    prefs.setShowFrozenInDrawer(!showFrozen)
                },
                onToggleGridView = { prefs.setDrawerGrid(!settings.drawerGrid) },
                onToggleAutoLaunch = {
                    prefs.setAutoLaunchSingleMatch(!autoLaunchSingle)
                },
                onLaunchApp = { entry ->
                    launchApp(entry.packageName)
                },
                onLongPressApp = { /* Bottom sheet shortcuts removed */ }
            )
        }

        if (pendingApkInstall == null && packageInstallState !is PackageInstallState.Idle) {
            val state = packageInstallState
            LaunchedEffect(state) {
                if (state is PackageInstallState.Success) {
                    kotlinx.coroutines.delay(4500)
                    viewModel.dismissPackageInstallState()
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp, start = 18.dp, end = 18.dp)
            ) {
                Surface(
                    color = SurfaceBlack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderHairline),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        when (state) {
                            is PackageInstallState.Installing -> {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = PureWhite,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Installing ${state.appLabel}...",
                                        fontSize = 11.sp,
                                        color = PureWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            is PackageInstallState.Success -> {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${state.appLabel} installed",
                                        fontSize = 11.sp,
                                        color = PureWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = PureWhite,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable {
                                        viewModel.dismissPackageInstallState()
                                        forceLaunchApp(state.packageName)
                                    }
                                ) {
                                    Text(
                                        "Open",
                                        fontSize = 10.sp,
                                        color = PureBlack,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            is PackageInstallState.Error -> {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        state.reason,
                                        fontSize = 11.sp,
                                        color = PureWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "✕",
                                    fontSize = 11.sp,
                                    color = TextDim,
                                    modifier = Modifier
                                        .clickable { viewModel.dismissPackageInstallState() }
                                        .padding(4.dp)
                                    )
                            }
                            else -> {}
                        }
                    }
                }
            }
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
                val pkg = mindfulPkg
                pendingMindfulPkg = null
                viewModel.startMindfulCooldown(pkg)
                viewModel.onAppLaunchRequested(pkg)
                forceLaunchApp(pkg)
            },
            onDismiss = { pendingMindfulPkg = null }
        )
    }

    val apkInfo = pendingApkInstall
    if (apkInfo != null) {
        val paths = apkInfo.splitPaths.ifEmpty { listOf(apkInfo.path) }
        PackageInstallDialog(
            apkInfo = apkInfo,
            shizukuBound = shizukuStatus.isReady,
            installState = packageInstallState,
            onInstall = { flags ->
                if (shizukuStatus.isReady) {
                    PackageInstallService.startInstall(context, paths, apkInfo.appLabel, apkInfo.packageName, flags)
                } else {
                    viewModel.installPackage(apkInfo.path, apkInfo.appLabel, apkInfo.packageName, flags)
                }
            },
            onUninstallAndInstall = { flags ->
                if (shizukuStatus.isReady) {
                    scope.launch {
                        PackageInstallTracker.update(PackageInstallState.Installing(apkInfo.appLabel, apkInfo.packageName))
                        withContext(Dispatchers.IO) {
                            shizuku.uninstallPackage(apkInfo.packageName)
                        }
                        PackageInstallService.startInstall(context, paths, apkInfo.appLabel, apkInfo.packageName, flags)
                    }
                } else {
                    viewModel.installPackage(apkInfo.path, apkInfo.appLabel, apkInfo.packageName, flags)
                }
            },
            onRunInBackground = {
                pendingApkInstall = null
            },
            onLaunchApp = { pkg ->
                pendingApkInstall = null
                viewModel.dismissPackageInstallState()
                forceLaunchApp(pkg)
            },
            onDismiss = {
                pendingApkInstall = null
                if (packageInstallState !is PackageInstallState.Installing) {
                    viewModel.dismissPackageInstallState()
                }
            }
        )
    }

    val loadingApk = apkLoadingState
    if (loadingApk != null) {
        PackageLoadingDialog(
            loadingState = loadingApk,
            onRunInBackground = {
                val targetUri = currentLoadingUri
                copyApkJob?.cancel()
                apkLoadingState = null
                if (targetUri != null) {
                    PackageInstallService.startAnalysis(context, targetUri, loadingApk.fileName)
                }
            },
            onCancel = {
                copyApkJob?.cancel()
                apkLoadingState = null
            }
        )
    }
    }
}

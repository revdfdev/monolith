package com.rehan.monolith

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AppEntry(
    val packageName: String,
    val label: String,
    val isFrozen: Boolean,
    val isSystem: Boolean,
    val icon: ImageBitmap?
)

private val PureBlack = Color(0xFF000000)
private val PureWhite = Color(0xFFFFFFFF)
private val SurfaceBlack = Color(0xB80A0A0A)
private val BorderHairline = Color(0xFF2C2C2C)
private val TextDim = Color(0xFF888888)
private val MonoFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
private val LocalTileSurface = androidx.compose.runtime.compositionLocalOf { SurfaceBlack }
private val LocalTileBorder = androidx.compose.runtime.compositionLocalOf { BorderHairline }

@Composable
private fun systemBottomSheetContainerColor(): Color {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = isSystemInDarkTheme()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.surfaceContainer
    } else {
        if (isDark) Color(0xFF1E1E1E) else Color(0xFFF2F2F7)
    }
}

@Composable
private fun systemBottomSheetContentColor(): Color {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = isSystemInDarkTheme()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.onSurface
    } else {
        if (isDark) PureWhite else PureBlack
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var shizuku: ShizukuManager
    private lateinit var prefs: LauncherPrefs
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTaskRoot &&
            intent?.action == Intent.ACTION_MAIN &&
            intent?.hasCategory(Intent.CATEGORY_LAUNCHER) == true
        ) {
            finish()
            return
        }
        enableEdgeToEdge()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        shizuku = ShizukuManager().apply { init() }
        prefs = LauncherPrefs(this)
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, 1024)

        handleIncomingApkIntent(intent)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = PureBlack,
                    surface = SurfaceBlack,
                    primary = PureWhite,
                    onPrimary = PureBlack,
                    onBackground = PureWhite,
                    onSurface = PureWhite
                )
            ) {
                MonolithLauncher(
                    shizuku = shizuku,
                    prefs = prefs,
                    appWidgetManager = appWidgetManager,
                    appWidgetHost = appWidgetHost,
                    incomingApkUri = incomingApkUri,
                    onConsumeIncomingApk = { incomingApkUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingApkIntent(intent)
    }

    private var incomingApkUri by mutableStateOf<Uri?>(null)

    private fun handleIncomingApkIntent(intent: Intent?) {
        val uri = intent?.data
        if (intent?.action in listOf(Intent.ACTION_VIEW, Intent.ACTION_INSTALL_PACKAGE) && uri != null) {
            incomingApkUri = uri
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            appWidgetHost.startListening()
        } catch (_: Throwable) {
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            appWidgetHost.stopListening()
        } catch (_: Throwable) {
        }
    }

    override fun onResume() {
        super.onResume()
        shizuku.refreshAndBind()
    }

    override fun onDestroy() {
        super.onDestroy()
        shizuku.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonolithLauncher(
    shizuku: ShizukuManager,
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

    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var pinnedPackages by remember { mutableStateOf(prefs.getPinnedPackages()) }
    var focusDistractions by remember { mutableStateOf(prefs.getFocusDistractions()) }
    var kidsApps by remember { mutableStateOf(prefs.getKidsApps()) }
    var kidsModeActive by remember { mutableStateOf(prefs.isKidsModeActive()) }
    var hasParentPin by remember { mutableStateOf(prefs.hasParentPin()) }
    var quickNote by remember { mutableStateOf(prefs.getQuickNote()) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var homeConfig by remember { mutableStateOf(prefs.getHomeConfig()) }
    var centerWidgets by remember { mutableStateOf(prefs.getCenterWidgets()) }
    var customWidgets by remember { mutableStateOf(prefs.getCustomWidgets()) }
    var showCustomizer by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showWidgetManager by remember { mutableStateOf(false) }
    var leftDockPkg by remember { mutableStateOf(prefs.getLeftDockPkg()) }
    var rightDockPkg by remember { mutableStateOf(prefs.getRightDockPkg()) }
    var dockPickerTarget by remember { mutableStateOf<String?>(null) }
    var systemWidgetIds by remember { mutableStateOf(prefs.getSystemWidgetIds()) }

    val widgetConfigureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == android.app.Activity.RESULT_OK && id != -1) {
            prefs.addSystemWidgetId(id)
            systemWidgetIds = prefs.getSystemWidgetIds()
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
                    systemWidgetIds = prefs.getSystemWidgetIds()
                }
            } else {
                prefs.addSystemWidgetId(id)
                systemWidgetIds = prefs.getSystemWidgetIds()
            }
        } else if (id != -1) {
            appWidgetHost.deleteAppWidgetId(id)
        }
    }
    var showSystemWidgetPicker by remember { mutableStateOf(false) }

    var focusActive by remember { mutableStateOf(prefs.isFocusModeActive()) }
    var grayscaleOn by remember { mutableStateOf(false) }
    var stayAwakeOn by remember { mutableStateOf(false) }
    var lockscreenMsgOn by remember { mutableStateOf(prefs.isLockscreenMessageEnabled()) }
    var lockscreenMsgText by remember { mutableStateOf(prefs.getLockscreenMessage()) }
    var lockscreenNotifMode by remember { mutableStateOf("QUIET") }
    var ambientDisplayOn by remember { mutableStateOf(prefs.isAmbientDisplayEnabled()) }
    var installersLocked by remember { mutableStateOf(false) }
    var showFrozen by remember { mutableStateOf(prefs.isShowFrozenInDrawer()) }
    var autoLaunchSingle by remember { mutableStateOf(prefs.isAutoLaunchSingleMatch()) }
    var selectedAppForSheet by remember { mutableStateOf<AppEntry?>(null) }
    var showKidsAppsPicker by remember { mutableStateOf(false) }
    var enterKidsAfterPick by remember { mutableStateOf(false) }
    var dailyLimits by remember { mutableStateOf(prefs.getDailyLimits()) }
    var mindfulPauseApps by remember { mutableStateOf(prefs.getMindfulPauseApps()) }
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
            val frozenSet = if (shizukuStatus.serviceBound) shizuku.getFrozenPackages() else emptySet()
            loadInstalledApps(context, frozenSet) { loaded ->
                apps = loaded
                if (!prefs.hasSeededDefaults() && pinnedPackages.isEmpty() && loaded.isNotEmpty()) {
                    val preferred = listOf(
                        "com.brave.browser",
                        "com.whatsapp",
                        "md.obsidian",
                        "com.anthropic.claude",
                        "com.openai.chatgpt",
                        "com.github.android",
                        "com.phonepe.app"
                    )
                    val available = loaded.map { it.packageName }.toSet()
                    val seeded = preferred.filter { available.contains(it) }.ifEmpty {
                        loaded.filter { !it.isSystem && !it.isFrozen }.take(5).map { it.packageName }
                    }
                    if (seeded.isNotEmpty()) {
                        prefs.setPinnedPackages(seeded)
                        pinnedPackages = seeded
                    }
                    prefs.markDefaultsSeeded()
                }
            }
            usageMinutesMap = readTodayAppUsageMinutes(context)
            if (shizukuStatus.serviceBound) {
                grayscaleOn = shizuku.isGrayscaleEnabled()
                stayAwakeOn = shizuku.getStayAwake()
                ambientDisplayOn = shizuku.isDozeAlwaysOn()
                lockscreenNotifMode = shizuku.getLockscreenNotificationMode()
            }
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
            if (shizukuStatus.serviceBound) {
                shizuku.setInstallersLocked(true)
            }
            kidsModeActive = true
            prefs.setKidsModeActive(true)
            drawerOpen = false
        }
    }

    fun exitKidsMode() {
        scope.launch {
            if (shizukuStatus.serviceBound) {
                shizuku.setInstallersLocked(false)
            }
            kidsModeActive = false
            prefs.setKidsModeActive(false)
            reloadAll()
            toast("Kids Mode OFF")
        }
    }

    fun toggleFocusMode() {
        scope.launch {
            if (!shizukuStatus.serviceBound) {
                shizuku.requestPermission()
                toast("Grant Shizuku permission first")
                return@launch
            }
            val next = !focusActive
            shizuku.setGrayscale(next)
            grayscaleOn = next
            shizuku.exec(if (next) "cmd notification set_dnd alarms" else "cmd notification set_dnd off")
            for (pkg in focusDistractions) {
                if (next) {
                    shizuku.forceStop(pkg)
                    shizuku.freezePackage(pkg)
                } else {
                    shizuku.unfreezePackage(pkg)
                }
            }
            focusActive = next
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
        // Subtle Sailfish-style top page indicator
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
                    lockscreenMsgOn = lockscreenMsgOn,
                    lockscreenMsgText = lockscreenMsgText,
                    onToggleLockscreenMsg = { next ->
                        scope.launch {
                            shizuku.setLockscreenOwnerInfo(next, lockscreenMsgText)
                            lockscreenMsgOn = next
                            prefs.setLockscreenMessageEnabled(next)
                            toast(if (next) "Lockscreen message set" else "Lockscreen message removed")
                        }
                    },
                    onSaveLockscreenMsg = { newMsg ->
                        scope.launch {
                            lockscreenMsgText = newMsg
                            prefs.setLockscreenMessage(newMsg)
                            if (lockscreenMsgOn) {
                                shizuku.setLockscreenOwnerInfo(true, newMsg)
                            }
                            toast("Lockscreen message saved")
                        }
                    },
                    onSetOledBlackLockscreen = {
                        val wm = android.app.WallpaperManager.getInstance(context)
                        try {
                            val bmp = android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888).apply {
                                eraseColor(android.graphics.Color.BLACK)
                            }
                            wm.setBitmap(bmp, null, true, android.app.WallpaperManager.FLAG_LOCK)
                            toast("Pitch black lockscreen wallpaper applied")
                        } catch (e: Exception) {
                            toast("Wallpaper set failed: ${e.message}")
                        }
                    },
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
                            hasParentPin = true
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
                            if (shizukuStatus.serviceBound) {
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
                    },
                    onExec = { cmd -> shizuku.exec(cmd) }
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
                    customWidgets = customWidgets,
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
                        systemWidgetIds = prefs.getSystemWidgetIds()
                    },
                    onExecShell = { cmd -> shizuku.exec(cmd) },
                    onTrimCaches = {
                        scope.launch {
                            if (shizukuStatus.serviceBound) {
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
                            if (!shizukuStatus.serviceBound) {
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
                            if (shizukuStatus.serviceBound) {
                                shizuku.exec("appops set ${BuildConfig.APPLICATION_ID} GET_USAGE_STATS allow")
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
                shizukuBound = shizukuStatus.serviceBound,
                onClose = { drawerOpen = false },
                onToggleShowFrozen = {
                    showFrozen = !showFrozen
                    prefs.setShowFrozenInDrawer(showFrozen)
                },
                onToggleAutoLaunch = {
                    autoLaunchSingle = !autoLaunchSingle
                    prefs.setAutoLaunchSingleMatch(autoLaunchSingle)
                },
                onLaunchApp = { entry ->
                    if (entry.isFrozen) {
                        selectedAppForSheet = entry
                    } else {
                        launchApp(entry.packageName)
                    }
                },
                onLongPressApp = { selectedAppForSheet = it },
                onExecShell = { cmd -> shizuku.exec(cmd) }
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
                        quickNote = noteDraft.trim()
                        prefs.setQuickNote(quickNote)
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
                    pinnedPackages = prefs.getPinnedPackages()
                    selectedAppForSheet = null
                }

                SheetAction(
                    icon = Icons.Rounded.ChildCare,
                    label = if (isKids) "Remove from Kids Mode" else "Add to Kids Mode"
                ) {
                    prefs.toggleKidsApp(sheetApp.packageName)
                    kidsApps = prefs.getKidsApps()
                    selectedAppForSheet = null
                }

                SheetAction(
                    icon = Icons.Rounded.DoNotDisturbOn,
                    label = if (isDistraction) "Remove from Focus Freeze" else "Add to Focus Freeze"
                ) {
                    prefs.toggleFocusDistraction(sheetApp.packageName)
                    focusDistractions = prefs.getFocusDistractions()
                    selectedAppForSheet = null
                }

                if (shizukuStatus.serviceBound) {
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
                    mindfulPauseApps = prefs.getMindfulPauseApps()
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
                                dailyLimits = prefs.getDailyLimits()
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
                homeConfig = updated
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
            customWidgets = customWidgets,
            onSaveWidgets = { updated ->
                centerWidgets = updated
                prefs.saveCenterWidgets(updated)
            },
            onAddCustomWidget = { title, cmd ->
                prefs.addCustomWidget(title, cmd)
                customWidgets = prefs.getCustomWidgets()
                centerWidgets = prefs.getCenterWidgets()
            },
            onRemoveCustomWidget = { id ->
                prefs.removeCustomWidget(id)
                customWidgets = prefs.getCustomWidgets()
                centerWidgets = prefs.getCenterWidgets()
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
                            systemWidgetIds = prefs.getSystemWidgetIds()
                        }
                    } else {
                        prefs.addSystemWidgetId(id)
                        systemWidgetIds = prefs.getSystemWidgetIds()
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
                    leftDockPkg = pkg
                } else {
                    prefs.setRightDockPkg(pkg)
                    rightDockPkg = pkg
                }
                dockPickerTarget = null
            },
            onResetDefault = {
                if (dockTarget == "LEFT") {
                    prefs.setLeftDockPkg(null)
                    leftDockPkg = null
                } else {
                    prefs.setRightDockPkg(null)
                    rightDockPkg = null
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
                pinnedPackages = prefs.getPinnedPackages()
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
                kidsApps = prefs.getKidsApps()
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
            shizukuBound = shizukuStatus.serviceBound,
            onInstall = { flags ->
                scope.launch {
                    val cmd = "pm install ${flags.joinToString(" ")} \"${apkInfo.path}\""
                    val r = shizuku.exec(cmd)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeLauncherPage(
    shizukuStatus: ShizukuStatus,
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
    customWidgets: List<CustomWidgetSpec>,
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
    onExecShell: suspend (String) -> ShellResult,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                            .background(if (shizukuStatus.serviceBound) PureWhite else TextDim)
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

        // Lumia / Sailfish Square-Tile Grid (Clock + Battery + Chosen Widgets + System Widgets + App Tiles)
        val deckItems = centerWidgets.filterNot { it == "APPS_GRID" }
        val showAppsTiles = centerWidgets.contains("APPS_GRID")

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
                        else -> {
                            val custom = customWidgets.firstOrNull { it.id == wid }
                            if (custom != null) {
                                CustomCenterWidget(custom, shizukuStatus.serviceBound, onExecShell)
                            }
                        }
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

            // Lumia / Sailfish App Tiles
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerOverlay(
    apps: List<AppEntry>,
    showFrozen: Boolean,
    autoLaunchSingle: Boolean,
    grayscaleOn: Boolean,
    pinnedPackages: Set<String>,
    focusDistractions: Set<String>,
    mindfulApps: Set<String> = emptySet(),
    usageMinutesMap: Map<String, Int> = emptyMap(),
    kidsApps: Set<String>,
    shizukuBound: Boolean,
    focusActive: Boolean = false,
    onClose: () -> Unit,
    onToggleShowFrozen: () -> Unit,
    onToggleAutoLaunch: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onExecShell: suspend (String) -> ShellResult
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var shellOutput by remember { mutableStateOf("") }

    val isShellMode = query.startsWith(">")
    val filtered = remember(apps, query, showFrozen, focusActive) {
        if (query.startsWith(">")) {
            emptyList()
        } else {
            apps.filter { entry ->
                ((!focusActive && showFrozen) || !entry.isFrozen) &&
                    (query.isBlank() ||
                        entry.label.contains(query, ignoreCase = true) ||
                        entry.packageName.contains(query, ignoreCase = true))
            }
        }
    }

    LaunchedEffect(filtered, query, autoLaunchSingle) {
        if (autoLaunchSingle && !isShellMode && query.length >= 2 && filtered.size == 1 && !filtered[0].isFrozen) {
            onLaunchApp(filtered[0])
            query = ""
        }
    }

    var drawerGrid by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (focusActive) Color(0xF8000000) else Color(0xEB050505))
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        "Search apps...",
                        fontSize = 14.sp,
                        color = TextDim
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        if (isShellMode) Icons.Rounded.Terminal else Icons.Rounded.Search,
                        contentDescription = null,
                        tint = PureWhite
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { if (query.isNotEmpty()) query = "" else onClose() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextDim)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (isShellMode && shizukuBound) {
                            val cmd = query.removePrefix(">").trim()
                            if (cmd.isNotEmpty()) {
                                scope.launch {
                                    val r = onExecShell(cmd)
                                    shellOutput = "$ $cmd\n${r.output.ifBlank { "(exit ${r.exitCode})" }}"
                                }
                            }
                        } else if (filtered.isNotEmpty() && !filtered[0].isFrozen) {
                            onLaunchApp(filtered[0])
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = LocalTileBorder.current,
                    focusedContainerColor = LocalTileSurface.current,
                    unfocusedContainerColor = LocalTileSurface.current,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
        }

        if (shellOutput.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = LocalTileSurface.current,
                contentColor = PureWhite,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LocalTileBorder.current),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("OUTPUT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                        Text("CLEAR", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = PureWhite, modifier = Modifier.clickable { shellOutput = "" })
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(shellOutput, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = PureWhite, maxLines = 10)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (!focusActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniModePill(if (drawerGrid) "GRID" else "LIST", true, { drawerGrid = !drawerGrid }, Modifier.weight(1f))
                MiniModePill("FROZEN", showFrozen, onToggleShowFrozen, Modifier.weight(1f))
                MiniModePill("1-MATCH", autoLaunchSingle, onToggleAutoLaunch, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))
        }

        if (focusActive) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onLaunchApp(app) }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            app.label,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else if (drawerGrid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 18.dp),
                modifier = Modifier.weight(1f)
            ) {
                gridItems(filtered, key = { it.packageName }) { app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) }
                            )
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (app.icon != null) {
                            Box {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.label,
                                    colorFilter = if (grayscaleOn) MonoFilter else null,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(13.dp))
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
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            app.label,
                            fontSize = 11.sp,
                            color = if (app.isFrozen) TextDim else PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val isPinned = pinnedPackages.contains(app.packageName)
                    val isFocus = focusDistractions.contains(app.packageName)
                    val isKids = kidsApps.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) }
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = null,
                                colorFilter = if (grayscaleOn) MonoFilter else null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            app.label,
                            fontSize = 15.sp,
                            color = if (app.isFrozen) TextDim else PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            val usedMins = usageMinutesMap[app.packageName] ?: 0
                            if (usedMins > 0) {
                                Text(
                                    "${usedMins}m",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextDim
                                )
                            }
                            if (mindfulApps.contains(app.packageName)) MonoBadge("MINDFUL")
                            if (isPinned) MonoBadge("PIN")
                            if (isKids) MonoBadge("KIDS")
                            if (isFocus) MonoBadge("FOCUS")
                            if (app.isFrozen) MonoBadge("FROZEN")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShizukuShelfPage(
    status: ShizukuStatus,
    apps: List<AppEntry>,
    kidsAppCount: Int,
    hasParentPin: Boolean,
    grayscaleOn: Boolean,
    stayAwakeOn: Boolean,
    installersLocked: Boolean,
    lockscreenMsgOn: Boolean,
    lockscreenMsgText: String,
    onToggleLockscreenMsg: (Boolean) -> Unit,
    onSaveLockscreenMsg: (String) -> Unit,
    onSetOledBlackLockscreen: () -> Unit,
    ambientDisplayOn: Boolean,
    onToggleAmbientDisplay: (Boolean) -> Unit,
    lockscreenNotifMode: String,
    onChangeLockscreenNotifMode: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onEnterKidsMode: () -> Unit,
    onConfigureKidsApps: () -> Unit,
    focusActive: Boolean,
    onToggleFocus: () -> Unit,
    onSaveParentPin: (String) -> Unit,
    onSetAnimScale: (Float) -> Unit,
    onToggleGrayscale: (Boolean) -> Unit,
    onToggleStayAwake: (Boolean) -> Unit,
    onToggleInstallersLock: (Boolean) -> Unit,
    onFreezeToggle: (String, Boolean) -> Unit,
    onUnfreezeAll: () -> Unit,
    onUninstallApp: (String) -> Unit,
    onInstallApkUri: (Uri) -> Unit,
    onTrimCaches: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onExec: suspend (String) -> ShellResult
) {
    val scope = rememberCoroutineScope()
    var showPinWizard by remember { mutableStateOf(false) }
    var enterKidsAfterPin by remember { mutableStateOf(false) }

    var freezerExpanded by remember { mutableStateOf(false) }
    var freezerQuery by remember { mutableStateOf("") }
    var installerExpanded by remember { mutableStateOf(false) }
    var uninstallQuery by remember { mutableStateOf("") }
    var confirmUninstallPkg by remember { mutableStateOf<AppEntry?>(null) }

    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onInstallApkUri(uri)
    }

    val frozenCount = remember(apps) { apps.count { it.isFrozen } }
    val userApps = remember(apps) { apps.filter { !it.isSystem } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SETTINGS", fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, color = TextDim)

        // Modes Card (moved from Home)
        Surface(
            color = LocalTileSurface.current,
        contentColor = PureWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LocalTileBorder.current),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("MODES", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniModePill("KIDS", false, onEnterKidsMode, Modifier.weight(1f))
                    MiniModePill("FOCUS", focusActive, onToggleFocus, Modifier.weight(1f))
                    MiniModePill("MONO", grayscaleOn, { onToggleGrayscale(!grayscaleOn) }, Modifier.weight(1f))
                }
            }
        }

        Surface(
            color = SurfaceBlack,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (status.serviceBound) PureWhite else BorderHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    if (status.serviceBound) "Enhanced Controls: Active" else "Enhanced Controls: Off",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PureWhite
                )
                Text(
                    if (status.serviceBound) "App freezing, installer lock & system tweaks ready" else "Grant Shizuku access to unlock app freezing & system tweaks",
                    fontSize = 11.sp,
                    color = TextDim
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!status.serviceBound) {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
                        ) {
                            Text("Enable")
                        }
                    }
                    OutlinedButton(onClick = onRefresh, border = BorderStroke(1.dp, BorderHairline)) {
                        Text("Refresh", color = PureWhite)
                    }
                    val ctx = LocalContext.current
                    val isDefaultHome = remember(status) {
                        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        val resolved = ctx.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                        resolved?.activityInfo?.packageName == ctx.packageName
                    }
                    OutlinedButton(onClick = onSetDefaultLauncher, border = BorderStroke(1.dp, BorderHairline)) {
                        Text(if (isDefaultHome) "Default Home ✓" else "Set Default Home", color = if (isDefaultHome) TextDim else PureWhite)
                    }
                }
            }
        }

        Surface(
            color = LocalTileSurface.current,
        contentColor = PureWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LocalTileBorder.current),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("KIDS SPACE ($kidsAppCount APPS)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (hasParentPin) {
                                onEnterKidsMode()
                            } else {
                                enterKidsAfterPin = true
                                showPinWizard = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.ChildCare, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Enter Kids")
                    }
                    OutlinedButton(
                        onClick = onConfigureKidsApps,
                        border = BorderStroke(1.dp, BorderHairline),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Choose Apps", color = PureWhite)
                    }
                }
                HorizontalDivider(color = BorderHairline)
                OutlinedButton(
                    onClick = { showPinWizard = true },
                    border = BorderStroke(1.dp, BorderHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Change Parent PIN (••••)", color = PureWhite)
                }
            }
        }

        if (showPinWizard) {
            PinSetupWizardDialog(
                onPinConfirmed = { newPin ->
                    onSaveParentPin(newPin)
                    showPinWizard = false
                    if (enterKidsAfterPin) {
                        enterKidsAfterPin = false
                        onEnterKidsMode()
                    }
                },
                onDismiss = {
                    showPinWizard = false
                    enterKidsAfterPin = false
                }
            )
        }

        // Separate Settings Section 1: App Freezer
        Surface(
            color = LocalTileSurface.current,
        contentColor = PureWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LocalTileBorder.current),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { freezerExpanded = !freezerExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("APP FREEZER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = PureWhite)
                        Text("$frozenCount frozen • Tap to ${if (freezerExpanded) "collapse" else "manage"}", fontSize = 11.sp, color = TextDim)
                    }
                    if (frozenCount > 0) {
                        OutlinedButton(
                            onClick = onUnfreezeAll,
                            enabled = status.serviceBound,
                            border = BorderStroke(1.dp, BorderHairline)
                        ) {
                            Text("Unfreeze All", fontSize = 11.sp, color = PureWhite)
                        }
                    }
                }
                if (freezerExpanded) {
                    OutlinedTextField(
                        value = freezerQuery,
                        onValueChange = { freezerQuery = it },
                        placeholder = { Text("Filter apps to freeze/unfreeze...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val freezeList = remember(userApps, freezerQuery) {
                        userApps.filter {
                            freezerQuery.isBlank() || it.label.contains(freezerQuery, ignoreCase = true)
                        }.sortedByDescending { it.isFrozen }.take(30)
                    }
                    freezeList.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, fontSize = 14.sp, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (app.isFrozen) "Frozen" else "Active", fontSize = 11.sp, color = TextDim)
                            }
                            Switch(
                                checked = app.isFrozen,
                                onCheckedChange = { freeze -> onFreezeToggle(app.packageName, freeze) },
                                enabled = status.serviceBound,
                                colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = PureWhite)
                            )
                        }
                    }
                }
            }
        }

        // Separate Settings Section 2: Install & Uninstall Apps
        Surface(
            color = LocalTileSurface.current,
        contentColor = PureWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LocalTileBorder.current),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { installerExpanded = !installerExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("INSTALL & UNINSTALL", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = PureWhite)
                        Text("APK install, lock stores, uninstall • Tap to ${if (installerExpanded) "collapse" else "manage"}", fontSize = 11.sp, color = TextDim)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Lock Stores & Package Installer", fontSize = 13.sp)
                    Switch(
                        checked = installersLocked,
                        onCheckedChange = onToggleInstallersLock,
                        enabled = status.serviceBound,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = PureWhite)
                    )
                }
                Button(
                    onClick = { apkPicker.launch("application/vnd.android.package-archive") },
                    enabled = status.serviceBound,
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Install APK via Shizuku")
                }
                if (installerExpanded) {
                    HorizontalDivider(color = BorderHairline)
                    OutlinedTextField(
                        value = uninstallQuery,
                        onValueChange = { uninstallQuery = it },
                        placeholder = { Text("Search user apps to uninstall...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val uninstallList = remember(userApps, uninstallQuery) {
                        userApps.filter {
                            uninstallQuery.isBlank() || it.label.contains(uninstallQuery, ignoreCase = true)
                        }.take(25)
                    }
                    uninstallList.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, fontSize = 14.sp, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.packageName, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { confirmUninstallPkg = app },
                                border = BorderStroke(1.dp, BorderHairline)
                            ) {
                                Text("Uninstall", fontSize = 11.sp, color = PureWhite)
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = LocalTileSurface.current,
        contentColor = PureWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LocalTileBorder.current),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SYSTEM TWEAKS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Grayscale Display", fontSize = 14.sp)
                    Switch(
                        checked = grayscaleOn,
                        onCheckedChange = onToggleGrayscale,
                        enabled = status.serviceBound,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = PureWhite)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Stay Awake on USB", fontSize = 14.sp)
                    Switch(
                        checked = stayAwakeOn,
                        onCheckedChange = onToggleStayAwake,
                        enabled = status.serviceBound,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = PureWhite)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Lockscreen Notification Mode", fontSize = 14.sp)
                    Text("Control native Android lockscreen notification density", fontSize = 11.sp, color = TextDim)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "CLEAN" to "Clean (Zero Cards)",
                            "QUIET" to "Quiet (Masked)",
                            "FULL" to "Full"
                        ).forEach { (mode, label) ->
                            val isSelected = lockscreenNotifMode == mode
                            Surface(
                                color = if (isSelected) PureWhite else PureBlack,
                                contentColor = if (isSelected) PureBlack else PureWhite,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) PureWhite else BorderHairline),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = status.serviceBound) {
                                        onChangeLockscreenNotifMode(mode)
                                    }
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Native Ambient Display (Doze AOD)", fontSize = 14.sp)
                        Text("Hardware-accelerated OLED ambient clock & notifications", fontSize = 11.sp, color = TextDim)
                    }
                    Switch(
                        checked = ambientDisplayOn,
                        onCheckedChange = onToggleAmbientDisplay,
                        enabled = status.serviceBound,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = PureWhite)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lockscreen Intention Message", fontSize = 14.sp)
                        Text("Display a custom focus motto on native lockscreen", fontSize = 11.sp, color = TextDim)
                    }
                    Switch(
                        checked = lockscreenMsgOn,
                        onCheckedChange = onToggleLockscreenMsg,
                        enabled = status.serviceBound,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = PureWhite)
                    )
                }
                if (lockscreenMsgOn) {
                    var draftMsg by remember(lockscreenMsgText) { mutableStateOf(lockscreenMsgText) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draftMsg,
                            onValueChange = { draftMsg = it },
                            placeholder = { Text("Motto (e.g. Stay intentional)", color = TextDim) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = BorderHairline,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { onSaveLockscreenMsg(draftMsg.trim()) },
                            enabled = status.serviceBound,
                            border = BorderStroke(1.dp, BorderHairline)
                        ) {
                            Text("Save", color = PureWhite)
                        }
                    }
                }
                OutlinedButton(
                    onClick = onSetOledBlackLockscreen,
                    border = BorderStroke(1.dp, BorderHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set Lockscreen Wallpaper to Pitch Black", color = PureWhite)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0f, 0.5f, 1.0f).forEach { scale ->
                        OutlinedButton(
                            onClick = { onSetAnimScale(scale) },
                            enabled = status.serviceBound,
                            border = BorderStroke(1.dp, BorderHairline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${scale}x", color = PureWhite)
                        }
                    }
                }
                OutlinedButton(
                    onClick = onTrimCaches,
                    enabled = status.serviceBound,
                    border = BorderStroke(1.dp, BorderHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = PureWhite)
                    Spacer(Modifier.width(6.dp))
                    Text("Trim All Caches (999G)", color = PureWhite)
                }
            }
        }
    }

    val toUninstall = confirmUninstallPkg
    if (toUninstall != null) {
        AlertDialog(
            onDismissRequest = { confirmUninstallPkg = null },
            containerColor = SurfaceBlack,
            title = { Text("Uninstall ${toUninstall.label}?", color = PureWhite) },
            text = { Text(toUninstall.packageName, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextDim) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmUninstallPkg = null
                        onUninstallApp(toUninstall.packageName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
                ) {
                    Text("Uninstall")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUninstallPkg = null }) {
                    Text("Cancel", color = TextDim)
                }
            }
        )
    }
}

@Composable
fun KidsModeLauncherScreen(
    kidsApps: List<AppEntry>,
    verifyPin: (String) -> Boolean,
    onLaunchApp: (String) -> Unit,
    onExitKidsMode: () -> Unit,
    onWrongPin: () -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Kids Space", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                Text("Play safely • Installs locked", fontSize = 12.sp, color = TextDim)
            }
            OutlinedButton(
                onClick = {
                    enteredPin = ""
                    showPinDialog = true
                },
                border = BorderStroke(1.dp, BorderHairline)
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Exit", color = PureWhite, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(22.dp))

        if (kidsApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No apps added to Kids Space yet.", color = TextDim, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                gridItems(kidsApps, key = { it.packageName }) { app ->
                    Surface(
                        color = SurfaceBlack,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BorderHairline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .clickable { onLaunchApp(app.packageName) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Text(
                                app.label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = PureWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        PinKeypadDialog(
            title = "Enter Parent PIN",
            subtitle = "4-digit PIN to exit Kids Space",
            expectedLength = 4,
            errorMessage = null,
            onPinEntered = { code ->
                if (verifyPin(code)) {
                    showPinDialog = false
                    onExitKidsMode()
                } else {
                    onWrongPin()
                }
            },
            onDismiss = { showPinDialog = false }
        )
    }
}

@Composable
private fun PinSetupWizardDialog(
    onPinConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var firstPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    PinKeypadDialog(
        title = if (step == 1) "Step 1 of 2 • New PIN" else "Step 2 of 2 • Confirm PIN",
        subtitle = if (step == 1) "Choose a 4-digit Parent PIN" else "Re-enter the 4-digit PIN to confirm",
        expectedLength = 4,
        errorMessage = errorMsg,
        onPinEntered = { code ->
            if (step == 1) {
                firstPin = code
                errorMsg = null
                step = 2
            } else {
                if (code == firstPin) {
                    onPinConfirmed(code)
                } else {
                    errorMsg = "PINs didn't match — try again"
                    step = 1
                    firstPin = ""
                }
            }
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun PinKeypadDialog(
    title: String,
    subtitle: String,
    expectedLength: Int,
    errorMessage: String?,
    onPinEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember(title) { mutableStateOf("") }

    fun pressDigit(d: String) {
        if (pin.length >= expectedLength) return
        val next = pin + d
        if (next.length == expectedLength) {
            pin = ""
            onPinEntered(next)
        } else {
            pin = next
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBlack,
        titleContentColor = PureWhite,
        textContentColor = PureWhite,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = TextDim)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(expectedLength) { idx ->
                        val filled = idx < pin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (filled) PureWhite else Color.Transparent)
                                .border(1.5.dp, if (filled) PureWhite else TextDim, CircleShape)
                        )
                    }
                }
                if (errorMessage != null) {
                    Text(errorMessage, fontSize = 11.sp, color = PureWhite)
                }
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { key ->
                                Surface(
                                    color = PureBlack,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, BorderHairline),
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            when (key) {
                                                "C" -> pin = ""
                                                "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                else -> pressDigit(key)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            key,
                                            fontSize = if (key == "C" || key == "⌫") 16.sp else 20.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (key == "C" || key == "⌫") TextDim else PureWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextDim) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCustomizerSheet(
    config: HomeConfig,
    onUpdate: (HomeConfig) -> Unit,
    onPickHomeApps: () -> Unit,
    onManageWidgets: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = systemBottomSheetContainerColor(),
        contentColor = systemBottomSheetContentColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("CUSTOMIZE HOME", fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, color = TextDim)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onManageWidgets,
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Widget Manager", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onPickHomeApps,
                    border = BorderStroke(1.dp, BorderHairline),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Home Apps", fontSize = 12.sp, color = PureWhite)
                }
            }

            Text("COLOR MODE (NORMAL)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniModePill("DYNAMIC", config.colorMode == "DYNAMIC", { onUpdate(config.copy(colorMode = "DYNAMIC")) }, Modifier.weight(1f))
                MiniModePill("OLED", config.colorMode == "OLED", { onUpdate(config.copy(colorMode = "OLED")) }, Modifier.weight(1f))
            }

            if (config.colorMode == "OLED" || config.ambienceStyle == "AMOLED") {
                Text("AMOLED ACCENT COLOR", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val accents = listOf(
                        "WHITE" to PureWhite,
                        "CYAN" to Color(0xFF00E5FF),
                        "EMERALD" to Color(0xFF00E676),
                        "AMBER" to Color(0xFFFFB300),
                        "ROSE" to Color(0xFFFF4081),
                        "PURPLE" to Color(0xFFD500F9),
                        "ORANGE" to Color(0xFFFF6D00)
                    )
                    accents.forEach { (name, color) ->
                        val isSelected = config.amoledAccent.equals(name, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) color.copy(alpha = 0.25f) else Color(0x331E1E1E))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else BorderHairline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onUpdate(config.copy(amoledAccent = name)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }

            CustomToggleRow("Wallpaper Blur", config.blurBackground) { onUpdate(config.copy(blurBackground = it)) }

            Text("AMBIENCE GRADIENT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SAILFISH", "MIST", "AMOLED").forEach { style ->
                    MiniModePill(
                        label = style,
                        active = config.ambienceStyle == style,
                        onClick = { onUpdate(config.copy(ambienceStyle = style)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("TILE FEEL", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("GLASS", "SOLID", "OUTLINE").forEach { style ->
                    MiniModePill(
                        label = style,
                        active = config.tileStyle == style,
                        onClick = { onUpdate(config.copy(tileStyle = style)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("APPS LAYOUT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniModePill("GRID", config.pinnedLayout == "GRID", { onUpdate(config.copy(pinnedLayout = "GRID")) }, Modifier.weight(1f))
                MiniModePill("LIST", config.pinnedLayout == "LIST", { onUpdate(config.copy(pinnedLayout = "LIST")) }, Modifier.weight(1f))
            }

            Text("CLOCK STYLE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("STACKED", "DIGITAL", "MINIMAL").forEach { style ->
                    MiniModePill(
                        label = style,
                        active = config.clockStyle == style,
                        onClick = { onUpdate(config.copy(clockStyle = style)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("BATTERY INDICATOR", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("RING", "BAR", "PILL").forEach { style ->
                    MiniModePill(
                        label = style,
                        active = config.batteryStyle == style,
                        onClick = { onUpdate(config.copy(batteryStyle = style)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("ALIGNMENT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniModePill("LEFT", !config.alignCenter, { onUpdate(config.copy(alignCenter = false)) }, Modifier.weight(1f))
                MiniModePill("CENTER", config.alignCenter, { onUpdate(config.copy(alignCenter = true)) }, Modifier.weight(1f))
            }

            HorizontalDivider(color = BorderHairline)
            CustomToggleRow("24-Hour Clock", config.is24Hour) { onUpdate(config.copy(is24Hour = it)) }
            CustomToggleRow("Show Clock", config.showClock) { onUpdate(config.copy(showClock = it)) }
            CustomToggleRow("Show Battery & System", config.showBattery) { onUpdate(config.copy(showBattery = it)) }
            CustomToggleRow("Show Modes Strip", config.showModes) { onUpdate(config.copy(showModes = it)) }
            CustomToggleRow("Show Pomodoro Ring", config.showTimer) { onUpdate(config.copy(showTimer = it)) }
            CustomToggleRow("Show Stopwatch", config.showStopwatch) { onUpdate(config.copy(showStopwatch = it)) }
            CustomToggleRow("Show Screen Time & Alarm", config.showUsageAlarm) { onUpdate(config.copy(showUsageAlarm = it)) }
            CustomToggleRow("Show Quick Actions", config.showQuickActions) { onUpdate(config.copy(showQuickActions = it)) }
            CustomToggleRow("Show Scratchpad", config.showNote) { onUpdate(config.copy(showNote = it)) }
            CustomToggleRow("Show App Icons on Home", config.showHomeIcons) { onUpdate(config.copy(showHomeIcons = it)) }
            val ctx = LocalContext.current
            OutlinedButton(
                onClick = {
                    val intent = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choose Ambience Wallpaper")
                    ctx.startActivity(intent)
                },
                border = BorderStroke(1.dp, BorderHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Ambience Wallpaper", color = PureWhite)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FocusRingWidget(
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
private fun StopwatchCenterWidget(
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
private fun AlarmUsageCenterWidget(
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
private fun QuickActionsCenterWidget(
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
private fun CustomCenterWidget(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetManagerSheet(
    activeWidgets: List<String>,
    customWidgets: List<CustomWidgetSpec>,
    onSaveWidgets: (List<String>) -> Unit,
    onAddCustomWidget: (String, String) -> Unit,
    onRemoveCustomWidget: (String) -> Unit,
    onAddSystemWidget: () -> Unit,
    onDismiss: () -> Unit
) {
    val catalog = remember {
        listOf(
            "APPS_GRID" to "Apps Grid",
            "FOCUS_5217" to "52 / 17 Deep Work Ring",
            "POMODORO" to "Pomodoro 25 / 5 Ring",
            "STOPWATCH" to "Stopwatch",
            "ALARM_USAGE" to "Screen Time & Alarm",
            "USAGE_LIMITS" to "App Usage Cover (Curbox)",
            "QUICK_ACTIONS" to "Quick Actions",
            "NOTES" to "Scratchpad"
        )
    }
    var customTitle by remember { mutableStateOf("") }
    var customCmd by remember { mutableStateOf("") }

    fun labelFor(id: String): String {
        return catalog.firstOrNull { it.first == id }?.second
            ?: customWidgets.firstOrNull { it.id == id }?.title
            ?: id
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = systemBottomSheetContainerColor(),
        contentColor = systemBottomSheetContentColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("WIDGET MANAGER", fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, color = TextDim)
            Button(
                onClick = onAddSystemWidget,
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add System Widget (Android)")
            }

            Text("ACTIVE DECK", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            activeWidgets.forEachIndexed { idx, id ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderHairline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${idx + 1}. ${labelFor(id)}", fontSize = 13.sp, color = PureWhite, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (idx > 0) {
                            Text("▲", fontSize = 12.sp, color = PureWhite, modifier = Modifier.clickable {
                                val list = activeWidgets.toMutableList()
                                val tmp = list[idx - 1]; list[idx - 1] = list[idx]; list[idx] = tmp
                                onSaveWidgets(list)
                            }.padding(4.dp))
                        }
                        if (activeWidgets.size > 1) {
                            Text("✕", fontSize = 12.sp, color = TextDim, modifier = Modifier.clickable {
                                if (id.startsWith("CUSTOM_")) onRemoveCustomWidget(id)
                                else onSaveWidgets(activeWidgets.filterNot { it == id })
                            }.padding(4.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderHairline)
            Text("ADD BUILT-IN WIDGET", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            catalog.filterNot { activeWidgets.contains(it.first) }.forEach { (id, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSaveWidgets(activeWidgets + id) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, fontSize = 13.sp, color = PureWhite)
                    Text("+ ADD", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = PureWhite)
                }
            }

            HorizontalDivider(color = BorderHairline)
            Text("CREATE CUSTOM WIDGET", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                placeholder = { Text("Title (e.g. Uptime)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = customCmd,
                onValueChange = { customCmd = it },
                placeholder = { Text("Shell cmd or text (e.g. uptime -p)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (customTitle.isNotBlank() && customCmd.isNotBlank()) {
                        onAddCustomWidget(customTitle, customCmd)
                        customTitle = ""
                        customCmd = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Custom Widget")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class SystemWidgetItem(
    val info: AppWidgetProviderInfo,
    val appLabel: String,
    val widgetLabel: String,
    val icon: ImageBitmap?
)

@Composable
private fun SystemWidgetPickerDialog(
    appWidgetManager: AppWidgetManager,
    onSelectProvider: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var query by remember { mutableStateOf("") }
    val allWidgets = remember {
        appWidgetManager.installedProviders.mapNotNull { info ->
            val pkg = info.provider.packageName
            val appLabel = try {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (_: Throwable) {
                pkg
            }
            val widgetLabel = try {
                info.loadLabel(pm).ifBlank { appLabel }
            } catch (_: Throwable) {
                appLabel
            }
            val iconBmp = try {
                val d = info.loadIcon(context, 0) ?: pm.getApplicationIcon(pkg)
                drawableToImageBitmap(d)
            } catch (_: Throwable) {
                null
            }
            SystemWidgetItem(info, appLabel, widgetLabel, iconBmp)
        }.sortedWith(compareBy({ it.appLabel.lowercase(Locale.getDefault()) }, { it.widgetLabel.lowercase(Locale.getDefault()) }))
    }
    val filtered = remember(query, allWidgets) {
        if (query.isBlank()) allWidgets
        else allWidgets.filter {
            it.appLabel.contains(query, ignoreCase = true) ||
                it.widgetLabel.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBlack,
        titleContentColor = PureWhite,
        textContentColor = PureWhite,
        title = {
            Text("SYSTEM WIDGETS (${allWidgets.size})", fontSize = 13.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search Clock, Weather, Spotify...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.info.provider.flattenToString() }) { item ->
                        val sizeTag = if (item.info.minWidth >= 180) "2×1" else "1×1"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                                .clickable { onSelectProvider(item.info) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.icon != null) {
                                Image(
                                    bitmap = item.icon,
                                    contentDescription = item.widgetLabel,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.widgetLabel, fontSize = 14.sp, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.appLabel, fontSize = 11.sp, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(sizeTag, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextDim) }
        }
    )
}

@Composable
private fun PickSingleAppDialog(
    title: String,
    apps: List<AppEntry>,
    onSelect: (String) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    var q by remember { mutableStateOf("") }
    val filtered = remember(apps, q) {
        apps.filter { !it.isFrozen && (q.isBlank() || it.label.contains(q, ignoreCase = true)) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBlack,
        title = { Text(title, color = PureWhite) },
        text = {
            Column(modifier = Modifier.height(340.dp)) {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    placeholder = { Text("Filter apps...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(app.packageName) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.label, fontSize = 14.sp, color = PureWhite)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onResetDefault) {
                Text("Reset Default", color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextDim)
            }
        }
    )
}

@Composable
private fun QuickActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        color = SurfaceBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderHairline),
        modifier = Modifier
            .size(38.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = PureWhite, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PickAppsMultiDialog(
    title: String,
    confirmLabel: String,
    apps: List<AppEntry>,
    selectedPackages: Set<String>,
    onTogglePackage: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var q by remember { mutableStateOf("") }
    val filtered = remember(apps, q) {
        apps.filter {
            !it.isFrozen && (q.isBlank() || it.label.contains(q, ignoreCase = true))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = systemBottomSheetContainerColor(),
        title = { Text(title, color = PureWhite) },
        text = {
            Column(modifier = Modifier.height(360.dp)) {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    placeholder = { Text("Filter apps...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.packageName }) { app ->
                        val checked = selectedPackages.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTogglePackage(app.packageName) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = PureWhite, checkmarkColor = PureBlack)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(app.label, fontSize = 14.sp, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextDim)
            }
        }
    )
}

@Composable
private fun CustomToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = PureWhite)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = PureWhite)
        )
    }
}

@Composable
private fun BatteryWidget(
    stats: SysStats,
    style: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val pctFloat by animateFloatAsState(
        targetValue = (stats.batteryPct.coerceIn(0, 100)) / 100f,
        animationSpec = tween(600),
        label = "batteryPct"
    )

    Surface(
        color = LocalTileSurface.current,
        contentColor = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LocalTileBorder.current),
        modifier = modifier.clickable { onClick() }
    ) {
        AnimatedContent(
            targetState = style,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "batteryStyle"
        ) { s ->
            when (s) {
                "RING" -> Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            drawArc(
                                color = BorderHairline,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = stroke
                            )
                            drawArc(
                                color = PureWhite,
                                startAngle = -90f,
                                sweepAngle = 360f * pctFloat,
                                useCenter = false,
                                style = stroke
                            )
                        }
                        Text(
                            "${stats.batteryPct}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            if (stats.isCharging) "CHARGING" else "BATTERY",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextDim
                        )
                        Text(
                            "${stats.freeRamGb}G RAM",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                        Text(
                            "${stats.freeStorageGb}G FREE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextDim
                        )
                    }
                }

                "PILL" -> Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("POWER", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                    Text(
                        "${stats.batteryPct}%" + if (stats.isCharging) " ⚡" else "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite
                    )
                    Text(
                        "${stats.freeRamGb}G RAM • ${stats.freeStorageGb}G",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim
                    )
                }

                else -> Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (stats.isCharging) "CHARGING" else "BATTERY",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextDim
                        )
                        Text(
                            "${stats.batteryPct}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(BorderHairline)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pctFloat)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(PureWhite)
                        )
                    }
                    Text(
                        "${stats.freeRamGb}G RAM • ${stats.freeStorageGb}G FREE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniModePill(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (active) PureWhite else LocalTileSurface.current, tween(180), label = "pillBg")
    val border by animateColorAsState(if (active) PureWhite else LocalTileBorder.current, tween(180), label = "pillBorder")
    val fg by animateColorAsState(if (active) PureBlack else PureWhite, tween(180), label = "pillFg")
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, border),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

@Composable
private fun MonoBadge(text: String) {
    Text(
        text,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        color = PureWhite,
        modifier = Modifier
            .border(1.dp, BorderHairline, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp, color = PureWhite)
    }
}

private data class SysStats(val batteryPct: Int, val isCharging: Boolean, val freeRamGb: String, val freeStorageGb: String)

private data class UsageAlarmInfo(val hasUsagePermission: Boolean, val screenTime: String, val nextAlarm: String)

private fun readUsageAndAlarm(context: Context): UsageAlarmInfo {
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
        return UsageAlarmInfo(false, "", alarmStr)
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
    return UsageAlarmInfo(true, screenTime, alarmStr)
}

private fun readSystemStats(context: Context): SysStats {
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

private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
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

private val appIconCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()

private suspend fun loadInstalledApps(
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

private data class PendingApkInfo(
    val path: String,
    val appLabel: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val permissionCount: Int,
    val installedVersionName: String?,
    val installedVersionCode: Long?,
    val installType: String // "NEW", "UPGRADE", "DOWNGRADE", "SAME"
)

private fun inspectApkFile(context: Context, file: File): PendingApkInfo {
    val pm = context.packageManager
    val pkgInfo = try {
        pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_PERMISSIONS)
    } catch (_: Throwable) {
        null
    }
    val appInfo = pkgInfo?.applicationInfo?.apply {
        sourceDir = file.absolutePath
        publicSourceDir = file.absolutePath
    }
    val label = try {
        if (appInfo != null) pm.getApplicationLabel(appInfo).toString() else file.name
    } catch (_: Throwable) {
        file.name
    }

    val pkg = pkgInfo?.packageName ?: "unknown"
    val newCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pkgInfo?.longVersionCode ?: 0L
    } else {
        @Suppress("DEPRECATION")
        pkgInfo?.versionCode?.toLong() ?: 0L
    }

    val installedPkgInfo = try {
        pm.getPackageInfo(pkg, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    val installedCode = installedPkgInfo?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else @Suppress("DEPRECATION") it.versionCode.toLong()
    }

    val installType = when {
        installedPkgInfo == null -> "NEW"
        installedCode != null && newCode > installedCode -> "UPGRADE"
        installedCode != null && newCode < installedCode -> "DOWNGRADE"
        else -> "SAME"
    }

    val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        appInfo?.minSdkVersion ?: 21
    } else 21

    return PendingApkInfo(
        path = file.absolutePath,
        appLabel = label,
        packageName = pkg,
        versionName = pkgInfo?.versionName ?: "?",
        versionCode = newCode,
        targetSdk = appInfo?.targetSdkVersion ?: 0,
        minSdk = minSdk,
        permissionCount = pkgInfo?.requestedPermissions?.size ?: 0,
        installedVersionName = installedPkgInfo?.versionName,
        installedVersionCode = installedCode,
        installType = installType
    )
}

private suspend fun exportAppApk(context: Context, pkg: String, label: String): String =
    withContext(Dispatchers.IO) {
        try {
            val ai = context.packageManager.getApplicationInfo(pkg, 0)
            val src = File(ai.sourceDir)
            val outDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Monolith"
            )
            if (!outDir.exists()) outDir.mkdirs()
            val safeName = label.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val dest = File(outDir, "${safeName}_$pkg.apk")
            src.copyTo(dest, overwrite = true)
            "Exported to Downloads/Monolith/${dest.name}"
        } catch (t: Throwable) {
            "Export failed: ${t.message ?: "error"}"
        }
    }

private fun readTodayAppUsageMinutes(context: Context): Map<String, Int> {
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
private fun UsageLimitsCoverWidget(
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
            Text("Curbox Cover", fontSize = 10.sp, color = TextDim, maxLines = 1)
        }
    }
}

@Composable
private fun AppLimitGateDialog(
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

@Composable
private fun MindfulPauseDialog(
    appLabel: String,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(5) }
    val progress by animateFloatAsState(
        targetValue = (5 - secondsLeft) / 5f,
        animationSpec = tween(durationMillis = 950, easing = LinearEasing),
        label = "breathProgress"
    )

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0050505))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = SurfaceBlack,
                contentColor = PureWhite,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, BorderHairline),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {} // Consume inner click
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "CURBOX MINDFUL PAUSE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.6.sp,
                        color = TextDim
                    )

                    Spacer(Modifier.height(18.dp))

                    Box(
                        modifier = Modifier.size(92.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = PureWhite,
                            trackColor = Color(0x33FFFFFF),
                            strokeWidth = 3.dp
                        )
                        Text(
                            if (secondsLeft > 0) "${secondsLeft}s" else "✓",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        appLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = PureWhite
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        if (secondsLeft > 0) "Take a breath before diving in" else "Still intentional about opening?",
                        fontSize = 12.sp,
                        color = TextDim,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0x22FFFFFF),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDismiss() }
                        ) {
                            Text(
                                "Cancel",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextDim,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        Surface(
                            color = if (secondsLeft == 0) PureWhite else Color(0x33FFFFFF),
                            contentColor = if (secondsLeft == 0) PureBlack else TextDim,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = secondsLeft == 0) { onContinue() }
                        ) {
                            Text(
                                if (secondsLeft > 0) "Wait" else "Proceed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageInstallDialog(
    apkInfo: PendingApkInfo,
    shizukuBound: Boolean,
    onInstall: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var replace by remember { mutableStateOf(true) }
    var downgrade by remember(apkInfo.installType) { mutableStateOf(apkInfo.installType == "DOWNGRADE") }
    var grantAll by remember { mutableStateOf(false) }
    var bypassLowSdk by remember(apkInfo.targetSdk) { mutableStateOf(apkInfo.targetSdk < 24) }
    var spoofStore by remember { mutableStateOf(false) }

    val statusBadge = when (apkInfo.installType) {
        "UPGRADE" -> "UPGRADE (${apkInfo.installedVersionName ?: "?"} → ${apkInfo.versionName})"
        "DOWNGRADE" -> "DOWNGRADE (${apkInfo.installedVersionName ?: "?"} → ${apkInfo.versionName})"
        "SAME" -> "REINSTALL (${apkInfo.versionName})"
        else -> "NEW INSTALL"
    }

    val statusBadgeColor = when (apkInfo.installType) {
        "UPGRADE" -> PureWhite
        "DOWNGRADE" -> Color(0xFFFF6B6B)
        "SAME" -> TextDim
        else -> PureWhite
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = systemBottomSheetContainerColor(),
        title = {
            Column {
                Text("INSTALLER • ${apkInfo.appLabel}", color = PureWhite, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    statusBadge,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = statusBadgeColor
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${apkInfo.packageName}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                Text("Version: ${apkInfo.versionName} (${apkInfo.versionCode})", fontSize = 11.sp, color = PureWhite)
                Text("Target SDK: ${apkInfo.targetSdk} • Min SDK: ${apkInfo.minSdk} • ${apkInfo.permissionCount} permissions", fontSize = 11.sp, color = TextDim)
                Spacer(Modifier.height(4.dp))
                CustomToggleRow("Replace existing (-r)", replace) { replace = it }
                CustomToggleRow("Allow downgrade (-d)", downgrade) { downgrade = it }
                CustomToggleRow("Grant all permissions (-g)", grantAll) { grantAll = it }
                CustomToggleRow("Bypass low target SDK", bypassLowSdk) { bypassLowSdk = it }
                CustomToggleRow("Spoof Google Play Store (-i)", spoofStore) { spoofStore = it }
                if (!shizukuBound) {
                    Text("Enable Enhanced Controls in Settings for silent background install.", fontSize = 11.sp, color = TextDim)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val flags = buildList {
                        if (replace) add("-r")
                        if (downgrade) add("-d")
                        if (grantAll) add("-g")
                        if (bypassLowSdk) add("--bypass-low-target-sdk-block")
                        if (spoofStore) add("-i com.android.vending")
                    }
                    onInstall(flags)
                },
                enabled = shizukuBound,
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
            ) {
                Text(if (apkInfo.installType == "DOWNGRADE") "Downgrade" else "Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextDim) }
        }
    )
}

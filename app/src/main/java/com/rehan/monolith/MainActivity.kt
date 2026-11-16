package com.rehan.monolith

import com.rehan.monolith.data.*
import com.rehan.monolith.shizuku.*
import com.rehan.monolith.ui.common.*
import com.rehan.monolith.ui.home.*
import com.rehan.monolith.ui.widgets.*
import com.rehan.monolith.ui.drawer.*
import com.rehan.monolith.ui.shelf.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.install.*
import com.rehan.monolith.ui.wellbeing.*

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val shizuku: ShizukuManager by inject()
    private val prefs: LauncherPrefs by inject()
    private val appRepository: AppRepository by inject()
    private val viewModel: LauncherViewModel by viewModel()
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
        // Monolith is always dark: transparent bars with light icons, never the light scrim
        // that 3-button navigation gets in system light mode.
        val darkBars = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = darkBars, navigationBarStyle = darkBars)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
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
                    viewModel = viewModel,
                    shizuku = shizuku,
                    appRepository = appRepository,
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
            val forwardIntent = Intent(this, PackageInstallActivity::class.java).apply {
                action = intent.action
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(forwardIntent)
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
        appRepository.refresh()
        viewModel.onLauncherResumed()
    }
}

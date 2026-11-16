package com.rehan.monolith.ui.install

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.rehan.monolith.shizuku.ShizukuManager
import com.rehan.monolith.ui.common.PureBlack
import com.rehan.monolith.ui.common.PureWhite
import com.rehan.monolith.ui.common.SurfaceBlack
import com.rehan.monolith.ui.home.PackageInstallState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

class PackageInstallActivity : ComponentActivity() {

    private val shizuku: ShizukuManager by inject()
    private var copyJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stagedPath = intent?.getStringExtra(PackageInstallService.EXTRA_STAGED_PATH)
        val uri = intent?.data
        if (stagedPath == null && uri == null) {
            finish()
            return
        }

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
                var apkLoadingState by remember { mutableStateOf<ApkLoadingState?>(null) }
                var pendingApkInstall by remember { mutableStateOf<PendingApkInfo?>(null) }
                val installState by PackageInstallTracker.state.collectAsState()

                LaunchedEffect(stagedPath, uri) {
                    if (stagedPath != null) {
                        val file = File(stagedPath)
                        if (file.exists()) {
                            val info = withContext(Dispatchers.IO) {
                                inspectApkFile(this@PackageInstallActivity, file)
                            }
                            pendingApkInstall = info
                            return@LaunchedEffect
                        }
                    }

                    if (uri != null) {
                        copyJob?.cancel()
                        copyJob = lifecycleScope.launch {
                            var out: File? = null
                            try {
                                val info = withContext(Dispatchers.IO) {
                                    var totalSize = -1L
                                    var displayName = "package.apk"
                                    try {
                                        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                                            if (cursor.moveToFirst()) {
                                                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                                if (nameIdx >= 0) cursor.getString(nameIdx)?.let { displayName = it }
                                                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                                                if (sizeIdx >= 0) totalSize = cursor.getLong(sizeIdx)
                                            }
                                        }
                                    } catch (_: Throwable) {}

                                    withContext(Dispatchers.Main) {
                                        apkLoadingState = ApkLoadingState(displayName, 0L, totalSize)
                                    }

                                    val extension = if (displayName.contains(".")) displayName.substring(displayName.lastIndexOf('.')) else ".apk"
                                    val targetFile = File(externalCacheDir ?: cacheDir, "install_${System.currentTimeMillis()}$extension")
                                    out = targetFile

                                    contentResolver.openInputStream(uri)?.use { input ->
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
                                    inspectApkFile(this@PackageInstallActivity, targetFile)
                                }
                                apkLoadingState = null
                                pendingApkInstall = info
                            } catch (_: kotlinx.coroutines.CancellationException) {
                                apkLoadingState = null
                                try { out?.delete() } catch (_: Throwable) {}
                                finish()
                            } catch (t: Throwable) {
                                apkLoadingState = null
                                try { out?.delete() } catch (_: Throwable) {}
                                Toast.makeText(this@PackageInstallActivity, "Failed to read package: ${t.message ?: "error"}", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                }

                val loading = apkLoadingState
                if (loading != null) {
                    PackageLoadingDialog(
                        loadingState = loading,
                        onRunInBackground = {
                            val targetUri = uri
                            copyJob?.cancel()
                            apkLoadingState = null
                            if (targetUri != null) {
                                PackageInstallService.startAnalysis(this@PackageInstallActivity, targetUri, loading.fileName)
                            }
                            finish()
                        },
                        onCancel = {
                            copyJob?.cancel()
                            apkLoadingState = null
                            finish()
                        }
                    )
                }

                val apkInfo = pendingApkInstall
                if (apkInfo != null) {
                    val paths = apkInfo.splitPaths.ifEmpty { listOf(apkInfo.path) }
                    PackageInstallDialog(
                        apkInfo = apkInfo,
                        shizukuBound = shizuku.status.value.isReady,
                        installState = installState,
                        onInstall = { flags ->
                            if (shizuku.status.value.isReady) {
                                PackageInstallService.startInstall(this@PackageInstallActivity, paths, apkInfo.appLabel, apkInfo.packageName, flags)
                            } else {
                                shizuku.requestPermission()
                            }
                        },
                        onUninstallAndInstall = { flags ->
                            lifecycleScope.launch {
                                PackageInstallTracker.update(PackageInstallState.Installing(apkInfo.appLabel, apkInfo.packageName))
                                withContext(Dispatchers.IO) {
                                    shizuku.uninstallPackage(apkInfo.packageName)
                                }
                                PackageInstallService.startInstall(this@PackageInstallActivity, paths, apkInfo.appLabel, apkInfo.packageName, flags)
                            }
                        },
                        onInstallViaSystem = {
                            installViaSystem(apkInfo)
                        },
                        onRunInBackground = {
                            finish()
                        },
                        onLaunchApp = { pkg ->
                            PackageInstallTracker.reset()
                            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(launchIntent)
                            }
                            finish()
                        },
                        onDismiss = {
                            if (installState !is PackageInstallState.Installing) {
                                PackageInstallTracker.reset()
                            }
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun installViaSystem(apkInfo: PendingApkInfo) {
        try {
            val file = File(apkInfo.path)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        } catch (t: Throwable) {
            Toast.makeText(this, "System installer failed: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

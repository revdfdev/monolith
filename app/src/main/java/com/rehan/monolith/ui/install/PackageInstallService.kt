package com.rehan.monolith.ui.install

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rehan.monolith.R
import com.rehan.monolith.data.AppRepository
import com.rehan.monolith.shizuku.ShizukuManager
import com.rehan.monolith.ui.home.PackageInstallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

object PackageInstallTracker {
    private val _state = MutableStateFlow<PackageInstallState>(PackageInstallState.Idle)
    val state: StateFlow<PackageInstallState> = _state.asStateFlow()

    fun update(next: PackageInstallState) {
        _state.value = next
    }

    fun reset() {
        _state.value = PackageInstallState.Idle
    }
}

class PackageInstallService : Service(), KoinComponent {

    private val shizuku: ShizukuManager by inject()
    private val appRepository: AppRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: intent?.getStringExtra(EXTRA_ACTION) ?: ACTION_INSTALL
        if (action == ACTION_ANALYZE) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(EXTRA_URI)
            }
            val displayName = intent?.getStringExtra(EXTRA_DISPLAY_NAME) ?: "package.apk"
            if (uri == null) {
                stopSelf(startId)
                return START_NOT_STICKY
            }
            handleAnalysis(uri, displayName, startId)
            return START_NOT_STICKY
        }

        val singleApkPath = intent?.getStringExtra(EXTRA_APK_PATH)
        val splitPaths = intent?.getStringArrayListExtra(EXTRA_SPLIT_PATHS)
        val allPaths = when {
            !splitPaths.isNullOrEmpty() -> splitPaths.toList()
            !singleApkPath.isNullOrBlank() -> listOf(singleApkPath)
            else -> emptyList()
        }
        val appLabel = intent?.getStringExtra(EXTRA_APP_LABEL) ?: "Package"
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: "unknown"
        val installFlags = intent?.getStringArrayListExtra(EXTRA_FLAGS) ?: arrayListOf("-r")

        if (allPaths.isEmpty()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val appIcon: Bitmap? = try {
            loadApkIconBitmap(this, File(allPaths.first()))
        } catch (_: Throwable) {
            null
        }

        val ongoingNotif = buildOngoingNotification(appLabel, allPaths.size, appIcon, 10, "Preparing...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, ongoingNotif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, ongoingNotif)
        }

        PackageInstallTracker.update(PackageInstallState.Installing(appLabel, packageName))

        serviceScope.launch {
            val progressJob = launch {
                var current = 15
                while (current < 92) {
                    delay(350)
                    current += if (current < 50) 8 else if (current < 75) 4 else 2
                    val statusText = when {
                        current < 35 -> "Starting session..."
                        current < 75 -> "Streaming splits ($current%)..."
                        else -> "Verifying package ($current%)..."
                    }
                    val notif = buildOngoingNotification(appLabel, allPaths.size, appIcon, current, statusText)
                    NotificationManagerCompat.from(this@PackageInstallService).notify(NOTIFICATION_ID, notif)
                }
            }

            try {
                val result = shizuku.installSplitApks(allPaths, installFlags)
                progressJob.cancel()

                allPaths.forEach { path ->
                    try {
                        val file = File(path)
                        file.delete()
                        file.parentFile?.let { parent ->
                            if (parent.name.startsWith("bundle_")) {
                                parent.deleteRecursively()
                            }
                        }
                    } catch (_: Throwable) {}
                }

                if (result.ok) {
                    PackageInstallTracker.update(PackageInstallState.Success(appLabel, packageName))
                    appRepository.refresh()
                    notifySuccess(appLabel, packageName, appIcon)
                } else {
                    val errorMsg = mapInstallError(result.output, result.exitCode)
                    PackageInstallTracker.update(PackageInstallState.Error(appLabel, errorMsg))
                    notifyFailure(appLabel, errorMsg, appIcon)
                }
            } catch (t: Throwable) {
                progressJob.cancel()
                val err = t.message ?: "Unexpected installation error"
                PackageInstallTracker.update(PackageInstallState.Error(appLabel, err))
                notifyFailure(appLabel, err, appIcon)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun handleAnalysis(uri: Uri, displayName: String, startId: Int) {
        val extension = if (displayName.contains(".")) displayName.substring(displayName.lastIndexOf('.')) else ".apk"
        val targetFile = File(externalCacheDir ?: cacheDir, "install_${System.currentTimeMillis()}$extension")

        val ongoingNotif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_install)
            .setContentTitle(displayName)
            .setContentText("Reading package...")
            .setProgress(100, 0, false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, ongoingNotif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, ongoingNotif)
        }

        serviceScope.launch {
            try {
                var totalBytes = -1L
                try {
                    contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (sizeIdx >= 0) totalBytes = cursor.getLong(sizeIdx)
                        }
                    }
                } catch (_: Throwable) {}

                contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        var lastNotif = System.currentTimeMillis()
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            copied += read
                            val now = System.currentTimeMillis()
                            if (now - lastNotif > 300) {
                                val percent = if (totalBytes > 0) ((copied * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                                val statusText = if (totalBytes > 0) {
                                    "${formatFileSize(copied)} / ${formatFileSize(totalBytes)} ($percent%)"
                                } else {
                                    "${formatFileSize(copied)} read"
                                }
                                val updatedNotif = NotificationCompat.Builder(this@PackageInstallService, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_notification_install)
                                    .setContentTitle(displayName)
                                    .setContentText(statusText)
                                    .setProgress(100, percent, totalBytes <= 0)
                                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                                    .setOngoing(true)
                                    .setOnlyAlertOnce(true)
                                    .setShowWhen(false)
                                    .build()
                                NotificationManagerCompat.from(this@PackageInstallService).notify(NOTIFICATION_ID, updatedNotif)
                                lastNotif = now
                            }
                        }
                    }
                }

                val info = inspectApkFile(this@PackageInstallService, targetFile)
                val appIcon = loadApkIconBitmap(this@PackageInstallService, targetFile)

                notifyAnalysisComplete(info, targetFile, appIcon)
            } catch (t: Throwable) {
                notifyAnalysisFailure(displayName, t.message ?: "Failed to read package")
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
    }

    private fun notifyAnalysisComplete(info: PendingApkInfo, file: File, appIcon: Bitmap?) {
        val installIntent = Intent(this, PackageInstallActivity::class.java).apply {
            putExtra(EXTRA_STAGED_PATH, file.absolutePath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID + 10,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_install)
            .apply {
                if (appIcon != null) setLargeIcon(appIcon)
            }
            .setContentTitle(info.appLabel)
            .setContentText("Package ready to install")
            .setAutoCancel(true)
            .addAction(0, "Install", pendingIntent)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID + 3, notif)
    }

    private fun notifyAnalysisFailure(displayName: String, reason: String) {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_install)
            .setContentTitle("Failed to read: $displayName")
            .setContentText(reason)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID + 4, notif)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Package Installation",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows progress and completion of package installations"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildOngoingNotification(
        appLabel: String,
        splitCount: Int,
        appIcon: Bitmap?,
        progress: Int = 10,
        statusText: String = "Installing..."
    ) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_install)
            .apply {
                if (appIcon != null) setLargeIcon(appIcon)
            }
            .setContentTitle(appLabel)
            .setContentText(statusText)
            .setProgress(100, progress, false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()

    private fun notifySuccess(appLabel: String, packageName: String, appIcon: Bitmap?) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
        }

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_install)
            .apply {
                if (appIcon != null) setLargeIcon(appIcon)
            }
            .setContentTitle(appLabel)
            .setContentText("Installed")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                    addAction(0, "Open", pendingIntent)
                }
            }
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID + 1, notif)
    }

    private fun notifyFailure(appLabel: String, errorReason: String, appIcon: Bitmap?) {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_install)
            .apply {
                if (appIcon != null) setLargeIcon(appIcon)
            }
            .setContentTitle("Install Failed: $appLabel")
            .setContentText(errorReason)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID + 2, notif)
    }

    private fun mapInstallError(output: String, exitCode: Int): String {
        return when {
            output.contains("INSTALL_FAILED_VERSION_DOWNGRADE") -> "Existing version is newer. Enable 'Allow downgrade'."
            output.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") -> "Signature mismatch with installed package."
            output.contains("INSTALL_FAILED_INSUFFICIENT_STORAGE") -> "Device storage is full."
            output.contains("INSTALL_FAILED_OLDER_SDK") -> "App requires a newer Android version."
            output.contains("INSTALL_FAILED_DEPRECATED_SDK_VERSION") -> "Target SDK blocked. Enable 'Bypass target SDK restrictions'."
            output.contains("INSTALL_PARSE_FAILED") -> "Invalid or corrupted package file."
            output.isNotBlank() -> output.lines().firstOrNull { it.contains("Failure") || it.contains("Error") } ?: output
            else -> "Shell exit code $exitCode"
        }
    }

    companion object {
        const val CHANNEL_ID = "monolith_package_install"
        const val NOTIFICATION_ID = 2048

        const val ACTION_INSTALL = "action_install"
        const val ACTION_ANALYZE = "action_analyze"

        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
        const val EXTRA_STAGED_PATH = "extra_staged_path"

        const val EXTRA_APK_PATH = "extra_apk_path"
        const val EXTRA_SPLIT_PATHS = "extra_split_paths"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_FLAGS = "extra_flags"

        fun startAnalysis(
            context: Context,
            uri: Uri,
            displayName: String
        ) {
            val intent = Intent(context, PackageInstallService::class.java).apply {
                action = ACTION_ANALYZE
                putExtra(EXTRA_ACTION, ACTION_ANALYZE)
                putExtra(EXTRA_URI, uri)
                putExtra(EXTRA_DISPLAY_NAME, displayName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startInstall(
            context: Context,
            apkPaths: List<String>,
            appLabel: String,
            packageName: String,
            flags: List<String>
        ) {
            val intent = Intent(context, PackageInstallService::class.java).apply {
                action = ACTION_INSTALL
                putExtra(EXTRA_ACTION, ACTION_INSTALL)
                putStringArrayListExtra(EXTRA_SPLIT_PATHS, ArrayList(apkPaths))
                if (apkPaths.isNotEmpty()) putExtra(EXTRA_APK_PATH, apkPaths.first())
                putExtra(EXTRA_APP_LABEL, appLabel)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putStringArrayListExtra(EXTRA_FLAGS, ArrayList(flags))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startInstall(
            context: Context,
            apkPath: String,
            appLabel: String,
            packageName: String,
            flags: List<String>
        ) {
            startInstall(context, listOf(apkPath), appLabel, packageName, flags)
        }
    }
}

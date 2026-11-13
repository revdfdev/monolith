package com.rehan.monolith.ui.install

import com.rehan.monolith.*
import com.rehan.monolith.data.*
import com.rehan.monolith.shizuku.*
import com.rehan.monolith.ui.common.*
import com.rehan.monolith.ui.home.*
import com.rehan.monolith.ui.widgets.*
import com.rehan.monolith.ui.drawer.*
import com.rehan.monolith.ui.shelf.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.wellbeing.*

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal data class PendingApkInfo(
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

internal fun inspectApkFile(context: Context, file: File): PendingApkInfo {
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

internal suspend fun exportAppApk(context: Context, pkg: String, label: String): String =
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

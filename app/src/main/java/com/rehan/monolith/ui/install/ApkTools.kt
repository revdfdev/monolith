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
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

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
    val installType: String, // "NEW", "UPGRADE", "DOWNGRADE", "SAME"
    val sizeBytes: Long = 0L,
    val sizeFormatted: String = "",
    val icon: ImageBitmap? = null,
    val signaturesMatch: Boolean = true,
    val isBundle: Boolean = false,
    val splitPaths: List<String> = emptyList(),
    val supportedAbis: List<String> = emptyList()
)

internal fun formatFileSize(bytes: Long): String = when {
    bytes <= 0 -> "0 B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
}

internal fun drawableToBitmap(drawable: Drawable): Bitmap {
    val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
    val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    return bmp
}

internal fun loadApkIconBitmap(context: Context, baseApkFile: File, bundleDir: File? = null): Bitmap? {
    if (bundleDir != null) {
        val pngIcon = File(bundleDir, "icon.png")
        if (pngIcon.exists() && pngIcon.length() > 0) {
            try {
                BitmapFactory.decodeFile(pngIcon.absolutePath)?.let { return it }
            } catch (_: Throwable) {}
        }
    }

    val pm = context.packageManager
    val pkgInfo = try {
        pm.getPackageArchiveInfo(baseApkFile.absolutePath, 0)
    } catch (_: Throwable) {
        null
    }
    val appInfo = pkgInfo?.applicationInfo ?: return null
    appInfo.sourceDir = baseApkFile.absolutePath
    appInfo.publicSourceDir = baseApkFile.absolutePath

    try {
        val assetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
        val addAssetPath = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
        addAssetPath.invoke(assetManager, baseApkFile.absolutePath)
        val res = Resources(assetManager, context.resources.displayMetrics, context.resources.configuration)
        if (appInfo.icon != 0) {
            val drawable = ResourcesCompat.getDrawable(res, appInfo.icon, null)
            if (drawable != null) {
                return drawableToBitmap(drawable)
            }
        }
    } catch (_: Throwable) {}

    return try {
        val drawable = appInfo.loadIcon(pm)
        drawableToBitmap(drawable)
    } catch (_: Throwable) {
        null
    }
}

internal fun inspectApkFile(context: Context, file: File): PendingApkInfo {
    val pm = context.packageManager
    val lowerName = file.name.lowercase(Locale.ROOT)
    val isBundleArchive = lowerName.endsWith(".xapk") || lowerName.endsWith(".apks") || lowerName.endsWith(".apkm") || isMultiApkZip(file)

    val unpackDir = if (isBundleArchive) {
        File(context.externalCacheDir ?: context.cacheDir, "bundle_${System.currentTimeMillis()}").apply { mkdirs() }
    } else null

    val (baseApkFile, allSplits, totalBundleSize) = if (isBundleArchive && unpackDir != null) {
        val extracted = extractApksFromArchive(file, unpackDir)
        val base = findBaseApk(pm, extracted) ?: file
        val deviceSplits = filterCompatibleSplits(extracted)
        Triple(base, deviceSplits, deviceSplits.sumOf { it.length() })
    } else {
        Triple(file, listOf(file), file.length())
    }

    val pkgInfo = try {
        pm.getPackageArchiveInfo(baseApkFile.absolutePath, PackageManager.GET_PERMISSIONS)
    } catch (_: Throwable) {
        null
    }
    val appInfo = pkgInfo?.applicationInfo?.apply {
        sourceDir = baseApkFile.absolutePath
        publicSourceDir = baseApkFile.absolutePath
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

    val sizeBytes = if (isBundleArchive) totalBundleSize else file.length()
    val sizeFormatted = formatFileSize(sizeBytes)
    val rawIcon = loadApkIconBitmap(context, baseApkFile, unpackDir)
    val iconBitmap = rawIcon?.asImageBitmap()

    var signaturesMatch = true
    if (installedPkgInfo != null) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val newSignatures = pm.getPackageArchiveInfo(baseApkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                    ?.signingInfo?.apkContentsSigners
                val installedSignatures = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                    ?.signingInfo?.apkContentsSigners
                if (newSignatures != null && installedSignatures != null) {
                    val newSet = newSignatures.map { it.toByteArray().contentHashCode() }.toSet()
                    val installedSet = installedSignatures.map { it.toByteArray().contentHashCode() }.toSet()
                    signaturesMatch = newSet.intersect(installedSet).isNotEmpty()
                }
            } else {
                @Suppress("DEPRECATION")
                val newSignatures = pm.getPackageArchiveInfo(baseApkFile.absolutePath, PackageManager.GET_SIGNATURES)?.signatures
                @Suppress("DEPRECATION")
                val installedSignatures = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)?.signatures
                if (newSignatures != null && installedSignatures != null) {
                    val newSet = newSignatures.map { it.toCharsString() }.toSet()
                    val installedSet = installedSignatures.map { it.toCharsString() }.toSet()
                    signaturesMatch = newSet.intersect(installedSet).isNotEmpty()
                }
            }
        } catch (_: Throwable) {
            signaturesMatch = true
        }
    }

    val abis = detectSupportedAbis(baseApkFile)

    return PendingApkInfo(
        path = baseApkFile.absolutePath,
        appLabel = label,
        packageName = pkg,
        versionName = pkgInfo?.versionName ?: "?",
        versionCode = newCode,
        targetSdk = appInfo?.targetSdkVersion ?: 0,
        minSdk = minSdk,
        permissionCount = pkgInfo?.requestedPermissions?.size ?: 0,
        installedVersionName = installedPkgInfo?.versionName,
        installedVersionCode = installedCode,
        installType = installType,
        sizeBytes = sizeBytes,
        sizeFormatted = sizeFormatted,
        icon = iconBitmap,
        signaturesMatch = signaturesMatch,
        isBundle = isBundleArchive,
        splitPaths = allSplits.map { it.absolutePath },
        supportedAbis = abis
    )
}

private fun isMultiApkZip(file: File): Boolean = try {
    ZipFile(file).use { zip ->
        zip.entries().asSequence().count { it.name.endsWith(".apk", ignoreCase = true) } > 1
    }
} catch (_: Throwable) {
    false
}

private fun extractApksFromArchive(file: File, destDir: File): List<File> {
    val extracted = mutableListOf<File>()
    try {
        ZipFile(file).use { zip ->
            for (entry in zip.entries()) {
                if (!entry.isDirectory) {
                    val lower = entry.name.lowercase(Locale.ROOT)
                    if (lower.endsWith(".apk") || lower == "icon.png" || lower.endsWith("/icon.png")) {
                        val safeName = File(entry.name).name
                        val target = File(destDir, safeName)
                        zip.getInputStream(entry).use { input ->
                            target.outputStream().use { output -> input.copyTo(output) }
                        }
                        if (safeName.endsWith(".apk", ignoreCase = true)) {
                            extracted += target
                        }
                    }
                }
            }
        }
    } catch (_: Throwable) {}
    return extracted
}

private fun findBaseApk(pm: PackageManager, apks: List<File>): File? {
    if (apks.isEmpty()) return null
    apks.firstOrNull { it.name.equals("base.apk", true) || it.name.startsWith("base-", true) }?.let { return it }
    for (apk in apks) {
        val pi = try { pm.getPackageArchiveInfo(apk.absolutePath, 0) } catch (_: Throwable) { null }
        if (pi?.splitNames == null || pi.splitNames.isEmpty()) {
            return apk
        }
    }
    return apks.maxByOrNull { it.length() }
}

private fun filterCompatibleSplits(apks: List<File>): List<File> {
    if (apks.size <= 1) return apks
    val deviceAbis = Build.SUPPORTED_ABIS.toSet()
    val knownAbis = setOf("arm64-v8a", "armeabi-v7a", "armeabi", "x86_64", "x86", "mips", "mips64")

    return apks.filter { file ->
        val name = file.name.lowercase(Locale.ROOT)
        val abiInName = knownAbis.firstOrNull { name.contains(it) }
        if (abiInName != null) {
            abiInName in deviceAbis
        } else {
            true
        }
    }
}

private fun detectSupportedAbis(file: File): List<String> = try {
    ZipFile(file).use { zip ->
        val abis = mutableSetOf<String>()
        for (entry in zip.entries()) {
            val name = entry.name
            if (name.startsWith("lib/") && !entry.isDirectory) {
                val parts = name.split('/')
                if (parts.size >= 2) {
                    abis += parts[1]
                }
            }
        }
        abis.toList()
    }
} catch (_: Throwable) {
    emptyList()
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

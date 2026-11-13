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

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PackageInstallDialog(
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

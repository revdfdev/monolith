package com.rehan.monolith.ui.install

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rehan.monolith.ui.common.BorderHairline
import com.rehan.monolith.ui.common.CustomToggleRow
import com.rehan.monolith.ui.common.LocalTileBorder
import com.rehan.monolith.ui.common.LocalTileSurface
import com.rehan.monolith.ui.common.PureBlack
import com.rehan.monolith.ui.common.PureWhite
import com.rehan.monolith.ui.common.SurfaceBlack
import com.rehan.monolith.ui.common.TextDim
import com.rehan.monolith.ui.home.PackageInstallState

@Composable
internal fun PackageInstallDialog(
    apkInfo: PendingApkInfo,
    shizukuBound: Boolean,
    installState: PackageInstallState = PackageInstallState.Idle,
    onInstall: (List<String>) -> Unit,
    onUninstallAndInstall: (List<String>) -> Unit = {},
    onInstallViaSystem: () -> Unit = {},
    onRunInBackground: () -> Unit = {},
    onLaunchApp: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var replace by remember { mutableStateOf(true) }
    var downgrade by remember(apkInfo.installType) { mutableStateOf(apkInfo.installType == "DOWNGRADE") }
    var grantAll by remember { mutableStateOf(false) }
    var bypassLowSdk by remember(apkInfo.targetSdk) { mutableStateOf(apkInfo.targetSdk < 24) }
    var spoofStore by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    val statusBadge = when (apkInfo.installType) {
        "UPGRADE" -> "Upgrade (${apkInfo.installedVersionName ?: "?"} → ${apkInfo.versionName})"
        "DOWNGRADE" -> "Downgrade (${apkInfo.installedVersionName ?: "?"} → ${apkInfo.versionName})"
        "SAME" -> "Reinstall (${apkInfo.versionName})"
        else -> "New (${apkInfo.versionName})"
    }

    val statusBadgeColor = when (apkInfo.installType) {
        "UPGRADE" -> PureWhite
        "DOWNGRADE" -> Color(0xFFFF6B6B)
        "SAME" -> TextDim
        else -> PureWhite
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceBlack,
            border = BorderStroke(1.dp, BorderHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header: Icon + Name + Package
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (apkInfo.icon != null) {
                        Image(
                            bitmap = apkInfo.icon,
                            contentDescription = apkInfo.appLabel,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = LocalTileSurface.current,
                            border = BorderStroke(1.dp, LocalTileBorder.current)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Widgets,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            apkInfo.appLabel,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            apkInfo.packageName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Horizontally Scrollable Metadata Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = LocalTileSurface.current,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LocalTileBorder.current)
                    ) {
                        Text(
                            statusBadge,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = statusBadgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    if (apkInfo.isBundle || apkInfo.splitPaths.size > 1) {
                        Surface(
                            color = LocalTileSurface.current,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LocalTileBorder.current)
                        ) {
                            Text(
                                "${apkInfo.splitPaths.size.coerceAtLeast(1)} Splits",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = PureWhite,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (apkInfo.supportedAbis.isNotEmpty()) {
                        Surface(
                            color = LocalTileSurface.current,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LocalTileBorder.current)
                        ) {
                            Text(
                                apkInfo.supportedAbis.first(),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (apkInfo.sizeFormatted.isNotBlank()) {
                        Surface(
                            color = LocalTileSurface.current,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LocalTileBorder.current)
                        ) {
                            Text(
                                apkInfo.sizeFormatted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Surface(
                        color = LocalTileSurface.current,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LocalTileBorder.current)
                    ) {
                        Text(
                            "SDK ${apkInfo.targetSdk}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextDim,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                when (installState) {
                    is PackageInstallState.Installing -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = PureWhite,
                                trackColor = BorderHairline
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Installing ${apkInfo.appLabel}",
                                fontSize = 13.sp,
                                color = PureWhite,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = onRunInBackground,
                                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Background")
                            }
                        }
                    }

                    is PackageInstallState.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Installed",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Spacer(Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Done", color = PureWhite)
                                }
                                Button(
                                    onClick = { onLaunchApp(apkInfo.packageName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Open")
                                }
                            }
                        }
                    }

                    is PackageInstallState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Installation Failed",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                installState.reason,
                                fontSize = 12.sp,
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }

                    is PackageInstallState.Idle -> {
                        // Options Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showAdvanced = !showAdvanced }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "OPTIONS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.4.sp,
                                color = TextDim
                            )
                            Icon(
                                if (showAdvanced) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextDim,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = showAdvanced) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CustomToggleRow("Replace existing", replace) { replace = it }
                                CustomToggleRow("Allow downgrade", downgrade) { downgrade = it }
                                CustomToggleRow("Grant permissions", grantAll) { grantAll = it }
                                CustomToggleRow("Bypass target SDK", bypassLowSdk) { bypassLowSdk = it }
                                CustomToggleRow("Claim Play Store origin", spoofStore) { spoofStore = it }
                            }
                        }

                        if (!apkInfo.signaturesMatch) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = Color(0x33FF5252),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0x66FF5252)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Signature Mismatch: Package signing key differs from the installed version.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF8A80),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        val buildFlags = {
                            buildList {
                                if (replace) add("-r")
                                if (downgrade) add("-d")
                                if (grantAll) add("-g")
                                if (bypassLowSdk) add("--bypass-low-target-sdk-block")
                                if (spoofStore) {
                                    add("-i")
                                    add("com.android.vending")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = TextDim)
                            }

                            if (!shizukuBound) {
                                Button(
                                    onClick = onInstallViaSystem,
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Install (System)")
                                }
                            } else if (!apkInfo.signaturesMatch) {
                                Button(
                                    onClick = { onUninstallAndInstall(buildFlags()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = PureWhite),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Text("Uninstall & Reinstall", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            } else {
                                Button(
                                    onClick = { onInstall(buildFlags()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (apkInfo.installType == "DOWNGRADE") "Downgrade" else "Install")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

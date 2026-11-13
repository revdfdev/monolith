package com.rehan.monolith.ui.shelf

import com.rehan.monolith.*
import com.rehan.monolith.data.*
import com.rehan.monolith.shizuku.*
import com.rehan.monolith.ui.common.*
import com.rehan.monolith.ui.home.*
import com.rehan.monolith.ui.widgets.*
import com.rehan.monolith.ui.drawer.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.install.*
import com.rehan.monolith.ui.wellbeing.*

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ShizukuShelfPage(
    status: ShizukuStatus,
    apps: List<AppEntry>,
    kidsAppCount: Int,
    hasParentPin: Boolean,
    grayscaleOn: Boolean,
    stayAwakeOn: Boolean,
    installersLocked: Boolean,
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
                        Text("Android always-on display setting (doze_always_on)", fontSize = 11.sp, color = TextDim)
                    }
                    Switch(
                        checked = ambientDisplayOn,
                        onCheckedChange = onToggleAmbientDisplay,
                        enabled = status.serviceBound,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = PureWhite)
                    )
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

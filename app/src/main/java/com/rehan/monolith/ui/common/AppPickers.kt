package com.rehan.monolith.ui.common

import com.rehan.monolith.*
import com.rehan.monolith.data.*
import com.rehan.monolith.shizuku.*
import com.rehan.monolith.ui.home.*
import com.rehan.monolith.ui.widgets.*
import com.rehan.monolith.ui.drawer.*
import com.rehan.monolith.ui.shelf.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.install.*
import com.rehan.monolith.ui.wellbeing.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PickSingleAppDialog(
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
internal fun QuickActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
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
internal fun PickAppsMultiDialog(
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
internal fun CustomToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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

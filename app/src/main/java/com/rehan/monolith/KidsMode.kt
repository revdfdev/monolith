package com.rehan.monolith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
internal fun PinSetupWizardDialog(
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
internal fun PinKeypadDialog(
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

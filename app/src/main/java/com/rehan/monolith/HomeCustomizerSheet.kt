package com.rehan.monolith

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeCustomizerSheet(
    config: HomeConfig,
    onUpdate: (HomeConfig) -> Unit,
    onPickHomeApps: () -> Unit,
    onManageWidgets: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = systemBottomSheetContainerColor(),
        contentColor = systemBottomSheetContentColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("CUSTOMIZE HOME", fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, color = TextDim)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onManageWidgets,
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Widget Manager", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onPickHomeApps,
                    border = BorderStroke(1.dp, BorderHairline),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Home Apps", fontSize = 12.sp, color = PureWhite)
                }
            }

            Text("COLOR MODE (NORMAL)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniModePill("DYNAMIC", config.colorMode == "DYNAMIC", { onUpdate(config.copy(colorMode = "DYNAMIC")) }, Modifier.weight(1f))
                MiniModePill("OLED", config.colorMode == "OLED", { onUpdate(config.copy(colorMode = "OLED")) }, Modifier.weight(1f))
            }

            if (config.colorMode == "OLED" || config.ambienceStyle == "AMOLED") {
                Text("AMOLED ACCENT COLOR", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val accents = listOf(
                        "WHITE" to PureWhite,
                        "CYAN" to Color(0xFF00E5FF),
                        "EMERALD" to Color(0xFF00E676),
                        "AMBER" to Color(0xFFFFB300),
                        "ROSE" to Color(0xFFFF4081),
                        "PURPLE" to Color(0xFFD500F9),
                        "ORANGE" to Color(0xFFFF6D00)
                    )
                    accents.forEach { (name, color) ->
                        val isSelected = config.amoledAccent.equals(name, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) color.copy(alpha = 0.25f) else Color(0x331E1E1E))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else BorderHairline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onUpdate(config.copy(amoledAccent = name)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }

            CustomToggleRow("Wallpaper Blur", config.blurBackground) { onUpdate(config.copy(blurBackground = it)) }

            Text("AMBIENCE GRADIENT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SAILFISH", "MIST", "AMOLED").forEach { style ->
                    MiniModePill(
                        label = style,
                        active = config.ambienceStyle == style,
                        onClick = { onUpdate(config.copy(ambienceStyle = style)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("TILE FEEL", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("GLASS", "SOLID", "OUTLINE").forEach { style ->
                    MiniModePill(
                        label = style,
                        active = config.tileStyle == style,
                        onClick = { onUpdate(config.copy(tileStyle = style)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("APPS LAYOUT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniModePill("GRID", config.pinnedLayout == "GRID", { onUpdate(config.copy(pinnedLayout = "GRID")) }, Modifier.weight(1f))
                MiniModePill("LIST", config.pinnedLayout == "LIST", { onUpdate(config.copy(pinnedLayout = "LIST")) }, Modifier.weight(1f))
            }

            Text("CLOCK STYLE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("STACKED", "DIGITAL", "MINIMAL").forEach { style ->
                    MiniModePill(
                        label = style,
                        active = config.clockStyle == style,
                        onClick = { onUpdate(config.copy(clockStyle = style)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("BATTERY INDICATOR", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("RING", "BAR", "PILL").forEach { style ->
                    MiniModePill(
                        label = style,
                        active = config.batteryStyle == style,
                        onClick = { onUpdate(config.copy(batteryStyle = style)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("ALIGNMENT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniModePill("LEFT", !config.alignCenter, { onUpdate(config.copy(alignCenter = false)) }, Modifier.weight(1f))
                MiniModePill("CENTER", config.alignCenter, { onUpdate(config.copy(alignCenter = true)) }, Modifier.weight(1f))
            }

            HorizontalDivider(color = BorderHairline)
            CustomToggleRow("24-Hour Clock", config.is24Hour) { onUpdate(config.copy(is24Hour = it)) }
            CustomToggleRow("Show Clock", config.showClock) { onUpdate(config.copy(showClock = it)) }
            CustomToggleRow("Show Battery & System", config.showBattery) { onUpdate(config.copy(showBattery = it)) }
            CustomToggleRow("Show Modes Strip", config.showModes) { onUpdate(config.copy(showModes = it)) }
            CustomToggleRow("Show Pomodoro Ring", config.showTimer) { onUpdate(config.copy(showTimer = it)) }
            CustomToggleRow("Show Stopwatch", config.showStopwatch) { onUpdate(config.copy(showStopwatch = it)) }
            CustomToggleRow("Show Screen Time & Alarm", config.showUsageAlarm) { onUpdate(config.copy(showUsageAlarm = it)) }
            CustomToggleRow("Show Quick Actions", config.showQuickActions) { onUpdate(config.copy(showQuickActions = it)) }
            CustomToggleRow("Show Scratchpad", config.showNote) { onUpdate(config.copy(showNote = it)) }
            CustomToggleRow("Show App Icons on Home", config.showHomeIcons) { onUpdate(config.copy(showHomeIcons = it)) }
            val ctx = LocalContext.current
            OutlinedButton(
                onClick = {
                    val intent = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choose Ambience Wallpaper")
                    ctx.startActivity(intent)
                },
                border = BorderStroke(1.dp, BorderHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Ambience Wallpaper", color = PureWhite)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

package com.rehan.monolith

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetManagerSheet(
    activeWidgets: List<String>,
    customWidgets: List<CustomWidgetSpec>,
    onSaveWidgets: (List<String>) -> Unit,
    onAddCustomWidget: (String, String) -> Unit,
    onRemoveCustomWidget: (String) -> Unit,
    onAddSystemWidget: () -> Unit,
    onDismiss: () -> Unit
) {
    val catalog = remember {
        listOf(
            "APPS_GRID" to "Apps Grid",
            "FOCUS_5217" to "52 / 17 Deep Work Ring",
            "POMODORO" to "Pomodoro 25 / 5 Ring",
            "STOPWATCH" to "Stopwatch",
            "ALARM_USAGE" to "Screen Time & Alarm",
            "USAGE_LIMITS" to "App Usage Cover (Curbox)",
            "QUICK_ACTIONS" to "Quick Actions",
            "NOTES" to "Scratchpad"
        )
    }
    var customTitle by remember { mutableStateOf("") }
    var customCmd by remember { mutableStateOf("") }

    fun labelFor(id: String): String {
        return catalog.firstOrNull { it.first == id }?.second
            ?: customWidgets.firstOrNull { it.id == id }?.title
            ?: id
    }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("WIDGET MANAGER", fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, color = TextDim)
            Button(
                onClick = onAddSystemWidget,
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add System Widget (Android)")
            }

            Text("ACTIVE DECK", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            activeWidgets.forEachIndexed { idx, id ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderHairline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${idx + 1}. ${labelFor(id)}", fontSize = 13.sp, color = PureWhite, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (idx > 0) {
                            Text("▲", fontSize = 12.sp, color = PureWhite, modifier = Modifier.clickable {
                                val list = activeWidgets.toMutableList()
                                val tmp = list[idx - 1]; list[idx - 1] = list[idx]; list[idx] = tmp
                                onSaveWidgets(list)
                            }.padding(4.dp))
                        }
                        if (activeWidgets.size > 1) {
                            Text("✕", fontSize = 12.sp, color = TextDim, modifier = Modifier.clickable {
                                if (id.startsWith("CUSTOM_")) onRemoveCustomWidget(id)
                                else onSaveWidgets(activeWidgets.filterNot { it == id })
                            }.padding(4.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderHairline)
            Text("ADD BUILT-IN WIDGET", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            catalog.filterNot { activeWidgets.contains(it.first) }.forEach { (id, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSaveWidgets(activeWidgets + id) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, fontSize = 13.sp, color = PureWhite)
                    Text("+ ADD", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = PureWhite)
                }
            }

            HorizontalDivider(color = BorderHairline)
            Text("CREATE CUSTOM WIDGET", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                placeholder = { Text("Title (e.g. Uptime)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = customCmd,
                onValueChange = { customCmd = it },
                placeholder = { Text("Shell cmd or text (e.g. uptime -p)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (customTitle.isNotBlank() && customCmd.isNotBlank()) {
                        onAddCustomWidget(customTitle, customCmd)
                        customTitle = ""
                        customCmd = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Custom Widget")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

internal data class SystemWidgetItem(
    val info: AppWidgetProviderInfo,
    val appLabel: String,
    val widgetLabel: String,
    val icon: ImageBitmap?
)

@Composable
internal fun SystemWidgetPickerDialog(
    appWidgetManager: AppWidgetManager,
    onSelectProvider: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var query by remember { mutableStateOf("") }
    val allWidgets = remember {
        appWidgetManager.installedProviders.mapNotNull { info ->
            val pkg = info.provider.packageName
            val appLabel = try {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (_: Throwable) {
                pkg
            }
            val widgetLabel = try {
                info.loadLabel(pm).ifBlank { appLabel }
            } catch (_: Throwable) {
                appLabel
            }
            val iconBmp = try {
                val d = info.loadIcon(context, 0) ?: pm.getApplicationIcon(pkg)
                drawableToImageBitmap(d)
            } catch (_: Throwable) {
                null
            }
            SystemWidgetItem(info, appLabel, widgetLabel, iconBmp)
        }.sortedWith(compareBy({ it.appLabel.lowercase(Locale.getDefault()) }, { it.widgetLabel.lowercase(Locale.getDefault()) }))
    }
    val filtered = remember(query, allWidgets) {
        if (query.isBlank()) allWidgets
        else allWidgets.filter {
            it.appLabel.contains(query, ignoreCase = true) ||
                it.widgetLabel.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBlack,
        titleContentColor = PureWhite,
        textContentColor = PureWhite,
        title = {
            Text("SYSTEM WIDGETS (${allWidgets.size})", fontSize = 13.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search Clock, Weather, Spotify...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.info.provider.flattenToString() }) { item ->
                        val sizeTag = if (item.info.minWidth >= 180) "2×1" else "1×1"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                                .clickable { onSelectProvider(item.info) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.icon != null) {
                                Image(
                                    bitmap = item.icon,
                                    contentDescription = item.widgetLabel,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.widgetLabel, fontSize = 14.sp, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.appLabel, fontSize = 11.sp, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(sizeTag, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextDim) }
        }
    )
}

package com.rehan.monolith.ui.drawer

import com.rehan.monolith.*
import com.rehan.monolith.data.*
import com.rehan.monolith.shizuku.*
import com.rehan.monolith.ui.common.*
import com.rehan.monolith.ui.home.*
import com.rehan.monolith.ui.widgets.*
import com.rehan.monolith.ui.shelf.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.install.*
import com.rehan.monolith.ui.wellbeing.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerOverlay(
    apps: List<AppEntry>,
    showFrozen: Boolean,
    autoLaunchSingle: Boolean,
    grayscaleOn: Boolean,
    pinnedPackages: Set<String>,
    focusDistractions: Set<String>,
    mindfulApps: Set<String> = emptySet(),
    usageMinutesMap: Map<String, Int> = emptyMap(),
    kidsApps: Set<String>,
    shizukuBound: Boolean,
    focusActive: Boolean = false,
    onClose: () -> Unit,
    onToggleShowFrozen: () -> Unit,
    onToggleAutoLaunch: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onExecShell: suspend (String) -> ShellResult
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var shellOutput by remember { mutableStateOf("") }

    val isShellMode = query.startsWith(">")
    val filtered = remember(apps, query, showFrozen, focusActive) {
        if (query.startsWith(">")) {
            emptyList()
        } else {
            apps.filter { entry ->
                ((!focusActive && showFrozen) || !entry.isFrozen) &&
                    (query.isBlank() ||
                        entry.label.contains(query, ignoreCase = true) ||
                        entry.packageName.contains(query, ignoreCase = true))
            }
        }
    }

    LaunchedEffect(filtered, query, autoLaunchSingle) {
        if (autoLaunchSingle && !isShellMode && query.length >= 2 && filtered.size == 1 && !filtered[0].isFrozen) {
            onLaunchApp(filtered[0])
            query = ""
        }
    }

    var drawerGrid by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (focusActive) Color(0xF8000000) else Color(0xEB050505))
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        "Search apps...",
                        fontSize = 14.sp,
                        color = TextDim
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        if (isShellMode) Icons.Rounded.Terminal else Icons.Rounded.Search,
                        contentDescription = null,
                        tint = PureWhite
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { if (query.isNotEmpty()) query = "" else onClose() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextDim)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (isShellMode && shizukuBound) {
                            val cmd = query.removePrefix(">").trim()
                            if (cmd.isNotEmpty()) {
                                scope.launch {
                                    val r = onExecShell(cmd)
                                    shellOutput = "$ $cmd\n${r.output.ifBlank { "(exit ${r.exitCode})" }}"
                                }
                            }
                        } else if (filtered.isNotEmpty() && !filtered[0].isFrozen) {
                            onLaunchApp(filtered[0])
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = LocalTileBorder.current,
                    focusedContainerColor = LocalTileSurface.current,
                    unfocusedContainerColor = LocalTileSurface.current,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
        }

        if (shellOutput.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = LocalTileSurface.current,
                contentColor = PureWhite,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LocalTileBorder.current),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("OUTPUT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                        Text("CLEAR", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = PureWhite, modifier = Modifier.clickable { shellOutput = "" })
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(shellOutput, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = PureWhite, maxLines = 10)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (!focusActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniModePill(if (drawerGrid) "GRID" else "LIST", true, { drawerGrid = !drawerGrid }, Modifier.weight(1f))
                MiniModePill("FROZEN", showFrozen, onToggleShowFrozen, Modifier.weight(1f))
                MiniModePill("1-MATCH", autoLaunchSingle, onToggleAutoLaunch, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))
        }

        if (focusActive) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onLaunchApp(app) }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            app.label,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else if (drawerGrid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 18.dp),
                modifier = Modifier.weight(1f)
            ) {
                gridItems(filtered, key = { it.packageName }) { app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) }
                            )
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (app.icon != null) {
                            Box {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.label,
                                    colorFilter = if (grayscaleOn) MonoFilter else null,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                )
                                if (mindfulApps.contains(app.packageName)) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(PureWhite)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            app.label,
                            fontSize = 11.sp,
                            color = if (app.isFrozen) TextDim else PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val usedMins = usageMinutesMap[app.packageName] ?: 0
                        if (usedMins > 0) {
                            Text(
                                "${usedMins}m",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextDim,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val isPinned = pinnedPackages.contains(app.packageName)
                    val isFocus = focusDistractions.contains(app.packageName)
                    val isKids = kidsApps.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) }
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = null,
                                colorFilter = if (grayscaleOn) MonoFilter else null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            app.label,
                            fontSize = 15.sp,
                            color = if (app.isFrozen) TextDim else PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            val usedMins = usageMinutesMap[app.packageName] ?: 0
                            if (usedMins > 0) {
                                Text(
                                    "${usedMins}m",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextDim
                                )
                            }
                            if (mindfulApps.contains(app.packageName)) MonoBadge("MINDFUL")
                            if (isPinned) MonoBadge("PIN")
                            if (isKids) MonoBadge("KIDS")
                            if (isFocus) MonoBadge("FOCUS")
                            if (app.isFrozen) MonoBadge("FROZEN")
                        }
                    }
                }
            }
        }
    }
}

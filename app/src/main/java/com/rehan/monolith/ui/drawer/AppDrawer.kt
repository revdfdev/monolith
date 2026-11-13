package com.rehan.monolith.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehan.monolith.data.AppEntry
import com.rehan.monolith.ui.common.MonoBadge
import com.rehan.monolith.ui.common.MonoFilter
import com.rehan.monolith.ui.common.PureWhite
import com.rehan.monolith.ui.common.TextDim
import kotlinx.coroutines.launch

private val DrawerBackground = Color.Black
private val SearchSurface = Color(0xFF141414)

private sealed interface DrawerRow {
    val key: String
    data class Header(val letter: Char) : DrawerRow { override val key = "h_$letter" }
    data class App(val app: AppEntry) : DrawerRow { override val key = app.packageName }
}

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
    focusActive: Boolean = false,
    onClose: () -> Unit,
    onToggleShowFrozen: () -> Unit,
    onToggleAutoLaunch: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var grid by remember { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    val showPaused = showFrozen && !focusActive
    val searching = query.isNotBlank()
    val useGrid = grid && !focusActive

    val sections = remember(apps, showPaused) { buildDrawerSections(apps, showPaused) }
    val results = remember(apps, query, showPaused) { searchApps(apps, query, showPaused) }

    // Grid view is one continuous run of icons (no half-empty rows); list view gets letter headers.
    val rows: List<DrawerRow> = remember(sections, results, searching, useGrid) {
        when {
            searching -> results.map { DrawerRow.App(it) }
            useGrid -> sections.flatMap { s -> s.apps.map { DrawerRow.App(it) } }
            else -> sections.flatMap { s -> listOf(DrawerRow.Header(s.letter)) + s.apps.map { DrawerRow.App(it) } }
        }
    }
    val letterIndex: Map<Char, Int> = remember(rows) {
        buildMap {
            rows.forEachIndexed { i, row ->
                val letter = when (row) {
                    is DrawerRow.Header -> row.letter
                    is DrawerRow.App -> sectionLetter(row.app.label)
                }
                putIfAbsent(letter, i)
            }
        }
    }

    LaunchedEffect(results, autoLaunchSingle) {
        if (autoLaunchSingle && query.trim().length >= 2 && results.size == 1 && !results[0].isFrozen) {
            onLaunchApp(results[0])
            query = ""
        }
    }

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    fun jumpTo(letter: Char) {
        val idx = letterIndex[letter] ?: return
        scope.launch { if (useGrid) gridState.scrollToItem(idx) else listState.scrollToItem(idx) }
    }

    // Pulling down while the list is already at the top closes the drawer.
    val closeThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    val pullToClose = remember(onClose, closeThreshold) {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            // Close on release, not mid-drag, so the rest of the gesture can't land on home.
            override suspend fun onPreFling(available: Velocity): Velocity {
                val close = pulled > closeThreshold
                pulled = 0f
                if (close) onClose()
                return Velocity.Zero
            }
        }
    }

    fun launchFirst() {
        val first = (if (searching) results else sections.flatMap { it.apps }).firstOrNull { !it.isFrozen }
        if (first != null) {
            onLaunchApp(first)
            query = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DrawerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("drawer")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 6.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (searching) "${results.size} results" else "${sections.sumOf { it.apps.size }} apps",
                fontSize = 13.sp,
                color = TextDim,
                modifier = Modifier.weight(1f)
            )
            if (!focusActive) {
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("drawer_menu")) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Drawer options", tint = PureWhite)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (grid) "Show as list" else "Show as grid") },
                            onClick = { grid = !grid; menuOpen = false }
                        )
                        MenuCheck("Show paused apps", showFrozen) { onToggleShowFrozen() }
                        MenuCheck("Open single search match", autoLaunchSingle) { onToggleAutoLaunch() }
                    }
                }
            }
        }

        // No stretch overscroll: leftover pull at the top must reach pullToClose.
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val listModifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .nestedScroll(pullToClose)
                .testTag("drawer_list")

            if (useGrid) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(start = 12.dp, end = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = listModifier
                ) {
                    rows.forEach { row ->
                        when (row) {
                            is DrawerRow.Header -> item(key = row.key, span = { GridItemSpan(maxLineSpan) }) {
                                SectionHeader(row.letter)
                            }
                            is DrawerRow.App -> item(key = row.key) {
                                GridApp(row.app, grayscaleOn, mindfulApps, usageMinutesMap, onLaunchApp, onLongPressApp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 12.dp, end = 4.dp, bottom = 12.dp),
                    modifier = listModifier
                ) {
                    rows.forEach { row ->
                        when (row) {
                            is DrawerRow.Header -> item(key = row.key) { SectionHeader(row.letter) }
                            is DrawerRow.App -> item(key = row.key) {
                                val pkg = row.app.packageName
                                ListApp(
                                    app = row.app,
                                    grayscaleOn = grayscaleOn,
                                    showIcon = !focusActive,
                                    usageMinutesMap = usageMinutesMap,
                                    badges = buildList {
                                        if (pkg in mindfulApps) add("MINDFUL")
                                        if (pkg in pinnedPackages) add("PIN")
                                        if (pkg in kidsApps) add("KIDS")
                                        if (pkg in focusDistractions) add("FOCUS")
                                    },
                                    onLaunchApp = onLaunchApp,
                                    onLongPressApp = onLongPressApp
                                )
                            }
                        }
                    }
                }
            }

            if (!searching && sections.size > 1) {
                LetterScrubber(sections.map { it.letter }, ::jumpTo)
            }
        }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps", fontSize = 15.sp, color = TextDim) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextDim) },
            trailingIcon = {
                IconButton(onClick = { if (query.isNotEmpty()) query = "" else onClose() }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = if (query.isNotEmpty()) "Clear search" else "Close drawer",
                        tint = TextDim
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { launchFirst() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = SearchSurface,
                unfocusedContainerColor = SearchSurface,
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite,
                cursorColor = PureWhite
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("drawer_search")
        )
    }
}

@Composable
private fun MenuCheck(label: String, checked: Boolean, onToggle: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = PureWhite, checkmarkColor = Color.Black)
            )
        },
        onClick = onToggle
    )
}

@Composable
private fun SectionHeader(letter: Char) {
    Text(
        letter.toString(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextDim,
        modifier = Modifier
            .padding(start = 8.dp, top = 14.dp, bottom = 4.dp)
            .testTag("header_$letter")
    )
}

@Composable
private fun AppIcon(app: AppEntry, size: Int, grayscaleOn: Boolean) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size.dp)) {
        val icon = app.icon
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                colorFilter = if (grayscaleOn || app.isFrozen) MonoFilter else null,
                modifier = Modifier.size(size.dp)
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(size.dp).clip(CircleShape).background(Color(0xFF2A2A2A))
            ) {
                Text(sectionLetter(app.label).toString(), color = PureWhite, fontSize = (size / 2.4).sp)
            }
        }
        if (app.isFrozen) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.42).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A3A3A))
            ) {
                Icon(
                    Icons.Rounded.Pause,
                    contentDescription = "Paused",
                    tint = PureWhite,
                    modifier = Modifier.size((size * 0.3).dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridApp(
    app: AppEntry,
    grayscaleOn: Boolean,
    mindfulApps: Set<String>,
    usageMinutesMap: Map<String, Int>,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = { onLaunchApp(app) }, onLongClick = { onLongPressApp(app) })
            .alpha(if (app.isFrozen) 0.45f else 1f)
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .testTag("app_${app.packageName}")
    ) {
        Box {
            AppIcon(app, 52, grayscaleOn)
            if (app.packageName in mindfulApps) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(PureWhite).align(Alignment.TopEnd))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(app.label, fontSize = 12.sp, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val mins = usageMinutesMap[app.packageName] ?: 0
        if (mins > 0) Text("${mins}m", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextDim, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListApp(
    app: AppEntry,
    grayscaleOn: Boolean,
    showIcon: Boolean,
    usageMinutesMap: Map<String, Int>,
    badges: List<String>,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = { onLaunchApp(app) }, onLongClick = { onLongPressApp(app) })
            .alpha(if (app.isFrozen) 0.45f else 1f)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("app_${app.packageName}")
    ) {
        if (showIcon) {
            AppIcon(app, 40, grayscaleOn)
            Spacer(Modifier.width(14.dp))
        }
        Text(
            app.label,
            fontSize = 16.sp,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val mins = usageMinutesMap[app.packageName] ?: 0
        if (mins > 0) Text("${mins}m", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
        badges.forEach {
            Spacer(Modifier.width(4.dp))
            MonoBadge(it)
        }
    }
}

@Composable
private fun LetterScrubber(letters: List<Char>, onLetter: (Char) -> Unit) {
    var height by remember { mutableIntStateOf(1) }
    fun letterAt(y: Float): Char =
        letters[(y / height * letters.size).toInt().coerceIn(0, letters.lastIndex)]
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(28.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .onSizeChanged { height = it.height.coerceAtLeast(1) }
            .pointerInput(letters) { detectTapGestures { onLetter(letterAt(it.y)) } }
            .pointerInput(letters) {
                detectVerticalDragGestures { change, _ -> onLetter(letterAt(change.position.y)) }
            }
            .testTag("scrubber")
    ) {
        letters.forEach { letter ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).testTag("scrubber_$letter")) {
                Text(letter.toString(), fontSize = 11.sp, color = TextDim, maxLines = 1)
            }
        }
    }
}

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

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext


internal val PureBlack = Color(0xFF000000)
internal val PureWhite = Color(0xFFFFFFFF)
internal val SurfaceBlack = Color(0xB80A0A0A)
internal val BorderHairline = Color(0xFF2C2C2C)
internal val TextDim = Color(0xFF888888)
internal val MonoFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
internal val LocalTileSurface = androidx.compose.runtime.compositionLocalOf { SurfaceBlack }
internal val LocalTileBorder = androidx.compose.runtime.compositionLocalOf { BorderHairline }

@Composable
internal fun systemBottomSheetContainerColor(): Color {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = isSystemInDarkTheme()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.surfaceContainer
    } else {
        if (isDark) Color(0xFF1E1E1E) else Color(0xFFF2F2F7)
    }
}

@Composable
internal fun systemBottomSheetContentColor(): Color {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = isSystemInDarkTheme()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.onSurface
    } else {
        if (isDark) PureWhite else PureBlack
    }
}

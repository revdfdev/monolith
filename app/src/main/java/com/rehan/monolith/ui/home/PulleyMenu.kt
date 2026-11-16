package com.rehan.monolith.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehan.monolith.ui.common.BorderHairline
import com.rehan.monolith.ui.common.PureWhite

data class PulleyMenuItem(
    val id: String,
    val label: String,
    val action: () -> Unit
)

class PulleyMenuState(
    val density: Density,
    val itemCount: Int,
    val maxOffsetPx: Float = 260f * density.density
) {
    var currentOffsetPx by mutableFloatStateOf(0f)
        internal set
    var activeIndex by mutableIntStateOf(-1)
        internal set

    fun calculateIndex(offsetDp: Float): Int {
        if (itemCount == 0 || offsetDp < 45f) return -1
        val indexFromBottom = ((offsetDp - 45f) / 42f).toInt()
        val clamped = indexFromBottom.coerceIn(0, itemCount - 1)
        return (itemCount - 1) - clamped
    }

    fun onDragDelta(delta: Float, haptics: HapticFeedback?) {
        val next = (currentOffsetPx + delta * 0.5f).coerceIn(0f, maxOffsetPx)
        currentOffsetPx = next
        val offsetDp = next / density.density
        val newIdx = calculateIndex(offsetDp)
        if (newIdx != activeIndex) {
            activeIndex = newIdx
            haptics?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    suspend fun release(haptics: HapticFeedback?, onAction: (Int) -> Unit) {
        val chosen = activeIndex
        activeIndex = -1
        if (currentOffsetPx > 0f) {
            try {
                Animatable(currentOffsetPx).animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                ) {
                    currentOffsetPx = this.value
                }
            } catch (_: Throwable) {
                currentOffsetPx = 0f
            }
        }
        currentOffsetPx = 0f
        if (chosen in 0 until itemCount) {
            haptics?.performHapticFeedback(HapticFeedbackType.LongPress)
            onAction(chosen)
        }
    }
}

@Composable
fun PulleyMenuView(
    items: List<PulleyMenuItem>,
    state: PulleyMenuState,
    modifier: Modifier = Modifier
) {
    val offsetDp = with(LocalDensity.current) { state.currentOffsetPx.toDp() }
    val progress = (state.currentOffsetPx / (state.density.density * 120f)).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(offsetDp)
            .clipToBounds()
            .padding(horizontal = 24.dp)
            .testTag("pulley_menu"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(PureWhite.copy(alpha = 0.3f + 0.7f * progress))
                .testTag("pulley_indicator")
        )
        Spacer(Modifier.height(12.dp))

        items.forEachIndexed { index, item ->
            val isSelected = index == state.activeIndex
            val targetAlpha = if (isSelected) 1f else 0.45f
            val alpha by animateFloatAsState(targetAlpha, tween(120), label = "itemAlpha_$index")
            val scale by animateFloatAsState(if (isSelected) 1.08f else 1f, tween(120), label = "itemScale_$index")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .testTag("pulley_item_${item.id}"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Text(
                        "•  ",
                        fontSize = 12.sp,
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = item.label,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 1.6.sp,
                    color = PureWhite.copy(alpha = alpha)
                )
                if (isSelected) {
                    Text(
                        "  •",
                        fontSize = 12.sp,
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(1.dp)
                .background(BorderHairline)
        )
        Spacer(Modifier.height(8.dp))
    }
}

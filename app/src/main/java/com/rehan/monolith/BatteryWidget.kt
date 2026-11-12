package com.rehan.monolith

import android.graphics.Canvas
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun BatteryWidget(
    stats: SysStats,
    style: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val pctFloat by animateFloatAsState(
        targetValue = (stats.batteryPct.coerceIn(0, 100)) / 100f,
        animationSpec = tween(600),
        label = "batteryPct"
    )

    Surface(
        color = LocalTileSurface.current,
        contentColor = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LocalTileBorder.current),
        modifier = modifier.clickable { onClick() }
    ) {
        AnimatedContent(
            targetState = style,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "batteryStyle"
        ) { s ->
            when (s) {
                "RING" -> Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            drawArc(
                                color = BorderHairline,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = stroke
                            )
                            drawArc(
                                color = PureWhite,
                                startAngle = -90f,
                                sweepAngle = 360f * pctFloat,
                                useCenter = false,
                                style = stroke
                            )
                        }
                        Text(
                            "${stats.batteryPct}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            if (stats.isCharging) "CHARGING" else "BATTERY",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextDim
                        )
                        Text(
                            "${stats.freeRamGb}G RAM",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                        Text(
                            "${stats.freeStorageGb}G FREE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextDim
                        )
                    }
                }

                "PILL" -> Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("POWER", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                    Text(
                        "${stats.batteryPct}%" + if (stats.isCharging) " ⚡" else "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite
                    )
                    Text(
                        "${stats.freeRamGb}G RAM • ${stats.freeStorageGb}G",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim
                    )
                }

                else -> Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (stats.isCharging) "CHARGING" else "BATTERY",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextDim
                        )
                        Text(
                            "${stats.batteryPct}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(BorderHairline)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pctFloat)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(PureWhite)
                        )
                    }
                    Text(
                        "${stats.freeRamGb}G RAM • ${stats.freeStorageGb}G FREE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim
                    )
                }
            }
        }
    }
}

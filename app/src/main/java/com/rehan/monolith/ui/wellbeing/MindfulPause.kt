package com.rehan.monolith.ui.wellbeing

import com.rehan.monolith.*
import com.rehan.monolith.data.*
import com.rehan.monolith.shizuku.*
import com.rehan.monolith.ui.common.*
import com.rehan.monolith.ui.home.*
import com.rehan.monolith.ui.widgets.*
import com.rehan.monolith.ui.drawer.*
import com.rehan.monolith.ui.shelf.*
import com.rehan.monolith.ui.kids.*
import com.rehan.monolith.ui.install.*

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun MindfulPauseDialog(
    appLabel: String,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(5) }
    val progress by animateFloatAsState(
        targetValue = (5 - secondsLeft) / 5f,
        animationSpec = tween(durationMillis = 950, easing = LinearEasing),
        label = "breathProgress"
    )

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0050505))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = SurfaceBlack,
                contentColor = PureWhite,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, BorderHairline),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {} // Consume inner click
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "CURBOX MINDFUL PAUSE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.6.sp,
                        color = TextDim
                    )

                    Spacer(Modifier.height(18.dp))

                    Box(
                        modifier = Modifier.size(92.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = PureWhite,
                            trackColor = Color(0x33FFFFFF),
                            strokeWidth = 3.dp
                        )
                        Text(
                            if (secondsLeft > 0) "${secondsLeft}s" else "✓",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        appLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = PureWhite
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        if (secondsLeft > 0) "Take a breath before diving in" else "Still intentional about opening?",
                        fontSize = 12.sp,
                        color = TextDim,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0x22FFFFFF),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDismiss() }
                        ) {
                            Text(
                                "Cancel",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextDim,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        Surface(
                            color = if (secondsLeft == 0) PureWhite else Color(0x33FFFFFF),
                            contentColor = if (secondsLeft == 0) PureBlack else TextDim,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = secondsLeft == 0) { onContinue() }
                        ) {
                            Text(
                                if (secondsLeft > 0) "Wait" else "Proceed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

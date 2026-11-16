package com.rehan.monolith.ui.install

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rehan.monolith.ui.common.BorderHairline
import com.rehan.monolith.ui.common.LocalTileBorder
import com.rehan.monolith.ui.common.LocalTileSurface
import com.rehan.monolith.ui.common.PureBlack
import com.rehan.monolith.ui.common.PureWhite
import com.rehan.monolith.ui.common.SurfaceBlack
import com.rehan.monolith.ui.common.TextDim

internal data class ApkLoadingState(
    val fileName: String = "package.apk",
    val bytesRead: Long = 0L,
    val totalBytes: Long = -1L
)

@Composable
internal fun PackageLoadingDialog(
    loadingState: ApkLoadingState,
    onRunInBackground: () -> Unit = {},
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceBlack,
            border = BorderStroke(1.dp, BorderHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = LocalTileSurface.current,
                        border = BorderStroke(1.dp, LocalTileBorder.current)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.FileDownload,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Reading Package",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            loadingState.fileName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                val hasTotal = loadingState.totalBytes > 0
                if (hasTotal) {
                    val fraction = (loadingState.bytesRead.toFloat() / loadingState.totalBytes.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PureWhite,
                        trackColor = BorderHairline
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PureWhite,
                        trackColor = BorderHairline
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sizeText = if (hasTotal) {
                        val percent = (loadingState.bytesRead * 100 / loadingState.totalBytes).toInt()
                        "${formatFileSize(loadingState.bytesRead)} / ${formatFileSize(loadingState.totalBytes)} ($percent%)"
                    } else {
                        "${formatFileSize(loadingState.bytesRead)} read"
                    }
                    Text(
                        sizeText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim
                    )
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = TextDim)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onRunInBackground,
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Background")
                    }
                }
            }
        }
    }
}

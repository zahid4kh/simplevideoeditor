package ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.VideoFile
import ui.MetadataRow
import ui.SectionLabel
import ui.formatTimeMs


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeftPanel(
    videoFile: VideoFile?,
    isLoading: Boolean,
    isTrimMode: Boolean,
    trimStart: Long,
    trimEnd: Long,
    currentPositionMs: Long,
    targetFps: Int?,
    onUploadClick: () -> Unit,
    onToggleTrimMode: () -> Unit,
    onSetTrimStart: () -> Unit,
    onSetTrimEnd: () -> Unit,
    onSetTargetFps: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onUploadClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (videoFile == null) "Upload Video" else "Change Video")
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }

        if (videoFile == null) {
            Text(
                "Upload an MP4 file\nto get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            return@Column
        }

        HorizontalDivider()

        SectionLabel("VIDEO INFO")

        MetadataRow("File", videoFile.name)
        MetadataRow("Duration", videoFile.formattedDuration)
        MetadataRow("Size", videoFile.formattedSize)
        MetadataRow(
            "FPS",
            if (videoFile.fps > 0) "%.2f fps".format(videoFile.fps) else "—"
        )

        HorizontalDivider()

        SectionLabel("OUTPUT FPS")

        val presetFps = listOf(25, 30, 60)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().animateContentSize()
        ) {
            FilterChip(
                selected = targetFps == null,
                onClick = { onSetTargetFps(null) },
                label = { Text("Source", style = MaterialTheme.typography.labelSmall) }
            )
            presetFps.forEach { fps ->
                FilterChip(
                    selected = targetFps == fps,
                    onClick = { onSetTargetFps(fps) },
                    label = { Text("$fps", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        val sliderFps = (targetFps ?: videoFile.fps.toInt().coerceIn(25, 60)).toFloat()
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "25",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    if (targetFps != null) "$targetFps fps" else "Source (${if (videoFile.fps > 0) "%.0f".format(videoFile.fps) else "?"})",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (targetFps != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    "60",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Slider(
                value = sliderFps,
                onValueChange = { onSetTargetFps(it.toInt()) },
                valueRange = 25f..60f,
                steps = 34,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (targetFps != null) {
            Text(
                "Re-encoding to $targetFps fps (slower export)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        HorizontalDivider()

        SectionLabel("EDIT")

        OutlinedButton(
            onClick = onToggleTrimMode,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isTrimMode) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(
                1.dp,
                if (isTrimMode) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.outline
            ),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isTrimMode) "Exit Trim Mode" else "Trim Video")
        }

        AnimatedVisibility(visible = isTrimMode) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Drag the timeline handles or use\nthe buttons below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Start: ${formatTimeMs(trimStart)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "End:   ${formatTimeMs(trimEnd)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Clip:  ${formatTimeMs(trimEnd - trimStart)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSetTrimStart,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FiberManualRecord, null, modifier = Modifier.size(12.dp))
                            Text(
                                "Set\nStart",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSetTrimEnd,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FiberManualRecord, null, modifier = Modifier.size(12.dp))
                            Text(
                                "Set\nEnd",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Text(
                    "Current: ${formatTimeMs(currentPositionMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

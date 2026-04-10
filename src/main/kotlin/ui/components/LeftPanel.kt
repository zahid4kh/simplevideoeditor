package ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.ImageClip
import data.TextClip
import data.VideoFile
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
    isMuted: Boolean,
    imageClips: List<ImageClip>,
    textClips: List<TextClip>,
    selectedClipId: String?,
    onUploadClick: () -> Unit,
    onToggleTrimMode: () -> Unit,
    onSetTrimStart: () -> Unit,
    onSetTrimEnd: () -> Unit,
    onSetTargetFps: (Int?) -> Unit,
    onToggleMute: () -> Unit,
    onSelectClip: (String?) -> Unit,
    onUpdateImageScale: (String, Float) -> Unit,
    onUpdateTextValue: (String, TextFieldValue) -> Unit,
    onUpdateTextFontSize: (String, Float) -> Unit,
    onUpdateTextColor: (String, Color) -> Unit,
    onUpdateTextBgColor: (String, Color) -> Unit,
    onRemoveClip: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val selectedImageClip = imageClips.find { it.id == selectedClipId }
    val selectedTextClip  = textClips.find  { it.id == selectedClipId }

    Box {
        Column(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onUploadClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerHoverIcon(PointerIcon.Hand),
                contentPadding = PaddingValues(vertical = 10.dp),
                shape = MaterialTheme.shapes.medium
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
                    "Upload an MP4 or WebM file\nto get started",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                return@Column
            }

            if (selectedClipId != null && (selectedImageClip != null || selectedTextClip != null)) {
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel(
                        if (selectedImageClip != null) "IMAGE CLIP" else "TEXT CLIP"
                    )
                    IconButton(
                        onClick = { onSelectClip(null) },
                        modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Deselect",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }

                if (selectedTextClip != null) {
                    OutlinedTextField(
                        value = selectedTextClip.textValue,
                        onValueChange = { onUpdateTextValue(selectedTextClip.id, it) },
                        label = { Text("Overlay Text", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Text),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
                    )
                }


                if (selectedImageClip != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF26A69A)
                            )
                            Text(
                                text = selectedImageClip.imagePath.substringAfterLast('/'),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }

                if (selectedImageClip != null) {
                    ClipSlider(
                        label = "Scale",
                        value = selectedImageClip.scale,
                        valueRange = 0.05f..1f,
                        displayText = "%.0f%%".format(selectedImageClip.scale * 100),
                        onValueChange = { onUpdateImageScale(selectedImageClip.id, it) }
                    )
                }

                if (selectedTextClip != null) {
                    ClipSlider(
                        label = "Font Size",
                        value = selectedTextClip.fontSize,
                        valueRange = 8f..120f,
                        displayText = "%.0fsp".format(selectedTextClip.fontSize),
                        onValueChange = { onUpdateTextFontSize(selectedTextClip.id, it) }
                    )

                    // ── Text color ─────────────────────────────────────────────
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "TEXT COLOR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            ColorSwatchRow(
                                colors = textColorPresets,
                                selectedColor = selectedTextClip.textColor,
                                matchByRgb = true,
                                onColorSelected = { onUpdateTextColor(selectedTextClip.id, it) }
                            )
                        }
                    }

                    // ── Background color ───────────────────────────────────────
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "BACKGROUND",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            ColorSwatchRow(
                                colors = bgColorPresets,
                                selectedColor = selectedTextClip.bgColor,
                                matchByRgb = true,
                                onColorSelected = { picked ->
                                    // If currently transparent, bump opacity to 70% when a color is picked
                                    val alpha = if (selectedTextClip.bgColor.alpha == 0f) 0.7f
                                                else selectedTextClip.bgColor.alpha
                                    onUpdateTextBgColor(selectedTextClip.id, picked.copy(alpha = alpha))
                                }
                            )
                            ClipSlider(
                                label = "Opacity",
                                value = selectedTextClip.bgColor.alpha,
                                valueRange = 0f..1f,
                                displayText = if (selectedTextClip.bgColor.alpha == 0f) "None"
                                              else "%.0f%%".format(selectedTextClip.bgColor.alpha * 100),
                                onValueChange = { alpha ->
                                    onUpdateTextBgColor(
                                        selectedTextClip.id,
                                        selectedTextClip.bgColor.copy(alpha = alpha)
                                    )
                                }
                            )
                            if (selectedTextClip.bgColor.alpha == 0f) {
                                Text(
                                    "Opacity 0% = no background",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onRemoveClip(selectedClipId)
                        onSelectClip(null)
                    },
                    modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remove Clip")
                }
            }

            HorizontalDivider()
            SectionLabel("VIDEO INFO")
            MetadataRow("File", videoFile.name)
            MetadataRow("Duration", videoFile.formattedDuration)
            MetadataRow("Size", videoFile.formattedSize)
            MetadataRow("FPS", if (videoFile.fps > 0) "%.2f fps".format(videoFile.fps) else "—")

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
                    label = { Text("Source", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                )
                presetFps.forEach { fps ->
                    FilterChip(
                        selected = targetFps == fps,
                        onClick = { onSetTargetFps(fps) },
                        label = { Text("$fps", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
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
                    Text("25", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(
                        if (targetFps != null) "$targetFps fps"
                        else "Source (${if (videoFile.fps > 0) "%.0f".format(videoFile.fps) else "?"})",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (targetFps != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text("60", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Slider(
                    value = sliderFps,
                    onValueChange = { onSetTargetFps(it.toInt()) },
                    valueRange = 25f..60f,
                    steps = 34,
                    modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand)
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
                modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isTrimMode) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isTrimMode) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.outline
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isTrimMode) "Exit Trim Mode" else "Trim Video")
            }

            AnimatedVisibility(visible = isTrimMode) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Drag the timeline handles or use the buttons below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Start: ${formatTimeMs(trimStart)}", style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("End:   ${formatTimeMs(trimEnd)}", style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Clip:  ${formatTimeMs(trimEnd - trimStart)}", style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.tertiary)
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
                                Text("Set\nStart", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            }
                        }
                        OutlinedButton(
                            onClick = onSetTrimEnd,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FiberManualRecord, null, modifier = Modifier.size(12.dp))
                                Text("Set\nEnd", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
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

            HorizontalDivider()
            SectionLabel("AUDIO")

            OutlinedButton(
                onClick = onToggleMute,
                modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isMuted) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isMuted) "Muted (export silent)" else "Mute Audio")
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState = scrollState),
            style = LocalScrollbarStyle.current.copy(
                hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .pointerHoverIcon(PointerIcon.Hand)
        )
    }
}

// ── Color presets ──────────────────────────────────────────────────────────────

private val textColorPresets = listOf(
    Color.White,
    Color(0xFFFFD600),   // Yellow
    Color.Black,
    Color(0xFFEF5350),   // Red
    Color(0xFF00E5FF),   // Cyan
    Color(0xFF76FF03),   // Lime
)

private val bgColorPresets = listOf(
    Color.Black,
    Color.White,
    Color(0xFF1A237E),   // Dark navy
    Color(0xFF4A148C),   // Dark purple
    Color(0xFF1B5E20),   // Dark green
    Color(0xFF880E4F),   // Dark pink
)

@Composable
private fun ColorSwatchRow(
    colors: List<Color>,
    selectedColor: Color,
    matchByRgb: Boolean,
    onColorSelected: (Color) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.forEach { color ->
            val isSelected = if (matchByRgb)
                color.red == selectedColor.red && color.green == selectedColor.green && color.blue == selectedColor.blue
            else color == selectedColor

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    // Checkmark — pick contrast color so it's visible on any swatch
                    val brightness = color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (brightness > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayText: String,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                displayText,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand)
        )
    }
}

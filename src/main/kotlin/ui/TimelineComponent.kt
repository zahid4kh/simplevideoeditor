package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import data.ImageClip
import data.TextClip
import java.awt.Cursor

private val LABEL_WIDTH = 64.dp
private val TRACK_HEIGHT = 44.dp
private val RULER_HEIGHT = 28.dp
private val HANDLE_WIDTH = 12.dp
private val CLIP_CORNER = 6.dp
private val ROW_PADDING = 4.dp

private val videoTrackColor   = Color(0xFF1565C0)
private val imageTrackColor   = Color(0xFF00695C)
private val textTrackColor    = Color(0xFF6A1B9A)
private val handleLightTint   = Color.White.copy(alpha = 0.25f)
private val playheadColor     = Color(0xFFEF5350)

@Composable
fun TimelineComponent(
    durationMs: Long,
    currentPositionMs: Long,
    trimStart: Long,
    trimEnd: Long,
    isTrimMode: Boolean,
    imageClips: List<ImageClip>,
    textClips: List<TextClip>,
    selectedClipId: String?,
    onSeek: (Long) -> Unit,
    onSelectClip: (String?) -> Unit,
    onRemoveClip: (String) -> Unit,
    onUpdateClipRange: (id: String, startMs: Long, endMs: Long) -> Unit,
    onAddImage: () -> Unit,
    onAddText: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (durationMs <= 0L) return

    val density = LocalDensity.current
    val verticalScrollState = rememberScrollState()

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            TimelineRuler(
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                trimStart = trimStart,
                trimEnd = trimEnd,
                isTrimMode = isTrimMode,
                onSeek = onSeek
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            TrackRow(
                label = "VIDEO",
                labelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            ) { trackWidthPx ->
                VideoTrackBar(
                    durationMs = durationMs,
                    currentPositionMs = currentPositionMs,
                    trackWidthPx = trackWidthPx
                )
            }

            imageClips.forEachIndexed { index, clip ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                TrackRow(
                    label = "IMG ${index + 1}",
                    labelColor = Color(0xFF26A69A)
                ) { trackWidthPx ->
                    ClipBlock(
                        clipId = clip.id,
                        startMs = clip.startMs,
                        endMs = clip.endMs,
                        durationMs = durationMs,
                        trackWidthPx = trackWidthPx,
                        baseColor = imageTrackColor,
                        label = clip.imagePath.substringAfterLast('/').take(18),
                        isSelected = clip.id == selectedClipId,
                        onSelect = { onSelectClip(clip.id) },
                        onRemove = { onRemoveClip(clip.id) },
                        onUpdateRange = onUpdateClipRange
                    )
                }
            }

            textClips.forEachIndexed { index, clip ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                TrackRow(
                    label = "TXT ${index + 1}",
                    labelColor = Color(0xFFAB47BC)
                ) { trackWidthPx ->
                    ClipBlock(
                        clipId = clip.id,
                        startMs = clip.startMs,
                        endMs = clip.endMs,
                        durationMs = durationMs,
                        trackWidthPx = trackWidthPx,
                        baseColor = textTrackColor,
                        label = clip.textValue.text.take(20).ifBlank { "Text" },
                        isSelected = clip.id == selectedClipId,
                        onSelect = { onSelectClip(clip.id) },
                        onRemove = { onRemoveClip(clip.id) },
                        onUpdateRange = onUpdateClipRange
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = LABEL_WIDTH + 8.dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AddTrackButton(
                    label = "Add Image",
                    icon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(15.dp)) },
                    tint = Color(0xFF26A69A),
                    onClick = onAddImage
                )
                AddTrackButton(
                    label = "Add Text",
                    icon = { Icon(Icons.Default.TextFields, null, modifier = Modifier.size(15.dp)) },
                    tint = Color(0xFFAB47BC),
                    onClick = onAddText
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(verticalScrollState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 2.dp)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize().zIndex(10f)) {
            val totalW = constraints.maxWidth.toFloat()
            val labelPx = with(density) { LABEL_WIDTH.toPx() }
            val trackW = totalW - labelPx
            val playheadX = labelPx + (currentPositionMs.toFloat() / durationMs * trackW)

            Box(
                modifier = Modifier
                    .absoluteOffset { androidx.compose.ui.unit.IntOffset(playheadX.toInt(), 0) }
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(playheadColor)
            )
            Canvas(modifier = Modifier
                .absoluteOffset {
                    androidx.compose.ui.unit.IntOffset(
                        (playheadX - with(density) { 6.dp.toPx() }).toInt(), 0
                    )
                }
                .size(12.dp, 10.dp)
            ) {
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width / 2f, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    },
                    color = playheadColor
                )
            }
        }
    }
}

@Composable
private fun TimelineRuler(
    durationMs: Long,
    currentPositionMs: Long,
    trimStart: Long,
    trimEnd: Long,
    isTrimMode: Boolean,
    onSeek: (Long) -> Unit
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Row(modifier = Modifier.fillMaxWidth().height(RULER_HEIGHT)) {
        Box(
            modifier = Modifier
                .width(LABEL_WIDTH)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatTimeMs(currentPositionMs),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(surfaceVariant.copy(alpha = 0.4f))
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        onSeek(((offset.x / size.width) * durationMs).toLong().coerceIn(0, durationMs))
                    }
                }
                .pointerInput(durationMs) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onSeek(((change.position.x / size.width) * durationMs).toLong().coerceIn(0, durationMs))
                    }
                }
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.TEXT_CURSOR)))
        ) {
            val w = size.width
            val h = size.height

            if (isTrimMode) {
                val s = (trimStart.toFloat() / durationMs) * w
                val e = (trimEnd.toFloat() / durationMs) * w
                drawRect(
                    color = tertiary.copy(alpha = 0.25f),
                    topLeft = Offset(s, 0f),
                    size = androidx.compose.ui.geometry.Size(e - s, h)
                )
            }

            val intervalMs = when {
                durationMs <= 15_000L  -> 1_000L
                durationMs <= 60_000L  -> 5_000L
                durationMs <= 300_000L -> 15_000L
                durationMs <= 600_000L -> 30_000L
                else                   -> 60_000L
            }
            var tick = 0L
            while (tick <= durationMs) {
                val x = (tick.toFloat() / durationMs) * w
                val isMajor = tick % (intervalMs * 5) == 0L
                drawLine(
                    color = onSurface.copy(alpha = if (isMajor) 0.4f else 0.18f),
                    start = Offset(x, if (isMajor) 0f else h * 0.5f),
                    end = Offset(x, h),
                    strokeWidth = if (isMajor) 1.5f else 1f
                )
                tick += intervalMs
            }
        }
    }
}

@Composable
private fun TrackRow(
    label: String,
    labelColor: Color,
    content: @Composable BoxScope.(trackWidthPx: Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TRACK_HEIGHT + ROW_PADDING * 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(LABEL_WIDTH)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                color = labelColor
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                .padding(vertical = ROW_PADDING)
        ) {
            val trackWidthPx = constraints.maxWidth.toFloat()
            content(trackWidthPx)
        }
    }
}

@Composable
private fun BoxScope.VideoTrackBar(
    durationMs: Long,
    currentPositionMs: Long,
    trackWidthPx: Float
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(CLIP_CORNER))
            .background(videoTrackColor.copy(alpha = 0.85f))
    ) {
        val progressFrac = (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progressFrac)
                .background(Color.White.copy(alpha = 0.12f))
        )
        Text(
            text = "VIDEO",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun BoxScope.ClipBlock(
    clipId: String,
    startMs: Long,
    endMs: Long,
    durationMs: Long,
    trackWidthPx: Float,
    baseColor: Color,
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onUpdateRange: (String, Long, Long) -> Unit
) {
    val density = LocalDensity.current
    val handleWidthPx = with(density) { HANDLE_WIDTH.toPx() }

    var localStartMs by remember(clipId) { mutableStateOf(startMs) }
    var localEndMs by remember(clipId) { mutableStateOf(endMs) }

    val startFrac = localStartMs.toFloat() / durationMs
    val widthFrac = (localEndMs - localStartMs).toFloat() / durationMs

    val clipLeftPx = startFrac * trackWidthPx
    val clipWidthPx = (widthFrac * trackWidthPx).coerceAtLeast(handleWidthPx * 2 + 4f)

    val clipLeftDp  = with(density) { clipLeftPx.toDp() }
    val clipWidthDp = with(density) { clipWidthPx.toDp() }

    Box(
        modifier = Modifier
            .offset { androidx.compose.ui.unit.IntOffset(clipLeftPx.toInt(), 0) }
            .width(clipWidthDp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(CLIP_CORNER))
            .background(baseColor.copy(alpha = if (isSelected) 1f else 0.78f))
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(CLIP_CORNER)
                ) else Modifier
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(clipId) {
                detectTapGestures(onTap = { onSelect() })
            }
            .pointerInput(clipId, durationMs, trackWidthPx) {
                var accumulatedDragMs = 0f
                detectDragGestures(
                    onDragStart = { accumulatedDragMs = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragMs += (dragAmount.x / trackWidthPx) * durationMs
                        val deltaMs = accumulatedDragMs.toLong()
                        if (deltaMs != 0L) {
                            val duration = localEndMs - localStartMs
                            val newStart = (localStartMs + deltaMs).coerceIn(0L, durationMs - duration)
                            val newEnd = newStart + duration
                            localStartMs = newStart
                            localEndMs = newEnd
                            onUpdateRange(clipId, newStart, newEnd)
                            accumulatedDragMs -= deltaMs
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HANDLE_WIDTH + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .pointerInput(clipId) { detectTapGestures { onRemove() } },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Remove clip",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(HANDLE_WIDTH)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = CLIP_CORNER, bottomStart = CLIP_CORNER))
                .background(handleLightTint)
                .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.W_RESIZE_CURSOR)))
                .pointerInput(clipId, durationMs, trackWidthPx) {
                    var accumulatedDragMs = 0f
                    detectDragGestures(
                        onDragStart = { accumulatedDragMs = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDragMs += (dragAmount.x / trackWidthPx) * durationMs
                            val deltaMs = accumulatedDragMs.toLong()
                            if (deltaMs != 0L) {
                                val newStart = (localStartMs + deltaMs).coerceIn(0L, localEndMs - 1_000L)
                                localStartMs = newStart
                                onUpdateRange(clipId, newStart, localEndMs)
                                accumulatedDragMs -= deltaMs
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                    )
                    if (it < 2) Spacer(Modifier.height(2.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(HANDLE_WIDTH)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = CLIP_CORNER, bottomEnd = CLIP_CORNER))
                .background(handleLightTint)
                .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.E_RESIZE_CURSOR)))
                .pointerInput(clipId, durationMs, trackWidthPx) {
                    var accumulatedDragMs = 0f
                    detectDragGestures(
                        onDragStart = { accumulatedDragMs = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDragMs += (dragAmount.x / trackWidthPx) * durationMs
                            val deltaMs = accumulatedDragMs.toLong()
                            if (deltaMs != 0L) {
                                val newEnd = (localEndMs + deltaMs).coerceIn(localStartMs + 1_000L, durationMs)
                                localEndMs = newEnd
                                onUpdateRange(clipId, localStartMs, newEnd)
                                accumulatedDragMs -= deltaMs
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                    )
                    if (it < 2) Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun AddTrackButton(
    label: String,
    icon: @Composable () -> Unit,
    tint: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.6f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
    ) {
        icon()
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours   = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centis  = (ms % 1000) / 10
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d.%02d".format(minutes, seconds, centis)
}

package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun TimelineComponent(
    durationMs: Long,
    currentPositionMs: Long,
    trimStart: Long,
    trimEnd: Long,
    isTrimMode: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (durationMs <= 0L) return

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val playheadColor = MaterialTheme.colorScheme.primary
    val trimHighlight = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
    val trimBorder = MaterialTheme.colorScheme.tertiary
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTimeMs(currentPositionMs),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatTimeMs(durationMs),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }

        Spacer(Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((fraction * durationMs).toLong())
                    }
                }
                .pointerInput(durationMs) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((fraction * durationMs).toLong())
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val trackH = h * 0.28f
            val trackY = h * 0.36f

            drawRect(
                color = trackColor,
                topLeft = Offset(0f, trackY),
                size = Size(w, trackH)
            )

            val progressFrac = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            drawRect(
                color = progressColor,
                topLeft = Offset(0f, trackY),
                size = Size(w * progressFrac, trackH)
            )

            if (isTrimMode) {
                val startFrac = (trimStart.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val endFrac = (trimEnd.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val rx = w * startFrac
                val rw = w * (endFrac - startFrac)

                drawRect(
                    color = trimHighlight,
                    topLeft = Offset(rx, trackY),
                    size = Size(rw, trackH)
                )

                drawLine(
                    color = trimBorder,
                    start = Offset(rx, trackY - 4f),
                    end = Offset(rx, trackY + trackH + 4f),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = trimBorder,
                    start = Offset(rx + rw, trackY - 4f),
                    end = Offset(rx + rw, trackY + trackH + 4f),
                    strokeWidth = 2.dp.toPx()
                )
            }

            val tickIntervalMs = when {
                durationMs <= 30_000L -> 5_000L
                durationMs <= 120_000L -> 15_000L
                durationMs <= 600_000L -> 60_000L
                else -> 300_000L
            }
            var tick = tickIntervalMs
            while (tick < durationMs) {
                val x = (tick.toFloat() / durationMs.toFloat()) * w
                drawLine(
                    color = tickColor,
                    start = Offset(x, trackY - 5f),
                    end = Offset(x, trackY + trackH + 5f),
                    strokeWidth = 1.dp.toPx()
                )
                tick += tickIntervalMs
            }

            val px = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * w
            drawLine(
                color = playheadColor,
                start = Offset(px, 0f),
                end = Offset(px, h),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = playheadColor,
                radius = 5.dp.toPx(),
                center = Offset(px, trackY)
            )
        }
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centis = (ms % 1000) / 10
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d.%02d".format(minutes, seconds, centis)
    }
}

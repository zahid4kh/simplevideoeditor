package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.ImageClip
import data.TextClip
import data.VideoFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import services.VideoPlayerService
import viewmodel.VideoEditorViewModel
import javax.imageio.ImageIO
import java.io.File
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun VideoPlayerComponent(
    videoPath: String,
    viewModel: VideoEditorViewModel,
    modifier: Modifier = Modifier,
    showSurface: Boolean = true,
    isMuted: Boolean = false
) {
    var initError by remember { mutableStateOf<String?>(null) }
    val playerService = remember { VideoPlayerService() }

    val isInitialized = remember {
        try {
            playerService.createComponent()
            true
        } catch (e: Exception) {
            initError = "VLC not found or failed to load: ${e.message}"
            false
        }
    }

    LaunchedEffect(initError) {
        initError?.let { viewModel.reportError(it) }
    }

    LaunchedEffect(videoPath) {
        playerService.loadMedia(videoPath)
    }

    LaunchedEffect(isMuted) {
        playerService.setMute(isMuted)
    }

    LaunchedEffect(isInitialized) {
        if (!isInitialized) return@LaunchedEffect
        var wasPlaying = false
        while (isActive) {
            delay(150.milliseconds)
            val pos = playerService.getTime()
            val dur = playerService.getDuration()
            viewModel.updatePosition(pos)
            if (dur > 0) viewModel.updateDuration(dur)
            val playing = playerService.isPlaying()
            if (wasPlaying && !playing) viewModel.onPlaybackEnded()
            wasPlaying = playing
        }
    }

    LaunchedEffect(Unit) { viewModel.playRequest.collect { playerService.play() } }
    LaunchedEffect(Unit) { viewModel.pauseRequest.collect { playerService.pause() } }
    LaunchedEffect(Unit) { viewModel.seekRequest.collect { playerService.seekTo(it) } }

    DisposableEffect(Unit) { onDispose { playerService.release() } }

    val currentFrame by playerService.frameState
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isInitialized && showSurface) {
            currentFrame?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxW = constraints.maxWidth.toFloat()
                val boxH = constraints.maxHeight.toFloat()
                val video = uiState.videoFile

                if (video != null && video.width > 0 && video.height > 0) {
                    val scaleW = boxW / video.width
                    val scaleH = boxH / video.height
                    val scale = min(scaleW, scaleH)
                    val vW = (video.width * scale).toInt()
                    val vH = (video.height * scale).toInt()
                    val vX = ((boxW - vW) / 2).toInt()
                    val vY = ((boxH - vH) / 2).toInt()

                    Box(
                        modifier = Modifier
                            .absoluteOffset { IntOffset(vX, vY) }
                            .size(
                                width = with(androidx.compose.ui.platform.LocalDensity.current) { vW.toDp() },
                                height = with(androidx.compose.ui.platform.LocalDensity.current) { vH.toDp() }
                            )
                    ) {
                        uiState.imageClips
                            .filter { uiState.currentPositionMs in it.startMs..it.endMs }
                            .forEach { clip ->
                                key(clip.id) {
                                    ImageClipOverlay(clip = clip, videoW = video.width, videoH = video.height, displayW = vW, displayH = vH)
                                }
                            }

                        uiState.textClips
                            .filter { uiState.currentPositionMs in it.startMs..it.endMs }
                            .forEach { clip ->
                                key(clip.id) {
                                    TextClipOverlay(clip = clip, videoW = video.width, displayW = vW, displayH = vH)
                                }
                            }
                    }
                }
            }
        } else if (!isInitialized) {
            Text("Failed to initialize player", color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun BoxScope.ImageClipOverlay(clip: ImageClip, videoW: Int, videoH: Int, displayW: Int, displayH: Int) {
    val bitmap = remember(clip.imagePath) {
        runCatching { ImageIO.read(File(clip.imagePath))?.toComposeImageBitmap() }.getOrNull()
    } ?: return

    val imgW = (videoW * clip.scale).toInt().coerceAtLeast(1)
    val imgH = if (bitmap.width > 0)
        (imgW.toFloat() / bitmap.width * bitmap.height).toInt().coerceAtLeast(1)
    else imgW

    val offsetX = (clip.xFraction * videoW - imgW / 2f).toInt().coerceIn(0, (videoW - imgW).coerceAtLeast(0))
    val offsetY = (clip.yFraction * videoH - imgH / 2f).toInt().coerceIn(0, (videoH - imgH).coerceAtLeast(0))

    val displayScaleX = displayW.toFloat() / videoW
    val displayScaleY = displayH.toFloat() / videoH

    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .absoluteOffset { IntOffset((offsetX * displayScaleX).toInt(), (offsetY * displayScaleY).toInt()) }
            .size(
                width = with(androidx.compose.ui.platform.LocalDensity.current) { (imgW * displayScaleX).toInt().toDp() },
                height = with(androidx.compose.ui.platform.LocalDensity.current) { (imgH * displayScaleY).toInt().toDp() }
            )
    )
}

@Composable
private fun TextClipOverlay(clip: TextClip, videoW: Int, displayW: Int, displayH: Int) {
    val displayScale = displayW.toFloat() / videoW

    Text(
        text = clip.textValue.text.ifBlank { " " },
        fontSize = (clip.fontSize * displayScale).sp,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        lineHeight = (clip.fontSize * displayScale * 1.2f).sp,
        style = androidx.compose.material3.LocalTextStyle.current.copy(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.85f),
                offset = Offset(2f * displayScale, 2f * displayScale),
                blurRadius = 6f * displayScale
            )
        ),
        modifier = Modifier
            .widthIn(max = with(androidx.compose.ui.platform.LocalDensity.current) { displayW.toDp() })
            .onSizeChanged { }
            .wrapContentSize()
            .drawWithContent {
                drawContent()
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(displayW, displayH) {
                    val x = (clip.xFraction * displayW - placeable.width / 2f).toInt()
                        .coerceIn(0, displayW - placeable.width)
                    val y = (clip.yFraction * displayH - placeable.height / 2f).toInt()
                        .coerceIn(0, displayH - placeable.height)
                    placeable.placeRelative(x, y)
                }
            }
    )
}

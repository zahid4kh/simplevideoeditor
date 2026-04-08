package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import services.VideoPlayerService
import viewmodel.VideoEditorViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun VideoPlayerComponent(
    videoPath: String,
    viewModel: VideoEditorViewModel,
    modifier: Modifier = Modifier
) {
    var initError by remember(videoPath) { mutableStateOf<String?>(null) }
    val playerService = remember(videoPath) { VideoPlayerService() }
    val mediaPlayerComponent = remember(videoPath) {
        try {
            playerService.createPlayer(videoPath)
        } catch (e: Exception) {
            initError = "VLC not found or failed to load: ${e.message}"
            null
        }
    }

    LaunchedEffect(initError) {
        initError?.let { viewModel.reportError(it) }
    }

    LaunchedEffect(mediaPlayerComponent) {
        if (mediaPlayerComponent == null) return@LaunchedEffect
        var wasPlaying = false
        while (isActive) {
            delay(150.milliseconds)
            val pos = playerService.getTime()
            val dur = playerService.getDuration()
            viewModel.updatePosition(pos)
            if (dur > 0) viewModel.updateDuration(dur)

            val playing = playerService.isPlaying()
            if (wasPlaying && !playing) {
                viewModel.onPlaybackEnded()
            }
            wasPlaying = playing
        }
    }

    LaunchedEffect(Unit) {
        viewModel.playRequest.collect { playerService.play() }
    }

    LaunchedEffect(Unit) {
        viewModel.pauseRequest.collect { playerService.pause() }
    }

    LaunchedEffect(Unit) {
        viewModel.seekRequest.collect { positionMs -> playerService.seekTo(positionMs) }
    }

    DisposableEffect(videoPath) {
        onDispose { playerService.release() }
    }

    if (mediaPlayerComponent != null) {
        SwingPanel(
            background = Color.Black,
            modifier = modifier,
            factory = { mediaPlayerComponent }
        )
    } else {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Failed to initialize player", color = Color.White.copy(alpha = 0.6f))
        }
    }
}

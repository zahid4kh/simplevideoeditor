package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.ExportStatus
import data.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import services.FFmpegService
import java.io.File

class VideoEditorViewModel : ViewModel() {
    private val ffmpegService = FFmpegService()

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val seekRequest = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val playRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pauseRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun loadVideo(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, showFileChooser = false) }

            val file = File(path)
            if (!file.exists()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "File not found: $path") }
                return@launch
            }

            val metadata = withContext(Dispatchers.IO) { ffmpegService.extractMetadata(path) }

            val durationMs = ((metadata["duration"]?.toDoubleOrNull() ?: 0.0) * 1000).toLong()
            val fps = metadata["fps"]?.toDoubleOrNull() ?: 0.0

            val videoFile = VideoFile(
                path = path,
                name = file.name,
                durationMs = durationMs,
                sizeBytes = file.length(),
                fps = fps
            )

            _uiState.update {
                it.copy(
                    videoFile = videoFile,
                    isLoading = false,
                    isPlaying = false,
                    currentPositionMs = 0L,
                    trimStart = 0L,
                    trimEnd = durationMs,
                    isTrimMode = false,
                    exportStatus = ExportStatus.IDLE,
                    exportMessage = ""
                )
            }
        }
    }

    fun play() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = true) }
            playRequest.emit(Unit)
        }
    }

    fun pause() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = false) }
            pauseRequest.emit(Unit)
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        val duration = _uiState.value.videoFile?.durationMs ?: return
        val clamped = positionMs.coerceIn(0L, duration)
        viewModelScope.launch {
            _uiState.update { it.copy(currentPositionMs = clamped) }
            seekRequest.emit(clamped)
        }
    }

    fun updatePosition(positionMs: Long) {
        if (positionMs > 0) {
            _uiState.update { it.copy(currentPositionMs = positionMs) }
        }
    }

    fun updateDuration(durationMs: Long) {
        if (durationMs > 0) {
            _uiState.update { state ->
                state.copy(
                    videoFile = state.videoFile?.copy(durationMs = durationMs),
                    trimEnd = if (state.trimEnd == 0L) durationMs else state.trimEnd
                )
            }
        }
    }

    fun onPlaybackEnded() {
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun setTrimStart(ms: Long) {
        val end = _uiState.value.trimEnd
        _uiState.update { it.copy(trimStart = ms.coerceAtMost(end - 1000L).coerceAtLeast(0L)) }
    }

    fun setTrimEnd(ms: Long) {
        val start = _uiState.value.trimStart
        val duration = _uiState.value.videoFile?.durationMs ?: 0L
        _uiState.update {
            it.copy(trimEnd = ms.coerceAtLeast(start + 1000L).coerceAtMost(duration))
        }
    }

    fun toggleTrimMode() {
        _uiState.update { it.copy(isTrimMode = !it.isTrimMode) }
    }

    fun setTargetFps(fps: Int?) {
        _uiState.update { it.copy(targetFps = fps) }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun exportTrimmed() {
        val state = _uiState.value
        val videoFile = state.videoFile ?: return

        val inputFile = File(videoFile.path)
        val baseName = inputFile.nameWithoutExtension
        val outputFileName = "${baseName}_${System.currentTimeMillis()}.mp4"
        val outputPath = File(inputFile.parentFile, outputFileName).absolutePath

        viewModelScope.launch {
            _uiState.update { it.copy(exportStatus = ExportStatus.RUNNING, exportMessage = "Exporting…") }

            val success = ffmpegService.trimVideo(
                inputPath = videoFile.path,
                outputPath = outputPath,
                startMs = state.trimStart,
                endMs = state.trimEnd,
                targetFps = state.targetFps,
                muteAudio = state.isMuted
            )

            _uiState.update {
                it.copy(
                    exportStatus = if (success) ExportStatus.SUCCESS else ExportStatus.ERROR,
                    exportMessage = if (success) "Saved to $outputPath" else "Export failed. Make sure FFmpeg is installed."
                )
            }

            delay(3000)
            dismissExportStatus()
        }
    }

    fun reportError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isLoading = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissExportStatus() {
        _uiState.update { it.copy(exportStatus = ExportStatus.IDLE, exportMessage = "") }
    }

    fun openFileChooser() {
        _uiState.update { it.copy(showFileChooser = true) }
    }

    fun closeFileChooser() {
        _uiState.update { it.copy(showFileChooser = false) }
    }

    data class EditorUiState(
        val videoFile: VideoFile? = null,
        val isPlaying: Boolean = false,
        val currentPositionMs: Long = 0L,
        val trimStart: Long = 0L,
        val trimEnd: Long = 0L,
        val targetFps: Int? = null,
        val exportStatus: ExportStatus = ExportStatus.IDLE,
        val exportMessage: String = "",
        val errorMessage: String? = null,
        val isLoading: Boolean = false,
        val isTrimMode: Boolean = false,
        val isMuted: Boolean = false,
        val showFileChooser: Boolean = false
    )
}

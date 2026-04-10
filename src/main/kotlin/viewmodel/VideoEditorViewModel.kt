package viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.ExportStatus
import data.ImageClip
import data.TextClip
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
            val width = metadata["width"]?.toIntOrNull() ?: 0
            val height = metadata["height"]?.toIntOrNull() ?: 0

            val videoFile = VideoFile(
                path = path,
                name = file.name,
                durationMs = durationMs,
                sizeBytes = file.length(),
                fps = fps,
                width = width,
                height = height
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

    // ── Trim ───────────────────────────────────────────────────────────────────

    fun setTrimStart(ms: Long) {
        val end = _uiState.value.trimEnd
        _uiState.update { it.copy(trimStart = ms.coerceAtMost(end - 1000L).coerceAtLeast(0L)) }
    }

    fun setTrimEnd(ms: Long) {
        val start = _uiState.value.trimStart
        val duration = _uiState.value.videoFile?.durationMs ?: 0L
        _uiState.update { it.copy(trimEnd = ms.coerceAtLeast(start + 1000L).coerceAtMost(duration)) }
    }

    fun toggleTrimMode() {
        _uiState.update { it.copy(isTrimMode = !it.isTrimMode) }
    }

    // ── Output settings ────────────────────────────────────────────────────────

    fun setTargetFps(fps: Int?) {
        _uiState.update { it.copy(targetFps = fps) }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    // ── Image clips ────────────────────────────────────────────────────────────

    fun addImageClip(imagePath: String) {
        val state = _uiState.value
        val durationMs = state.videoFile?.durationMs ?: return
        val startMs = state.currentPositionMs
        val endMs = (startMs + 3_000L).coerceAtMost(durationMs)
        val clip = ImageClip(imagePath = imagePath, startMs = startMs, endMs = endMs)
        _uiState.update {
            it.copy(
                imageClips = it.imageClips + clip,
                selectedClipId = clip.id,
                showImageChooser = false
            )
        }
    }

    fun updateImageClipScale(clipId: String, scale: Float) {
        _uiState.update { state ->
            state.copy(
                imageClips = state.imageClips.map {
                    if (it.id == clipId) it.copy(scale = scale.coerceIn(0.05f, 1f)) else it
                }
            )
        }
    }

    // ── Text clips ─────────────────────────────────────────────────────────────

    fun addTextClip() {
        val state = _uiState.value
        val durationMs = state.videoFile?.durationMs ?: return
        val startMs = state.currentPositionMs
        val endMs = (startMs + 3_000L).coerceAtMost(durationMs)
        val clip = TextClip(startMs = startMs, endMs = endMs)
        _uiState.update {
            it.copy(
                textClips = it.textClips + clip,
                selectedClipId = clip.id
            )
        }
    }

    fun updateTextClipValue(clipId: String, value: TextFieldValue) {
        _uiState.update { state ->
            state.copy(
                textClips = state.textClips.map {
                    if (it.id == clipId) it.copy(textValue = value) else it
                }
            )
        }
    }

    fun updateTextClipFontSize(clipId: String, fontSize: Float) {
        _uiState.update { state ->
            state.copy(
                textClips = state.textClips.map {
                    if (it.id == clipId) it.copy(fontSize = fontSize.coerceIn(8f, 120f)) else it
                }
            )
        }
    }

    fun updateTextClipTextColor(clipId: String, color: Color) {
        _uiState.update { state ->
            state.copy(
                textClips = state.textClips.map {
                    if (it.id == clipId) it.copy(textColor = color) else it
                }
            )
        }
    }

    fun updateTextClipBgColor(clipId: String, color: Color) {
        _uiState.update { state ->
            state.copy(
                textClips = state.textClips.map {
                    if (it.id == clipId) it.copy(bgColor = color) else it
                }
            )
        }
    }

    fun removeClip(clipId: String) {
        _uiState.update { state ->
            state.copy(
                imageClips = state.imageClips.filter { it.id != clipId },
                textClips = state.textClips.filter { it.id != clipId },
                selectedClipId = if (state.selectedClipId == clipId) null else state.selectedClipId
            )
        }
    }

    fun updateClipRange(clipId: String, startMs: Long, endMs: Long) {
        val durationMs = _uiState.value.videoFile?.durationMs ?: return
        val s = startMs.coerceIn(0L, durationMs - 1_000L)
        val e = endMs.coerceIn(s + 1_000L, durationMs)
        _uiState.update { state ->
            state.copy(
                imageClips = state.imageClips.map {
                    if (it.id == clipId) it.copy(startMs = s, endMs = e) else it
                },
                textClips = state.textClips.map {
                    if (it.id == clipId) it.copy(startMs = s, endMs = e) else it
                }
            )
        }
    }

    fun updateClipPosition(clipId: String, xFraction: Float, yFraction: Float) {
        val x = xFraction.coerceIn(0f, 1f)
        val y = yFraction.coerceIn(0f, 1f)
        _uiState.update { state ->
            state.copy(
                imageClips = state.imageClips.map {
                    if (it.id == clipId) it.copy(xFraction = x, yFraction = y) else it
                },
                textClips = state.textClips.map {
                    if (it.id == clipId) it.copy(xFraction = x, yFraction = y) else it
                }
            )
        }
    }

    fun selectClip(clipId: String?) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun openFileChooser() {
        _uiState.update { it.copy(showFileChooser = true) }
    }

    fun closeFileChooser() {
        _uiState.update { it.copy(showFileChooser = false) }
    }

    fun openImageChooser() {
        _uiState.update { it.copy(showImageChooser = true) }
    }

    fun closeImageChooser() {
        _uiState.update { it.copy(showImageChooser = false) }
    }

    fun exportTrimmed() {
        val state = _uiState.value
        val videoFile = state.videoFile ?: return

        val inputFile = File(videoFile.path)
        val baseName = inputFile.nameWithoutExtension
        val extension = inputFile.extension.lowercase().ifEmpty { "mp4" }
        val outputFileName = "${baseName}_${System.currentTimeMillis()}.$extension"
        val outputPath = File(inputFile.parentFile, outputFileName).absolutePath

        viewModelScope.launch {
            _uiState.update { it.copy(exportStatus = ExportStatus.RUNNING, exportMessage = "Exporting…") }

            val success = ffmpegService.trimVideo(
                inputPath = videoFile.path,
                outputPath = outputPath,
                startMs = state.trimStart,
                endMs = state.trimEnd,
                targetFps = state.targetFps,
                muteAudio = state.isMuted,
                imageClips = state.imageClips,
                textClips = state.textClips
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

    // ── UI State ───────────────────────────────────────────────────────────────

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
        val showFileChooser: Boolean = false,
        // Clip tracks
        val imageClips: List<ImageClip> = emptyList(),
        val textClips: List<TextClip> = emptyList(),
        val selectedClipId: String? = null,
        val showImageChooser: Boolean = false
    )
}

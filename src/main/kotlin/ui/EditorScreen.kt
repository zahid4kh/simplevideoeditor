package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import deskit.dialogs.file.filechooser.FileChooserDialog
import ui.components.EditorTopBar
import ui.components.EmptyVideoState
import ui.components.LeftPanel
import ui.components.PlaybackControls
import viewmodel.MainViewModel
import viewmodel.VideoEditorViewModel
import java.awt.Cursor

@Composable
fun EditorScreen(
    mainViewModel: MainViewModel,
    editorViewModel: VideoEditorViewModel
) {
    val uiState by editorViewModel.uiState.collectAsState()
    val mainUiState by mainViewModel.uiState.collectAsState()

    var leftPanelWidthDp by remember { mutableStateOf(260.dp) }
    var isDraggingDivider by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val openFileChooser: () -> Unit = editorViewModel::openFileChooser
    val hideVideoSurface = uiState.errorMessage != null

    Scaffold(
        topBar = {
            EditorTopBar(
                darkMode = mainUiState.darkMode,
                onToggleDarkMode = mainViewModel::toggleDarkMode,
                hasVideo = uiState.videoFile != null,
                exportStatus = uiState.exportStatus,
                onExport = editorViewModel::exportTrimmed
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Top area: left panel + video canvas ────────────────────────────
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LeftPanel(
                    videoFile = uiState.videoFile,
                    isLoading = uiState.isLoading,
                    isTrimMode = uiState.isTrimMode,
                    trimStart = uiState.trimStart,
                    trimEnd = uiState.trimEnd,
                    currentPositionMs = uiState.currentPositionMs,
                    targetFps = uiState.targetFps,
                    isMuted = uiState.isMuted,
                    imageClips = uiState.imageClips,
                    textClips = uiState.textClips,
                    selectedClipId = uiState.selectedClipId,
                    onUploadClick = openFileChooser,
                    onToggleTrimMode = editorViewModel::toggleTrimMode,
                    onSetTrimStart = { editorViewModel.setTrimStart(uiState.currentPositionMs) },
                    onSetTrimEnd = { editorViewModel.setTrimEnd(uiState.currentPositionMs) },
                    onSetTargetFps = editorViewModel::setTargetFps,
                    onToggleMute = editorViewModel::toggleMute,
                    onSelectClip = editorViewModel::selectClip,
                    onUpdateClipPosition = editorViewModel::updateClipPosition,
                    onUpdateImageScale = editorViewModel::updateImageClipScale,
                    onUpdateTextValue = editorViewModel::updateTextClipValue,
                    onUpdateTextFontSize = editorViewModel::updateTextClipFontSize,
                    onRemoveClip = editorViewModel::removeClip,
                    modifier = Modifier
                        .width(leftPanelWidthDp)
                        .fillMaxHeight()
                )

                // Resizable divider
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxHeight()
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isDraggingDivider = true },
                                onDragEnd = { isDraggingDivider = false },
                                onDragCancel = { isDraggingDivider = false }
                            ) { change, dragAmount ->
                                change.consume()
                                val deltaDp = with(density) { dragAmount.x.toDp() }
                                leftPanelWidthDp = (leftPanelWidthDp + deltaDp).coerceIn(180.dp, 480.dp)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    VerticalDivider(
                        color = if (isDraggingDivider) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                }

                // Video canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.videoFile != null) {
                        VideoPlayerComponent(
                            videoPath = uiState.videoFile!!.path,
                            viewModel = editorViewModel,
                            modifier = Modifier.fillMaxSize(),
                            showSurface = !hideVideoSurface,
                            isMuted = uiState.isMuted
                        )
                    } else if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        EmptyVideoState(onUpload = openFileChooser)
                    }
                }
            }

            // ── Bottom: playback controls + multi-track timeline ───────────────
            AnimatedVisibility(visible = uiState.videoFile != null && !uiState.isLoading) {
                Column {
                    HorizontalDivider()
                    PlaybackControls(
                        isPlaying = uiState.isPlaying,
                        onPlayPause = editorViewModel::togglePlayPause,
                        onRewind = { editorViewModel.seekTo(0L) },
                        onSkipEnd = { editorViewModel.seekTo(uiState.videoFile?.durationMs ?: 0L) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp)
                    )
                    HorizontalDivider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 320.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        TimelineComponent(
                            durationMs = uiState.videoFile?.durationMs ?: 0L,
                            currentPositionMs = uiState.currentPositionMs,
                            trimStart = uiState.trimStart,
                            trimEnd = uiState.trimEnd,
                            isTrimMode = uiState.isTrimMode,
                            imageClips = uiState.imageClips,
                            textClips = uiState.textClips,
                            selectedClipId = uiState.selectedClipId,
                            onSeek = editorViewModel::seekTo,
                            onSelectClip = editorViewModel::selectClip,
                            onRemoveClip = editorViewModel::removeClip,
                            onUpdateClipRange = editorViewModel::updateClipRange,
                            onAddImage = editorViewModel::openImageChooser,
                            onAddText = editorViewModel::addTextClip,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = editorViewModel::dismissError,
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage!!) },
            confirmButton = {
                TextButton(onClick = editorViewModel::dismissError) { Text("OK") }
            }
        )
    }

    if (uiState.showFileChooser) {
        FileChooserDialog(
            title = "Select Video (MP4 or WebM)",
            allowedExtensions = listOf("mp4", "webm"),
            resizableFileInfoDialog = true,
            onFileSelected = { file -> editorViewModel.loadVideo(file.absolutePath) },
            onCancel = editorViewModel::closeFileChooser
        )
    }

    if (uiState.showImageChooser) {
        FileChooserDialog(
            title = "Select Image",
            allowedExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp"),
            resizableFileInfoDialog = true,
            onFileSelected = { file -> editorViewModel.addImageClip(file.absolutePath) },
            onCancel = editorViewModel::closeImageChooser
        )
    }
}

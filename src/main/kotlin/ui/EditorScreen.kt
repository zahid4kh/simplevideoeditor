package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.ExportStatus
import data.VideoFile
import ui.components.EditorTopBar
import ui.components.EmptyVideoState
import ui.components.LeftPanel
import ui.components.PlaybackControls
import viewmodel.MainViewModel
import viewmodel.VideoEditorViewModel
import java.awt.Cursor
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

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

    val openFileChooser: () -> Unit = {
        SwingUtilities.invokeLater {
            val chooser = JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("MP4 Videos (*.mp4)", "mp4", "MP4")
                dialogTitle = "Select MP4 Video"
                fileSelectionMode = JFileChooser.FILES_ONLY
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                editorViewModel.loadVideo(chooser.selectedFile.absolutePath)
            }
        }
    }

    val openSaveDialog: () -> Unit = saveDialog@{
        val videoFile = uiState.videoFile ?: return@saveDialog
        SwingUtilities.invokeLater {
            val default = videoFile.name.removeSuffix(".mp4").removeSuffix(".MP4") + "_export.mp4"
            val chooser = JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("MP4 Videos (*.mp4)", "mp4")
                dialogTitle = "Save Exported Video"
                selectedFile = java.io.File(videoFile.path).parentFile?.let { java.io.File(it, default) }
                    ?: java.io.File(default)
            }
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                var path = chooser.selectedFile.absolutePath
                if (!path.endsWith(".mp4", ignoreCase = true)) path += ".mp4"
                editorViewModel.exportTrimmed(path)
            }
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                darkMode = mainUiState.darkMode,
                onToggleDarkMode = mainViewModel::toggleDarkMode,
                hasVideo = uiState.videoFile != null,
                exportStatus = uiState.exportStatus,
                onExport = openSaveDialog
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LeftPanel(
                    videoFile = uiState.videoFile,
                    isLoading = uiState.isLoading,
                    isTrimMode = uiState.isTrimMode,
                    trimStart = uiState.trimStart,
                    trimEnd = uiState.trimEnd,
                    currentPositionMs = uiState.currentPositionMs,
                    targetFps = uiState.targetFps,
                    onUploadClick = openFileChooser,
                    onToggleTrimMode = editorViewModel::toggleTrimMode,
                    onSetTrimStart = { editorViewModel.setTrimStart(uiState.currentPositionMs) },
                    onSetTrimEnd = { editorViewModel.setTrimEnd(uiState.currentPositionMs) },
                    onSetTargetFps = editorViewModel::setTargetFps,
                    modifier = Modifier
                        .width(leftPanelWidthDp)
                        .fillMaxHeight()
                )

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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else if (uiState.videoFile != null) {
                        VideoPlayerComponent(
                            videoPath = uiState.videoFile!!.path,
                            viewModel = editorViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        EmptyVideoState(onUpload = openFileChooser)
                    }
                }
            }

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
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        TimelineComponent(
                            durationMs = uiState.videoFile?.durationMs ?: 0L,
                            currentPositionMs = uiState.currentPositionMs,
                            trimStart = uiState.trimStart,
                            trimEnd = uiState.trimEnd,
                            isTrimMode = uiState.isTrimMode,
                            onSeek = editorViewModel::seekTo,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

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

    if (uiState.exportStatus == ExportStatus.SUCCESS || uiState.exportStatus == ExportStatus.ERROR) {
        AlertDialog(
            onDismissRequest = editorViewModel::dismissExportStatus,
            icon = {
                Icon(
                    if (uiState.exportStatus == ExportStatus.SUCCESS) Icons.Default.CheckCircle
                    else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (uiState.exportStatus == ExportStatus.SUCCESS)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(if (uiState.exportStatus == ExportStatus.SUCCESS) "Export Complete" else "Export Failed")
            },
            text = { Text(uiState.exportMessage) },
            confirmButton = {
                TextButton(onClick = editorViewModel::dismissExportStatus) { Text("OK") }
            }
        )
    }
}

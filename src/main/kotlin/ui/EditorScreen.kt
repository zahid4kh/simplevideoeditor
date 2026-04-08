package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    hasVideo: Boolean,
    exportStatus: ExportStatus,
    onExport: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "Simple Video Editor",
                style = MaterialTheme.typography.titleMedium
            )
        },
        actions = {
            IconButton(onClick = onToggleDarkMode) {
                Icon(
                    imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme"
                )
            }
            if (hasVideo) {
                val isExporting = exportStatus == ExportStatus.RUNNING
                Button(
                    onClick = onExport,
                    enabled = !isExporting,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (isExporting) "Exporting…" else "Export")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeftPanel(
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.width(64.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onSkipEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onRewind) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Rewind to start")
        }
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onSkipEnd) {
            Icon(Icons.Default.SkipNext, contentDescription = "Skip to end")
        }
    }
}

@Composable
private fun EmptyVideoState(onUpload: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color.White.copy(alpha = 0.2f)
        )
        Text(
            "No video loaded",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Upload an MP4 file from the left panel\nor click below to get started.",
            color = Color.White.copy(alpha = 0.35f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        OutlinedButton(
            onClick = onUpload,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Upload MP4")
        }
    }
}

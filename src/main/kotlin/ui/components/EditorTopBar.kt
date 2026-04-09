package ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import data.ExportStatus


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
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
            IconButton(onClick = onToggleDarkMode, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                Icon(
                    imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme"
                )
            }
            if (hasVideo) {
                val isExporting = exportStatus == ExportStatus.RUNNING
                val isSuccess = exportStatus == ExportStatus.SUCCESS
                val isError = exportStatus == ExportStatus.ERROR
                Button(
                    onClick = onExport,
                    enabled = !isExporting,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
                    shape = MaterialTheme.shapes.medium,
                    colors = when {
                        isSuccess -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                        isError -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                        else -> ButtonDefaults.buttonColors()
                    }
                ) {
                    when {
                        isExporting -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Exporting…")
                        }
                        isSuccess -> {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Saved!")
                        }
                        isError -> {
                            Icon(Icons.Default.Error, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export Failed")
                        }
                        else -> {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export")
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}


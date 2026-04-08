package ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
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


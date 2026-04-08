package data

import kotlinx.serialization.Serializable


@Serializable
data class AppSettings(
    val darkMode: Boolean = false
)

data class VideoFile(
    val path: String,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val fps: Double
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }

    val formattedSize: String
        get() = when {
            sizeBytes >= 1_073_741_824L -> "%.1f GB".format(sizeBytes / 1_073_741_824.0)
            sizeBytes >= 1_048_576L -> "%.1f MB".format(sizeBytes / 1_048_576.0)
            sizeBytes >= 1024L -> "%.1f KB".format(sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }
}

enum class ExportStatus { IDLE, RUNNING, SUCCESS, ERROR }
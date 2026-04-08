package services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FFmpegService {

    suspend fun trimVideo(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val startSecs = startMs / 1000.0
        val durationSecs = (endMs - startMs) / 1000.0

        val cmd = listOf(
            "ffmpeg", "-y",
            "-ss", "%.3f".format(startSecs),
            "-i", inputPath,
            "-t", "%.3f".format(durationSecs),
            "-c", "copy",
            "-avoid_negative_ts", "make_zero",
            outputPath
        )

        runCommand(cmd, onProgress)
    }

    suspend fun mergeVideos(
        inputPaths: List<String>,
        outputPath: String,
        onProgress: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val listFile = File.createTempFile("ffmpeg_merge_", ".txt")
        try {
            listFile.writeText(inputPaths.joinToString("\n") { "file '$it'" })

            val cmd = listOf(
                "ffmpeg", "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", listFile.absolutePath,
                "-c", "copy",
                outputPath
            )

            runCommand(cmd, onProgress)
        } finally {
            listFile.delete()
        }
    }

    suspend fun extractMetadata(inputPath: String): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>()
        try {
            val process = ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-print_format", "json",
                "-show_streams", "-show_format",
                inputPath
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val durationRegex = """"duration"\s*:\s*"([\d.]+)"""".toRegex()
            val fpsRegex = """"r_frame_rate"\s*:\s*"(\d+)/(\d+)"""".toRegex()

            durationRegex.find(output)?.groupValues?.get(1)?.let {
                result["duration"] = it
            }
            fpsRegex.find(output)?.groupValues?.let { groups ->
                if (groups.size >= 3) {
                    val num = groups[1].toDoubleOrNull() ?: 0.0
                    val den = groups[2].toDoubleOrNull() ?: 1.0
                    if (den != 0.0) result["fps"] = (num / den).toString()
                }
            }
        } catch (_: Exception) {
            // ffprobe not available; caller will fall back to vlcj duration
        }
        result
    }

    private fun runCommand(cmd: List<String>, onProgress: (String) -> Unit): Boolean {
        return try {
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().forEachLine { line ->
                onProgress(line)
            }

            process.waitFor() == 0
        } catch (e: Exception) {
            onProgress("Error: ${e.message}")
            false
        }
    }
}

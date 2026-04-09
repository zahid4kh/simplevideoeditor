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
        targetFps: Int? = null,
        muteAudio: Boolean = false,
        onProgress: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val startSecs = startMs / 1000.0
        val durationSecs = (endMs - startMs) / 1000.0

        val isWebm = outputPath.endsWith(".webm", ignoreCase = true)

        val cmd = buildList {
            add("ffmpeg"); add("-y")
            add("-ss"); add("%.3f".format(startSecs))
            add("-i"); add(inputPath)
            add("-t"); add("%.3f".format(durationSecs))
            if (targetFps != null) {
                add("-vf"); add("fps=$targetFps")
                add("-c:v"); add(if (isWebm) "libvpx-vp9" else "libx264")
                if (muteAudio) add("-an") else { add("-c:a"); add("copy") }
            } else {
                if (muteAudio) { add("-c:v"); add("copy"); add("-an") }
                else { add("-c"); add("copy") }
            }
            add("-avoid_negative_ts"); add("make_zero")
            add(outputPath)
        }

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
            val avgFpsRegex = """"avg_frame_rate"\s*:\s*"(\d+)/(\d+)"""".toRegex()
            val realFpsRegex = """"r_frame_rate"\s*:\s*"(\d+)/(\d+)"""".toRegex()

            durationRegex.find(output)?.groupValues?.get(1)?.let {
                result["duration"] = it
            }

            fun parseRate(regex: Regex): Double? {
                val groups = regex.find(output)?.groupValues ?: return null
                val num = groups[1].toDoubleOrNull() ?: return null
                val den = groups[2].toDoubleOrNull()?.takeIf { it != 0.0 } ?: return null
                val fps = num / den
                return if (fps in 1.0..300.0) fps else null
            }

            val fps = parseRate(avgFpsRegex) ?: parseRate(realFpsRegex)
            if (fps != null) result["fps"] = fps.toString()
        } catch (_: Exception) {
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

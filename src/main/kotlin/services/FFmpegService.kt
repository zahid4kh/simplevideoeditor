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
        imageClips: List<data.ImageClip> = emptyList(),
        textClips: List<data.TextClip> = emptyList(),
        onProgress: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val startSecs = startMs / 1000.0
        val durationSecs = (endMs - startMs) / 1000.0

        val isWebm = outputPath.endsWith(".webm", ignoreCase = true)

        val cmd = buildList {
            add("ffmpeg"); add("-y")
            add("-ss"); add("%.3f".format(startSecs))
            add("-i"); add(inputPath)

            imageClips.forEach { clip ->
                add("-i"); add(clip.imagePath)
            }

            add("-t"); add("%.3f".format(durationSecs))

            val filterParts = mutableListOf<String>()

            imageClips.forEachIndexed { index, clip ->
                val inputIdx = index + 1
                val overlayStart = (clip.startMs - startMs) / 1000.0
                val overlayEnd = (clip.endMs - startMs) / 1000.0

                val label = "img$index"
                filterParts.add("[$inputIdx:v]scale=iw*${clip.scale}:-1[${label}_scaled]")

                val xPos = "W*${clip.xFraction}-w/2"
                val yPos = "H*${clip.yFraction}-h/2"

                val prevLabel = if (index == 0) "0:v" else "over$index"
                val nextLabel = "over${index + 1}"

                filterParts.add("[$prevLabel][${label}_scaled]overlay=x=$xPos:y=$yPos:enable='between(t,%.3f,%.3f)'[$nextLabel]".format(overlayStart, overlayEnd))
            }

            var lastOverlayLabel = if (imageClips.isEmpty()) "0:v" else "over${imageClips.size}"
            textClips.forEachIndexed { index, clip ->
                val overlayStart = (clip.startMs - startMs) / 1000.0
                val overlayEnd = (clip.endMs - startMs) / 1000.0
                val nextLabel = "txt$index"

                val xPos = "w*${clip.xFraction}-tw*${clip.xFraction}"
                val yPos = "h*${clip.yFraction}-th*${clip.yFraction}"

                val fontFile = File("src/main/composeResources/font/JetBrainsMono-Bold.ttf")
                val fontArg = if (fontFile.exists()) {
                    val escapedFontPath = fontFile.absolutePath.replace("\\", "/").replace(":", "\\:").replace("'", "'\\\\''")
                    ":fontfile='$escapedFontPath'"
                } else ""

                val tempTextFile = File.createTempFile("ffmpeg_text_", ".txt")
                tempTextFile.writeText(clip.textValue.text)
                val escapedTextPath = tempTextFile.absolutePath.replace("\\", "/").replace(":", "\\:").replace("'", "'\\\\''")

                val filterParams = "textfile='$escapedTextPath':fontsize=${clip.fontSize}:fontcolor=white$fontArg:x='clip($xPos,0,w-tw)':y='clip($yPos,0,h-th)':enable='between(t,%.3f,%.3f)':box=1:boxcolor=black@0.4:boxborderw=5:line_spacing=5".format(overlayStart, overlayEnd)
                filterParts.add("[$lastOverlayLabel]drawtext=$filterParams[$nextLabel]")
                lastOverlayLabel = nextLabel
            }

            if (targetFps != null) {
                filterParts.add("[$lastOverlayLabel]fps=$targetFps[finalv]")
                lastOverlayLabel = "finalv"
            }

            if (filterParts.isNotEmpty()) {
                add("-filter_complex"); add(filterParts.joinToString(";"))
                add("-map"); add("[$lastOverlayLabel]")
                if (!muteAudio) {
                    add("-map"); add("0:a?")
                }
            }

            if (targetFps != null || filterParts.isNotEmpty()) {
                if (isWebm) {
                    add("-c:v"); add("libvpx-vp9")
                    add("-b:v"); add("0")
                    add("-crf"); add("33")
                    add("-deadline"); add("good")
                    add("-cpu-used"); add("2")
                } else {
                    add("-c:v"); add("libx264")
                    add("-crf"); add("23")
                    add("-preset"); add("medium")
                    add("-pix_fmt"); add("yuv420p")
                }
                if (muteAudio) {
                    add("-an")
                } else {
                    if (filterParts.isEmpty()) {
                        add("-c:a"); add("copy")
                    } else {
                        add("-c:a"); add(if (isWebm) "libopus" else "aac")
                    }
                }
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
            val widthRegex = """"width"\s*:\s*(\d+)""".toRegex()
            val heightRegex = """"height"\s*:\s*(\d+)""".toRegex()

            durationRegex.find(output)?.groupValues?.get(1)?.let {
                result["duration"] = it
            }

            widthRegex.find(output)?.groupValues?.get(1)?.let {
                result["width"] = it
            }

            heightRegex.find(output)?.groupValues?.get(1)?.let {
                result["height"] = it
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

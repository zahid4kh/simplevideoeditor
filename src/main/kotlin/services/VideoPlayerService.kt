package services

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VideoPlayerService {
    private var mediaPlayerComponent: CallbackMediaPlayerComponent? = null
    private var currentUrl: String? = null

    val frameState: MutableState<ImageBitmap?> = mutableStateOf(null)

    private var cachedFrame: BufferedImage? = null

    private val bufferFormatCallback = object : BufferFormatCallback {
        override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
            cachedFrame = null
            return RV32BufferFormat(sourceWidth, sourceHeight)
        }

        override fun allocatedBuffers(buffers: Array<out ByteBuffer>) {}
    }

    private val renderCallback = object : RenderCallback {
        override fun display(
            mediaPlayer: MediaPlayer,
            nativeBuffers: Array<out ByteBuffer>,
            bufferFormat: BufferFormat
        ) {
            val buffer = nativeBuffers[0]
            val w = bufferFormat.width
            val h = bufferFormat.height
            if (w <= 0 || h <= 0) return

            val bi = cachedFrame?.takeIf { it.width == w && it.height == h }
                ?: BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB).also { cachedFrame = it }

            val data = (bi.raster.dataBuffer as DataBufferInt).data
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.asIntBuffer().get(data)
            buffer.rewind()

            frameState.value = bi.toComposeImageBitmap()
        }
    }

    fun createComponent(): CallbackMediaPlayerComponent {
        val component = CallbackMediaPlayerComponent(
            null, null, null, true,
            renderCallback, bufferFormatCallback, null
        )
        mediaPlayerComponent = component
        return component
    }

    fun loadMedia(url: String) {
        currentUrl = url
        val player = mediaPlayerComponent?.mediaPlayer() ?: return
        player.controls().stop()
        player.media().prepare(url)
    }

    fun play() {
        val player = mediaPlayerComponent?.mediaPlayer() ?: return
        if (player.media().isValid) {
            player.controls().play()
        } else {
            currentUrl?.let { player.media().play(it) }
        }
    }

    fun pause() {
        mediaPlayerComponent?.mediaPlayer()?.controls()?.pause()
    }

    fun seekTo(positionMs: Long) {
        mediaPlayerComponent?.mediaPlayer()?.controls()?.setTime(positionMs)
    }

    fun getTime(): Long =
        mediaPlayerComponent?.mediaPlayer()?.status()?.time() ?: 0L

    fun getDuration(): Long =
        mediaPlayerComponent?.mediaPlayer()?.status()?.length() ?: 0L

    fun isPlaying(): Boolean =
        mediaPlayerComponent?.mediaPlayer()?.status()?.isPlaying ?: false

    fun setVolume(volume: Int) {
        mediaPlayerComponent?.mediaPlayer()?.audio()?.setVolume(volume.coerceIn(0, 200))
    }

    fun setMute(muted: Boolean) {
        mediaPlayerComponent?.mediaPlayer()?.audio()?.isMute = muted
    }

    fun release() {
        mediaPlayerComponent?.mediaPlayer()?.release()
        mediaPlayerComponent = null
        currentUrl = null
    }
}

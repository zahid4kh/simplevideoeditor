package services

import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

class VideoPlayerService {
    private var mediaPlayerComponent: EmbeddedMediaPlayerComponent? = null
    private var currentUrl: String? = null

    fun createPlayer(url: String): EmbeddedMediaPlayerComponent {
        mediaPlayerComponent?.mediaPlayer()?.release()
        val component = EmbeddedMediaPlayerComponent()
        mediaPlayerComponent = component
        currentUrl = url
        component.mediaPlayer().media().prepare(url)
        return component
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

    fun release() {
        mediaPlayerComponent?.mediaPlayer()?.release()
        mediaPlayerComponent = null
        currentUrl = null
    }
}

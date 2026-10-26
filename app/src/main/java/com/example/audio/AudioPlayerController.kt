package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val isLoaded: Boolean = false,
    val error: String? = null
)

class AudioPlayerController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun loadAudio(audioBytes: ByteArray, filename: String = "personal_voice_${System.currentTimeMillis()}.wav") {
        stop()
        try {
            val file = AudioUtils.saveAudioToCache(context, audioBytes, filename)
            currentFile = file

            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    progressJob?.cancel()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = 0
                    )
                }
                setOnErrorListener { _, what, extra ->
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        error = "MediaPlayer error ($what, $extra)"
                    )
                    true
                }
            }

            val duration = mediaPlayer?.duration ?: 0
            _playbackState.value = PlaybackState(
                isPlaying = false,
                currentPositionMs = 0,
                durationMs = duration,
                isLoaded = true,
                error = null
            )
        } catch (e: Exception) {
            _playbackState.value = PlaybackState(
                isPlaying = false,
                isLoaded = false,
                error = "Failed to load audio: ${e.message}"
            )
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        if (!player.isPlaying) {
            player.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startProgressTracker()
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            progressJob?.cancel()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
        }
    }

    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        player.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun stop() {
        progressJob?.cancel()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        _playbackState.value = PlaybackState()
    }

    fun getCurrentAudioFile(): File? = currentFile

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                val total = mediaPlayer?.duration ?: _playbackState.value.durationMs
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = true,
                    currentPositionMs = current,
                    durationMs = total
                )
                delay(100)
            }
        }
    }

    fun release() {
        stop()
    }
}

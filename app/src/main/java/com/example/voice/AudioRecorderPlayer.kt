package com.example.voice

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val activeNoteId: Long? = null,
    val currentPositionMs: Int = 0,
    val totalDurationMs: Int = 0
)

class AudioRecorderPlayer(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var recordingTimerRunnable: Runnable? = null
    private var playbackProgressRunnable: Runnable? = null

    // --- Recording ---
    fun startRecording(targetFile: File): Boolean {
        if (_isRecording.value) return false
        stopPlayback()

        return try {
            currentRecordingFile = targetFile
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(targetFile.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationMs.value = 0L

            startRecordingTimer()
            true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Recording start failed: ${e.message}")
            recorder?.release()
            recorder = null
            _isRecording.value = false
            false
        }
    }

    private fun startRecordingTimer() {
        recordingTimerRunnable = object : Runnable {
            override fun run() {
                if (_isRecording.value) {
                    _recordingDurationMs.value = System.currentTimeMillis() - recordingStartTime
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.post(recordingTimerRunnable!!)
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        recordingTimerRunnable?.let { handler.removeCallbacks(it) }

        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            _isRecording.value = false
            val file = currentRecordingFile
            currentRecordingFile = null
            file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Recording stop failed: ${e.message}")
            recorder?.release()
            recorder = null
            _isRecording.value = false
            null
        }
    }

    fun cancelRecording() {
        val file = stopRecording()
        file?.delete()
    }

    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
    }

    // --- Playback ---
    fun playNote(noteId: Long, filePath: String) {
        if (_playbackState.value.isPlaying && _playbackState.value.activeNoteId == noteId) {
            pausePlayback()
            return
        }

        stopPlayback()

        try {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                }
                start()
            }
            player = mediaPlayer

            val duration = mediaPlayer.duration
            _playbackState.value = PlaybackState(
                isPlaying = true,
                activeNoteId = noteId,
                currentPositionMs = 0,
                totalDurationMs = duration
            )

            startPlaybackProgressTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Playback error: ${e.message}")
            stopPlayback()
        }
    }

    fun pausePlayback() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
            } else {
                it.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startPlaybackProgressTracker()
            }
        }
    }

    fun seekTo(positionMs: Int) {
        player?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun stopPlayback() {
        playbackProgressRunnable?.let { handler.removeCallbacks(it) }
        try {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        player = null
        _playbackState.value = PlaybackState(
            isPlaying = false,
            activeNoteId = null,
            currentPositionMs = 0,
            totalDurationMs = 0
        )
    }

    private fun startPlaybackProgressTracker() {
        playbackProgressRunnable = object : Runnable {
            override fun run() {
                player?.let {
                    if (it.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = it.currentPosition,
                            totalDurationMs = it.duration
                        )
                        handler.postDelayed(this, 150)
                    }
                }
            }
        }
        handler.post(playbackProgressRunnable!!)
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}

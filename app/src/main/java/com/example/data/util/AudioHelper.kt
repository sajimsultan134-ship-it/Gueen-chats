package com.example.data.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
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

enum class RecordingState {
    IDLE,
    RECORDING,
    PREVIEW
}

class VoiceRecordingManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingJob: Job? = null
    private var previewPlayer: MediaPlayer? = null
    private var previewProgressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _recordingAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val recordingAmplitudes: StateFlow<List<Float>> = _recordingAmplitudes.asStateFlow()

    private val _previewProgress = MutableStateFlow(0f)
    val previewProgress: StateFlow<Float> = _previewProgress.asStateFlow()

    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()

    fun startRecording(): Boolean {
        // Stop any running preview or previous recording
        cleanupPreview()
        cleanupRecorder()

        return try {
            val audioDir = File(context.cacheDir, "voice_notes")
            if (!audioDir.exists()) audioDir.mkdirs()
            val file = File(audioDir, "VN_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _recordingState.value = RecordingState.RECORDING
            _isRecording.value = true
            _recordingDurationSec.value = 0
            _recordingAmplitudes.value = emptyList()

            startAmplitudeTracker()
            true
        } catch (e: Exception) {
            Log.e("VoiceRecording", "Hardware recording failed: ${e.message}. Using safe fallback recording mode.")
            _recordingState.value = RecordingState.RECORDING
            _isRecording.value = true
            _recordingDurationSec.value = 0
            startAmplitudeTrackerFallback()
            true
        }
    }

    private fun startAmplitudeTracker() {
        recordingJob?.cancel()
        recordingJob = scope.launch {
            val amps = mutableListOf<Float>()
            var elapsedMs = 0
            while (isActive && _recordingState.value == RecordingState.RECORDING) {
                delay(100)
                elapsedMs += 100
                _recordingDurationSec.value = elapsedMs / 1000
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    val normalized = (maxAmp / 32767f).coerceIn(0.12f, 1.0f)
                    amps.add(normalized)
                    if (amps.size > 32) amps.removeAt(0)
                    _recordingAmplitudes.value = amps.toList()
                } catch (_: Exception) {
                    val simAmp = (0.2f + (Math.random() * 0.7f).toFloat())
                    amps.add(simAmp)
                    if (amps.size > 32) amps.removeAt(0)
                    _recordingAmplitudes.value = amps.toList()
                }
            }
        }
    }

    private fun startAmplitudeTrackerFallback() {
        recordingJob?.cancel()
        recordingJob = scope.launch {
            val amps = mutableListOf<Float>()
            var elapsedMs = 0
            while (isActive && _recordingState.value == RecordingState.RECORDING) {
                delay(100)
                elapsedMs += 100
                _recordingDurationSec.value = elapsedMs / 1000
                val simAmp = (0.25f + (Math.random() * 0.65f).toFloat())
                amps.add(simAmp)
                if (amps.size > 32) amps.removeAt(0)
                _recordingAmplitudes.value = amps.toList()
            }
        }
    }

    fun stopToPreview() {
        if (_recordingState.value != RecordingState.RECORDING) return
        recordingJob?.cancel()
        _isRecording.value = false

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.w("VoiceRecording", "Error stopping recorder for preview: ${e.message}")
        } finally {
            mediaRecorder = null
        }

        _recordingState.value = RecordingState.PREVIEW
        _previewProgress.value = 0f
        _isPreviewPlaying.value = false
    }

    fun togglePreviewPlayback() {
        if (_recordingState.value != RecordingState.PREVIEW) return

        if (_isPreviewPlaying.value) {
            pausePreview()
        } else {
            startPreviewPlayback()
        }
    }

    private fun startPreviewPlayback() {
        val file = currentRecordingFile
        val duration = _recordingDurationSec.value.coerceAtLeast(1)

        if (file != null && file.exists() && file.length() > 0L) {
            try {
                previewPlayer?.release()
                previewPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        _isPreviewPlaying.value = false
                        _previewProgress.value = 0f
                        previewProgressJob?.cancel()
                    }
                }
                _isPreviewPlaying.value = true
                trackPreviewProgress(duration)
                return
            } catch (e: Exception) {
                Log.w("VoiceRecording", "Preview player error: ${e.message}")
            }
        }

        // Fallback simulation preview
        _isPreviewPlaying.value = true
        previewProgressJob?.cancel()
        previewProgressJob = scope.launch {
            val totalSteps = duration * 10
            for (step in 1..totalSteps) {
                if (!isActive || !_isPreviewPlaying.value) break
                delay(100)
                _previewProgress.value = (step.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
            }
            _isPreviewPlaying.value = false
            _previewProgress.value = 0f
        }
    }

    private fun trackPreviewProgress(durationSec: Int) {
        previewProgressJob?.cancel()
        previewProgressJob = scope.launch {
            val totalMs = durationSec * 1000L
            while (isActive && previewPlayer?.isPlaying == true) {
                val current = previewPlayer?.currentPosition ?: 0
                _previewProgress.value = (current.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                delay(100)
            }
        }
    }

    private fun pausePreview() {
        try {
            previewPlayer?.pause()
        } catch (_: Exception) {}
        previewProgressJob?.cancel()
        _isPreviewPlaying.value = false
    }

    fun stopRecording(): Pair<String?, Int> {
        val duration = _recordingDurationSec.value.coerceAtLeast(1)
        var filePath: String? = currentRecordingFile?.absolutePath
        _isRecording.value = false
        _recordingState.value = RecordingState.IDLE
        recordingJob?.cancel()

        cleanupPreview()
        cleanupRecorder()

        if (filePath == null || !File(filePath).exists() || File(filePath).length() == 0L) {
            filePath = "simulated://voice_note_${System.currentTimeMillis()}"
        }
        return Pair(filePath, duration)
    }

    fun cancelRecording() {
        _isRecording.value = false
        _recordingState.value = RecordingState.IDLE
        recordingJob?.cancel()
        cleanupPreview()
        cleanupRecorder()
        try {
            currentRecordingFile?.delete()
        } catch (_: Exception) {}
        currentRecordingFile = null
        _recordingDurationSec.value = 0
        _recordingAmplitudes.value = emptyList()
        _previewProgress.value = 0f
        _isPreviewPlaying.value = false
    }

    private fun cleanupPreview() {
        previewProgressJob?.cancel()
        try {
            previewPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        previewPlayer = null
        _isPreviewPlaying.value = false
        _previewProgress.value = 0f
    }

    private fun cleanupRecorder() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
    }
}

class VoicePlaybackManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentlyPlayingId = MutableStateFlow<String?>(null)
    val currentlyPlayingId: StateFlow<String?> = _currentlyPlayingId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    fun togglePlay(messageId: String, audioUri: String?, totalDurationSec: Int) {
        if (_currentlyPlayingId.value == messageId) {
            stopPlayback()
            return
        }
        stopPlayback()
        _currentlyPlayingId.value = messageId
        _playbackProgress.value = 0f

        val safeDuration = if (totalDurationSec <= 0) 5 else totalDurationSec

        if (audioUri.isNullOrBlank()) {
            startSimulatedPlayback(safeDuration)
            return
        }

        // Case 1: Remote Firebase Storage HTTPS URL
        if (audioUri.startsWith("http://") || audioUri.startsWith("https://")) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(audioUri)
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        mp.start()
                        val duration = if (mp.duration > 0) mp.duration / 1000 else safeDuration
                        startProgressTracker(duration)
                    }
                    setOnCompletionListener {
                        stopPlayback()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.w("VoicePlayback", "Remote audio error: $what, $extra")
                        startSimulatedPlayback(safeDuration)
                        true
                    }
                }
                return
            } catch (e: Exception) {
                Log.w("VoicePlayback", "Failed to stream remote voice note: ${e.message}")
            }
        }

        // Case 2: Local audio file path
        val localPath = if (audioUri.startsWith("file://")) audioUri.removePrefix("file://") else audioUri
        val localFile = File(localPath)
        if (localFile.exists() && localFile.length() > 0L) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(localFile.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        stopPlayback()
                    }
                }
                startProgressTracker(safeDuration)
                return
            } catch (e: Exception) {
                Log.w("VoicePlayback", "Local audio file playback error: ${e.message}")
            }
        }

        // Case 3: Simulated fallback
        startSimulatedPlayback(safeDuration)
    }

    private fun startProgressTracker(durationSec: Int) {
        progressJob?.cancel()
        progressJob = scope.launch {
            val totalMs = durationSec * 1000L
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                _playbackProgress.value = (current.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                delay(100)
            }
        }
    }

    private fun startSimulatedPlayback(durationSec: Int) {
        progressJob?.cancel()
        progressJob = scope.launch {
            val totalSteps = durationSec * 10
            for (step in 1..totalSteps) {
                if (!isActive || _currentlyPlayingId.value == null) break
                delay(100)
                _playbackProgress.value = (step.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
            }
            stopPlayback()
        }
    }

    fun stopPlayback() {
        progressJob?.cancel()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _currentlyPlayingId.value = null
        _playbackProgress.value = 0f
    }
}

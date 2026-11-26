package com.example.pickup.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

// Reads the microphone with AudioRecord and sends out overlapping blocks of
// samples (values from -1 to 1) for the PitchDetector. Not covered in the labs.
// Reference: https://developer.android.com/reference/android/media/AudioRecord
// Reference: https://developer.android.com/reference/android/media/MediaRecorder.AudioSource
class AudioInput(private val context: Context) {

    companion object {
        // 44100 Hz is the only rate the AudioRecord docs guarantee on every device
        const val SAMPLE_RATE = 44100
        // 4096 samples = 93 ms, enough to hold several cycles of the low strings
        const val BLOCK_SIZE = 4096
        // Each block shares half its samples with the last, so readings come
        // twice as often (about 21 per second)
        private const val HOP_SIZE = BLOCK_SIZE / 2
    }

    // Recording only runs while something collects this Flow. Cancelling the
    // collector (leaving the screen) stops the loop and releases the mic.
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun blocks(): Flow<FloatArray> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT
        )
        val recorder = AudioRecord(
            chooseSource(),
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT, // floats from -1 to 1, no conversion needed
            maxOf(minBuffer, BLOCK_SIZE * 4 * 2) // bytes: 4 per float, room for 2 blocks
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Microphone unavailable" }

        val block = FloatArray(BLOCK_SIZE)
        val hop = FloatArray(HOP_SIZE)
        try {
            recorder.startRecording()
            while (currentCoroutineContext().isActive) {
                val read = recorder.read(hop, 0, HOP_SIZE, AudioRecord.READ_BLOCKING)
                if (read < 0) error("Microphone read failed: $read")
                // Slide the old samples left and put the new ones at the end
                System.arraycopy(block, HOP_SIZE, block, 0, BLOCK_SIZE - HOP_SIZE)
                System.arraycopy(hop, 0, block, BLOCK_SIZE - HOP_SIZE, HOP_SIZE)
                emit(block.copyOf())
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO) // blocking reads must stay off the main thread

    // UNPROCESSED skips noise suppression and automatic volume, which can
    // distort a guitar note. Not every phone supports it, so fall back to
    // VOICE_RECOGNITION, which is also lightly processed.
    private fun chooseSource(): Int {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val unprocessed = audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
        return if (unprocessed == "true") MediaRecorder.AudioSource.UNPROCESSED
        else MediaRecorder.AudioSource.VOICE_RECOGNITION
    }
}

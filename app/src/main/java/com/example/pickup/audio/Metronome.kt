package com.example.pickup.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

// Plays a steady click and vibrates on every beat. Not covered in the labs.
//
// Timing: instead of a timer (which drifts), each beat is written to the
// speaker as a block of audio exactly one beat long: a short click followed by
// silence. The speaker plays samples at a fixed rate, so the beats stay exactly
// in time. AudioTrack then tells us each time playback reaches the start of a
// beat, which is when the vibration and the beat dots are updated.
// Reference: https://developer.android.com/reference/android/media/AudioTrack
// Reference: https://developer.android.com/reference/android/media/AudioTrack.OnPlaybackPositionUpdateListener
// Reference: https://developer.android.com/reference/android/os/VibrationEffect
class Metronome(context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100
        const val MIN_BPM = 40
        const val MAX_BPM = 240
        private const val CHUNK = 1024 // samples written at a time, so Stop is quick
    }

    // null if the phone has no vibration motor
    private val vibrator: Vibrator? = context.getSystemService(VibratorManager::class.java)
        ?.defaultVibrator?.takeIf { it.hasVibrator() }
    val canVibrate: Boolean get() = vibrator != null

    // Which beat of the bar is sounding (0 = first beat), or null when stopped
    private val _beat = MutableStateFlow<Int?>(null)
    val beat: StateFlow<Int?> = _beat.asStateFlow()

    private var job: Job? = null

    fun start(scope: CoroutineScope, bpm: Int, beatsPerBar: Int, sound: Boolean, vibrate: Boolean) {
        stop()
        val samplesPerBeat = SAMPLE_RATE * 60 / bpm
        // The first beat of each bar is higher and louder so you can hear where the bar starts
        val accentBeat = beatAudio(samplesPerBeat, frequency = 1600f, volume = if (sound) 0.9f else 0f)
        val normalBeat = beatAudio(samplesPerBeat, frequency = 1000f, volume = if (sound) 0.6f else 0f)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .build()
            )
            .setBufferSizeInBytes(
                AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        // Called on the main thread each time playback passes another whole beat
        var beatsPlayed = 0
        track.setPositionNotificationPeriod(samplesPerBeat)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onPeriodicNotification(audioTrack: AudioTrack) {
                beatsPlayed++
                onBeat(beatsPlayed % beatsPerBar, vibrate)
            }
            override fun onMarkerReached(audioTrack: AudioTrack) = Unit
        }, Handler(Looper.getMainLooper()))

        job = scope.launch(Dispatchers.IO) {
            try {
                track.play()
                onBeat(0, vibrate) // the very first beat has no notification
                var beatIndex = 0
                while (isActive) {
                    val audio = if (beatIndex % beatsPerBar == 0) accentBeat else normalBeat
                    var offset = 0
                    // write() waits while the speaker's buffer is full, which keeps this loop in time
                    while (offset < audio.size && isActive) {
                        val count = minOf(CHUNK, audio.size - offset)
                        offset += track.write(audio, offset, count, AudioTrack.WRITE_BLOCKING).coerceAtLeast(0)
                    }
                    beatIndex++
                }
            } finally {
                // stop() has already cleared the beat; this only frees the speaker
                track.pause()
                track.flush()
                track.release()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _beat.value = null
    }

    private fun onBeat(beatInBar: Int, vibrate: Boolean) {
        _beat.value = beatInBar
        if (vibrate) {
            // Predefined effects feel crisp and match the phone's own haptics
            val effect = if (beatInBar == 0) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_CLICK
            vibrator?.vibrate(VibrationEffect.createPredefined(effect))
        }
    }

    // One beat of audio: a 30 ms "tock" that fades out fast, then silence
    private fun beatAudio(samplesPerBeat: Int, frequency: Float, volume: Float): FloatArray {
        val audio = FloatArray(samplesPerBeat)
        val clickLength = minOf(SAMPLE_RATE * 30 / 1000, samplesPerBeat)
        for (i in 0 until clickLength) {
            val time = i.toFloat() / SAMPLE_RATE
            audio[i] = volume * exp(-time / 0.008f) * sin(2 * PI * frequency * time).toFloat()
        }
        return audio
    }
}

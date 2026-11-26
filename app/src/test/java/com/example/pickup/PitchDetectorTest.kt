package com.example.pickup

import com.example.pickup.audio.AudioInput
import com.example.pickup.audio.NoteFrequency
import com.example.pickup.audio.PitchDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// Tests the tuner maths with made-up signals, so no microphone is needed.
class PitchDetectorTest {
    private val rate = AudioInput.SAMPLE_RATE
    private val detector = PitchDetector(rate)

    // A pure sine wave at the given frequency
    private fun sine(frequency: Float, amplitude: Float = 0.5f) =
        FloatArray(AudioInput.BLOCK_SIZE) { i -> amplitude * sin(2 * PI * frequency * i / rate).toFloat() }

    // Closer to a real string: a fundamental plus strong harmonics
    private fun guitarLike(frequency: Float) = FloatArray(AudioInput.BLOCK_SIZE) { i ->
        val t = 2 * PI * frequency * i / rate
        (0.3 * sin(t) + 0.4 * sin(2 * t) + 0.2 * sin(3 * t) + 0.1 * sin(4 * t)).toFloat()
    }

    private fun assertCentsClose(expected: Float, actual: Float?, toleranceCents: Float = 2f) {
        assertNotNull(actual)
        val cents = abs(NoteFrequency.centsBetween(actual!!, expected))
        assert(cents <= toleranceCents) { "expected $expected Hz, got $actual Hz ($cents cents off)" }
    }

    @Test fun noteFrequencies_matchStandardPitch() {
        assertEquals(440f, NoteFrequency.frequencyOf("A4"), 0.01f)
        assertEquals(110f, NoteFrequency.frequencyOf("A2"), 0.01f)
        assertEquals(82.41f, NoteFrequency.frequencyOf("E2"), 0.01f)
        assertEquals(77.78f, NoteFrequency.frequencyOf("Eb2"), 0.01f)
        assertEquals(185.00f, NoteFrequency.frequencyOf("F#3"), 0.01f)
    }

    @Test fun cents_signAndSize() {
        assertEquals(100f, NoteFrequency.centsBetween(NoteFrequency.frequencyOf("A#2"), 110f), 0.01f)
        assertEquals(-1200f, NoteFrequency.centsBetween(55f, 110f), 0.01f)
    }

    @Test fun detects_everyStandardString() {
        listOf("E2", "A2", "D3", "G3", "B3", "E4").forEach {
            assertCentsClose(NoteFrequency.frequencyOf(it), detector.detect(sine(NoteFrequency.frequencyOf(it))))
        }
    }

    @Test fun detects_lowDropDAndHalfStepDown() {
        assertCentsClose(73.42f, detector.detect(sine(73.42f)))   // D2
        assertCentsClose(77.78f, detector.detect(sine(77.78f)))   // Eb2
    }

    @Test fun detects_fundamental_notAHarmonic() {
        // The 2nd harmonic is the loudest part here; YIN must still find 82 Hz, not 165 Hz
        assertCentsClose(82.41f, detector.detect(guitarLike(82.41f)))
        assertCentsClose(146.83f, detector.detect(guitarLike(146.83f)))
    }

    @Test fun slightlyFlat_isMeasured() {
        val flat = 110f * 0.993f // about 12 cents flat
        assertCentsClose(flat, detector.detect(sine(flat)), 1f)
    }

    @Test fun silence_givesNull() {
        assertNull(detector.detect(FloatArray(AudioInput.BLOCK_SIZE)))
        assertNull(detector.detect(sine(110f, amplitude = 0.001f)))
    }

    @Test fun noise_givesNull() {
        val random = Random(1)
        assertNull(detector.detect(FloatArray(AudioInput.BLOCK_SIZE) { random.nextFloat() * 2 - 1 }))
    }
}

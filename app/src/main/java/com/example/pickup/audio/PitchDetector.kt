package com.example.pickup.audio

import kotlin.math.sqrt

// Finds the pitch (in Hz) of a block of microphone samples using the YIN
// algorithm. Plain Kotlin with no Android code, so it can be unit tested with
// made-up signals (see PitchDetectorTest).
//
// The idea: a note repeats itself every "period". YIN compares the signal with
// a copy of itself shifted by tau samples. The shift where they match best is
// the period, and frequency = sampleRate / period.
//
// Steps 2-5 below follow the paper:
// Reference: A. de Cheveigné and H. Kawahara, "YIN, a fundamental frequency
//   estimator for speech and music", J. Acoust. Soc. Am. 111(4), 1917-1930, 2002.
//   https://doi.org/10.1121/1.1458024
// Reference: GenAi see prompt(s) (4) in report
class PitchDetector(
    private val sampleRate: Int,
    private val minFrequency: Float = 60f,   // below a low C (65 Hz), for drop tunings
    private val maxFrequency: Float = 1000f, // well above the high E string (330 Hz)
    private val threshold: Float = 0.15f,    // how good a match must be (paper uses 0.1-0.15)
    private val minLevel: Float = 0.01f      // quieter than this counts as silence
) {
    // Returns the frequency in Hz, or null if there is no clear note.
    fun detect(samples: FloatArray): Float? {
        if (rms(samples) < minLevel) return null

        val tauMin = (sampleRate / maxFrequency).toInt()
        val tauMax = minOf((sampleRate / minFrequency).toInt(), samples.size / 2)
        val window = samples.size - tauMax
        if (tauMax <= tauMin + 1) return null

        // Step 2, difference function: how different the signal is from itself
        // shifted by tau. 0 would be a perfect match.
        val difference = FloatArray(tauMax + 1)
        for (tau in 1..tauMax) {
            var sum = 0f
            for (j in 0 until window) {
                val delta = samples[j] - samples[j + tau]
                sum += delta * delta
            }
            difference[tau] = sum
        }

        // Step 3, cumulative mean normalised difference: divides each value by
        // the average so far, so small shifts are not unfairly favoured.
        val normalised = FloatArray(tauMax + 1)
        normalised[0] = 1f
        var runningSum = 0f
        for (tau in 1..tauMax) {
            runningSum += difference[tau]
            normalised[tau] = if (runningSum == 0f) 1f else difference[tau] * tau / runningSum
        }

        // Step 4, absolute threshold: take the first dip below the threshold,
        // then slide to the bottom of that dip.
        var tau = tauMin
        while (tau < tauMax && normalised[tau] >= threshold) tau++
        if (tau >= tauMax) return null // no clear repeating pattern (noise)
        while (tau + 1 < tauMax && normalised[tau + 1] < normalised[tau]) tau++

        // Step 5, parabolic interpolation: fit a curve through the three points
        // around the dip to estimate the period between whole samples.
        val before = normalised[tau - 1]
        val at = normalised[tau]
        val after = normalised[tau + 1]
        val curve = before - 2 * at + after
        val shift = if (curve == 0f) 0f else 0.5f * (before - after) / curve
        val period = tau + shift.coerceIn(-1f, 1f)

        return sampleRate / period
    }

    // Root mean square: the average loudness of the block
    private fun rms(samples: FloatArray): Float {
        var sum = 0f
        for (sample in samples) sum += sample * sample
        return sqrt(sum / samples.size)
    }
}

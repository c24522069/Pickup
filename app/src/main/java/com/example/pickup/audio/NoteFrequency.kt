package com.example.pickup.audio

import kotlin.math.log2
import kotlin.math.pow

// Turns note names such as "A2" or "Eb3" into frequencies, and measures how
// far apart two frequencies are in cents (100 cents = one semitone).
// Formula: f = 440 * 2^((n - 69) / 12), where n is the MIDI note number (A4 = 69).
// Reference: https://en.wikipedia.org/wiki/MIDI_tuning_standard
// Reference: https://en.wikipedia.org/wiki/Cent_(music)
object NoteFrequency {
    private val semitones = mapOf(
        "C" to 0, "C#" to 1, "Db" to 1, "D" to 2, "D#" to 3, "Eb" to 3,
        "E" to 4, "F" to 5, "F#" to 6, "Gb" to 6, "G" to 7, "G#" to 8,
        "Ab" to 8, "A" to 9, "A#" to 10, "Bb" to 10, "B" to 11
    )

    // "A2" -> 110.0 Hz. The name is everything before the octave number.
    fun frequencyOf(note: String): Float {
        val name = note.takeWhile { !it.isDigit() }
        val octave = note.drop(name.length).toInt()
        val semitone = semitones[name] ?: error("Unknown note: $note")
        val midiNumber = (octave + 1) * 12 + semitone
        return (440.0 * 2.0.pow((midiNumber - 69) / 12.0)).toFloat()
    }

    // Negative = flat (too low), positive = sharp (too high)
    fun centsBetween(frequency: Float, target: Float): Float =
        (1200 * log2(frequency / target.toDouble())).toFloat()
}

package com.example.pickup.viewmodel

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pickup.audio.AudioInput
import com.example.pickup.audio.NoteFrequency
import com.example.pickup.audio.PitchDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

// Within this many cents a string counts as in tune (5 cents is hard to hear)
const val IN_TUNE_CENTS = 5f

// Everything the Tuner screen shows. Strings are numbered by index 0-5,
// lowest (thickest) string first.
data class TunerUiState(
    val listening: Boolean = false,
    val micError: Boolean = false,
    val hasReading: Boolean = false,       // false = silence, show "pluck a string"
    val frequency: Float = 0f,             // what the mic hears, in Hz
    val cents: Float = 0f,                 // distance from the target string
    val targetString: Int? = null,         // which string is being tuned
    val lockedString: Int? = null,         // chosen by tapping; null = automatic
    val tunedStrings: Set<Int> = emptySet()
) {
    val inTune: Boolean get() = hasReading && abs(cents) <= IN_TUNE_CENTS
}

// Holds the tuner state so it survives rotation, and runs the microphone
// loop in viewModelScope (the same scope the labs use for Room writes).
class TunerViewModel(private val audioInput: AudioInput) : ViewModel() {

    private val detector = PitchDetector(AudioInput.SAMPLE_RATE)

    private val _state = MutableStateFlow(TunerUiState())
    val state: StateFlow<TunerUiState> = _state.asStateFlow()

    private var tuningId: Int? = null
    private var targets: List<Float> = emptyList() // target Hz for each string
    private var listenJob: Job? = null
    private val recent = ArrayDeque<Float>()       // last few readings, for smoothing
    private var lastPitchTime = 0L
    private var inTuneCount = 0                     // readings in a row within IN_TUNE_CENTS

    // Called by the screen with the tuning's notes, e.g. ["D2", "A2", ...]
    fun setTuning(id: Int, notes: List<String>) {
        if (id == tuningId) return
        tuningId = id
        targets = notes.map { NoteFrequency.frequencyOf(it) }
        recent.clear()
        _state.update { it.copy(hasReading = false, targetString = null, lockedString = null, tunedStrings = emptySet()) }
    }

    // Tap a string to tune only that one; tap it again to go back to automatic
    fun toggleStringLock(index: Int) {
        _state.update {
            val locked = if (it.lockedString == index) null else index
            it.copy(lockedString = locked, targetString = locked ?: it.targetString)
        }
        inTuneCount = 0
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (listenJob?.isActive == true) return
        _state.update { it.copy(listening = true, micError = false) }
        listenJob = viewModelScope.launch {
            try {
                audioInput.blocks()
                    .map { block -> detector.detect(block) }
                    .flowOn(Dispatchers.Default) // the maths runs off the main thread
                    .collect { frequency -> onReading(frequency) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _state.update { it.copy(listening = false, micError = true, hasReading = false) }
            }
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
        _state.update { it.copy(listening = false, hasReading = false) }
    }

    private fun onReading(frequency: Float?) {
        val now = System.currentTimeMillis()
        if (frequency == null || targets.isEmpty()) {
            // Keep the last reading on screen briefly, then go back to "listening"
            if (now - lastPitchTime > 1500) {
                recent.clear()
                _state.update { it.copy(hasReading = false) }
            }
            return
        }
        lastPitchTime = now

        // Median of the last 5 readings ignores the odd wrong one
        recent.addLast(frequency)
        if (recent.size > 5) recent.removeFirst()
        val smoothed = recent.sorted()[recent.size / 2]

        val current = _state.value
        // Locked string, or whichever string's note is closest to what we hear
        val target = current.lockedString ?: targets.indices.minBy {
            abs(NoteFrequency.centsBetween(smoothed, targets[it]))
        }
        if (target != current.targetString) inTuneCount = 0
        val cents = NoteFrequency.centsBetween(smoothed, targets[target])

        // A string is ticked off once it stays in tune for about a third of a second
        inTuneCount = if (abs(cents) <= IN_TUNE_CENTS) inTuneCount + 1 else 0
        val tuned = if (inTuneCount >= 6) current.tunedStrings + target else current.tunedStrings

        _state.update {
            it.copy(hasReading = true, frequency = smoothed, cents = cents, targetString = target, tunedStrings = tuned)
        }
    }

    class Factory(private val audioInput: AudioInput) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TunerViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return TunerViewModel(audioInput) as T
        }
    }
}

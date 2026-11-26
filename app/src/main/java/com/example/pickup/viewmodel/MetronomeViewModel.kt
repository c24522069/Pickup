package com.example.pickup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pickup.audio.Metronome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MetronomeUiState(
    val bpm: Int = 100,
    val beatsPerBar: Int = 4,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val playing: Boolean = false
)

// Taps more than this far apart start a new tap-tempo count
private const val TAP_RESET_MS = 2000L

// Tap tempo: the average gap between the last few taps becomes the tempo.
// Plain function, so it can be unit tested. Returns null until there are 2 taps.
fun tempoFromTaps(tapTimesMs: List<Long>): Int? {
    if (tapTimesMs.size < 2) return null
    val gaps = tapTimesMs.zipWithNext { earlier, later -> later - earlier }
    val averageGap = gaps.average()
    return (60_000 / averageGap).toInt().coerceIn(Metronome.MIN_BPM, Metronome.MAX_BPM)
}

// Holds the metronome settings so they survive rotation. Same ViewModel +
// Factory structure as NoteViewModel (Lab 8).
class MetronomeViewModel(private val metronome: Metronome) : ViewModel() {

    private val _state = MutableStateFlow(MetronomeUiState())
    val state: StateFlow<MetronomeUiState> = _state.asStateFlow()

    val beat: StateFlow<Int?> = metronome.beat
    val canVibrate: Boolean = metronome.canVibrate

    private val tapTimes = mutableListOf<Long>()

    fun togglePlaying() {
        if (_state.value.playing) stop() else {
            _state.update { it.copy(playing = true) }
            restart()
        }
    }

    fun stop() {
        metronome.stop()
        _state.update { it.copy(playing = false) }
    }

    // While dragging the slider only the number changes; the beat restarts when
    // the finger lifts (see applyTempo), so the click does not stutter.
    fun previewBpm(bpm: Int) = _state.update { it.copy(bpm = bpm.coerceIn(Metronome.MIN_BPM, Metronome.MAX_BPM)) }

    fun applyTempo() = restart()

    fun changeBpm(by: Int) {
        previewBpm(_state.value.bpm + by)
        restart()
    }

    fun setBeatsPerBar(beats: Int) {
        _state.update { it.copy(beatsPerBar = beats) }
        restart()
    }

    fun setSound(on: Boolean) {
        _state.update { it.copy(sound = on) }
        restart()
    }

    fun setVibrate(on: Boolean) {
        _state.update { it.copy(vibrate = on) }
        restart()
    }

    fun tap(nowMs: Long = System.currentTimeMillis()) {
        if (tapTimes.isNotEmpty() && nowMs - tapTimes.last() > TAP_RESET_MS) tapTimes.clear()
        tapTimes += nowMs
        if (tapTimes.size > 5) tapTimes.removeAt(0) // only the last 4 gaps count
        tempoFromTaps(tapTimes)?.let { bpm ->
            previewBpm(bpm)
            restart()
        }
    }

    // Apply new settings straight away if the metronome is running
    private fun restart() {
        val s = _state.value
        if (s.playing) {
            metronome.start(viewModelScope, s.bpm, s.beatsPerBar, s.sound, s.vibrate && canVibrate)
        }
    }

    override fun onCleared() = metronome.stop()

    class Factory(private val metronome: Metronome) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MetronomeViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return MetronomeViewModel(metronome) as T
        }
    }
}

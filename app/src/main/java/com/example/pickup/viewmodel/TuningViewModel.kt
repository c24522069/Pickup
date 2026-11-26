package com.example.pickup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pickup.R
import com.example.pickup.data.local.SongEntity
import com.example.pickup.data.local.TuningEntity
import com.example.pickup.data.repository.PracticeRepository
import com.example.pickup.data.repository.TuningRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Adapted from NoteViewModel.kt (Lab 8): Flows from Room, writes in
// viewModelScope.launch, errors reported through a StateFlow, and a Factory.
// Reference: GenAi see prompt(s) (2) in report
class TuningViewModel(
    private val tuningRepository: TuningRepository,
    practiceRepository: PracticeRepository
) : ViewModel() {

    val tunings: Flow<List<TuningEntity>> = tuningRepository.tunings
    val songs: Flow<List<SongEntity>> = tuningRepository.songs

    // Ids of tunings with at least one practice session, as a Set for quick lookup
    val triedTuningIds: Flow<Set<Int>> = practiceRepository.triedTuningIds.map { it.toSet() }

    // The tuning the Tuner screen shows. null means "use the first tuning".
    private val _tunerTuningId = MutableStateFlow<Int?>(null)
    val tunerTuningId: StateFlow<Int?> = _tunerTuningId.asStateFlow()

    private val _error = MutableStateFlow<Int?>(null) // a string resource id
    val error = _error.asStateFlow()

    fun tuning(tuningId: Int): Flow<TuningEntity?> = tuningRepository.tuning(tuningId)

    fun songsForTuning(tuningId: Int): Flow<List<SongEntity>> =
        tuningRepository.songsForTuning(tuningId)

    fun selectTunerTuning(tuningId: Int) {
        _tunerTuningId.value = tuningId
    }

    fun toggleFavourite(song: SongEntity) = write { tuningRepository.toggleFavourite(song) }

    fun clearError() { _error.value = null }

    // Same pattern as write() in Lab 8's NoteViewModel
    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _error.value = R.string.error_save
            }
        }
    }

    class Factory(
        private val tuningRepository: TuningRepository,
        private val practiceRepository: PracticeRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TuningViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return TuningViewModel(tuningRepository, practiceRepository) as T
        }
    }
}

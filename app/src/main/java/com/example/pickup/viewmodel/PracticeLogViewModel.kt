package com.example.pickup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pickup.R
import com.example.pickup.data.local.PracticeSessionEntity
import com.example.pickup.data.repository.PracticeRepository
import com.example.pickup.data.repository.TuningRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

// One row of the practice log: the session plus the names to show for it
data class SessionListItem(
    val session: PracticeSessionEntity,
    val tuningName: String,
    val songTitle: String? // null means free play
)

data class PracticeStats(val streakDays: Int, val minutesThisWeek: Int)

// Session dates are stored as midnight UTC of the chosen day, which is the
// same format the Material DatePicker uses. These convert to and from LocalDate.
fun dateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun millisToDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

// Plain function (no Android code), so it can be unit tested.
// Streak: consecutive days with practice, ending today (or yesterday if
// today has nothing logged yet). Week: Monday to today.
// Reference: GenAi see prompt(s) (2) in report
fun calculateStats(sessions: List<PracticeSessionEntity>, today: LocalDate): PracticeStats {
    val practisedDays = sessions.map { millisToDate(it.date) }.toSet()

    var day = if (today in practisedDays) today else today.minusDays(1)
    var streak = 0
    while (day in practisedDays) {
        streak++
        day = day.minusDays(1)
    }

    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val minutesThisWeek = sessions
        .filter { millisToDate(it.date) in weekStart..today }
        .sumOf { it.durationMinutes }

    return PracticeStats(streak, minutesThisWeek)
}

// Adapted from NoteViewModel.kt (Lab 8), extended with update and the
// combined list. combine() is not in the labs.
// Reference: https://kotlinlang.org/docs/flow.html#combine
// Reference: GenAi see prompt(s) (2) in report
class PracticeLogViewModel(
    private val practiceRepository: PracticeRepository,
    tuningRepository: TuningRepository
) : ViewModel() {

    // Re-built whenever sessions, tunings or songs change
    val sessionItems: Flow<List<SessionListItem>> = combine(
        practiceRepository.sessions,
        tuningRepository.tunings,
        tuningRepository.songs
    ) { sessions, tunings, songs ->
        val tuningNames = tunings.associate { it.id to it.name }
        val songTitles = songs.associate { it.id to it.title }
        sessions.map { session ->
            SessionListItem(
                session = session,
                tuningName = tuningNames[session.tuningId] ?: "",
                songTitle = session.songId?.let { songTitles[it] }
            )
        }
    }

    val stats: Flow<PracticeStats> =
        practiceRepository.sessions.map { calculateStats(it, LocalDate.now()) }

    private val _error = MutableStateFlow<Int?>(null) // a string resource id
    val error = _error.asStateFlow()

    suspend fun getSession(sessionId: Int): PracticeSessionEntity? =
        practiceRepository.getSession(sessionId)

    // sessionId == null means a new session (INSERT), otherwise UPDATE
    fun saveSession(
        sessionId: Int?,
        tuningId: Int,
        songId: Int?,
        date: Long,
        durationMinutes: Int,
        note: String
    ) {
        val session = PracticeSessionEntity(
            id = sessionId ?: 0, // 0 lets Room generate the id
            date = date,
            durationMinutes = durationMinutes,
            note = note.trim(),
            tuningId = tuningId,
            songId = songId
        )
        write {
            if (sessionId == null) practiceRepository.insert(session)
            else practiceRepository.update(session)
        }
    }

    fun deleteSession(session: PracticeSessionEntity) = write { practiceRepository.delete(session) }

    // Used by Undo. id = 0 gives the restored session a new id, so the list
    // shows it as a new card instead of reusing the swiped-away one
    // (which would still be in its "dismissed" state and delete itself again).
    fun restoreSession(session: PracticeSessionEntity) =
        write { practiceRepository.insert(session.copy(id = 0)) }

    fun clearError() { _error.value = null }

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
        private val practiceRepository: PracticeRepository,
        private val tuningRepository: TuningRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PracticeLogViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return PracticeLogViewModel(practiceRepository, tuningRepository) as T
        }
    }
}

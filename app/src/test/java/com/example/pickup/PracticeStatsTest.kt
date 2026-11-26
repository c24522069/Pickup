package com.example.pickup

import com.example.pickup.data.local.PracticeSessionEntity
import com.example.pickup.viewmodel.calculateStats
import com.example.pickup.viewmodel.dateToMillis
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// Unit tests for the practice log stats (run on the computer, no phone needed).
// Reference: GenAi see prompt(s) (2) in report
class PracticeStatsTest {
    // Thursday 26 November 2026; that week starts on Monday 23 November
    private val today = LocalDate.of(2026, 11, 26)

    private fun session(date: LocalDate, minutes: Int = 10) =
        PracticeSessionEntity(date = dateToMillis(date), durationMinutes = minutes, note = "", tuningId = 1)

    @Test
    fun noSessions_givesZero() {
        val stats = calculateStats(emptyList(), today)
        assertEquals(0, stats.streakDays)
        assertEquals(0, stats.minutesThisWeek)
    }

    @Test
    fun streak_countsConsecutiveDaysEndingToday() {
        val sessions = listOf(session(today), session(today.minusDays(1)), session(today.minusDays(2)),
            session(today.minusDays(4))) // gap on day 3 ends the streak
        assertEquals(3, calculateStats(sessions, today).streakDays)
    }

    @Test
    fun streak_stillCountsIfTodayNotLoggedYet() {
        val sessions = listOf(session(today.minusDays(1)), session(today.minusDays(2)))
        assertEquals(2, calculateStats(sessions, today).streakDays)
    }

    @Test
    fun streak_twoSessionsSameDayCountOnce() {
        val sessions = listOf(session(today), session(today))
        assertEquals(1, calculateStats(sessions, today).streakDays)
    }

    @Test
    fun week_onlyCountsMondayToToday() {
        val sessions = listOf(
            session(LocalDate.of(2026, 11, 23), 30), // Monday: counted
            session(today, 25),                       // counted
            session(LocalDate.of(2026, 11, 22), 40)   // last Sunday: not counted
        )
        assertEquals(55, calculateStats(sessions, today).minutesThisWeek)
    }
}

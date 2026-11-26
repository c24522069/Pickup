package com.example.pickup.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Adapted from NoteDao.kt (Lab 6/8). @Update is from the Week 8 Room lecture.
@Dao
interface PracticeSessionDao {
    // Newest first, for the practice log
    @Query("SELECT * FROM practice_sessions ORDER BY date DESC, id DESC")
    fun observeSessions(): Flow<List<PracticeSessionEntity>>

    // Loads one session into the edit form
    @Query("SELECT * FROM practice_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: Int): PracticeSessionEntity?

    // A tuning counts as "tried" once it has at least one session
    @Query("SELECT DISTINCT tuningId FROM practice_sessions")
    fun observeTriedTuningIds(): Flow<List<Int>>

    @Insert
    suspend fun insert(session: PracticeSessionEntity)

    @Update
    suspend fun update(session: PracticeSessionEntity)

    @Delete
    suspend fun delete(session: PracticeSessionEntity)
}

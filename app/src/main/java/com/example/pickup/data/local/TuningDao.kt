package com.example.pickup.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Adapted from NoteDao.kt (Lab 6/8). Flow re-emits when the table changes.
@Dao
interface TuningDao {
    @Query("SELECT * FROM tunings ORDER BY id ASC")
    fun observeTunings(): Flow<List<TuningEntity>>

    @Query("SELECT * FROM tunings WHERE id = :tuningId")
    fun observeTuning(tuningId: Int): Flow<TuningEntity?>
}

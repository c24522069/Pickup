package com.example.pickup.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Adapted from NoteDao.kt (Lab 6/8). @Update is from the Week 8 Room lecture.
@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun observeSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE tuningId = :tuningId ORDER BY title ASC")
    fun observeSongsForTuning(tuningId: Int): Flow<List<SongEntity>>

    // Used when the user taps the favourite star
    @Update
    suspend fun update(song: SongEntity)
}

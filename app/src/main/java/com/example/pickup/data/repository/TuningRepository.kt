package com.example.pickup.data.repository

import com.example.pickup.data.local.SongDao
import com.example.pickup.data.local.SongEntity
import com.example.pickup.data.local.TuningDao
import com.example.pickup.data.local.TuningEntity
import kotlinx.coroutines.flow.Flow

// Sits between the ViewModels and the DAOs. The labs pass the DAO straight to
// the ViewModel; this extra layer is from the app's design document.
// Reference: https://developer.android.com/topic/architecture/data-layer
// Reference: GenAi see prompt(s) (1) in report
class TuningRepository(
    private val tuningDao: TuningDao,
    private val songDao: SongDao
) {
    val tunings: Flow<List<TuningEntity>> = tuningDao.observeTunings()
    val songs: Flow<List<SongEntity>> = songDao.observeSongs()

    fun tuning(tuningId: Int): Flow<TuningEntity?> = tuningDao.observeTuning(tuningId)

    fun songsForTuning(tuningId: Int): Flow<List<SongEntity>> =
        songDao.observeSongsForTuning(tuningId)

    // copy() makes the same song with only isFavourite flipped
    suspend fun toggleFavourite(song: SongEntity) =
        songDao.update(song.copy(isFavourite = !song.isFavourite))
}

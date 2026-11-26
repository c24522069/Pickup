package com.example.pickup.data.repository

import com.example.pickup.data.local.PracticeSessionDao
import com.example.pickup.data.local.PracticeSessionEntity
import kotlinx.coroutines.flow.Flow

// Sits between the ViewModels and the DAO (see TuningRepository).
// Reference: https://developer.android.com/topic/architecture/data-layer
class PracticeRepository(private val dao: PracticeSessionDao) {
    val sessions: Flow<List<PracticeSessionEntity>> = dao.observeSessions()
    val triedTuningIds: Flow<List<Int>> = dao.observeTriedTuningIds()

    suspend fun getSession(sessionId: Int) = dao.getSession(sessionId)
    suspend fun insert(session: PracticeSessionEntity) = dao.insert(session)
    suspend fun update(session: PracticeSessionEntity) = dao.update(session)
    suspend fun delete(session: PracticeSessionEntity) = dao.delete(session)
}

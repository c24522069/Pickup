package com.example.pickup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Adapted from Note.kt (Lab 6/8 Room). The foreign keys are not in the labs.
// Reference: https://developer.android.com/reference/androidx/room/ForeignKey
// Reference: GenAi see prompt(s) (1) in report
@Entity(
    tableName = "practice_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TuningEntity::class,
            parentColumns = ["id"],
            childColumns = ["tuningId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.SET_NULL // keep the session if its song is removed
        )
    ],
    indices = [Index("tuningId"), Index("songId")]
)
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // milliseconds since 1970 (epoch)
    val durationMinutes: Int,
    val note: String,
    val tuningId: Int,
    val songId: Int? = null // null means free play
)

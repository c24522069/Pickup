package com.example.pickup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Adapted from Note.kt (Lab 6/8 Room). The foreign key is not in the labs.
// Reference: https://developer.android.com/reference/androidx/room/ForeignKey
// Reference: GenAi see prompt(s) (1) in report
@Entity(
    tableName = "songs",
    foreignKeys = [
        ForeignKey(
            entity = TuningEntity::class,
            parentColumns = ["id"],
            childColumns = ["tuningId"],
            onDelete = ForeignKey.CASCADE // a song cannot exist without its tuning
        )
    ],
    indices = [Index("tuningId")]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val tuningId: Int,
    val isFavourite: Boolean = false
)

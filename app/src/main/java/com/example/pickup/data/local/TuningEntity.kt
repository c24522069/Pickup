package com.example.pickup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Adapted from Note.kt (Lab 6/8 Room). Each property becomes a column.
// Room has no list column type, so lists are stored as text:
//   stringNotes       low to high with octave, e.g. "D2 A2 D3 G3 A3 D4"
//   scales            e.g. "D major;D mixolydian"
//   chordProgressions e.g. "D G A;D C G"
// Reference: GenAi see prompt(s) (1) in report
@Entity(tableName = "tunings")
data class TuningEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val stringNotes: String,
    val scales: String,
    val chordProgressions: String
)

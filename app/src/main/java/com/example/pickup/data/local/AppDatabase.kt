package com.example.pickup.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

// Adapted from AppDatabase.kt (Lab 8). One shared instance for the whole app.
@Database(
    entities = [TuningEntity::class, SongEntity::class, PracticeSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tuningDao(): TuningDao
    abstract fun songDao(): SongDao
    abstract fun practiceSessionDao(): PracticeSessionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pickup.db"
                ).addCallback(preloadCallback).build().also { instance = it }
            }

        // onCreate runs only the first time, after Room has made the tables.
        // Reference: https://developer.android.com/reference/androidx/room/RoomDatabase.Callback
        // Reference: GenAi see prompt(s) (1) in report
        private val preloadCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                // Tunings go first because songs have a foreign key to them.
                // Each ? is filled from the array, so quotes in titles are safe.
                SeedData.tunings.forEach {
                    db.execSQL(
                        "INSERT INTO tunings (id, name, stringNotes, scales, chordProgressions) VALUES (?, ?, ?, ?, ?)",
                        arrayOf<Any>(it.id, it.name, it.stringNotes, it.scales, it.chordProgressions)
                    )
                }
                SeedData.songs.forEach {
                    db.execSQL(
                        "INSERT INTO songs (id, title, artist, tuningId, isFavourite) VALUES (?, ?, ?, ?, 0)",
                        arrayOf<Any>(it.id, it.title, it.artist, it.tuningId)
                    )
                }
            }
        }
    }
}

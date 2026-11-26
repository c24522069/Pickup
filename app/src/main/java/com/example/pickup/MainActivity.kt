package com.example.pickup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pickup.data.local.AppDatabase
import com.example.pickup.data.repository.PracticeRepository
import com.example.pickup.data.repository.TuningRepository
import com.example.pickup.navigation.AppNavHost
import com.example.pickup.ui.theme.PickupTheme

// Single activity: every screen is a composable inside AppNavHost.
// Database set-up adapted from MainActivity (Lab 8).
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = AppDatabase.getInstance(applicationContext)
        val tuningRepository = TuningRepository(database.tuningDao(), database.songDao())
        val practiceRepository = PracticeRepository(database.practiceSessionDao())
        setContent {
            PickupTheme {
                AppNavHost(tuningRepository, practiceRepository)
            }
        }
    }
}

package com.example.pickup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.pickup.data.local.AppDatabase
import com.example.pickup.data.repository.TuningRepository
import com.example.pickup.ui.theme.PickupTheme

// Reference: GenAi see prompt(s) (1) in report
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = AppDatabase.getInstance(applicationContext)
        val repository = TuningRepository(database.tuningDao(), database.songDao())
        setContent {
            PickupTheme {
                // Temporary Stage 1 check: proves Room was created and pre-loaded.
                // Stage 2 replaces this with the real screens and a ViewModel.
                val tunings by repository.tunings.collectAsState(initial = emptyList())
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LazyColumn(modifier = Modifier.padding(innerPadding)) {
                        items(tunings) { tuning ->
                            Text("${tuning.name}: ${tuning.stringNotes}")
                        }
                    }
                }
            }
        }
    }
}

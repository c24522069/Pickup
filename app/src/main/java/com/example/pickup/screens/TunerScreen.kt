package com.example.pickup.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pickup.R
import com.example.pickup.data.local.noteNames
import com.example.pickup.viewmodel.TuningViewModel

// PLACEHOLDER (stage 2): shows which tuning "Tune to this" picked.
// Stage 3 adds the microphone, needle and string buttons.
// Reference: GenAi see prompt(s) (2) in report
@Composable
fun TunerScreen(viewModel: TuningViewModel, modifier: Modifier = Modifier) {
    val tunings by viewModel.tunings.collectAsStateWithLifecycle(initialValue = emptyList())
    val tunerTuningId by viewModel.tunerTuningId.collectAsStateWithLifecycle()
    val tuning = tunings.find { it.id == tunerTuningId } ?: tunings.firstOrNull()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.tuner_title), style = MaterialTheme.typography.headlineMedium)
        if (tuning != null) {
            Text(tuning.name, style = MaterialTheme.typography.titleLarge)
            Text(tuning.noteNames.joinToString("  "), letterSpacing = 2.sp)
        }
        Text(stringResource(R.string.tuner_coming_soon))
    }
}

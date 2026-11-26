package com.example.pickup.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pickup.R

// PLACEHOLDER (stage 2): the metronome tab. Not in the design's file tree;
// added when the metronome was given its own tab (design change).
// Reference: GenAi see prompt(s) (2) in report
@Composable
fun MetronomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.metronome_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.metronome_coming_soon))
    }
}

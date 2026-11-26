package com.example.pickup.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pickup.R
import com.example.pickup.audio.Metronome
import com.example.pickup.viewmodel.MetronomeViewModel
import kotlin.math.roundToInt

private val BAR_LENGTHS = listOf(2, 3, 4, 6)

// Metronome tab. Slider, FilterChips and Switches are adapted from
// ActivitiesScreen (Lab 4).
@Composable
fun MetronomeScreen(viewModel: MetronomeViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val beat by viewModel.beat.collectAsStateWithLifecycle()

    // Stop the clicks when leaving the tab or the app. Same lifecycle pattern
    // as RegisterSensors (Lab 10), but only the stop side is needed.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            viewModel.stop()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.metronome_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )

        // Big tempo number, read out as one phrase, e.g. "100 beats per minute"
        val bpmDescription = stringResource(R.string.bpm_description, state.bpm)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clearAndSetSemantics { contentDescription = bpmDescription }
        ) {
            Text("${state.bpm}", fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.bpm), style = MaterialTheme.typography.titleMedium)
        }

        BeatDots(beatsPerBar = state.beatsPerBar, currentBeat = beat)

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { viewModel.changeBpm(-1) }) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.slower))
            }
            val tempoLabel = stringResource(R.string.tempo)
            Slider(
                value = state.bpm.toFloat(),
                onValueChange = { viewModel.previewBpm(it.roundToInt()) },
                onValueChangeFinished = viewModel::applyTempo,
                valueRange = Metronome.MIN_BPM.toFloat()..Metronome.MAX_BPM.toFloat(),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .semantics { contentDescription = tempoLabel }
            )
            FilledTonalIconButton(onClick = { viewModel.changeBpm(1) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.faster))
            }
        }

        // Tap along to a song to set its tempo
        OutlinedButton(
            onClick = { viewModel.tap() },
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text(stringResource(R.string.tap_tempo), style = MaterialTheme.typography.titleMedium)
        }
        Text(
            stringResource(R.string.tap_tempo_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.beats_per_bar), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BAR_LENGTHS.forEach { beats ->
                    FilterChip(
                        selected = state.beatsPerBar == beats,
                        onClick = { viewModel.setBeatsPerBar(beats) },
                        label = { Text("$beats") }
                    )
                }
            }
        }

        SettingSwitch(stringResource(R.string.click_sound), state.sound, viewModel::setSound)
        if (viewModel.canVibrate) {
            SettingSwitch(stringResource(R.string.vibrate_on_beat), state.vibrate, viewModel::setVibrate)
        } else {
            Text(
                stringResource(R.string.no_vibration),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = viewModel::togglePlaying,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(if (state.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
            Text(
                stringResource(if (state.playing) R.string.stop else R.string.start),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// One dot per beat in the bar. The current beat grows and lights up; the
// first beat uses a different colour because it is the accented one.
@Composable
private fun BeatDots(beatsPerBar: Int, currentBeat: Int?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(32.dp).clearAndSetSemantics { } // visual only
    ) {
        repeat(beatsPerBar) { index ->
            val active = index == currentBeat
            val size by animateDpAsState(if (active) 28.dp else 18.dp, label = "dotSize")
            val colour by animateColorAsState(
                when {
                    active && index == 0 -> MaterialTheme.colorScheme.tertiary
                    active -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "dotColour"
            )
            Box(modifier = Modifier.size(size).background(colour, CircleShape))
        }
    }
}

// Label on the left, switch on the right, as in ActivitiesScreen (Lab 4).
// The whole row is the switch, so the label is read with it and is tappable.
@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

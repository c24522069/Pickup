package com.example.pickup.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pickup.R
import com.example.pickup.audio.NoteFrequency
import com.example.pickup.components.DropdownField
import com.example.pickup.components.TunerNeedle
import com.example.pickup.data.local.noteNames
import com.example.pickup.viewmodel.TunerUiState
import com.example.pickup.viewmodel.TunerViewModel
import com.example.pickup.viewmodel.TuningViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

// Readings further off than this get "tune up/down" instead of "a little"
private const val CLOSE_CENTS = 20f

// Tuner screen: listens through the microphone and shows how far the
// string is from its target note.
// Reference: GenAi see prompt(s) (4) in report
@Composable
fun TunerScreen(
    tuningViewModel: TuningViewModel,
    tunerViewModel: TunerViewModel,
    modifier: Modifier = Modifier
) {
    val tunings by tuningViewModel.tunings.collectAsStateWithLifecycle(initialValue = emptyList())
    val tunerTuningId by tuningViewModel.tunerTuningId.collectAsStateWithLifecycle()
    val state by tunerViewModel.state.collectAsStateWithLifecycle()
    val tuning = tunings.find { it.id == tunerTuningId } ?: tunings.firstOrNull()

    // Give the ViewModel this tuning's target notes whenever the tuning changes
    LaunchedEffect(tuning?.id) {
        if (tuning != null) tunerViewModel.setTuning(tuning.id, tuning.stringNotes.split(" "))
    }

    // Microphone permission. Same idea as the notification permission in
    // Lab 10, using the Compose version of the launcher.
    // Reference: https://developer.android.com/training/permissions/requesting
    val context = LocalContext.current
    val activity = LocalActivity.current
    var hasPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        askedOnce = true
    }

    // Listen only while this screen is showing and the app is in the
    // foreground. Adapted from RegisterSensors (Lab 10): start on ON_START,
    // stop on ON_STOP, and stop when the screen is left.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, hasPermission) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (hasPermission) tunerViewModel.start()
                Lifecycle.Event.ON_STOP -> tunerViewModel.stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (hasPermission && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            tunerViewModel.start()
        }
        onDispose {
            owner.lifecycle.removeObserver(observer)
            tunerViewModel.stop()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.tuner_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            DropdownField(
                label = stringResource(R.string.label_tuning),
                selectedText = tuning?.name ?: "",
                options = tunings,
                optionText = { it.name },
                onOptionSelected = { tuningViewModel.selectTunerTuning(it.id) },
                modifier = Modifier.width(180.dp)
            )
        }

        if (tuning == null) return@Column

        if (!hasPermission) {
            // Ask again until the user ticks "don't ask again"; then only settings works
            val canAskAgain = !askedOnce || activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.RECORD_AUDIO
            ) == true
            MicPermissionCard(
                canAskAgain = canAskAgain,
                onAllow = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null))
                    )
                }
            )
        }

        val notes = tuning.noteNames
        val inTuneColour = if (isSystemInDarkTheme()) Color(0xFF66BB6A) else Color(0xFF2E7D32)
        val target = state.targetString

        // "String 5 · target 110 Hz": guitarists count strings from the high one
        Text(
            text = if (target != null) {
                stringResource(
                    R.string.string_target,
                    notes.size - target,
                    tunerTargetHz(tuning.stringNotes, target)
                )
            } else stringResource(R.string.pluck_any_string),
            style = MaterialTheme.typography.labelLarge
        )

        Text(
            text = if (target != null) notes[target] else "–",
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.inTune) inTuneColour else MaterialTheme.colorScheme.onSurface
        )

        TunerNeedle(
            cents = state.cents,
            hasReading = state.hasReading,
            inTune = state.inTune,
            inTuneColour = inTuneColour,
            description = needleDescription(state)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.flat), style = MaterialTheme.typography.labelMedium)
            Text(
                stringResource(R.string.in_tune),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(stringResource(R.string.sharp), style = MaterialTheme.typography.labelMedium)
        }

        // Words as well as colour, so it works for colour-blind users.
        // liveRegion makes TalkBack read it out when it changes.
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = if (state.inTune) inTuneColour else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (state.inTune) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Text(
                text = statusText(state),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        if (state.hasReading) {
            Text(
                stringResource(R.string.cents_and_hz, state.cents.roundToInt(), state.frequency),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        StringButtons(
            notes = notes,
            state = state,
            inTuneColour = inTuneColour,
            onToggle = tunerViewModel::toggleStringLock
        )

        Text(
            text = when {
                state.micError -> stringResource(R.string.mic_error)
                state.lockedString != null ->
                    stringResource(R.string.locked_to_string, notes.size - state.lockedString!!)
                state.listening -> stringResource(R.string.listening)
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// The six strings as round buttons, lowest first, like the design.
// Green = tuned, filled = the string being tuned now.
@Composable
private fun StringButtons(
    notes: List<String>,
    state: TunerUiState,
    inTuneColour: Color,
    onToggle: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        notes.forEachIndexed { index, note ->
            val stringNumber = notes.size - index
            val tuned = index in state.tunedStrings
            val current = index == state.targetString
            val locked = index == state.lockedString
            val description = buildString {
                append(stringResource(R.string.string_number_note, stringNumber, note))
                if (tuned) append(", ").append(stringResource(R.string.tuned))
                if (locked) append(", ").append(stringResource(R.string.selected))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    onClick = { onToggle(index) },
                    shape = CircleShape,
                    color = when {
                        tuned -> inTuneColour
                        current -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surface
                    },
                    contentColor = when {
                        tuned -> Color.White
                        current -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    // A thick ring marks the string you tapped to lock
                    border = BorderStroke(
                        if (locked) 3.dp else 1.dp,
                        if (locked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .size(48.dp) // minimum touch target
                        .semantics {
                            contentDescription = description
                            role = Role.Button
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = note, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Text("$stringNumber", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MicPermissionCard(canAskAgain: Boolean, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.mic_needed_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.mic_needed_body))
            if (canAskAgain) {
                Button(onClick = onAllow) { Text(stringResource(R.string.allow_microphone)) }
            } else {
                Text(stringResource(R.string.mic_denied_body))
                Button(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) }
            }
        }
    }
}

@Composable
private fun statusText(state: TunerUiState): String {
    val cents = state.cents
    return when {
        !state.hasReading -> stringResource(R.string.status_waiting)
        state.inTune -> stringResource(R.string.in_tune)
        cents < -CLOSE_CENTS -> stringResource(R.string.status_too_flat)
        cents < 0 -> stringResource(R.string.status_little_flat)
        cents > CLOSE_CENTS -> stringResource(R.string.status_too_sharp)
        else -> stringResource(R.string.status_little_sharp)
    }
}

@Composable
private fun needleDescription(state: TunerUiState): String {
    val cents = abs(state.cents.roundToInt())
    return when {
        !state.hasReading -> stringResource(R.string.status_waiting)
        state.inTune -> stringResource(R.string.in_tune)
        state.cents < 0 -> stringResource(R.string.cents_flat, cents)
        else -> stringResource(R.string.cents_sharp, cents)
    }
}

// Target frequency of one string, e.g. 110.0 for "A2"
private fun tunerTargetHz(stringNotes: String, index: Int): Float =
    NoteFrequency.frequencyOf(stringNotes.split(" ")[index])

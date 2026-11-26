package com.example.pickup.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pickup.R
import com.example.pickup.components.DropdownField
import com.example.pickup.data.local.PracticeSessionEntity
import com.example.pickup.viewmodel.PracticeLogViewModel
import com.example.pickup.viewmodel.TuningViewModel
import com.example.pickup.viewmodel.dateToMillis
import com.example.pickup.viewmodel.millisToDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private const val MAX_NOTE_LENGTH = 200

// Input screen for a new session (sessionId == null) or an existing one.
// Form state uses rememberSaveable as in ActivitiesScreen (Lab 4), so it
// survives rotation; the Slider is adapted from the same screen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSessionScreen(
    sessionId: Int?,
    initialTuningId: Int?,
    tuningViewModel: TuningViewModel,
    practiceViewModel: PracticeLogViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tunings by tuningViewModel.tunings.collectAsStateWithLifecycle(initialValue = emptyList())
    val allSongs by tuningViewModel.songs.collectAsStateWithLifecycle(initialValue = emptyList())

    var loaded by rememberSaveable { mutableStateOf(sessionId == null) }
    var tuningId by rememberSaveable { mutableStateOf(initialTuningId) }
    var songId by rememberSaveable { mutableStateOf<Int?>(null) }
    var date by rememberSaveable { mutableLongStateOf(dateToMillis(LocalDate.now())) }
    var minutes by rememberSaveable { mutableFloatStateOf(25f) }
    var note by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    // Editing: copy the saved session into the form once (not again after rotation)
    LaunchedEffect(sessionId) {
        if (!loaded && sessionId != null) {
            val session = practiceViewModel.getSession(sessionId)
            if (session == null) {
                onClose() // it was deleted elsewhere
            } else {
                tuningId = session.tuningId
                songId = session.songId
                date = session.date
                minutes = session.durationMinutes.toFloat()
                note = session.note
                loaded = true
            }
        }
    }

    val selectedTuning = tunings.find { it.id == tuningId } ?: tunings.firstOrNull()
    val songsForTuning = allSongs.filter { it.tuningId == selectedTuning?.id }
    val selectedSong = songsForTuning.find { it.id == songId }
    val freePlay = stringResource(R.string.free_play)

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
            }
            Text(
                text = stringResource(if (sessionId == null) R.string.new_session else R.string.edit_session),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            if (sessionId != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_session))
                }
            }
        }

        // Dropdowns instead of typing, as the design asks
        DropdownField(
            label = stringResource(R.string.label_tuning),
            selectedText = selectedTuning?.name ?: "",
            options = tunings,
            optionText = { it.name },
            onOptionSelected = {
                if (it.id != selectedTuning?.id) songId = null // old song belongs to another tuning
                tuningId = it.id
            }
        )

        // null in the list stands for "Free play"
        DropdownField(
            label = stringResource(R.string.label_song),
            selectedText = selectedSong?.title ?: freePlay,
            options = listOf(null) + songsForTuning,
            optionText = { it?.title ?: freePlay },
            onOptionSelected = { songId = it?.id }
        )

        // A read-only field that opens the date picker when tapped.
        // Reference: https://developer.android.com/develop/ui/compose/components/datepickers
        OutlinedTextField(
            value = millisToDate(date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_date)) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.choose_date))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    // clickable does not work on text fields, so watch for a tap directly
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        if (up != null) showDatePicker = true
                    }
                }
        )

        Column {
            Row {
                Text(stringResource(R.string.how_long), modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.minutes_short, minutes.roundToInt()),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            // 5 to 120 minutes in steps of 5: 24 positions, so 22 steps between the ends
            Slider(
                value = minutes,
                onValueChange = { minutes = it },
                valueRange = 5f..120f,
                steps = 22
            )
        }

        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= MAX_NOTE_LENGTH) note = it },
            label = { Text(stringResource(R.string.notes_optional)) },
            supportingText = { Text("${note.length} / $MAX_NOTE_LENGTH") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val tuning = selectedTuning ?: return@Button
                practiceViewModel.saveSession(
                    sessionId = sessionId,
                    tuningId = tuning.id,
                    songId = selectedSong?.id,
                    date = date,
                    durationMinutes = minutes.roundToInt(),
                    note = note
                )
                onClose()
            },
            enabled = selectedTuning != null && loaded,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save_session))
        }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel))
        }
    }

    if (showDatePicker) {
        val today = dateToMillis(LocalDate.now())
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date,
            // No logging practice in the future
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    // Confirmation before deleting, as with the AlertDialog in Lab 8
    if (confirmDelete && sessionId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_session)) },
            text = { Text(stringResource(R.string.delete_session_question)) },
            confirmButton = {
                TextButton(onClick = {
                    // @Delete finds the row by its primary key, so only the id matters here
                    practiceViewModel.deleteSession(
                        PracticeSessionEntity(
                            id = sessionId, date = date, durationMinutes = minutes.roundToInt(),
                            note = note, tuningId = selectedTuning?.id ?: 0, songId = songId
                        )
                    )
                    confirmDelete = false
                    onClose()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

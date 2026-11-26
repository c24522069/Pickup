package com.example.pickup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pickup.R
import com.example.pickup.components.SessionCard
import com.example.pickup.viewmodel.PracticeLogViewModel
import com.example.pickup.viewmodel.PracticeStats
import com.example.pickup.viewmodel.SessionListItem
import com.example.pickup.viewmodel.millisToDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// List screen populated from Room. LazyColumn + collectAsStateWithLifecycle
// adapted from Screen3 (Lab 8); FAB from AppContent (ThemeDemo lab);
// snackbar from MainActivity (Lab 8).
// Reference: GenAi see prompt(s) (2) in report
@Composable
fun PracticeLogScreen(
    viewModel: PracticeLogViewModel,
    snackbarHostState: SnackbarHostState,
    onAddSession: () -> Unit,
    onEditSession: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.sessionItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val stats by viewModel.stats.collectAsStateWithLifecycle(initialValue = PracticeStats(0, 0))
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.session_deleted)
    val undoLabel = stringResource(R.string.undo)

    // Delete, then offer Undo, which inserts the same session back
    val deleteWithUndo: (SessionListItem) -> Unit = { item ->
        viewModel.deleteSession(item.session)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = deletedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreSession(item.session)
        }
    }

    // Sessions arrive newest first, so groupBy keeps the days in order
    val byDay = items.groupBy { millisToDate(it.session.date) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text(stringResource(R.string.log_title), style = MaterialTheme.typography.headlineMedium) }
            item { StatsRow(stats) }

            if (items.isEmpty()) {
                item { Text(stringResource(R.string.no_sessions), modifier = Modifier.padding(top = 16.dp)) }
            }

            byDay.forEach { (day, sessions) ->
                item(key = "day-$day") {
                    Text(
                        text = dayLabel(day),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(sessions, key = { it.session.id }) { item ->
                    SwipeToDeleteSession(
                        item = item,
                        onClick = { onEditSession(item.session.id) },
                        onDelete = { deleteWithUndo(item) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddSession,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_session))
        }
    }
}

@Composable
private fun StatsRow(stats: PracticeStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            value = pluralStringResource(R.plurals.streak_days, stats.streakDays, stats.streakDays),
            label = stringResource(R.string.stat_in_a_row),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = stringResource(R.string.hours_minutes, stats.minutesThisWeek / 60, stats.minutesThisWeek % 60),
            label = stringResource(R.string.stat_this_week),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun dayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        // Formats the date in the phone's language, e.g. "26 Nov 2026"
        else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

// Swipe from right to left to delete. Not covered in the labs.
// Reference: https://developer.android.com/develop/ui/compose/touch-input/user-interactions/swipe-to-dismiss
@Composable
private fun SwipeToDeleteSession(
    item: SessionListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false, // only right-to-left deletes
        onDismiss = { onDelete() },
        backgroundContent = {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null, // the card itself announces the delete action
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                    .wrapContentSize(Alignment.CenterEnd)
                    .padding(16.dp)
            )
        }
    ) {
        SessionCard(item = item, onClick = onClick, onDelete = onDelete)
    }
}

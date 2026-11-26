package com.example.pickup.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pickup.R
import com.example.pickup.data.local.TuningEntity
import com.example.pickup.data.local.noteNames
import com.example.pickup.viewmodel.TuningViewModel
import java.time.LocalDate

private enum class TriedFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    TRIED(R.string.filter_tried),
    NOT_TRIED(R.string.filter_not_tried)
}

// List screen. LazyColumn of Room data as in Screen3 (Lab 8), FilterChips as
// in ActivitiesScreen (Lab 4).
// Reference: GenAi see prompt(s) (2) in report
@Composable
fun TuningLibraryScreen(
    viewModel: TuningViewModel,
    onOpenTuning: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tunings by viewModel.tunings.collectAsStateWithLifecycle(initialValue = emptyList())
    val triedIds by viewModel.triedTuningIds.collectAsStateWithLifecycle(initialValue = emptySet())
    var filter by rememberSaveable { mutableStateOf(TriedFilter.ALL) }

    val shownTunings = when (filter) {
        TriedFilter.ALL -> tunings
        TriedFilter.TRIED -> tunings.filter { it.id in triedIds }
        TriedFilter.NOT_TRIED -> tunings.filter { it.id !in triedIds }
    }
    // A different tuning each day of the year
    val tuningOfTheDay = tunings.getOrNull(LocalDate.now().dayOfYear % tunings.size.coerceAtLeast(1))

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.tunings_title), style = MaterialTheme.typography.headlineMedium)
        }

        if (tuningOfTheDay != null) {
            item { TuningOfTheDayCard(tuningOfTheDay, onTryIt = { onOpenTuning(tuningOfTheDay.id) }) }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriedFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(stringResource(option.labelRes)) }
                    )
                }
            }
        }

        if (shownTunings.isEmpty() && tunings.isNotEmpty()) {
            item { Text(stringResource(R.string.no_tunings_in_filter)) }
        }

        items(shownTunings, key = { it.id }) { tuning ->
            TuningRow(
                tuning = tuning,
                tried = tuning.id in triedIds,
                onClick = { onOpenTuning(tuning.id) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TuningOfTheDayCard(tuning: TuningEntity, onTryIt: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(stringResource(R.string.tuning_of_the_day), style = MaterialTheme.typography.labelMedium)
            Text(tuning.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = tuning.noteNames.joinToString("  "),
                style = MaterialTheme.typography.bodyMedium,
                letterSpacing = 2.sp
            )
            Button(onClick = onTryIt, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.try_it))
            }
        }
    }
}

@Composable
private fun TuningRow(tuning: TuningEntity, tried: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp) // comfortably above the 48dp touch target
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(tuning.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = tuning.noteNames.joinToString("  "),
                style = MaterialTheme.typography.bodySmall,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Text as well as colour, so the status does not rely on colour alone
        Text(
            text = stringResource(if (tried) R.string.tried else R.string.not_tried),
            style = MaterialTheme.typography.labelMedium,
            color = if (tried) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

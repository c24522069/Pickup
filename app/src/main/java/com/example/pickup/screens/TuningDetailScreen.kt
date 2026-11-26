package com.example.pickup.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pickup.R
import com.example.pickup.data.local.SongEntity
import com.example.pickup.data.local.TuningEntity
import com.example.pickup.data.local.noteNames
import com.example.pickup.data.local.progressionList
import com.example.pickup.data.local.scaleList
import com.example.pickup.viewmodel.TuningViewModel

// Extra screen: one tuning's strings, songs, scales and chord progressions.
@Composable
fun TuningDetailScreen(
    tuningId: Int,
    viewModel: TuningViewModel,
    onBack: () -> Unit,
    onTune: (Int) -> Unit,
    onLogPractice: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // remember() so a new Flow is not created on every recomposition
    val tuningFlow = remember(tuningId) { viewModel.tuning(tuningId) }
    val songsFlow = remember(tuningId) { viewModel.songsForTuning(tuningId) }
    val tuning by tuningFlow.collectAsStateWithLifecycle(initialValue = null)
    val songs by songsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val current = tuning ?: return // still loading

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(current.name, style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            text = stringResource(R.string.strings_low_to_high),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        StringNotesRow(current)

        // Tabs are not in the labs.
        // Reference: https://developer.android.com/develop/ui/compose/components/tabs
        val tabs = listOf(R.string.tab_songs, R.string.tab_scales, R.string.tab_chords)
        PrimaryTabRow(selectedTabIndex = selectedTab, modifier = Modifier.padding(top = 16.dp)) {
            tabs.forEachIndexed { index, labelRes ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(stringResource(labelRes)) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> SongList(songs, onToggleFavourite = viewModel::toggleFavourite)
                1 -> TextList(current.scaleList)
                else -> TextList(current.progressionList.map { it.joinToString("  –  ") })
            }
        }

        Button(onClick = { onTune(current.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tune_to_this))
        }
        OutlinedButton(
            onClick = { onLogPractice(current.id) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.log_practice))
        }
    }
}

// Six boxes, one per string, low to high
@Composable
private fun StringNotesRow(tuning: TuningEntity) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val notes = tuning.noteNames
        notes.forEachIndexed { index, note ->
            // Guitarists number strings from the high one, so the lowest is string 6
            val description = stringResource(R.string.string_number_note, notes.size - index, note)
            OutlinedCard(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = description }
            ) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun SongList(songs: List<SongEntity>, onToggleFavourite: (SongEntity) -> Unit) {
    if (songs.isEmpty()) {
        Text(stringResource(R.string.no_songs), modifier = Modifier.padding(top = 16.dp))
        return
    }
    val context = LocalContext.current
    val noBrowser = stringResource(R.string.no_browser)

    LazyColumn {
        items(songs, key = { it.id }) { song ->
            val search = stringResource(R.string.tutorial_search, song.title, song.artist)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Opens a YouTube search in the browser or YouTube app.
                // Implicit intent adapted from IntentLab ("Open TU Dublin Website").
                IconButton(onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(search))
                    )
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, noBrowser, Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        Icons.Filled.PlayCircle,
                        contentDescription = stringResource(R.string.find_tutorial, song.title)
                    )
                }
                IconToggleButton(
                    checked = song.isFavourite,
                    onCheckedChange = { onToggleFavourite(song) }
                ) {
                    Icon(
                        imageVector = if (song.isFavourite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = stringResource(
                            if (song.isFavourite) R.string.remove_favourite else R.string.add_favourite,
                            song.title
                        ),
                        tint = if (song.isFavourite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun TextList(lines: List<String>) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(lines) { line ->
            Text(line, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 12.dp))
            HorizontalDivider()
        }
    }
}

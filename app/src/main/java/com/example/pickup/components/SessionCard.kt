package com.example.pickup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pickup.R
import com.example.pickup.viewmodel.SessionListItem

// One practice session in the log. Card layout adapted from the
// SavedNotesScreen card (Simple Notes demo, Week 8).
// Reference: GenAi see prompt(s) (2) in report
@Composable
fun SessionCard(
    item: SessionListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deleteLabel = stringResource(R.string.delete_session)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            // Swiping is hard with TalkBack, so delete is also offered as an
            // accessibility action. Not covered in the labs.
            // Reference: https://developer.android.com/develop/ui/compose/accessibility/semantics
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(deleteLabel) { onDelete(); true }
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.tuningName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.songTitle ?: stringResource(R.string.free_play),
                    style = MaterialTheme.typography.titleMedium
                )
                if (item.session.note.isNotBlank()) {
                    Text(
                        text = item.session.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = stringResource(R.string.minutes_short, item.session.durationMinutes),
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.ReaderUi
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.read_at_time

/**
 * One row in the message details dialog's readers list: avatar, reader name and read time.
 */
@Composable
fun ReaderRow(reader: ReaderUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder for Avatar/Icon
        ProfilePictureView(
            filepath = reader.picturePath,
            modifier = Modifier.size(32.dp).padding(end = 8.dp),
        )
        Column {
            Text(
                text = reader.name,
                style = MaterialTheme.typography.bodyLarge
            )
            if (reader.readAtMillis > 0L) {
                Text(
                    text = stringResource(Res.string.read_at_time, millisToTimeDateOrYesterday(reader.readAtMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

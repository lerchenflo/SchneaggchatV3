package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent.SharedContentTab
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.shared_images
import schneaggchatv3mp.composeapp.generated.resources.shared_links
import schneaggchatv3mp.composeapp.generated.resources.shared_messages

/**
 * Everything shared in a chat as one row split into images | links | messages, each part with its
 * count. A tap opens the shared content screen on that part's tab. No background of its own.
 */
@Composable
fun SharedContentSplitButton(
    imageCount: Int,
    linkCount: Int,
    messageCount: Int,
    onClick: (SharedContentTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        SharedContentPart(
            icon = Icons.Default.PhotoLibrary,
            label = stringResource(Res.string.shared_images),
            count = imageCount,
            onClick = { onClick(SharedContentTab.IMAGES) },
            modifier = Modifier.weight(1f)
        )
        PartDivider()
        SharedContentPart(
            icon = Icons.Default.Link,
            label = stringResource(Res.string.shared_links),
            count = linkCount,
            onClick = { onClick(SharedContentTab.LINKS) },
            modifier = Modifier.weight(1f)
        )
        PartDivider()
        SharedContentPart(
            icon = Icons.AutoMirrored.Filled.Chat,
            label = stringResource(Res.string.shared_messages),
            count = messageCount,
            onClick = { onClick(SharedContentTab.MESSAGES) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SharedContentPart(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = count.toString(), style = MaterialTheme.typography.titleSmall)
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PartDivider() {
    VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

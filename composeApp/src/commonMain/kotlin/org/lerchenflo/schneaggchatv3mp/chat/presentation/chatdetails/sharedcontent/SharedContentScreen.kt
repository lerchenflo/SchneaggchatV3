@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.image.FullscreenImageDialog
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.systemEventText
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.CountSegmentedSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.rememberComboAnnotationSources
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.resolveComboAnnotationsToPlainText
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import org.lerchenflo.schneaggchatv3mp.utilities.toOpenableUrl
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.copy_link
import schneaggchatv3mp.composeapp.generated.resources.download
import schneaggchatv3mp.composeapp.generated.resources.go_to_message
import schneaggchatv3mp.composeapp.generated.resources.no_shared_images
import schneaggchatv3mp.composeapp.generated.resources.no_shared_links
import schneaggchatv3mp.composeapp.generated.resources.no_shared_messages
import schneaggchatv3mp.composeapp.generated.resources.audio
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.poll_closed
import schneaggchatv3mp.composeapp.generated.resources.poll_user_count
import schneaggchatv3mp.composeapp.generated.resources.shared_content_title
import schneaggchatv3mp.composeapp.generated.resources.shared_images
import schneaggchatv3mp.composeapp.generated.resources.shared_links
import schneaggchatv3mp.composeapp.generated.resources.shared_messages
import schneaggchatv3mp.composeapp.generated.resources.shared_messages_type_audio
import schneaggchatv3mp.composeapp.generated.resources.shared_messages_type_image
import schneaggchatv3mp.composeapp.generated.resources.shared_messages_type_poll
import schneaggchatv3mp.composeapp.generated.resources.shared_messages_type_system
import schneaggchatv3mp.composeapp.generated.resources.shared_messages_type_text
import schneaggchatv3mp.composeapp.generated.resources.shared_polls_subpoll_count

@Composable
fun SharedContentScreenRoot(
    chatId: String,
    isGroup: Boolean,
    initialTab: SharedContentTab,
) {
    val viewModel = koinViewModel<SharedContentViewModel> {
        parametersOf(chatId, isGroup, initialTab)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    SharedContentScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun SharedContentScreen(
    state: SharedContentState,
    onAction: (SharedContentAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = stringResource(Res.string.shared_content_title),
            onBackClick = { onAction(SharedContentAction.OnBackClick) }
        )

        HorizontalDivider()

        CountSegmentedSwitch(
            options = SharedContentTab.entries,
            selected = state.selectedTab,
            label = { stringResource(it.labelRes()) },
            showLeadingIcon = false,
            count = { tab ->
                when (tab) {
                    SharedContentTab.IMAGES -> state.images.size
                    SharedContentTab.LINKS -> state.links.size
                    SharedContentTab.MESSAGES -> state.messages.size
                }
            },
            onSelect = { onAction(SharedContentAction.OnTabSelected(it)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // weight, not fillMaxSize: the list has to take what is left below the title and the
        // switch, not the whole screen
        Box(modifier = Modifier.weight(1f)) {
            when (state.selectedTab) {
                SharedContentTab.IMAGES -> {
                    if (state.images.isEmpty()) {
                        EmptyHint(text = Res.string.no_shared_images, isLoading = state.isLoading)
                    } else {
                        SharedImageGrid(images = state.images, onAction = onAction)
                    }
                }

                SharedContentTab.LINKS -> {
                    if (state.links.isEmpty()) {
                        EmptyHint(text = Res.string.no_shared_links, isLoading = state.isLoading)
                    } else {
                        SharedLinkList(links = state.links, onAction = onAction)
                    }
                }

                SharedContentTab.MESSAGES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MessageTypeDropdown(
                            selected = state.messageType,
                            onSelect = { onAction(SharedContentAction.OnMessageTypeSelected(it)) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            if (state.messages.isEmpty()) {
                                EmptyHint(text = Res.string.no_shared_messages, isLoading = state.isLoadingMessages)
                            } else {
                                SharedMessageList(messages = state.messages, onAction = onAction)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedImageGrid(
    images: List<SharedImageItem>,
    onAction: (SharedContentAction) -> Unit,
) {
    // The item opened fullscreen, null while the grid is just being browsed
    var fullscreenItem by remember { mutableStateOf<SharedImageItem?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = images,
            key = { it.messageId ?: (it.pictureUrl + it.sendDate) }
        ) { image ->
            SharedImageCell(
                image = image,
                onClick = { fullscreenItem = image },
                onAction = onAction
            )
        }
    }

    fullscreenItem?.let { item ->
        FullscreenImageDialog(
            imageUrl = item.pictureUrl,
            onDismiss = { fullscreenItem = null },
            onDownload = { onAction(SharedContentAction.OnDownloadImageClick(item)) }
        )
    }
}

@Composable
private fun SharedImageCell(
    image: SharedImageItem,
    onClick: () -> Unit,
    onAction: (SharedContentAction) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(2.dp)) {
        AsyncImage(
            model = image.pictureUrl,
            contentDescription = image.caption.ifEmpty { null },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
        )

        SharedContentMenu(
            expanded = menuExpanded,
            messageId = image.messageId,
            onDismiss = { menuExpanded = false },
            onAction = onAction,
            extraItems = {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.download)) },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onAction(SharedContentAction.OnDownloadImageClick(image))
                    }
                )
            }
        )
    }
}

@Composable
private fun SharedLinkList(
    links: List<SharedLinkItem>,
    onAction: (SharedContentAction) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = links,
            // A message can hold several links, so the url has to be part of the key
            key = { (it.messageId ?: it.sendDate.toString()) + it.url }
        ) { link ->
            SharedLinkRow(
                link = link,
                onClick = { uriHandler.openUri(link.url.toOpenableUrl()) },
                onAction = onAction
            )
        }
    }
}

@Composable
private fun SharedLinkRow(
    link: SharedLinkItem,
    onClick: () -> Unit,
    onAction: (SharedContentAction) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        ListItem(
            headlineContent = {
                Text(
                    text = link.url,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = link.senderName + " · " + millisToString(
                        millis = link.sendDate,
                        format = "dd.MM.yyyy HH:mm"
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            )
        )

        SharedContentMenu(
            expanded = menuExpanded,
            messageId = link.messageId,
            onDismiss = { menuExpanded = false },
            onAction = onAction,
            extraItems = {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.copy_link)) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onAction(SharedContentAction.OnCopyLinkClick(link.url))
                    }
                )
            }
        )
    }
}

/** Picks which message type the MESSAGES tab lists. */
@Composable
private fun MessageTypeDropdown(
    selected: MessageType,
    onSelect: (MessageType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Icon(
                imageVector = selected.icon(),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(selected.labelRes()))
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MessageType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.labelRes())) },
                    leadingIcon = { Icon(type.icon(), contentDescription = null) },
                    trailingIcon = if (type == selected) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(type)
                    }
                )
            }
        }
    }
}

@Composable
private fun SharedMessageList(
    messages: List<SharedMessageItem>,
    onAction: (SharedContentAction) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = messages,
            // localPK is unique for every stored row, sent or not
            key = { it.localPK }
        ) { message ->
            SharedMessageRow(message = message, onAction = onAction)
        }
    }
}

/** Tapping a message jumps to it in the chat. */
@Composable
private fun SharedMessageRow(
    message: SharedMessageItem,
    onAction: (SharedContentAction) -> Unit,
) {
    val annotationSources = rememberComboAnnotationSources()
    val poll = message.poll

    val headline = when (message.type) {
        MessageType.SYSTEM -> message.systemEvent?.let { systemEventText(it) }.orEmpty()
        MessageType.AUDIO -> stringResource(Res.string.audio)
        MessageType.IMAGE -> message.text.ifBlank { stringResource(Res.string.image) }
        else -> message.text
    }

    val pollDetails = poll?.let {
        buildList {
            add(stringResource(Res.string.poll_user_count, it.voterCount.toString()))
            if (it.subPollCount > 0) add(stringResource(Res.string.shared_polls_subpoll_count, it.subPollCount.toString()))
            if (it.isClosed) add(stringResource(Res.string.poll_closed))
        }.joinToString(" · ")
    }

    ListItem(
        headlineContent = {
            Text(
                text = resolveComboAnnotationsToPlainText(headline, annotationSources),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = (if (message.type == MessageType.SYSTEM) "" else message.senderName + " · ") + millisToString(
                        millis = message.sendDate,
                        format = "dd.MM.yyyy HH:mm"
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                if (pollDetails != null) {
                    Text(
                        text = pollDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (poll.isClosed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        leadingContent = {
            val pictureUrl = message.pictureUrl
            if (message.type == MessageType.IMAGE && !pictureUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = pictureUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    imageVector = message.type.icon(),
                    contentDescription = null,
                    tint = if (poll?.isClosed == true) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier.clickable(enabled = message.messageId != null) {
            message.messageId?.let { onAction(SharedContentAction.OnGoToMessageClick(it)) }
        }
    )
}

/**
 * Long-press menu shared by the image and link tabs. "Go to message" is left out for a message that has no server
 * id yet - it could not be jumped to, and an entry that silently does nothing is worse than none.
 */
@Composable
private fun SharedContentMenu(
    expanded: Boolean,
    messageId: String?,
    onDismiss: () -> Unit,
    onAction: (SharedContentAction) -> Unit,
    extraItems: @Composable () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (messageId != null) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.go_to_message)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onAction(SharedContentAction.OnGoToMessageClick(messageId))
                }
            )
        }
        extraItems()
    }
}

@Composable
private fun EmptyHint(text: StringResource, isLoading: Boolean) {
    if (isLoading) return

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}

private fun SharedContentTab.labelRes(): StringResource = when (this) {
    SharedContentTab.IMAGES -> Res.string.shared_images
    SharedContentTab.LINKS -> Res.string.shared_links
    SharedContentTab.MESSAGES -> Res.string.shared_messages
}

private fun MessageType.labelRes(): StringResource = when (this) {
    MessageType.TEXT -> Res.string.shared_messages_type_text
    MessageType.IMAGE -> Res.string.shared_messages_type_image
    MessageType.POLL -> Res.string.shared_messages_type_poll
    MessageType.AUDIO -> Res.string.shared_messages_type_audio
    MessageType.SYSTEM -> Res.string.shared_messages_type_system
}

private fun MessageType.icon(): ImageVector = when (this) {
    MessageType.TEXT -> Icons.AutoMirrored.Filled.Message
    MessageType.IMAGE -> Icons.Default.Image
    MessageType.POLL -> Icons.Default.Poll
    MessageType.AUDIO -> Icons.Default.Mic
    MessageType.SYSTEM -> Icons.Default.Info
}

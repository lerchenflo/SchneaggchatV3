@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.image.FullscreenImageDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import org.lerchenflo.schneaggchatv3mp.utilities.toOpenableUrl
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.copy_link
import schneaggchatv3mp.composeapp.generated.resources.download
import schneaggchatv3mp.composeapp.generated.resources.go_to_message
import schneaggchatv3mp.composeapp.generated.resources.no_shared_images
import schneaggchatv3mp.composeapp.generated.resources.no_shared_links
import schneaggchatv3mp.composeapp.generated.resources.shared_content_title
import schneaggchatv3mp.composeapp.generated.resources.shared_images
import schneaggchatv3mp.composeapp.generated.resources.shared_links

@Composable
fun SharedContentScreenRoot(
    chatId: String,
    isGroup: Boolean,
    showLinks: Boolean,
) {
    val viewModel = koinViewModel<SharedContentViewModel> {
        parametersOf(chatId, isGroup, showLinks)
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

        SharedContentTabSwitch(
            selected = state.selectedTab,
            imageCount = state.images.size,
            linkCount = state.links.size,
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
            }
        }
    }
}

@Composable
private fun SharedContentTabSwitch(
    selected: SharedContentTab,
    imageCount: Int,
    linkCount: Int,
    onSelect: (SharedContentTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = SharedContentTab.entries

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, tab ->
            val count = when (tab) {
                SharedContentTab.IMAGES -> imageCount
                SharedContentTab.LINKS -> linkCount
            }

            SegmentedButton(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size)
            ) {
                Text(
                    text = "${stringResource(tab.labelRes())} ($count)",
                    maxLines = 1
                )
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

/**
 * Long-press menu shared by both tabs. "Go to message" is left out for a message that has no server
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
}

package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.sharedtext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.TextWriterRun
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.memberNameColor
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_extension_not_found
import schneaggchatv3mp.composeapp.generated.resources.shared_text_hide_author_colors
import schneaggchatv3mp.composeapp.generated.resources.shared_text_placeholder
import schneaggchatv3mp.composeapp.generated.resources.shared_text_show_author_colors

@Composable
fun SharedTextScreenRoot(
    extensionId: String,
    topBarActions: @Composable RowScope.() -> Unit = {},
) {
    val viewModel = koinViewModel<SharedTextViewModel> {
        parametersOf(extensionId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    SharedTextScreen(
        state = state,
        onAction = viewModel::onAction,
        topBarActions = topBarActions
    )
}

/** One text the whole group writes together, each part highlighted in its writer's name color. */
@Composable
fun SharedTextScreen(
    state: SharedTextState,
    onAction: (SharedTextAction) -> Unit,
    topBarActions: @Composable RowScope.() -> Unit = {},
) {
    val isReady = !state.isLoading && !state.extensionMissing

    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = state.title,
            onBackClick = { onAction(SharedTextAction.OnBackClick) },
            actions = {
                if (isReady) {
                    AuthorColorsToggle(
                        showAuthorColors = state.showAuthorColors,
                        onClick = { onAction(SharedTextAction.OnToggleAuthorColors) }
                    )
                }
                topBarActions()
            }
        )

        HorizontalDivider()

        when {
            state.isLoading -> CenteredBox { CircularProgressIndicator() }
            state.extensionMissing -> CenteredBox { Text(stringResource(Res.string.group_extension_not_found)) }
            else -> {
                if (state.showAuthorColors && state.authors.isNotEmpty()) {
                    AuthorLegend(authors = state.authors)
                    HorizontalDivider()
                }
                SharedTextEditor(
                    value = state.text,
                    writerRuns = state.writerRuns,
                    authors = state.authors,
                    showAuthorColors = state.showAuthorColors,
                    onValueChange = { onAction(SharedTextAction.OnTextChange(it)) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun AuthorColorsToggle(
    showAuthorColors: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        if (showAuthorColors) {
            Icon(
                imageVector = Icons.Default.FormatColorReset,
                contentDescription = stringResource(Res.string.shared_text_hide_author_colors)
            )
        } else {
            Icon(
                imageVector = Icons.Default.FormatColorFill,
                contentDescription = stringResource(Res.string.shared_text_show_author_colors)
            )
        }
    }
}

/** Name chips in each author's color, so the highlights can be told apart. */
@Composable
private fun AuthorLegend(
    authors: List<SharedTextAuthorUi>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(authors, key = { it.userId }) { author ->
            Text(
                text = author.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .background(
                        color = memberNameColor(author.color).copy(alpha = AUTHOR_COLOR_ALPHA),
                        shape = CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SharedTextEditor(
    value: TextFieldValue,
    writerRuns: List<TextWriterRun>,
    authors: List<SharedTextAuthorUi>,
    showAuthorColors: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val authorColors = remember(writerRuns, authors, showAuthorColors) {
        if (showAuthorColors) {
            AuthorColorsTransformation(
                writerRuns = writerRuns,
                colorOf = authors.associate { it.userId to memberNameColor(it.color) }
            )
        } else {
            VisualTransformation.None
        }
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.padding(16.dp),
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = authorColors,
        decorationBox = { innerTextField ->
            Box {
                if (value.text.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.shared_text_placeholder),
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SharedTextScreenPreview() {
    val text = "Packing list\nTent, stove and the good mood."
    SchneaggchatTheme {
        SharedTextScreen(
            state = SharedTextState(
                title = "Camping 2026",
                text = TextFieldValue(text),
                writerRuns = listOf(
                    TextWriterRun(length = 13, writtenBy = "me"),
                    TextWriterRun(length = 15, writtenBy = "anna"),
                    TextWriterRun(length = text.length - 28, writtenBy = "me"),
                ),
                authors = listOf(
                    SharedTextAuthorUi(userId = "me", name = "Lena (you)", color = 0xFF4CAF50.toInt(), isMe = true),
                    SharedTextAuthorUi(userId = "anna", name = "Anna", color = 0xFFFF9800.toInt(), isMe = false),
                ),
                isLoading = false,
            ),
            onAction = {}
        )
    }
}

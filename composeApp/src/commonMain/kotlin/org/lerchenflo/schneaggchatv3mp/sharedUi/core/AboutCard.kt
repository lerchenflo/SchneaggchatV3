package org.lerchenflo.schneaggchatv3mp.sharedUi.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.about_quote
import schneaggchatv3mp.composeapp.generated.resources.edit

/**
 * One card for the texts about a person or group (user pick 2026-11-24, mockup C): an optional
 * [title] ("Über emma"), then [AboutCardPart]s separated by [AboutCardDivider], e.g. what the person
 * says about themselves (status) and what their friends write about them. Used by the chat details
 * and the own profile in the settings, so both look the same.
 */
@Composable
fun AboutCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
            }
            content()
        }
    }
}

/**
 * One text of an [AboutCard]: a [label] line (with [leading], e.g. a small picture or an icon), then
 * [text], or [emptyText] while there is none. With [onEditClick] the label line gets an edit button
 * and an empty part a button labeled [emptyActionText]; without it the part is read-only and
 * [onClick] (e.g. an info snackbar) handles a tap. [isQuote] shows the text in quotes and italic,
 * for what a person says about themselves.
 */
@Composable
fun AboutCardPart(
    label: String,
    text: String?,
    emptyText: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    isQuote: Boolean = false,
    hint: String? = null,
    onEditClick: (() -> Unit)? = null,
    emptyActionText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val shownText = text?.replace("\\n", "\n")?.takeIf { it.isNotBlank() }
    val tapAction = onEditClick ?: onClick

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (tapAction != null) Modifier.clickable(onClick = tapAction) else Modifier)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (onEditClick != null && shownText != null) {
                TextButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(Res.string.edit))
                }
            }
        }

        Column(modifier = Modifier.padding(end = 12.dp)) {
            if (shownText != null) {
                Text(
                    text = if (isQuote) stringResource(Res.string.about_quote, shownText) else shownText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = if (isQuote) FontStyle.Italic else FontStyle.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onEditClick != null && emptyActionText != null) {
                    FilledTonalButton(onClick = onEditClick, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = emptyActionText)
                    }
                }
            }

            if (hint != null) {
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/** Thin line between two parts of an [AboutCard]. */
@Composable
fun AboutCardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

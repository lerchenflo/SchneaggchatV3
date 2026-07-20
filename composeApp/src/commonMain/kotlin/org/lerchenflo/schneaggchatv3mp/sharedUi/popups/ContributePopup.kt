@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.sharedUi.popups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.contribute_bug_hint
import schneaggchatv3mp.composeapp.generated.resources.contribute_bug_title
import schneaggchatv3mp.composeapp.generated.resources.contribute_feature_hint
import schneaggchatv3mp.composeapp.generated.resources.contribute_feature_title
import schneaggchatv3mp.composeapp.generated.resources.contribute_later
import schneaggchatv3mp.composeapp.generated.resources.contribute_message
import schneaggchatv3mp.composeapp.generated.resources.contribute_open_form
import schneaggchatv3mp.composeapp.generated.resources.contribute_title

/**
 * Nudges the user to send bug reports and feature requests. Shown from the chat
 * selector at most once every two weeks, and never in the same launch as the
 * changelog popup.
 */
@Composable
fun ContributePopup(
    onDismiss: () -> Unit,
    onOpenReportForm: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        ContributePopupContent(
            onDismiss = onDismiss,
            onOpenReportForm = onOpenReportForm
        )
    }
}

/**
 * Sheet body, split out from [ContributePopup] so it can be previewed — a
 * ModalBottomSheet renders in its own window and shows up empty in previews.
 */
@Composable
private fun ContributePopupContent(
    onDismiss: () -> Unit,
    onOpenReportForm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.contribute_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = stringResource(Res.string.contribute_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ContributeHint(
            icon = Icons.Default.BugReport,
            iconTint = MaterialTheme.colorScheme.error,
            title = stringResource(Res.string.contribute_bug_title),
            text = stringResource(Res.string.contribute_bug_hint)
        )

        ContributeHint(
            icon = Icons.Default.Lightbulb,
            iconTint = MaterialTheme.colorScheme.tertiary,
            title = stringResource(Res.string.contribute_feature_title),
            text = stringResource(Res.string.contribute_feature_hint)
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onOpenReportForm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(Res.string.contribute_open_form))
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(Res.string.contribute_later))
        }
    }
}

@Composable
private fun ContributeHint(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    text: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Preview(
    showBackground = true,
    apiLevel = 36
)
@Composable
private fun ContributePopupPreview() {
    SchneaggchatTheme {
        // Stands in for the sheet container the content normally sits in
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                Spacer(Modifier.height(20.dp))
                ContributePopupContent(
                    onDismiss = {},
                    onOpenReportForm = {}
                )
            }
        }
    }
}

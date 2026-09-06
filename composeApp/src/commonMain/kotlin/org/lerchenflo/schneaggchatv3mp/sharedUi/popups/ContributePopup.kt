package org.lerchenflo.schneaggchatv3mp.sharedUi.popups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.contribute_bug_hint
import schneaggchatv3mp.composeapp.generated.resources.contribute_bug_title
import schneaggchatv3mp.composeapp.generated.resources.contribute_donate_action
import schneaggchatv3mp.composeapp.generated.resources.contribute_donate_apple
import schneaggchatv3mp.composeapp.generated.resources.contribute_donate_mac
import schneaggchatv3mp.composeapp.generated.resources.contribute_donate_note
import schneaggchatv3mp.composeapp.generated.resources.contribute_donate_text
import schneaggchatv3mp.composeapp.generated.resources.contribute_donate_title
import schneaggchatv3mp.composeapp.generated.resources.contribute_feature_hint
import schneaggchatv3mp.composeapp.generated.resources.contribute_feature_title
import schneaggchatv3mp.composeapp.generated.resources.contribute_later
import schneaggchatv3mp.composeapp.generated.resources.contribute_message
import schneaggchatv3mp.composeapp.generated.resources.contribute_open_form
import schneaggchatv3mp.composeapp.generated.resources.contribute_title

/**
 * Nudges the user to send bug reports and feature requests or to donate. Shown from the
 * chat selector at most once per CONTRIBUTE_POPUP_INTERVAL_MILLIS, and never in the same
 * launch as the changelog popup.
 *
 * Deliberately not dismissable by back press or by tapping outside — the user picks one
 * of the three actions, "maybe later" included.
 */
@Composable
fun ContributePopup(
    onDismiss: () -> Unit,
    onOpenReportForm: () -> Unit,
    onOpenDonationPage: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                ContributePopupContent(
                    onDismiss = onDismiss,
                    onOpenReportForm = onOpenReportForm,
                    onOpenDonationPage = onOpenDonationPage
                )
            }
        }
    }
}

/**
 * Dialog body, split out from [ContributePopup] so it can be previewed — a Dialog renders
 * in its own window and shows up empty in previews.
 */
@Composable
private fun ContributePopupContent(
    onDismiss: () -> Unit,
    onOpenReportForm: () -> Unit,
    onOpenDonationPage: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.contribute_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(Res.string.contribute_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ContributeOption(
                title = stringResource(Res.string.contribute_open_form),
                onClick = onOpenReportForm
            ) {
                ContributeDetail(
                    icon = Icons.Default.BugReport,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = stringResource(Res.string.contribute_bug_title),
                    text = stringResource(Res.string.contribute_bug_hint)
                )
                ContributeDetail(
                    icon = Icons.Default.Lightbulb,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = stringResource(Res.string.contribute_feature_title),
                    text = stringResource(Res.string.contribute_feature_hint)
                )
            }

            ContributeOption(
                title = stringResource(Res.string.contribute_donate_title),
                onClick = onOpenDonationPage
            ) {
                Text(
                    text = stringResource(Res.string.contribute_donate_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ContributeDetail(
                    icon = Icons.Default.LaptopMac,
                    iconTint = MaterialTheme.colorScheme.primary,
                    text = stringResource(Res.string.contribute_donate_mac)
                )
                ContributeDetail(
                    icon = Icons.Default.CardMembership,
                    iconTint = MaterialTheme.colorScheme.primary,
                    text = stringResource(Res.string.contribute_donate_apple)
                )
                Text(
                    text = stringResource(Res.string.contribute_donate_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(Res.string.contribute_donate_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
        ) {
            Text(text = stringResource(Res.string.contribute_later))
        }
    }
}

@Composable
private fun ContributeOption(
    title: String,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                content()
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ContributeDetail(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    title: String? = null,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                ContributePopupContent(
                    onDismiss = {},
                    onOpenReportForm = {},
                    onOpenDonationPage = {}
                )
            }
        }
    }
}

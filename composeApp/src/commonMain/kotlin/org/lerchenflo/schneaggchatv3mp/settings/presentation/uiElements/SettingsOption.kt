package org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SettingsOption(
    icon: ImageVector,
    text: String,
    subtext: String? = null,
    onClick: () -> Unit,
    rightSideIcon: @Composable () -> Unit = {}
){

    Box(
        modifier = Modifier
            .clickable{
                onClick()
            }
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 20.dp,
                bottom = 20.dp
            ),
        ){
        Row(
            verticalAlignment = Alignment.CenterVertically

        ) {
            Icon(
                contentDescription = text,
                modifier = Modifier.size(35.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                imageVector = icon
            )

            Spacer(modifier = Modifier.width(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = text,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium
                )

                if (subtext != null){
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 0.65f)

                    )
                }
            }

            rightSideIcon()
        }

    }
}

@Preview(
    showBackground = true)
@Composable
private fun Settingsoptionpreview() {
    SettingsOption(
        icon = Icons.Default.Usb,
        text = "Default text bla bla",
        subtext = "this are cool settings for a good settings option. please handle with care and do not disturb",
        onClick = {},
        rightSideIcon = {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.error

            )
        }
    )
}
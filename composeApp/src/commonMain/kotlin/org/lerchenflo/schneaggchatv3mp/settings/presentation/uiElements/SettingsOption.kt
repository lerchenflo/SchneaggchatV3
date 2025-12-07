package org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsOption(
    icon: ImageVector,
    text: String,
    subtext: String? = null,
    onClick: () -> Unit
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
            )
            ,

        //.background(MaterialTheme.colorScheme.onSurface)
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

            Column {
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
        }

    }
}
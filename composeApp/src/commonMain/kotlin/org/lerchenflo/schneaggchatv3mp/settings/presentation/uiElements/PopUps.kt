package org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults.textContentColor
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.utilities.ThemeSetting
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.theme

@Composable
fun ThemeSelector(
    onConfirm:(ThemeSetting) -> Unit,
    onThemeSelected : (ThemeSetting) -> Unit,
    onDismiss:() -> Unit,
    selectedTheme:ThemeSetting
){
    var tempSelection by mutableStateOf(selectedTheme)

    AlertDialog(
        onDismissRequest = {onDismiss()},
        confirmButton = {
            TextButton(
                onClick = { onConfirm (tempSelection)}
            ) {
                Text(stringResource(Res.string.ok), color = textContentColor)
            }
        },
        dismissButton =
            {
                TextButton(
                    onClick = { onDismiss() }
                ) {
                    Text(stringResource(Res.string.cancel), color = textContentColor)
                }
            },
        //icon = { Icon(Icons.Default.Palette, contentDescription = null) },
        title = { Text(text = stringResource(Res.string.theme)) },
        text = { Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            // Radio group for theme selection
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                ThemeSetting.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (theme == tempSelection),
                                onClick = {
                                    tempSelection = theme
                                          onThemeSelected(theme)
                                          },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
/*
                        RadioButton(
                            selected = (theme == tempSelection),
                            onClick = null // null because the row handles the click
                        )

 */
                        Icon(
                            imageVector = theme.getIcon(),
                            contentDescription = null,
                            tint = if(theme == tempSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = theme.toUiText().asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        },
        shape = MaterialTheme.shapes.large,
    )
}

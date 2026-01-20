package org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults.textContentColor
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.utilities.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageSetting
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.language
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

@Composable
fun LanguageSelector(
    onConfirm:(LanguageSetting) -> Unit,
    onLanguageSelected : (LanguageSetting) -> Unit,
    onDismiss:() -> Unit,
    selectedLanguage: LanguageSetting
){
    var tempSelection by mutableStateOf(selectedLanguage)

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
        title = { Text(text = stringResource(Res.string.language)) },
        text = { Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            // Radio group for language selection
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                LanguageSetting.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (language == tempSelection),
                                onClick = {
                                    tempSelection = language
                                    onLanguageSelected(language)
                                          },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = language.getIcon(),
                            contentDescription = null,
                            tint = if(language == tempSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = language.toUiText().asString(),
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

@Composable
fun ChangeDialog(
    title: String,
    initialValue: String,
    placeholder: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    confirmButtonText: String = "Change",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    validator: ((String) -> String?)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    var newValue by remember { mutableStateOf(initialValue) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(text = title)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = newValue,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp
                    ),
                    singleLine = singleLine,
                    onValueChange = { 
                        newValue = it
                        errorMessage = validator?.invoke(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                                onDismiss()
                            }
                            false
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            })
                        },
                    placeholder = { Text(placeholder) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType
                    ),
                    isError = errorMessage != null
                )
                
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validationError = validator?.invoke(newValue)
                    if (validationError == null) {
                        onConfirm(newValue)
                        onDismiss()
                    } else {
                        errorMessage = validationError
                    }
                }
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        shape = MaterialTheme.shapes.large,
    )
}

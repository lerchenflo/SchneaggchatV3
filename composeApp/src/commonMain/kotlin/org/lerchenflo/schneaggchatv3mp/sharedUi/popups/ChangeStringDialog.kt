package org.lerchenflo.schneaggchatv3mp.sharedUi.popups

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.UiText.StringResourceText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.change
import schneaggchatv3mp.composeapp.generated.resources.name_too_long
import schneaggchatv3mp.composeapp.generated.resources.name_too_short


@Composable
fun ChangeStringDialog(
    title: String,
    oldString: String,
    maxLines: Int = 5,
    placeholder: String = "",
    errorMessage: ErrorMessage? = null,
    onDismiss: () -> Unit,
    updateString: (String) -> Unit,
    thirdButton: @Composable (() -> Unit)? = null
){
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current // Also helpful to hide keyboard

    var stringValue by retain { mutableStateOf(TextFieldValue(oldString)) } // remembersaveable to survive screen rotation

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    updateString(stringValue.text.trim())
                },
            ) {
                Text(
                    text = stringResource(Res.string.change)
                )
            }
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text(
                        text = stringResource(Res.string.cancel)
                    )
                }

                if (thirdButton != null) {
                    thirdButton()
                }
            }
        },
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) { // sorry iphone user aber ihr dürfen ned zu viel Zeilen macha. I krigs ned zum richta
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        })
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = title,
                    )

                    if(errorMessage != null){
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage.toUiText().asString(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = stringValue,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 14.sp // You can adjust this value as needed
                        ),
                        maxLines = maxLines,
                        onValueChange = { newValue ->
                            stringValue = newValue
                        },
                        modifier = Modifier
                            .onPreviewKeyEvent { event ->
                                // Check if the key is 'Escape' and it's a 'KeyDown' event
                                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                                    onDismiss()
                                }
                                false // Pass all other events (letters, backspace, etc.) to the TextField
                            }
                            .fillMaxWidth(),
                        placeholder = { Text(placeholder) }
                    )

                }
            }

        },
    )
}

enum class ErrorMessage{
    NAME_TO_LONG,
    NAME_TO_SHORT;
    fun toUiText(): UiText = when (this) {
        NAME_TO_LONG -> StringResourceText(Res.string.name_too_long)
        NAME_TO_SHORT -> StringResourceText(Res.string.name_too_short)
    }
}


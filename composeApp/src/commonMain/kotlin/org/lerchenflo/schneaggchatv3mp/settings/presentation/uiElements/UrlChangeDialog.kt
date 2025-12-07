package org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.save

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlChangeDialog (
    onDismiss: ()-> Unit,
    onConfirm: (String)-> Unit,
    serverUrl: String
){
    var internalServerUrl by remember(serverUrl) {
        mutableStateOf(serverUrl)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.change_server_url)) },
        text = {
            Column(){
                OutlinedTextField(
                    value = internalServerUrl,
                    singleLine = true,
                    onValueChange = { internalServerUrl = it },
                    placeholder = { Text( text ="https://schneaggchatv3.lerchenflo.eu") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = stringResource(Res.string.change_server_url)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick ={
                onConfirm(internalServerUrl)
            }) {
                Text(text = stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.cancel))
            }
        }
    )
}

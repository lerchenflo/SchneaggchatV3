package org.lerchenflo.schneaggchatv3mp.settings.presentation

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.save

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlChangeDialog (
    onDismiss: ()-> Unit,
    onConfirm: ()-> Unit
){
    val settingsViewModel = koinViewModel<SettingsViewModel>()

    LaunchedEffect(Unit){
        settingsViewModel.init()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.change_server_url)) },
        text = {
            Contents(settingsViewModel)
        },
        confirmButton = {
            TextButton(onClick ={
                onConfirm()
                settingsViewModel.saveServerUrl()
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

@Composable
private fun Contents(
    viewModel: SettingsViewModel
){
    Column(){
        OutlinedTextField(
            value = viewModel.serverURL,
            singleLine = true,
            onValueChange = { viewModel.updateServerUrl(it) },
            placeholder = { Text( text ="https://example.com:1234") }, // todo: evtl in da strings oder die default url. oder todo löscha des goht o
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

}
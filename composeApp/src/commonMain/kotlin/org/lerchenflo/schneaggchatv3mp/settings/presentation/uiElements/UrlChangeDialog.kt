package org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL_TEST
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.save
import schneaggchatv3mp.composeapp.generated.resources.server_not_reachable
import schneaggchatv3mp.composeapp.generated.resources.test_server_connection
import schneaggchatv3mp.composeapp.generated.resources.use_default_server
import schneaggchatv3mp.composeapp.generated.resources.use_test_server


/** State of the "test server" button in [UrlChangeDialog]; held by the dialog's host ViewModel. */
sealed class CheckStatus {
    object Idle : CheckStatus()
    object Loading : CheckStatus()
    object Success : CheckStatus()
    object Error : CheckStatus()
}


/**
 * Edits the server URL. The server test runs in the host's ViewModel: [checkStatus] is its result,
 * [onTestServer] starts it for the entered URL and [onUrlEdited] resets it to [CheckStatus.Idle].
 * The host resets the check when it opens or closes the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlChangeDialog (
    onDismiss: ()-> Unit,
    onConfirm: (String)-> Unit,
    serverUrl: String,
    checkStatus: CheckStatus,
    onTestServer: (String) -> Unit,
    onUrlEdited: () -> Unit,
){
    var internalServerUrl by remember(serverUrl) {
        mutableStateOf(serverUrl)
    }

    val testServer: () -> Unit = {
        if (checkStatus != CheckStatus.Loading){
            if (!internalServerUrl.startsWith("http://") && !internalServerUrl.startsWith("https://")){
                internalServerUrl = "https://$internalServerUrl" //Default https
            }

            onTestServer(internalServerUrl)
        }
    }



    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.change_server_url)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = internalServerUrl,
                    singleLine = true,
                    onValueChange = {
                        internalServerUrl = it
                        onUrlEdited()
                    },
                    placeholder = { Text( text = BASE_SERVER_URL) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                testServer()
                            }
                        ) {

                            when (checkStatus) {
                                CheckStatus.Idle -> Icon(
                                    imageVector = Icons.Default.WifiFind,
                                    contentDescription = stringResource(Res.string.test_server_connection)
                                )
                                CheckStatus.Loading -> CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp)
                                )
                                CheckStatus.Success -> Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(Res.string.ok),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                is CheckStatus.Error -> Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = stringResource(Res.string.server_not_reachable),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }

                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    ),
                    isError = checkStatus == CheckStatus.Error,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(Res.string.use_default_server),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable{
                            internalServerUrl = BASE_SERVER_URL
                            onConfirm(internalServerUrl)
                        }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.use_test_server),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable{
                            internalServerUrl = BASE_SERVER_URL_TEST
                            onConfirm(internalServerUrl)
                        }
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
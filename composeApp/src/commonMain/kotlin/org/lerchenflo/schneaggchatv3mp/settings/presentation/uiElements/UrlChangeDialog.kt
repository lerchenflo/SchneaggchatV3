package org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.save
import schneaggchatv3mp.composeapp.generated.resources.use_default_server


private sealed class CheckStatus {
    object Idle : CheckStatus()
    object Loading : CheckStatus()
    object Success : CheckStatus()
    object Error : CheckStatus()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlChangeDialog (
    onDismiss: ()-> Unit,
    onConfirm: (String)-> Unit,
    serverUrl: String,
){
    var internalServerUrl by remember(serverUrl) {
        mutableStateOf(serverUrl)
    }

    val coroutinescope = rememberCoroutineScope()

    var checkStatus by remember { mutableStateOf<CheckStatus>(CheckStatus.Idle) }

    val appRepository : AppRepository = koinInject()

    val testServer: () -> Unit = {
        if (checkStatus != CheckStatus.Loading){
            checkStatus = CheckStatus.Loading
            coroutinescope.launch {
                if (!internalServerUrl.startsWith("http://") && !internalServerUrl.startsWith("https://")){
                    internalServerUrl = internalServerUrl.prependIndent("https://") //Default https
                }

                val success = appRepository.testServer(internalServerUrl)

                checkStatus = if (success){
                    CheckStatus.Success
                } else CheckStatus.Error
            }
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
                        checkStatus = CheckStatus.Idle
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
                                    contentDescription = "Test server"
                                )
                                CheckStatus.Loading -> CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp)
                                )
                                CheckStatus.Success -> Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "OK",
                                    tint = Color.Green
                                )
                                is CheckStatus.Error -> Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "server error",
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
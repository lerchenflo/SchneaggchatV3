package org.lerchenflo.schneaggchatv3mp.todolist.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.OWNID
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugEditor
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugPlatform
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugPriority
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugStatus
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugType
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.assignedto
import schneaggchatv3mp.composeapp.generated.resources.buglastchanged
import schneaggchatv3mp.composeapp.generated.resources.bugplatform
import schneaggchatv3mp.composeapp.generated.resources.bugpriority
import schneaggchatv3mp.composeapp.generated.resources.bugstatus
import schneaggchatv3mp.composeapp.generated.resources.bugtype
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.enteredby
import schneaggchatv3mp.composeapp.generated.resources.save

@Composable
fun ShowAddPopup(
    onDismiss: () -> Unit,
    onSubmit: (TodoEntry) -> Unit,

    ){

    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }

    var selectedType by rememberSaveable { mutableStateOf(BugType.BugReport) }
    var selectedPlatform by rememberSaveable { mutableStateOf(BugPlatform.Multiplatform) }

    // Dropdown expansions
    var typeExpanded by remember { mutableStateOf(false) }
    var platformExpanded by remember { mutableStateOf(false) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.add),
            )
        },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(4.dp))


                //Type dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = stringResource(Res.string.bugtype),
                    )

                    Box{
                        TextButton(
                            onClick = { typeExpanded = true },
                        ) {
                            Text(text = selectedType.toString())
                        }

                        DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                        ){
                            BugType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.toString()) },
                                    onClick = {
                                        selectedType = BugType.fromInt(type.value)
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                //Platform dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = stringResource(Res.string.bugplatform),
                    )

                    Box{
                        TextButton(
                            onClick = { platformExpanded = true },
                        ) {
                            Text(text = selectedPlatform.toString())
                        }

                        DropdownMenu(
                            expanded = platformExpanded,
                            onDismissRequest = { platformExpanded = false },
                        ){
                            BugPlatform.entries.forEach { platform ->
                                DropdownMenuItem(
                                    text = { Text(platform.toString()) },
                                    onClick = {
                                        selectedPlatform = BugPlatform.fromInt(platform.value)
                                        platformExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Inhalt") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    singleLine = false,
                )


            }


        },

        confirmButton = {
            val newtodo = TodoEntry(
                id = 0L,
                senderId = OWNID?: 0L,
                platform = selectedPlatform.value,
                type = selectedType.value,
                editorId = 0,
                content = content,
                title = title,
                lastChanged = "0",
                priority = BugPriority.Normal.value
            )

            TextButton(onClick = { onSubmit(newtodo) }) {
                Text(stringResource(Res.string.save))
            }

        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },


        )

}
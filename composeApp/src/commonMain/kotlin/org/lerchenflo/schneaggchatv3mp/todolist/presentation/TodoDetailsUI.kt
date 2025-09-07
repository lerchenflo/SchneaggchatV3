package org.lerchenflo.schneaggchatv3mp.todolist.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import org.jetbrains.compose.resources.Resource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugEditor
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugPlatform
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugPriority
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugStatus
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugType
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.assignedto
import schneaggchatv3mp.composeapp.generated.resources.buglastchanged
import schneaggchatv3mp.composeapp.generated.resources.bugplatform
import schneaggchatv3mp.composeapp.generated.resources.bugpriority
import schneaggchatv3mp.composeapp.generated.resources.bugstatus
import schneaggchatv3mp.composeapp.generated.resources.bugtype
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.enteredby
import schneaggchatv3mp.composeapp.generated.resources.not_assigned
import schneaggchatv3mp.composeapp.generated.resources.save
import schneaggchatv3mp.composeapp.generated.resources.status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowTodoDetails(
    todoEntry: TodoEntry,
    onDismiss: () -> Unit,
    onSave: (TodoEntry) -> Unit,
    onDelete: (() -> Unit)? = null
){

    var title by rememberSaveable { mutableStateOf(todoEntry.title) }
    var content by rememberSaveable { mutableStateOf(todoEntry.content) }
    var enteredBy by rememberSaveable { mutableStateOf(todoEntry.senderAsString) }

    var selectedEditor by rememberSaveable { mutableStateOf(BugEditor.fromInt(todoEntry.editorId)) }
    var selectedStatus by rememberSaveable { mutableStateOf(BugStatus.fromInt(todoEntry.status)) }
    var selectedType by rememberSaveable { mutableStateOf(BugType.fromInt(todoEntry.type)) }
    var selectedPlatform by rememberSaveable { mutableStateOf(BugPlatform.fromInt(todoEntry.platform)) }
    var selectedPriority by rememberSaveable { mutableStateOf(BugPriority.fromInt(todoEntry.priority)) }


    // Dropdown expansions
    var editorsExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var platformExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }





    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = title, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )

                if (onDelete != null) {
                    IconButton(onClick = { onDelete() }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        },
        text = {
            Column {
                //Entered by text
                Text(
                    text = stringResource(Res.string.enteredby) + " " + enteredBy,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = stringResource(Res.string.buglastchanged) + " " + millisToString(todoEntry.lastChanged.toLong()), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))


                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth()
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
                        TextButton(onClick = { typeExpanded = true }) {
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
                        TextButton(onClick = { platformExpanded = true }) {
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


                //Editor selector dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = stringResource(Res.string.assignedto),
                    )

                    Box{
                        TextButton(onClick = { editorsExpanded = true }) {
                            Text(text = selectedEditor.toString())
                        }

                        DropdownMenu(
                            expanded = editorsExpanded,
                            onDismissRequest = { editorsExpanded = false },
                        ){
                            BugEditor.entries.forEach { editor ->
                                DropdownMenuItem(
                                    text = { Text(editor.toString()) },
                                    onClick = {
                                        selectedEditor = BugEditor.fromInt(editor.value)
                                        editorsExpanded = false
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
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(4.dp))

                //Status und priority
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = stringResource(Res.string.bugstatus),
                    )
                    Box{
                        TextButton(onClick = { statusExpanded = true }) {
                            Text(text = selectedStatus.toString())
                        }

                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false },
                        ){
                            BugStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.toString()) },
                                    onClick = {
                                        selectedStatus = BugStatus.fromInt(status.value)
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }


                    Text(
                        text = stringResource(Res.string.bugpriority),
                    )
                    Box{
                        TextButton(onClick = { priorityExpanded = true }) {
                            Text(text = selectedPriority.toString())
                        }

                        DropdownMenu(
                            expanded = priorityExpanded,
                            onDismissRequest = { priorityExpanded = false },
                        ){
                            BugPriority.entries.forEach { priority ->
                                DropdownMenuItem(
                                    text = { Text(priority.toString()) },
                                    onClick = {
                                        selectedPriority = BugPriority.fromInt(priority.value)
                                        priorityExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }


        },
        confirmButton = {
            //TODO: Recreate todoentry with the new values from all fields

            val newtodo = TodoEntry(
                id = todoEntry.id,
                senderId = todoEntry.senderId,
                platform = selectedPlatform.value,
                type = selectedType.value,
                editorId = selectedEditor.value,
                content = content,
                title = title,
                lastChanged = todoEntry.lastChanged,
                senderAsString = todoEntry.senderAsString,
                status = selectedStatus.value,
                priority = selectedPriority.value
            )

            /*
            TextButton(onClick = onSave()) {
                Text(stringResource(Res.string.save))
            }

             */
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },


    )

}
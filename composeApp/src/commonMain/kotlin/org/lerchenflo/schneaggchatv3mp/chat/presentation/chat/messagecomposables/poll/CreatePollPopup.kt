package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.poll_create
import schneaggchatv3mp.composeapp.generated.resources.poll_create_description
import schneaggchatv3mp.composeapp.generated.resources.poll_create_description_placeholder
import schneaggchatv3mp.composeapp.generated.resources.poll_create_screentitle
import schneaggchatv3mp.composeapp.generated.resources.poll_create_title
import schneaggchatv3mp.composeapp.generated.resources.poll_create_title_placeholder
import schneaggchatv3mp.composeapp.generated.resources.poll_expiry_title
import schneaggchatv3mp.composeapp.generated.resources.poll_expiry_title_info
import schneaggchatv3mp.composeapp.generated.resources.poll_expiry_title_withdate
import schneaggchatv3mp.composeapp.generated.resources.poll_options_error
import schneaggchatv3mp.composeapp.generated.resources.poll_options_placeholder
import schneaggchatv3mp.composeapp.generated.resources.poll_options_title
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowcustom
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowcustom_info
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowcustom_withcount
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowmultiple
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowmultiple_info
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowmultiple_withcount
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_infinite_custom_and_selected_answers_warning
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_infinite_custom_answers_warning
import schneaggchatv3mp.composeapp.generated.resources.poll_visibility_title
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PollDialog(
    onDismiss: () -> Unit = {},
    onCreatePoll: (NetworkUtils.PollCreateRequest) -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var allowCustomAnswers by remember { mutableStateOf(false) }
    var allowedCustomAnswerCount by remember { mutableStateOf(1) }

    var allowMultipleAnswers by remember { mutableStateOf(false) }
    var allowedAnswerCount by remember { mutableStateOf(1) }

    var visibility by remember { mutableStateOf(PollVisibility.PUBLIC) }
    
    var expiresAt by remember { mutableStateOf<LocalDateTime?>(null) }
    var showExpiresAtDatePickerDialog by remember { mutableStateOf(false) }

    val options = remember {
        mutableStateListOf("", "")
    }

    //Errors
    var titleError by remember { mutableStateOf(false) }
    var optionsError by remember { mutableStateOf(false) }

    fun validateInputs() : Boolean {
        if (title.length < 2) {
            titleError = true
            return false
        }

        if (options.filter { it.isNotEmpty() }.size < 2) {
            optionsError = true
            return false
        }

        return true
    }

    //Reset errors with launchedeffect
    LaunchedEffect(title, options.toList(), options.size) {
        if (titleError) {
            if (title.length >= 2) {
                titleError = false
            }
        }

        if (optionsError) {
            if (options.filter { it.isNotEmpty() }.size >= 2) {
                optionsError = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)

        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {

                //Title to navigate back for ios users
                ActivityTitle(
                    title = stringResource(Res.string.poll_create_screentitle),
                    onBackClick = onDismiss
                )

                Spacer(modifier = Modifier.height(16.dp))

                //Title text input
                Text(stringResource(Res.string.poll_create_title))

                OutlinedTextField(
                    value = title,
                    maxLines = 1,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.poll_create_title_placeholder)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    isError = titleError
                )

                Spacer(modifier = Modifier.height(16.dp))


                //Description text input
                Text(stringResource(Res.string.poll_create_description))

                OutlinedTextField(
                    value = description,
                    maxLines = 1,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.poll_create_description_placeholder)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))


                //Poll options
                Text(text = stringResource(Res.string.poll_options_title))

                if (optionsError) {
                    Text(text = stringResource(Res.string.poll_options_error),
                        color = MaterialTheme.colorScheme.error)
                }

                options.forEachIndexed { index, value ->

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = value,
                        onValueChange = { options[index] = it },
                        placeholder = { Text(stringResource(Res.string.poll_options_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                //Auto add new entry if last entry is not empty
                if (options[options.size-1].isNotEmpty()) {
                    options.add("")
                }


                Spacer(modifier = Modifier.height(24.dp))


                //settings
                SettingsSwitch(
                    titletext = if (!allowMultipleAnswers) {
                        stringResource(Res.string.poll_settings_allowmultiple)
                    }
                    else if (allowedAnswerCount in 1..9) {
                        stringResource(
                            Res.string.poll_settings_allowmultiple_withcount, //TODO: Quantity string?? für 1 answer: https://developer.android.com/guide/topics/resources/string-resource
                            allowedAnswerCount.toString()
                        )
                    }
                    else {
                        stringResource(Res.string.poll_settings_allowmultiple_withcount, "∞")
                    },
                    infotext = stringResource(Res.string.poll_settings_allowmultiple_info),
                    switchchecked = allowMultipleAnswers,
                    onSwitchChange = { allowMultipleAnswers = it},
                    icon = null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (allowMultipleAnswers) {
                    Slider(
                        value = allowedAnswerCount.toFloat(),
                        onValueChange = {allowedAnswerCount = it.toInt()},
                        valueRange = 1f..10f,
                        steps = 9,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSwitch(
                    modifier = Modifier.fillMaxWidth(),
                    titletext = if (!allowCustomAnswers) {
                        stringResource(Res.string.poll_settings_allowcustom)
                    }
                    else if (allowedCustomAnswerCount in 1..9) {
                        stringResource(
                            Res.string.poll_settings_allowcustom_withcount,
                            allowedCustomAnswerCount.toString()
                        )
                    }
                    else {
                        stringResource(Res.string.poll_settings_allowcustom_withcount, "∞")
                    },
                    infotext = stringResource(Res.string.poll_settings_allowcustom_info),
                    switchchecked = allowCustomAnswers,
                    onSwitchChange = {
                        allowCustomAnswers = it
                        if (allowCustomAnswers) {
                            allowedCustomAnswerCount = 1
                        }
                                     },
                    icon = null
                )

                if (allowCustomAnswers) {
                    Slider(
                        value = allowedCustomAnswerCount.toFloat(),
                        onValueChange = {allowedCustomAnswerCount = it.toInt()},
                        valueRange = 1f..10f,
                        steps = 9,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                //Warning field if user selects both options
                if (allowMultipleAnswers && allowedCustomAnswerCount == 10 || allowedCustomAnswerCount == 10) {

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(
                                color = Color(red = 255, green = 165, blue = 0),
                                shape = RoundedCornerShape(15.dp)
                            )
                    ) {
                        Text(
                            text = if (allowMultipleAnswers) {
                                stringResource(Res.string.poll_settings_infinite_custom_and_selected_answers_warning)
                            } else stringResource(Res.string.poll_settings_infinite_custom_answers_warning),
                            textAlign = TextAlign.Center,
                        )
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))


                //Answer visibility
                var dropDownMenuShown by remember { mutableStateOf(false) }

                NormalButton(
                    text = stringResource(Res.string.poll_visibility_title, visibility.toUiText().asString()),
                    onClick = {
                        dropDownMenuShown = true
                    },
                    primary = false,
                    showOutline = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    DropdownMenu(
                        expanded = dropDownMenuShown,
                        onDismissRequest = {dropDownMenuShown = false},
                        modifier = Modifier.fillMaxWidth(),
                        scrollState = rememberScrollState(),
                    ) {
                        PollVisibility.entries.forEach { entry ->
                            DropdownMenuItem(
                                onClick = {
                                    visibility = entry
                                    dropDownMenuShown = false
                                },
                                text = {
                                    Text(
                                        text = entry.toUiText().asString()
                                    )
                                },
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))


                //Timer for poll expiration
                SettingsSwitch(
                    modifier = Modifier.fillMaxWidth(),
                    titletext = if (expiresAt == null) {stringResource(Res.string.poll_expiry_title)} else {
                        stringResource(Res.string.poll_expiry_title_withdate, "${expiresAt!!.day.toString().padStart(2, '0')}.${expiresAt!!.month.ordinal.toString().padStart(2, '0')}.${expiresAt!!.year} ${expiresAt!!.hour.toString().padStart(2, '0')}:${expiresAt!!.minute.toString().padStart(2, '0')}")
                    },
                    infotext = stringResource(Res.string.poll_expiry_title_info),
                    switchchecked = expiresAt != null,
                    onSwitchChange = {
                        if (expiresAt != null) {
                            expiresAt = null
                        } else {
                            showExpiresAtDatePickerDialog = true
                        }
                                     },
                    icon = null
                )
                //Popup for date picker
                if (showExpiresAtDatePickerDialog) {
                    AlertDialog(
                        onDismissRequest = { showExpiresAtDatePickerDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                // Handle confirm action
                                showExpiresAtDatePickerDialog = false
                            }) {
                                Text(stringResource(Res.string.ok))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showExpiresAtDatePickerDialog = false
                                expiresAt = null
                            }) {
                                Text(stringResource(Res.string.cancel))
                            }
                        },
                        text = {
                            WheelDateTimePicker(
                                modifier = Modifier.fillMaxWidth(),
                                rowCount = 3,
                                minDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                                textColor = MaterialTheme.colorScheme.onSurface,
                                selectorProperties = WheelPickerDefaults.selectorProperties(
                                    enabled = true,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ),
                                onSnappedDateTime = {
                                    expiresAt = it
                                }
                            )
                        }
                    )
                }


                Spacer(modifier = Modifier.height(16.dp))

                //Submit / Cancel row
                Row(
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    NormalButton(
                        onClick = { onDismiss() },
                        text = stringResource(Res.string.cancel),
                        primary = false,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NormalButton(
                        onClick = {
                            if (!validateInputs()) return@NormalButton

                            onCreatePoll(
                                NetworkUtils.PollCreateRequest(
                                    title = title,
                                    description = description,
                                    maxAnswers = if (allowMultipleAnswers) {
                                        if (allowedAnswerCount == 10) {
                                            null
                                        } else {
                                            allowedAnswerCount
                                        }
                                    } else 1,
                                    customAnswersEnabled = allowCustomAnswers,
                                    maxAllowedCustomAnswers = allowedCustomAnswerCount,
                                    visibility = visibility,
                                    closeDate = expiresAt?.toInstant(TimeZone.UTC)
                                        ?.toEpochMilliseconds(),
                                    voteOptions = options
                                        .filter { it.trim().isNotEmpty() }
                                        .map {
                                            NetworkUtils.PollVoteOptionCreateRequest(
                                                text = it
                                            )
                                        }
                                )
                            )

                            onDismiss()
                        },
                        text = stringResource(Res.string.poll_create),
                    )
                }

            }
        }
    }
}


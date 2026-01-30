package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
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
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.poll_create_screentitle
import schneaggchatv3mp.composeapp.generated.resources.poll_create_title
import schneaggchatv3mp.composeapp.generated.resources.poll_create_title_placeholder
import schneaggchatv3mp.composeapp.generated.resources.poll_options_placeholder
import schneaggchatv3mp.composeapp.generated.resources.poll_options_title
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowcustom
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowcustom_info
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowcustom_withcount
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowmultiple
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowmultiple_info
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_infinite_custom_and_selected_answers_warning
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_infinite_custom_answers_warning
import schneaggchatv3mp.composeapp.generated.resources.poll_visibility_title

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PollDialog(
    onDismiss: () -> Unit = {},
    onCreatePoll: (PollMessage) -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var allowCustomAnswers by remember { mutableStateOf(false) }
    var allowMultipleAnswers by remember { mutableStateOf(false) }
    var allowedCustomAnswerCount by remember { mutableStateOf(1) }
    var visibility by remember { mutableStateOf(PollVisibility.PUBLIC) }

    val options = remember {
        mutableStateListOf("")
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
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                //Poll options
                Text(text = stringResource(Res.string.poll_options_title))

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
                    titletext = stringResource(Res.string.poll_settings_allowmultiple),
                    infotext = stringResource(Res.string.poll_settings_allowmultiple_info),
                    switchchecked = allowMultipleAnswers,
                    onSwitchChange = { allowMultipleAnswers = it},
                    icon = null,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        valueRange = 2f..10f,
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

            }
        }
    }
}


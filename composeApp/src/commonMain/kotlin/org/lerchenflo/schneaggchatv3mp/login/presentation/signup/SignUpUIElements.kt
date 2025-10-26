@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.URL_PRIVACY
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.InputTextField
import org.lerchenflo.schneaggchatv3mp.sharedUi.NormalButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.accept_agb_pt1
import schneaggchatv3mp.composeapp.generated.resources.accept_agb_pt2
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.create_account
import schneaggchatv3mp.composeapp.generated.resources.create_account_subtitle
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.password
import schneaggchatv3mp.composeapp.generated.resources.password_again
import schneaggchatv3mp.composeapp.generated.resources.select_gebi_date
import schneaggchatv3mp.composeapp.generated.resources.username
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

//Signup element für Username und email
@Composable
fun SignUpForm1(
    usernameText: String,
    onusernameTextChange: (String) -> Unit,
    usernameerrorText: String?,
    emailText: String,
    onemailTextChange: (String) -> Unit,
    emailerrorText: String?,
    ongebidateselected: (LocalDate?) -> Unit,
    selectedgebidate: LocalDate?,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier
    ) {
        SignUpHeaderText(
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        InputTextField(
            text = usernameText,
            onValueChange = onusernameTextChange,
            label = stringResource(Res.string.username),
            hint = stringResource(Res.string.username),
            isInputSecret = false,
            errortext = usernameerrorText,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        InputTextField(
            text = emailText,
            onValueChange = onemailTextChange,
            label = stringResource(Res.string.email),
            hint = stringResource(Res.string.email),
            isInputSecret = false,
            errortext = emailerrorText,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))


        // Datepicker
        var showDatePicker by remember { mutableStateOf(false) }

        NormalButton(
            text = selectedgebidate?.toString() ?: stringResource(Res.string.select_gebi_date),
            onClick = {
                showDatePicker = true
            },
            modifier = Modifier
                .fillMaxWidth(),
            primary = false
        )

        if (showDatePicker) {
            DatePickerDialogPopup(
                onDateSelected = { selectedDate ->
                    ongebidateselected(selectedDate)
                },
                onDismiss = { showDatePicker = false }
            )
        }

    }
}

@Composable
fun SignUpForm2(
    passwordText: String,
    onpasswordTextChange: (String) -> Unit,
    passworderrorText: String?,
    password2Text: String,
    onpassword2TextChange: (String) -> Unit,
    password2errorText: String?,
    onSignupButtonClick: () -> Unit,
    signupbuttondisabled: Boolean = false,
    signupbuttonloading: Boolean = false,
    onCheckBoxCheckedChange: (Boolean) -> Unit,
    checkboxChecked: Boolean,
    
    
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier
    ) {
        InputTextField(
            text = passwordText,
            onValueChange = onpasswordTextChange,
            label = stringResource(Res.string.password),
            hint = stringResource(Res.string.password),
            isInputSecret = true,
            errortext = passworderrorText,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        InputTextField(
            text = password2Text,
            onValueChange = onpassword2TextChange,
            label = stringResource(Res.string.password_again),
            hint = stringResource(Res.string.password),
            isInputSecret = true,
            errortext = password2errorText,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checkboxChecked,
                onCheckedChange = {onCheckBoxCheckedChange(it)},
            )

            Spacer(modifier = Modifier.width(2.dp))

            val urihandler = LocalUriHandler.current
            val text1 = stringResource(Res.string.accept_agb_pt1)
            val text2 = stringResource(Res.string.accept_agb_pt2)

            Row{
                Text(
                    text = text1
                )
                Text(
                    text = text2,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable{
                            urihandler.openUri(URL_PRIVACY)
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NormalButton(
            text = stringResource(Res.string.create_account),
            onClick = onSignupButtonClick,
            disabled = signupbuttondisabled,
            isLoading = signupbuttonloading,
            modifier = Modifier
                .fillMaxWidth()
        )


    }
}


@Composable
fun SignUpHeaderText(
    alignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        Text(
            text = stringResource(Res.string.create_account),
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = stringResource(Res.string.create_account_subtitle),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogPopup(
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    // 3. Convert the selected milliseconds to LocalDate
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
                        onDateSelected(localDate)
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        confirmButton = {


            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }

        },
    ) {
        // 4. Place the DatePicker inside the dialog
        DatePicker(state = datePickerState)
    }
}
@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
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
import schneaggchatv3mp.composeapp.generated.resources.create_account
import schneaggchatv3mp.composeapp.generated.resources.create_account_subtitle
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.password
import schneaggchatv3mp.composeapp.generated.resources.password_again
import schneaggchatv3mp.composeapp.generated.resources.username
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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

        //TODO: Ned immer zoaga
        //TODO: Gender als string printen zum luaga obs format passt
        DateDropdownPicker(
            onDateSelected = { ongebidateselected(it) },
            modifier = Modifier.fillMaxWidth()
        )

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
fun DateDropdownPicker(
    initialDate: LocalDate? = null,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    minYear: Int = 1900,
    maxYear: Int = run {
        // default to current system year
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        now.date.year
    },
    monthNames: List<String> = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ),
    labelDay: String = "Day",
    labelMonth: String = "Month",
    labelYear: String = "Year",
) {
    // Initialize selections from initialDate (or null)
    var selectedDay by remember { mutableStateOf(initialDate?.dayOfMonth) }
    var selectedMonth by remember { mutableStateOf(initialDate?.monthNumber) } // 1..12
    var selectedYear by remember { mutableStateOf(initialDate?.year) }

    // Dropdown expanded states
    var dayExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    val daysList = (1..31)
    val yearsList = remember(minYear, maxYear) {
        (minYear..maxYear).toList().reversed()
    }

    Row(modifier = modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // DAY dropdown
        ExposedDropdownMenuBox(
            expanded = dayExpanded,
            onExpandedChange = { dayExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedDay?.toString() ?: "",
                onValueChange = {},
                label = { Text(labelDay) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded)
                }
            )

            ExposedDropdownMenu(
                expanded = dayExpanded,
                onDismissRequest = { dayExpanded = false }
            ) {
                daysList.forEach { d ->
                    DropdownMenuItem(
                        text = { Text(d.toString()) },
                        onClick = {
                            selectedDay = d
                            dayExpanded = false
                        }
                    )
                }
            }
        }

        // MONTH dropdown
        ExposedDropdownMenuBox(
            expanded = monthExpanded,
            onExpandedChange = { monthExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedMonth?.let { monthNames.getOrNull(it - 1) } ?: "",
                onValueChange = {},
                label = { Text(labelMonth) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) }
            )
            ExposedDropdownMenu(
                expanded = monthExpanded,
                onDismissRequest = { monthExpanded = false },
                modifier = Modifier
                    .wrapContentWidth()
                    .heightIn(max = 300.dp)
            ) {
                monthNames.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedMonth = index + 1
                            monthExpanded = false
                        }
                    )
                }
            }
        }

        // YEAR dropdown
        ExposedDropdownMenuBox(
            expanded = yearExpanded,
            onExpandedChange = { yearExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedYear?.toString() ?: "",
                onValueChange = {},
                label = { Text(labelYear) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) }
            )
            ExposedDropdownMenu(
                expanded = yearExpanded,
                onDismissRequest = { yearExpanded = false },
                modifier = Modifier
                    .wrapContentWidth()
                    .heightIn(max = 300.dp)
            ) {
                yearsList.forEach { y ->
                    DropdownMenuItem(
                        text = { Text(y.toString()) },
                        onClick = {
                            selectedYear = y
                            yearExpanded = false
                        }
                    )
                }
            }
        }
    }
}
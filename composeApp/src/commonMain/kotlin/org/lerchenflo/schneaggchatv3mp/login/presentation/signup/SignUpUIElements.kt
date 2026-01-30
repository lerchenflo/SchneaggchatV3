@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.URL_PRIVACY
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.InputTextField
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.accept_agb_pt1
import schneaggchatv3mp.composeapp.generated.resources.accept_agb_pt2
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.create_account
import schneaggchatv3mp.composeapp.generated.resources.create_account_subtitle
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.password
import schneaggchatv3mp.composeapp.generated.resources.password_again
import schneaggchatv3mp.composeapp.generated.resources.profile_picture
import schneaggchatv3mp.composeapp.generated.resources.select_gebi_date
import schneaggchatv3mp.composeapp.generated.resources.select_profile_pic
import schneaggchatv3mp.composeapp.generated.resources.tooltip_birthdate
import schneaggchatv3mp.composeapp.generated.resources.tooltip_email
import schneaggchatv3mp.composeapp.generated.resources.tooltip_password
import schneaggchatv3mp.composeapp.generated.resources.tooltip_password_repeat
import schneaggchatv3mp.composeapp.generated.resources.tooltip_profile_picture
import schneaggchatv3mp.composeapp.generated.resources.tooltip_terms
import schneaggchatv3mp.composeapp.generated.resources.tooltip_username
import schneaggchatv3mp.composeapp.generated.resources.username
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

//Signup element für Username und email
@OptIn(ExperimentalMaterial3Api::class)
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
    gebiErrorText: String?,
    selectedProfilePic: ByteArray?,
    profilePicErrorText: String?,
    onProfilePicClick: () -> Unit,
    onBackClick: () -> Unit,
    focus: SignupFocusRequesters,
    modifier: Modifier = Modifier
){

    ActivityTitle(
        title = stringResource(Res.string.create_account),
        onBackClick = {
            onBackClick()
        })

    Column(
        modifier = modifier
    ) {


        /* // Der isch mir zu stark random in da gegend -fabi
        SignUpHeaderText(
            modifier = Modifier
                .fillMaxWidth(),
            onBackClick = onBackClick
        )

         */

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clickable { onProfilePicClick() }
            ) {
                Image(
                    painter = if (selectedProfilePic != null) BitmapPainter(selectedProfilePic.decodeToImageBitmap()) else painterResource(Res.drawable.icon_nutzer),
                    contentDescription = stringResource(Res.string.profile_picture),
                    modifier = Modifier.fillMaxSize()
                        .clip(CircleShape)
                )
                
                // Overlay icon to indicate "Edit/Add"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.select_profile_pic),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(4.dp))
                val profilePicTooltipState = rememberTooltipState()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(stringResource(Res.string.tooltip_profile_picture)) } },
                    state = profilePicTooltipState
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            if (profilePicErrorText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                println("Profilepic missing text shown")
                Text(
                    text = profilePicErrorText,
                    color = Color.Red,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        InputTextField(
            text = usernameText,
            onValueChange = onusernameTextChange,
            label = stringResource(Res.string.username),
            hint = stringResource(Res.string.username),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            errortext = usernameerrorText,
            focusRequester = focus.username,
            nextFocusRequester = focus.email,
            tooltip = stringResource(Res.string.tooltip_username),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        InputTextField(
            text = emailText,
            onValueChange = onemailTextChange,
            label = stringResource(Res.string.email),
            hint = stringResource(Res.string.email),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Email,
            errortext = emailerrorText,
            focusRequester = focus.email,
            nextFocusRequester = focus.date,
            tooltip = stringResource(Res.string.tooltip_email),
            modifier = Modifier.fillMaxWidth()
        )



        //Spacer(modifier = Modifier.width(8.dp))





       Spacer(modifier = Modifier.height(16.dp))


        // Datepicker
        var showDatePicker by remember { mutableStateOf(false) }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Button(
                onClick = {
                    showDatePicker = true //todo server backend fehlt
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus.date)
                    .onPreviewKeyEvent { event ->
                        // Detect TAB key press
                        if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                            focus.password.requestFocus()
                            true // we handled it
                        } else false
                    }
            ){
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(12.dp))
                if(selectedgebidate != null){
                    Text(
                        text = "${selectedgebidate.day.toString().padStart(2, '0')}.${selectedgebidate.month.number.toString().padStart(2, '0')}.${selectedgebidate.year}"
                    )
                }else{
                    Text(
                        text = stringResource(Res.string.select_gebi_date)
                    )
                }

            }
            Spacer(modifier = Modifier.width(8.dp))
            val dateTooltipState = rememberTooltipState()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(stringResource(Res.string.tooltip_birthdate)) } },
                state = dateTooltipState
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        if (gebiErrorText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = gebiErrorText,
                color = Color.Red,
            )
        }


        if (showDatePicker) {
            BirthdatePickerPopup(
                onDateSelected = { selectedDate ->
                    ongebidateselected(selectedDate)
                },
                onDismiss = { showDatePicker = false}
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    agbErrorText: String?,
    focus: SignupFocusRequesters,
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
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Password,
            errortext = passworderrorText,
            focusRequester = focus.password,
            nextFocusRequester = focus.password2,
            tooltip = stringResource(Res.string.tooltip_password),
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        InputTextField(
            text = password2Text,
            onValueChange = onpassword2TextChange,
            label = stringResource(Res.string.password_again),
            hint = stringResource(Res.string.password),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Password,
            errortext = password2errorText,
            focusRequester = focus.password2,
            nextFocusRequester = focus.terms,
            tooltip = stringResource(Res.string.tooltip_password_repeat),
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
                modifier = Modifier
                    .focusRequester(focus.terms)
                    .onPreviewKeyEvent { event ->
                        // Detect TAB key press
                        if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                            focus.signup.requestFocus()
                            true
                        } else false
                    }
            )

            Spacer(modifier = Modifier.width(2.dp))

            val urihandler = LocalUriHandler.current
            val text1 = stringResource(Res.string.accept_agb_pt1)
            val text2 = stringResource(Res.string.accept_agb_pt2)

            val annotatedString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    append(text1)
                }

                pushStringAnnotation(
                    tag = "URL",
                    annotation = URL_PRIVACY
                )
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(text2)
                }
                pop()
            }

            ClickableText(
                text = annotatedString,
                onClick = { offset ->
                    annotatedString.getStringAnnotations(
                        tag = "URL",
                        start = offset,
                        end = offset
                    ).firstOrNull()?.let { annotation ->
                        urihandler.openUri(annotation.item)
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            val termsTooltipState = rememberTooltipState()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(stringResource(Res.string.tooltip_terms)) } },
                state = termsTooltipState
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }

        if (agbErrorText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = agbErrorText,
                color = Color.Red,
                modifier = Modifier.fillMaxWidth()
            )
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
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
){
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        ActivityTitle(
            title = stringResource(Res.string.create_account),
            onBackClick = {
                onBackClick()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.create_account_subtitle),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun BirthdatePickerPopup(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    defaultDate: LocalDate? = null

) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date // TimeZone.currentSystemDefault() künnt fehler uf Iphone werfa (crash)
    val endOfCurrentYear = LocalDate(year = today.year, monthNumber = 12, dayOfMonth = 31)
    val _defaultDate = defaultDate ?: today.minus(18, DateTimeUnit.YEAR)

    var selectedDate by remember {
        mutableStateOf(_defaultDate)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(selectedDate)
                onDismiss()
            }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        text = {
            // In this library, the parameter is 'onDatelineChanged' or 'onValueChange'
            // and the styling is handled via specific parameters, not a single 'style' object.
            WheelDatePicker(
                modifier = Modifier.fillMaxWidth(),
                startDate = selectedDate,
                minDate = LocalDate(1900, 1, 1),
                maxDate = endOfCurrentYear,
                rowCount = 5,
                textColor = MaterialTheme.colorScheme.onSurface,
                selectorProperties = WheelPickerDefaults.selectorProperties(
                    enabled = true,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ),
                onSnappedDate = { snappedDate: LocalDate ->
                    selectedDate = snappedDate
                }
            )
        }
    )
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    MaterialTheme {

        val focus = SignupFocusRequesters(
            profilePic = remember { FocusRequester() },
            username = remember { FocusRequester() },
            email = remember { FocusRequester() },
            date = remember { FocusRequester() },
            password = remember { FocusRequester() },
            password2 = remember { FocusRequester() },
            terms = remember { FocusRequester() },
            signup = remember { FocusRequester() },
        )

        SignUpForm1(
            usernameText = "",
            onusernameTextChange = {},
            usernameerrorText = "",
            emailText = "",
            onemailTextChange = {},
            emailerrorText = "",
            ongebidateselected = {},
            onProfilePicClick = {},
            onBackClick = {},
            focus = focus,
            selectedgebidate = null,
            gebiErrorText = null,
            selectedProfilePic = null,
            profilePicErrorText = null

            )
    }
}
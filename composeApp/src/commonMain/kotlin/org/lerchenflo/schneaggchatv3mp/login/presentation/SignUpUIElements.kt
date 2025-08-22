package org.lerchenflo.schneaggchatv3mp.login.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.NormalButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.create_account
import schneaggchatv3mp.composeapp.generated.resources.create_account_subtitle
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.password
import schneaggchatv3mp.composeapp.generated.resources.password_again
import schneaggchatv3mp.composeapp.generated.resources.username

//Signup element für Username und email
@Composable
fun SignUpForm1(
    usernameText: String,
    onusernameTextChange: (String) -> Unit,
    usernameerrorText: String?,
    emailText: String,
    onemailTextChange: (String) -> Unit,
    emailerrorText: String?,
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
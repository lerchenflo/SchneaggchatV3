package org.lerchenflo.schneaggchatv3mp.login.presentation.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.NormalButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.login
import schneaggchatv3mp.composeapp.generated.resources.loginsubtitle
import schneaggchatv3mp.composeapp.generated.resources.password
import schneaggchatv3mp.composeapp.generated.resources.sign_up
import schneaggchatv3mp.composeapp.generated.resources.username

@Composable
fun InputTextField(
    text: String,
    onValueChange: (String) -> Unit = {},
    label: String,
    hint: String,
    errortext: String? = null,
    imeAction: ImeAction = ImeAction.Default,
    keyboardType: KeyboardType = KeyboardType.Unspecified,
    onDoneClick: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    var isPasswordVisible by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
        )
        Spacer(modifier = Modifier.height(6.dp))

        // build the field modifier conditionally (only attach focusRequester if provided)
        var fieldModifier = Modifier.fillMaxWidth()
        if (focusRequester != null) {
            fieldModifier = fieldModifier.focusRequester(focusRequester)
        }
        fieldModifier = fieldModifier.onPreviewKeyEvent { keyEvent ->
            // desktop Tab handling
            if (keyEvent.type == KeyEventType.KeyDown) {
                when (keyEvent.key) {
                    Key.Tab -> {
                        nextFocusRequester?.requestFocus()
                        true
                    }
                    Key.Enter -> {
                        if(imeAction == ImeAction.Next){
                            nextFocusRequester?.requestFocus()
                            true
                        }else if(imeAction == ImeAction.Done){
                            onDoneClick()
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            } else {
                false
            }
        }

        OutlinedTextField(
            modifier = fieldModifier,
            value = text,
            onValueChange = onValueChange,
            visualTransformation = if (keyboardType == KeyboardType.Password){
                if (!isPasswordVisible){
                    PasswordVisualTransformation(mask = '*')
                }else VisualTransformation.None
            }else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            placeholder = {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = imeAction,
                keyboardType = keyboardType,
                capitalization = if(keyboardType == KeyboardType.Email) KeyboardCapitalization.None else KeyboardCapitalization.Unspecified,
                autoCorrectEnabled = keyboardType != KeyboardType.Email

            ),
            keyboardActions = KeyboardActions(
                onNext = { nextFocusRequester?.requestFocus() },
                onDone = {onDoneClick()}
            ),
            trailingIcon = {
                if (keyboardType == KeyboardType.Password){
                    IconButton(
                        onClick = {
                            isPasswordVisible = !isPasswordVisible
                        },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ){
                        when {
                            isPasswordVisible -> {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hide password"
                                )
                            }
                            !isPasswordVisible -> {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Show password"
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    if (errortext != null){
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = errortext,
            color = Color.Red
        )
    }
}



@Composable
fun ClickableLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
){
    Text(
        text = text,
        modifier = modifier
            .clickable(onClick = onClick),
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center
    )
}

@Composable
fun LoginHeaderText(
    alignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        Text(
            text = stringResource(Res.string.login),
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = stringResource(Res.string.loginsubtitle),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}



@Composable
fun LoginFormSection(
    usernameText: String,
    onusernameTextChange: (String) -> Unit,
    passwordText: String,
    onPasswordTextChange: (String) -> Unit,
    onLoginButtonClick: () -> Unit,
    onSignupButtonClick: () -> Unit,
    passwordTextError: String? = null,
    loginbuttondisabled: Boolean = false,
    loginbuttonloading: Boolean = false,
    usernameFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    loginFocusRequester: FocusRequester,

    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier
    ) {
        InputTextField(
            text = usernameText,
            onValueChange = onusernameTextChange,
            label = stringResource(Res.string.username),
            hint = stringResource(Res.string.username),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            errortext = null,
            focusRequester = usernameFocusRequester,
            nextFocusRequester = passwordFocusRequester,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        InputTextField(
            text = passwordText,
            onValueChange = onPasswordTextChange,
            label = stringResource(Res.string.password),
            hint = stringResource(Res.string.password),
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Password,
            errortext = passwordTextError,
            focusRequester = passwordFocusRequester,
            nextFocusRequester = loginFocusRequester,
            onDoneClick = onLoginButtonClick, // bei done drucka direkt login
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        NormalButton(
            text = stringResource(Res.string.login),
            onClick = onLoginButtonClick,
            disabled = loginbuttondisabled,
            isLoading = loginbuttonloading,
            focusRequester = loginFocusRequester,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))


        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically

        ) {

            Spacer(
                modifier = Modifier
                    .weight(0.5f)
            )
            Text(
                text = stringResource(Res.string.sign_up),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable {
                        onSignupButtonClick()
                    }
                    .padding(4.dp),
                textAlign = TextAlign.Center
            )
            Spacer(
                modifier = Modifier
                    .weight(0.5f)
            )
        }

    }
}
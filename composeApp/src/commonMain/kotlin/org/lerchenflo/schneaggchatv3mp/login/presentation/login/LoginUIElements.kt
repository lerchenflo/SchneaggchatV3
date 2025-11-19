package org.lerchenflo.schneaggchatv3mp.login.presentation.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.settings.presentation.UrlChangeDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.NormalButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.login
import schneaggchatv3mp.composeapp.generated.resources.loginsubtitle
import schneaggchatv3mp.composeapp.generated.resources.password
import schneaggchatv3mp.composeapp.generated.resources.sign_up
import schneaggchatv3mp.composeapp.generated.resources.username

// todo keyboard options für signup optimiera
@Composable
fun InputTextField(
    text: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    errortext: String? = null,
    isInputSecret: Boolean,
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
            if ((keyEvent.key == Key.Tab || keyEvent.key == Key.Enter) && keyEvent.type == KeyEventType.KeyDown) {
                nextFocusRequester?.requestFocus()
                true
            } else {
                false
            }
        }

        OutlinedTextField(
            modifier = fieldModifier,
            value = text,
            onValueChange = onValueChange,
            visualTransformation = if (isInputSecret){
                if (!isPasswordVisible){
                    PasswordVisualTransformation(mask = '*')
                }else VisualTransformation.None
            }else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
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
                imeAction = if (isInputSecret) ImeAction.Done else ImeAction.Next,
                keyboardType = if (isInputSecret) KeyboardType.Password else KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(
                onNext = { nextFocusRequester?.requestFocus() },
                onDone = { /* you may want to trigger login here, or clear focus */ }
            ),
            trailingIcon = {
                if (isInputSecret){
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


@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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
            isInputSecret = false,
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
            isInputSecret = true,
            errortext = passwordTextError,
            focusRequester = passwordFocusRequester,
            nextFocusRequester = loginFocusRequester,
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

        var showUrlChangeDialog by remember {mutableStateOf(false)}

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically

        ) {
            val iconSize = 48.dp
            IconButton(
                onClick = {
                    showUrlChangeDialog = true
                }
            ){
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "change serverurl", // todo strings
                    modifier = Modifier.size(iconSize)
                )
            }
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
            // Empty space to balance the icon on the left
            Spacer(modifier = Modifier.size(iconSize)) // same width as IconButton
        }

        if(showUrlChangeDialog){
            UrlChangeDialog(
                onDismiss = {showUrlChangeDialog = false},
                onConfirm = {showUrlChangeDialog = false}
            )
        }

    }
}
package org.lerchenflo.schneaggchatv3mp.login.presentation.emailverifiedcheck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.ChangeDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.isEmailValid
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change
import schneaggchatv3mp.composeapp.generated.resources.change_email
import schneaggchatv3mp.composeapp.generated.resources.email_check_problem
import schneaggchatv3mp.composeapp.generated.resources.email_check_verified
import schneaggchatv3mp.composeapp.generated.resources.email_not_verified_email_has_been_sent1
import schneaggchatv3mp.composeapp.generated.resources.email_not_verified_email_has_been_sent2
import schneaggchatv3mp.composeapp.generated.resources.email_not_verified_screen_title
import schneaggchatv3mp.composeapp.generated.resources.invalid_email
import schneaggchatv3mp.composeapp.generated.resources.logout
import schneaggchatv3mp.composeapp.generated.resources.resend_verification

@Composable
fun EmailVerifiedCheckScreenRoot() {
    val viewModel: EmailVerifiedCheckViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    EmailNotVerifiedScreen(
        state = state,
        onAction = viewModel::onAction
    )
}


@Composable
fun EmailNotVerifiedScreen(
    state: EmailVerifiedCheckState,
    onAction: (EmailVerifiedCheckAction) -> Unit
) {

    val smallSpacer = 12.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 24.dp
            ),
    ) {

        //Top Email icon
        Icon(
            imageVector = Icons.Outlined.Email,
            contentDescription = "Email icon",
            modifier = Modifier
                .size(120.dp)
        )


        //Title
        Text(
            text = stringResource(Res.string.email_not_verified_screen_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        //Body text
        val str1 = stringResource(Res.string.email_not_verified_email_has_been_sent1)
        val str2 = stringResource(Res.string.email_not_verified_email_has_been_sent2)
        val annotatedString = buildAnnotatedString {
            append(str1)
            withStyle(
                style = SpanStyle(
                    //color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(state.currentEmail)
            }
            append(str2)
        }
        Text(
            text = annotatedString
        )

        Spacer(modifier = Modifier.height(smallSpacer))

        HorizontalDivider(modifier = Modifier.height(4.dp))

        Spacer(modifier = Modifier.height(smallSpacer))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            //Retry login button (Unused if user update push works)
            NormalButton(
                text = stringResource(Res.string.email_check_verified),
                onClick = {
                    onAction(EmailVerifiedCheckAction.OnCheckVerificationClick)
                },
                isLoading = state.isLoading,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                },
            )


            //Resend email
            NormalButton(
                onClick = {
                    onAction(EmailVerifiedCheckAction.OnResendEmailClick)
                },
                primary = false,
                text = stringResource(Res.string.resend_verification),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                },
            )

            Spacer(modifier = Modifier.height(smallSpacer))

            HorizontalDivider(modifier = Modifier.height(4.dp))

            Spacer(modifier = Modifier.height(smallSpacer))

            //Change email
            NormalButton(
                onClick = {
                    onAction(EmailVerifiedCheckAction.OnChangeEmailStart)
                },
                text = stringResource(Res.string.change_email),
                primary = false,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                },
            )

        }


        if (state.showChangeEmailPopup) {
            val invalidEmailString = stringResource(Res.string.invalid_email)
            ChangeDialog(
                title = stringResource(Res.string.change_email),
                initialValue = state.userData?.email ?: state.currentEmail,
                onDismiss = { onAction(EmailVerifiedCheckAction.OnChangeEmailDismiss) },
                onConfirm = {
                    onAction(EmailVerifiedCheckAction.OnChangeEmailText(it))
                },
                keyboardType = KeyboardType.Email,
                confirmButtonText = stringResource(Res.string.change),
                validator = { newValue ->
                    if (!isEmailValid(newValue)) {
                        invalidEmailString
                    } else null
                }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            NormalButton(
                onClick = {
                    onAction(EmailVerifiedCheckAction.OnLogoutClick)
                },
                text = stringResource(Res.string.logout),
                destructive = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                },
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(15.dp)
                    )
                    .padding(4.dp)
            ){
                Text(
                    text = stringResource(Res.string.email_check_problem),
                    modifier = Modifier.clickable{
                        onAction(EmailVerifiedCheckAction.OnRequestSupportClick)
                    }
                )
            }
        }

    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    apiLevel = 36
)
@Composable
private fun EmailnotverifiedPreview() {
    SchneaggchatTheme {
        EmailNotVerifiedScreen(
            state = EmailVerifiedCheckState(
                currentEmail = "Defaultemail@gmail.com"
            ),
            onAction = {  }
        )
    }
}
package org.lerchenflo.schneaggchatv3mp.login.presentation.emailverifiedcheck

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.ChangeDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.emailProviderWarning
import org.lerchenflo.schneaggchatv3mp.sharedUi.loading.RoundLoadingIndicator
import org.lerchenflo.schneaggchatv3mp.utilities.isEmailValid
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change
import schneaggchatv3mp.composeapp.generated.resources.change_email
import schneaggchatv3mp.composeapp.generated.resources.email_check_loading_data
import schneaggchatv3mp.composeapp.generated.resources.email_check_problem
import schneaggchatv3mp.composeapp.generated.resources.email_check_verified
import schneaggchatv3mp.composeapp.generated.resources.email_not_verified_email_has_been_sent1
import schneaggchatv3mp.composeapp.generated.resources.email_not_verified_email_has_been_sent2
import schneaggchatv3mp.composeapp.generated.resources.email_not_verified_screen_title
import schneaggchatv3mp.composeapp.generated.resources.email_provider_warning
import schneaggchatv3mp.composeapp.generated.resources.invalid_email
import schneaggchatv3mp.composeapp.generated.resources.logout
import schneaggchatv3mp.composeapp.generated.resources.resend_verification

@Composable
fun EmailVerifiedCheckScreenRoot() {
    val viewModel: EmailVerifiedCheckViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.userData == null || (state.userData != null && state.userData!!.emailVerifiedAt != null)) {
        EmailCheckLoadingIndicator()
    } else{
        EmailNotVerifiedScreen(
            state = state,
            onAction = viewModel::onAction
        )
    }
}

@Composable
fun EmailCheckLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RoundLoadingIndicator(
                visible = true,
                onClick = {},
                size = 50.dp,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.email_check_loading_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 24.dp,
                vertical = 24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        //Top Email icon in tinted circle
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        //Title
        Text(
            text = stringResource(Res.string.email_not_verified_screen_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(smallSpacer))

        //Body text
        val str1 = stringResource(Res.string.email_not_verified_email_has_been_sent1)
        val str2 = stringResource(Res.string.email_not_verified_email_has_been_sent2)
        val annotatedString = buildAnnotatedString {
            append(str1)
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(state.currentEmail)
            }
            append(str2)
        }
        Text(
            text = annotatedString,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            //Check verification - primary action, confirms the user has verified
            NormalButton(
                onClick = {
                    onAction(EmailVerifiedCheckAction.OnCheckVerificationClick)
                },
                isLoading = state.isLoading,
                text = stringResource(Res.string.email_check_verified),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                },
            )

            Spacer(modifier = Modifier.height(smallSpacer))

            //Resend email - fallback when nothing arrived
            NormalButton(
                onClick = {
                    onAction(EmailVerifiedCheckAction.OnResendEmailClick)
                },
                primary = false,
                disabled = !state.canResendEmail,
                isLoading = state.isResendingEmail,
                text = stringResource(Res.string.resend_verification),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                },
            )

            Spacer(modifier = Modifier.height(4.dp))

            //Change email - rare correction, muted so it doesn't compete with Resend
            TextButton(
                onClick = {
                    onAction(EmailVerifiedCheckAction.OnChangeEmailStart)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(Res.string.change_email),
                    style = MaterialTheme.typography.bodySmall
                )
            }

        }

        Spacer(modifier = Modifier.height(32.dp))

        if (state.showChangeEmailPopup) {
            val invalidEmailString = stringResource(Res.string.invalid_email)
            val providerWarningString = stringResource(Res.string.email_provider_warning)
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
                    if (!isEmailValid(newValue)) invalidEmailString else null
                },
                warningValidator = { newValue ->
                    emailProviderWarning(newValue, providerWarningString)
                }
            )
        }

        NormalButton(
            onClick = {
                onAction(EmailVerifiedCheckAction.OnLogoutClick)
            },
            text = stringResource(Res.string.logout),
            destructive = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))
            },
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
            onClick = {
                onAction(EmailVerifiedCheckAction.OnRequestSupportClick)
            }
        ) {
            Icon(
                imageVector = Icons.Filled.HelpOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(text = stringResource(Res.string.email_check_problem))
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
                currentEmail = "Defaultemail@gmail.com",
                canResendEmail = true
            ),
            onAction = {  }
        )
    }
}
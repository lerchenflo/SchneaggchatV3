package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.InputTextField
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.email_provider_warning

private val ALLOWED_DOMAINS = listOf(
    "gmail.com",
    "yahoo.com", "ymail.com",
    "outlook.com", "hotmail.com", "live.com", "msn.com",
    "icloud.com", "me.com", "mac.com",
    "gmx.com", "gmx.net", "gmx.de", "gmx.at", "gmx.ch"
)

fun emailProviderWarning(email: String, warningMessage: String): String? {
    val atIndex = email.indexOf('@')
    if (atIndex == -1) return null
    val domain = email.substring(atIndex + 1).lowercase()
    val isKnown = ALLOWED_DOMAINS.any { it.startsWith(domain) }
    return if (isKnown) null else warningMessage
}

@Composable
fun EmailInputField(
    text: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    errortext: String? = null,
    tooltip: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    modifier: Modifier = Modifier
) {
    val warningMessage = stringResource(Res.string.email_provider_warning)
    val warning = emailProviderWarning(text, warningMessage)

    Column(modifier = modifier) {
        InputTextField(
            value = text,
            onValueChange = onValueChange,
            label = label,
            hint = hint,
            errortext = errortext,
            tooltip = tooltip,
            imeAction = imeAction,
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )
        if (warning != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = warning,
                color = Color(0xFFF57C00),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

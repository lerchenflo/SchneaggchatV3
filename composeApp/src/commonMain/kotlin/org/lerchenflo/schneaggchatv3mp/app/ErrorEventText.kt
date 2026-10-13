package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.errorCodeToMessage
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_code_format

/** Text of the global error popup: error code line, then every message the event carries. */
@Composable
fun ErrorEvent.toStringComposable(): String = buildString {
    val code = errorCode ?: error?.errorCode
    if (code != null) {
        appendLine(stringResource(Res.string.error_code_format, code.toString(), errorCodeToMessage(code)))
    }

    error?.message?.let { appendLine(it) }
    errorMessage?.let { appendLine(it) }
    errorMessageUiText?.let { appendLine(it.asString()) }
}

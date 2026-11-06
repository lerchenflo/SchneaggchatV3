package org.lerchenflo.schneaggchatv3mp.login.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_invalid_credentials
import schneaggchatv3mp.composeapp.generated.resources.error_login_server_error
import schneaggchatv3mp.composeapp.generated.resources.error_login_too_many_requests
import schneaggchatv3mp.composeapp.generated.resources.offline

/** Only wrong credentials may read as the user's fault. */
class LoginErrorEventsTest {

    private fun DataError.loginText() = (toLoginErrorEvent().errorMessageUiText as UiText.StringResourceText).resId

    @Test
    fun `a rejected login reports invalid credentials`() {
        assertThat(DataError.Unauthorized().loginText()).isEqualTo(Res.string.error_invalid_credentials)
    }

    @Test
    fun `a rate limit is not reported as wrong credentials`() {
        assertThat(DataError.TooManyRequests().loginText()).isEqualTo(Res.string.error_login_too_many_requests)
    }

    @Test
    fun `being offline is not reported as wrong credentials`() {
        assertThat(DataError.NoInternetConnection.loginText()).isEqualTo(Res.string.offline)
        assertThat(DataError.NetworkTimeout().loginText()).isEqualTo(Res.string.offline)
    }

    @Test
    fun `a server error is not reported as wrong credentials`() {
        assertThat(DataError.ServerError(errorCode = 503).loginText()).isEqualTo(Res.string.error_login_server_error)
    }
}

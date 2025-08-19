package org.lerchenflo.schneaggchatv3mp.login.Domain

class LoginUseCase {
    operator fun invoke(username: String, password: String): Boolean {
        // todo Actual authentication logic would go here
        return username.isNotBlank() && password.isNotBlank()
    }
}
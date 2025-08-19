import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import org.lerchenflo.schneaggchatv3mp.login.Domain.LoginUseCase

class LoginViewModel(
    private val loginUseCase: LoginUseCase = LoginUseCase()
) : ViewModel() {
    // TextField states
    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    // UI state
    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Update username state
    fun updateUsername(newValue: String) {
        username = newValue
        clearError() // Clear error when user types
    }

    // Update password state
    fun updatePassword(newValue: String) {
        password = newValue
        clearError()
    }

    // Handle login logic
    suspend fun login(onSuccess: () -> Unit) {
        if (validateInput()) {
            try {
                isLoading = true
                // Simulate network call
                delay(1000)

                // todo implement login logic
                // Actual login logic would be:
                // val result = loginUseCase(username, password)

                if (username == "valid" && password == "valid") {
                    onSuccess()
                } else {
                    errorMessage = "Invalid credentials"
                }
            } catch (e: Exception) {
                errorMessage = "Connection error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Input validation
    private fun validateInput(): Boolean {
        return when {
            username.isBlank() -> {
                errorMessage = "Username required"
                false
            }
            password.isBlank() -> {
                errorMessage = "Password required"
                false
            }
            else -> true
        }
    }

    private fun clearError() {
        errorMessage = null
    }
}
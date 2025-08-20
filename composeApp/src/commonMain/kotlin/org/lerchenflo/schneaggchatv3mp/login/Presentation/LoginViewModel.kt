import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.mp.KoinPlatform.getKoin
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.SharedViewModel

class LoginViewModel(
) : ViewModel() {
    // TextField states
    val sharedViewModel: SharedViewModel = getKoin().get()



    var username by mutableStateOf("")

    var password by mutableStateOf("")

    var loginButtonDisabled by mutableStateOf(false)

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
    fun login(onLoginSuccess: () -> Unit) {
        if (validateInput()) {
            try {
                isLoading = true

                // Use the sharedViewModel's login function with a callback
                sharedViewModel.login(username, password) { success, message ->
                    if (success) {
                        println("Login erfolgreich")
                        onLoginSuccess()
                    } else {
                        errorMessage = message
                    }
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
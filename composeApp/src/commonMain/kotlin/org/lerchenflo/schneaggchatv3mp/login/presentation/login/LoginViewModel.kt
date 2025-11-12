import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.database.AppRepository


class LoginViewModel(
    private val appRepository: AppRepository
): ViewModel() {


    var username by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var loginButtonDisabled by mutableStateOf(true)
        private set

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
        try {
            isLoading = true

            // Use the sharedViewModel's login function with a callback
            appRepository.login(username, password) { success, message ->
                if (success) {
                    println("Login erfolgreich")
                    CoroutineScope(Dispatchers.Main).launch { // launch on main thread (to avoid crash)
                        onLoginSuccess()
                    }
                } else {
                    println("LOGIN ERROR: $message")
                }
            }

        } catch (e: Exception) {
            errorMessage = "Connection error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    private fun clearError() {
        errorMessage = null

        if (username.isNotBlank() && password.isNotBlank()){
            loginButtonDisabled = false
        }
    }
}
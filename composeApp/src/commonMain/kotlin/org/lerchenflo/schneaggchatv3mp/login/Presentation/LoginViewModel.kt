import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform.getKoin
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.network.util.ResponseReason
import org.lerchenflo.schneaggchatv3mp.network.util.toEnumOrNull
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.acc_locked
import schneaggchatv3mp.composeapp.generated.resources.acc_not_exist
import schneaggchatv3mp.composeapp.generated.resources.feature_disabled
import schneaggchatv3mp.composeapp.generated.resources.offline
import schneaggchatv3mp.composeapp.generated.resources.password_wrong
import schneaggchatv3mp.composeapp.generated.resources.unknown_error
import kotlin.reflect.KClass


class LoginViewModel: ViewModel() {

    //Custom Factory für desktop fix
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                return LoginViewModel() as T
            }
        }
    }




    // TextField states
    val sharedViewModel: SharedViewModel = getKoin().get()


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
        if (validateInput()) {
            try {
                isLoading = true

                // Use the sharedViewModel's login function with a callback
                sharedViewModel.login(username, password) { success, message ->
                    if (success) {
                        println("Login erfolgreich")
                        onLoginSuccess()
                    } else {

                        viewModelScope.launch {
                            val responsereason = message.toEnumOrNull<ResponseReason>(true)

                            errorMessage = when(responsereason){
                                ResponseReason.NO_INTERNET,
                                ResponseReason.TIMEOUT -> getString(Res.string.offline)
                                ResponseReason.notfound -> getString(Res.string.acc_not_exist)
                                ResponseReason.wrong -> getString(Res.string.password_wrong)
                                ResponseReason.feature_disabled -> getString(Res.string.feature_disabled) //Haha wenn des kut denn isch was los
                                ResponseReason.account_temp_locked -> getString(Res.string.acc_locked)
                                ResponseReason.unknown_error -> getString(Res.string.unknown_error)

                                ResponseReason.too_big,
                                ResponseReason.none,
                                ResponseReason.exists,
                                ResponseReason.email_exists,
                                ResponseReason.forbidden,
                                ResponseReason.nomember,
                                ResponseReason.same,
                                null -> getString(Res.string.unknown_error)
                            }

                        }

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

        if (username.isNotBlank() && password.isNotBlank()){
            loginButtonDisabled = false
        }
    }
}
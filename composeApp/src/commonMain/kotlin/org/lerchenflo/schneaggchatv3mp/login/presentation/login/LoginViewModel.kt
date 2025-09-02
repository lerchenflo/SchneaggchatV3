import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform.getKoin
import org.lerchenflo.schneaggchatv3mp.chat.presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.network.util.ResponseReason
import org.lerchenflo.schneaggchatv3mp.network.util.toEnumOrNull
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.acc_locked
import schneaggchatv3mp.composeapp.generated.resources.acc_not_exist
import schneaggchatv3mp.composeapp.generated.resources.birthdateerror
import schneaggchatv3mp.composeapp.generated.resources.feature_disabled
import schneaggchatv3mp.composeapp.generated.resources.offline
import schneaggchatv3mp.composeapp.generated.resources.password_wrong
import schneaggchatv3mp.composeapp.generated.resources.unknown_error


class LoginViewModel(
    private val appRepository: AppRepository
): ViewModel() {



    fun trysavedcredslogin(onLoginSuccess: () -> Unit){
        viewModelScope.launch {
            val prefmanager = getKoin().get<Preferencemanager>()
            val (username, password) = prefmanager.getAutologinCreds()

            if (username.isNotBlank() && password.isNotBlank()){
                println("Username: $username passwort $password")
                //Form usfülla
                updateUsername(username)
                updatePassword(password)
                login(onLoginSuccess)
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

                            ResponseReason.invalid_birthdate -> getString(Res.string.birthdateerror)
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

    private fun clearError() {
        errorMessage = null

        if (username.isNotBlank() && password.isNotBlank()){
            loginButtonDisabled = false
        }
    }
}
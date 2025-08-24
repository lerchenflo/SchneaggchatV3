import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform.getKoin
import org.lerchenflo.schneaggchatv3mp.chat.presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.network.util.ResponseReason
import org.lerchenflo.schneaggchatv3mp.network.util.toEnumOrNull
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cannot_be_empty
import schneaggchatv3mp.composeapp.generated.resources.email_exists
import schneaggchatv3mp.composeapp.generated.resources.invalid_email
import schneaggchatv3mp.composeapp.generated.resources.password_missing_requirements
import schneaggchatv3mp.composeapp.generated.resources.password_needs_to_be_the_same
import schneaggchatv3mp.composeapp.generated.resources.requirement_digit
import schneaggchatv3mp.composeapp.generated.resources.requirement_forbidden
import schneaggchatv3mp.composeapp.generated.resources.requirement_length
import schneaggchatv3mp.composeapp.generated.resources.requirement_special
import schneaggchatv3mp.composeapp.generated.resources.unknown_error
import schneaggchatv3mp.composeapp.generated.resources.username_exists
import schneaggchatv3mp.composeapp.generated.resources.username_too_long
import kotlin.reflect.KClass


class SignUpViewModel(
    private val appRepository: AppRepository
): ViewModel() {




    // TextField states
    val sharedViewModel: SharedViewModel = getKoin().get()

    var username by mutableStateOf("")
        private set
    fun updateUsername(newValue: String) {
        username = newValue
        updateState() // Clear error when user types
    }
    var usernameerrorMessage by mutableStateOf<String?>(null)
        private set



    var password by mutableStateOf("")
        private set
    fun updatePassword(newValue: String) {
        password = newValue
        updateState()
    }
    var passworderrorMessage by mutableStateOf<String?>(null)
        private set

    var password2 by mutableStateOf("")
        private set
    fun updatePassword2(newValue: String) {
        password2 = newValue
        updateState()
    }
    var password2errorMessage by mutableStateOf<String?>(null)
        private set


    var email by mutableStateOf("")
        private set
    fun updateEmail(newValue: String) {
        email = newValue
        updateState()
    }
    var emailerrorMessage by mutableStateOf<String?>(null)
        private set


    //TODO: DAtepicker für gebi  und gender (Es wird als string gschickt also eig a dropdown mit nam custom input dazua)

    var gebidate by mutableStateOf<LocalDate?>(null)
        private set
    fun updategebidate(newValue: LocalDate?) {
        gebidate = newValue
        updateState()
    }
    var gebidateerrorMessage by mutableStateOf<String?>(null)
        private set


    var gender by mutableStateOf("")
        private set
    fun updategender(newValue: String) {
        gender = newValue
        updateState()
    }
    var gendererrorMessage by mutableStateOf<String?>(null)
        private set


    var genderslidervalue by mutableStateOf(0f)
        private set
    fun updategenderslidervalue(newValue: Float) {
        genderslidervalue = newValue
        updateState()
    }



    var signupButtonDisabled by mutableStateOf(true)
        private set

    var isLoading by mutableStateOf(false)
        private set


    fun signup(onCreateSuccess: () -> Unit) {
        viewModelScope.launch {
            if (validateInput()) {
                try {
                    isLoading = true

                    // Use the sharedViewModel's login function with a callback
                    appRepository.createAccount(username, email, password, gender, gebidate.toString()) { success, message ->
                        if (success) {
                            println("Account erstellen erfolgreich")

                            appRepository.login(username, password) { success, message ->
                                if (success){
                                    onCreateSuccess()
                                }
                            }


                        } else {

                            viewModelScope.launch {
                                val responsereason = message.toEnumOrNull<ResponseReason>(true)

                                if (responsereason == ResponseReason.exists){
                                    usernameerrorMessage = getString(Res.string.username_exists)
                                }else if (responsereason == ResponseReason.email_exists){
                                    emailerrorMessage = getString(Res.string.email_exists)
                                }else if (responsereason == ResponseReason.too_big){
                                    usernameerrorMessage = getString(Res.string.username_too_long)
                                }else if (responsereason == ResponseReason.unknown_error){
                                    password2errorMessage = getString(Res.string.unknown_error)
                                }

                            }

                        }
                    }

                } catch (e: Exception) {
                    password2errorMessage = e.message
                } finally {
                    isLoading = false
                }
            }
        }

    }

    /*
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

                            password2errorMessage = when(responsereason){
                                ResponseReason.NO_INTERNET,
                                ResponseReason.TIMEOUT -> getString(Res.string.offline)
                                ResponseReason.notfound -> getString(Res.string.acc_not_exist)
                                ResponseReason.wrong -> getString(Res.string.password_wrong)
                                ResponseReason.feature_disabled -> getString(Res.string.feature_disabled) //Haha wenn des kut denn isch was los
                                ResponseReason.account_temp_locked -> getString(Res.string.acc_locked)
                                ResponseReason.unknown_error -> getString(Res.string.unknown_error)

                                ResponseReason.too_big,
                                ResponseReason.none,
                                ResponseReason.exists -> getString(Res.string.username_exists)
                                ResponseReason.email_exists -> getString(Res.string.email_exists)
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

     */

    suspend fun validateInput(): Boolean {
        var isValid = true

        // Username check
        if (username.isBlank()) {
            usernameerrorMessage = getString(Res.string.cannot_be_empty)
            isValid = false
        } else {
            usernameerrorMessage = null
        }

        if (email.isBlank()) {
            emailerrorMessage = getString(Res.string.cannot_be_empty)
            isValid = false
        } else if (!isEmailValid(email.toString())) {
            emailerrorMessage = getString(Res.string.invalid_email)
            isValid = false
        }else{
            emailerrorMessage = null
        }

        // Password check
        if (password.isBlank()) {
            passworderrorMessage = getString(Res.string.cannot_be_empty)
            isValid = false
        } else {
            // Check password requirements
            val missingRequirements = getMissingPasswordRequirements(password)
            if (missingRequirements.isNotEmpty()) {
                passworderrorMessage = getString(Res.string.password_missing_requirements, missingRequirements.joinToString(", "))

                isValid = false
            } else {
                passworderrorMessage = null
            }
        }

        // Password2 check
        if (password2.isBlank()) {
            password2errorMessage = getString(Res.string.cannot_be_empty)
            isValid = false
        } else if (password != password2) {
            password2errorMessage = getString(Res.string.password_needs_to_be_the_same)
            isValid = false
        } else {
            password2errorMessage = null
        }

        return isValid
    }


    private fun updateState() {

        viewModelScope.launch {
            validateInput()

            if (username.isNotBlank() && password.isNotBlank() && password2.isNotBlank() && email.isNotBlank()){
                signupButtonDisabled = false
            }else{
                signupButtonDisabled = true
            }
        }


    }


    private fun isEmailValid(email: String): Boolean {
        // More comprehensive RFC 5322 compliant regex
        val emailRegex = "^[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\$"

        println("Validating email: $email")

        // Additional checks beyond regex
        if (email.isEmpty()) {
            println("Email validation failed: Email is empty")
            return false
        }

        if (email.length > 254) {
            println("Email validation failed: Email exceeds maximum length of 254 characters")
            return false
        }

        // Check for exactly one @ symbol
        val atCount = email.count { it == '@' }
        if (atCount != 1) {
            println("Email validation failed: Email must contain exactly one @ symbol (found $atCount)")
            return false
        }

        // Split into local and domain parts
        val parts = email.split('@')
        val localPart = parts[0]
        val domainPart = parts[1]

        // Check local part length (max 64 chars per RFC)
        if (localPart.isEmpty()) {
            println("Email validation failed: Local part (before @) is empty")
            return false
        }

        if (localPart.length > 64) {
            println("Email validation failed: Local part exceeds maximum length of 64 characters")
            return false
        }

        // Check domain part
        if (domainPart.isEmpty()) {
            println("Email validation failed: Domain part (after @) is empty")
            return false
        }

        // Check domain part has at least one dot
        if (!domainPart.contains('.')) {
            println("Email validation failed: Domain part must contain at least one dot")
            return false
        }

        // Check domain part segments
        val domainSegments = domainPart.split('.')
        for (segment in domainSegments) {
            if (segment.isEmpty()) {
                println("Email validation failed: Domain contains empty segment")
                return false
            }
            if (segment.length > 63) {
                println("Email validation failed: Domain segment '$segment' exceeds maximum length of 63 characters")
                return false
            }
        }

        // Final regex check
        if (!email.matches(emailRegex.toRegex())) {
            println("Email validation failed: Email format doesn't match RFC 5322 standard")
            return false
        }

        println("Email validation passed: $email")
        return true
    }


    private suspend fun getMissingPasswordRequirements(password: String): List<String> {
        val missing = mutableListOf<String>()

        // Length check
        if (password.length < 8) {
            missing.add(getString(Res.string.requirement_length))
        }

        // Digit check
        if (!password.any { it.isDigit() }) {
            missing.add(getString(Res.string.requirement_digit))
        }

        // Special character check
        if (!password.any { !it.isLetterOrDigit() }) {
            missing.add(getString(Res.string.requirement_special))
        }

        // Common forbidden patterns
        val forbiddenPatterns = listOf(
            "123", "234", "345", "456", "567", "678", "789", "890",
            "987", "876", "765", "654", "543", "432", "321", "210",
            "geheim", "test", "1234", "password", "qwerty", "asdfgh",
            "zxcvbn", "letmein", "admin", "welcome", "111111", "abc123",
            "passwort", "fortnite"
        )

        if (forbiddenPatterns.any { password.contains(it, ignoreCase = true) }) {
            missing.add(getString(Res.string.requirement_forbidden))
        }

        return missing
    }
}
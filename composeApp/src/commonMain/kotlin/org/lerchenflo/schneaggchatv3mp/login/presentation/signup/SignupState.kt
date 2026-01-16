package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import io.github.ismoy.imagepickerkmp.domain.models.PhotoResult
import kotlinx.datetime.LocalDate

data class SignupState(
    val isLoading: Boolean = false,

    val usernameState: InputfieldState = InputfieldState(),
    val passwordState: InputfieldState = InputfieldState(),
    val passwordRetypeState: InputfieldState = InputfieldState(),
    val emailState: InputfieldState = InputfieldState(),

    val gebiDate: LocalDate? = null, //"1994-5-25", //Year month day
    val gebiErrorText: String? = null,

    val profilePic: ByteArray? = null,
    val profilePicErrorText: String? = null,

    val agbsAccepted: Boolean = false,
    val agbsErrorText: String? = null

    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SignupState

        if (isLoading != other.isLoading) return false
        if (agbsAccepted != other.agbsAccepted) return false
        if (usernameState != other.usernameState) return false
        if (passwordState != other.passwordState) return false
        if (passwordRetypeState != other.passwordRetypeState) return false
        if (emailState != other.emailState) return false
        if (gebiDate != other.gebiDate) return false
        if (gebiErrorText != other.gebiErrorText) return false
        if (!profilePic.contentEquals(other.profilePic)) return false
        if (profilePicErrorText != other.profilePicErrorText) return false
        if (agbsErrorText != other.agbsErrorText) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + agbsAccepted.hashCode()
        result = 31 * result + usernameState.hashCode()
        result = 31 * result + passwordState.hashCode()
        result = 31 * result + passwordRetypeState.hashCode()
        result = 31 * result + emailState.hashCode()
        result = 31 * result + (gebiDate?.hashCode() ?: 0)
        result = 31 * result + (gebiErrorText?.hashCode() ?: 0)
        result = 31 * result + (profilePic?.contentHashCode() ?: 0)
        result = 31 * result + (profilePicErrorText?.hashCode() ?: 0)
        result = 31 * result + (agbsErrorText?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return """
            |SignupState(
            |  isLoading=$isLoading
            |  usernameState=$usernameState
            |  passwordState=${passwordState.copy(text = "***")} 
            |  passwordRetypeState=${passwordRetypeState.copy(text = "***")}
            |  emailState=$emailState
            |  gebiDate=$gebiDate
            |  gebiErrorText=$gebiErrorText
            |  profilePic=${if (profilePic != null) "[${profilePic.size} bytes]" else "null"}
            |  profilePicErrorText=$profilePicErrorText
            |  agbsAccepted=$agbsAccepted
            |  agbsErrorText=$agbsErrorText
            |)
        """.trimMargin()
    }

}

data class InputfieldState(
    val text: String = "",
    val errorMessage: String? = null,
    val enabled: Boolean = true
){
    fun isCorrect() : Boolean{
        return !text.isEmpty() && errorMessage == null
    }
}

sealed interface SignupAction {
    data class OnUsernameTextChange(val newText: String) : SignupAction
    data class OnPasswordTextChange(val newText: String, val retrypasswordfield: Boolean) : SignupAction
    data class OnEmailTextChange(val newText: String) : SignupAction
    data object OnSignUpButtonPress : SignupAction

    data class OnGebiDateChange(val newDate: LocalDate) : SignupAction
    data class OnProfilepicSelected(val profilePicResult: GalleryPhotoResult) : SignupAction


    data class OnAgbChecked(val checked: Boolean) : SignupAction

    data object OnBackClicked : SignupAction
}

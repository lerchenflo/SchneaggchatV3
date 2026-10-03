package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.groupextentions

sealed interface ExtensionAction {
    data class OpenExtension(val type: ExtensionType) : ExtensionAction

    data class AddExtension(val type: ExtensionType) : ExtensionAction
}
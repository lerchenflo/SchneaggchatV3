package org.lerchenflo.schneaggchatv3mp.settings.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.Draft
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.Preferencemanager

class SettingsRepository(
    private val preferencemanager: Preferencemanager

) {
    fun getUsemd() : Flow<Boolean> {
        return preferencemanager.getUseMdFlow()
    }

    fun getDraft(chatId: String, group: Boolean) : Flow<String?> {
        return preferencemanager.getDraftsFlow(
            chatId = chatId,
            group = group
        )
    }

    suspend fun saveDraft(chatId: String, group: Boolean, string: String) {
        preferencemanager.addDraft(
            draft = Draft(
                chatId = chatId,
                group = group,
                string = string
            )
        )
    }

}
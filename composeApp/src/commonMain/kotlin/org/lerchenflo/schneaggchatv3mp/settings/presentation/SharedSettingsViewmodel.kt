package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class SharedSettingsViewmodel(
    private val appVersion: AppVersion,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val loggingRepository: Logger,
    private val applicationScope: ApplicationScope,
) : ViewModel() {

    var serverUrl by mutableStateOf("")
        private set

    var devSettingsEnabled by mutableStateOf(false)
        private set

    var ownUser by mutableStateOf<User?>(null)
        private set

    var isAndroid by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { // Developer Settings
            settingsRepository.getDevSettingsFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Developer Settings preference: ${exception.message}")
                }
                .collect { value ->
                    devSettingsEnabled = value
                }
        }

        viewModelScope.launch { // Server URL
            settingsRepository.getServerUrlFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Server URL preference: ${exception.message}")
                }
                .collect { value ->
                    serverUrl = value
                }
        }

        viewModelScope.launch { // Own user
            // Follows the logged-in account: the settings tab root keeps this ViewModel across a
            // logout and the next login, so reading the id once would keep showing the old account.
            SessionCache.authState
                .map { (it as? SessionCache.AuthState.LoggedIn)?.userId }
                .distinctUntilChanged()
                .flatMapLatest { ownId -> ownId?.let { userRepository.getUserByIdFlow(it) } ?: flowOf(null) }
                .collect { value ->
                    ownUser = value
                }
        }

        isAndroid = appVersion.isAndroid()
    }

    // Developer Settings

    fun updateDevSettings(newValue: Boolean){
        applicationScope.launchOnMain {
            settingsRepository.setDevSettings(newValue)
            SessionCache.updateDeveloper(newValue)
        }
    }


    fun updateServerUrl(newValue: String) {
        serverUrl = newValue
        viewModelScope.launch {
            settingsRepository.saveServerUrl(newValue)
        }
    }


}
package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.birthdays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.ageOnNextBirthday
import org.lerchenflo.schneaggchatv3mp.utilities.daysUntilNextBirthday
import org.lerchenflo.schneaggchatv3mp.utilities.isBirthdayToday
import org.lerchenflo.schneaggchatv3mp.utilities.nextBirthdayMonth

class BirthdaysViewModel(
    private val appRepository: AppRepository,
    private val navigator: Navigator,
) : ViewModel() {

    private val _state = MutableStateFlow(BirthdaysState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val ownId = SessionCache.requireLoggedIn()?.userId
            val ownUserFlow = if (ownId != null) appRepository.getUserByIdFlow(ownId) else flowOf(null)
            val friendsFlow = appRepository.getFriendsFlow("")

            ownUserFlow.combine(friendsFlow) { ownUser, friends ->
                buildList {
                    ownUser?.let { add(it to true) }
                    friends.forEach { add(it to false) }
                }
            }.collectLatest { usersWithOwnFlag ->
                val entries = usersWithOwnFlag
                    .mapNotNull { (user, isOwn) -> user.toBirthdayEntry(isOwn) }
                    .sortedWith(compareBy({ it.daysUntil }, { it.user.displayName }))

                _state.update { it.copy(entries = entries, isLoading = false) }
            }
        }
    }

    private fun User.toBirthdayEntry(isOwn: Boolean): BirthdayEntry? {
        val daysUntil = daysUntilNextBirthday(birthDate) ?: return null
        val month = nextBirthdayMonth(birthDate) ?: return null
        return BirthdayEntry(
            user = this,
            daysUntil = daysUntil,
            nextBirthdayMonth = month,
            turningAge = ageOnNextBirthday(birthDate),
            isToday = isBirthdayToday(birthDate),
            isOwn = isOwn,
        )
    }

    fun onAction(action: BirthdaysAction) {
        when (action) {
            is BirthdaysAction.OnEntryClick -> onEntryClick(action.entry)
            BirthdaysAction.OnBackClick -> onBackClick()
        }
    }

    private fun onEntryClick(entry: BirthdayEntry) {
        if (entry.isOwn) return
        viewModelScope.launch {
            navigator.navigate(Route.Chat(chatId = entry.user.id, isGroup = false))
        }
    }

    private fun onBackClick() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }
}

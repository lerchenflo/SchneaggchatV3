package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.birthdays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.UserButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.iso8601DateFormatter
import org.lerchenflo.schneaggchatv3mp.utilities.monthNameResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.birthday_in_days
import schneaggchatv3mp.composeapp.generated.resources.birthday_tomorrow
import schneaggchatv3mp.composeapp.generated.resources.birthday_turns_age
import schneaggchatv3mp.composeapp.generated.resources.birthdays
import schneaggchatv3mp.composeapp.generated.resources.no_birthdays
import schneaggchatv3mp.composeapp.generated.resources.today
import schneaggchatv3mp.composeapp.generated.resources.you_with_brackets

@Composable
fun BirthdaysScreenRoot() {
    val viewModel: BirthdaysViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    BirthdaysScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun BirthdaysScreen(
    state: BirthdaysState,
    onAction: (BirthdaysAction) -> Unit,
) {
    val ownId = SessionCache.requireLoggedIn()?.userId ?: ""

    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = stringResource(Res.string.birthdays),
            onBackClick = { onAction(BirthdaysAction.OnBackClick) }
        )

        HorizontalDivider()

        if (state.entries.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.no_birthdays),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
            ) {
                var previousMonth: Int? = null
                state.entries.forEach { entry ->
                    if (entry.nextBirthdayMonth != previousMonth) {
                        previousMonth = entry.nextBirthdayMonth
                        stickyHeader {
                            MonthHeader(month = entry.nextBirthdayMonth)
                        }
                    }
                    item(key = entry.user.id) {
                        BirthdayRow(
                            entry = entry,
                            ownId = ownId,
                            onAction = onAction
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(monthNameResource(month)),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun BirthdayRow(
    entry: BirthdayEntry,
    ownId: String,
    onAction: (BirthdaysAction) -> Unit,
) {
    val youSuffix = stringResource(Res.string.you_with_brackets)
    val dateText = iso8601DateFormatter(iso8601Format = entry.user.birthDate.orEmpty(), format = "dd.MM.")
    val whenText = when {
        entry.isToday -> stringResource(Res.string.today)
        entry.daysUntil == 1 -> stringResource(Res.string.birthday_tomorrow)
        else -> stringResource(Res.string.birthday_in_days, entry.daysUntil)
    }
    val subtitle = buildList {
        add(dateText)
        add(whenText)
        entry.turningAge?.let { add(stringResource(Res.string.birthday_turns_age, it)) }
    }.joinToString(" · ")

    val chatListItem = entry.user.toChatListItem().let {
        if (entry.isOwn) it.copy(nickName = entry.user.displayName + youSuffix) else it
    }

    Surface(
        color = if (entry.isToday || entry.isOwn)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else
            Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        UserButton(
            ownId = ownId,
            chat = chatListItem,
            showNotiIcons = false,
            bottomTextOverride = subtitle,
            onClickGes = { onAction(BirthdaysAction.OnEntryClick(entry)) }
        )
    }
}

private fun previewUser(id: String, name: String, birthDate: String?): User = User(
    id = id,
    name = name,
    description = null,
    status = null,
    friendshipStatus = NetworkUtils.FriendshipStatus.ACCEPTED,
    birthDate = birthDate,
    emailVerifiedAt = null,
    createdAt = null,
    profilePicUpdatedAt = 0L,
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BirthdaysScreenPreview() {
    SchneaggchatTheme {
        BirthdaysScreen(
            state = BirthdaysState(
                isLoading = false,
                entries = listOf(
                    BirthdayEntry(
                        user = previewUser("me", "Me", "1998-08-28"),
                        daysUntil = 0,
                        nextBirthdayMonth = 8,
                        turningAge = 28,
                        isToday = true,
                        isOwn = true,
                    ),
                    BirthdayEntry(
                        user = previewUser("1", "Anna Beispiel", "2004-08-30"),
                        daysUntil = 2,
                        nextBirthdayMonth = 8,
                        turningAge = 22,
                        isToday = false,
                        isOwn = false,
                    ),
                    BirthdayEntry(
                        user = previewUser("2", "Max Mustermann", "1995-09-13"),
                        daysUntil = 16,
                        nextBirthdayMonth = 9,
                        turningAge = 31,
                        isToday = false,
                        isOwn = false,
                    ),
                    BirthdayEntry(
                        user = previewUser("3", "Lena Ohnesorge", "1900-10-05"),
                        daysUntil = 38,
                        nextBirthdayMonth = 10,
                        turningAge = null,
                        isToday = false,
                        isOwn = false,
                    ),
                )
            ),
            onAction = {}
        )
    }
}

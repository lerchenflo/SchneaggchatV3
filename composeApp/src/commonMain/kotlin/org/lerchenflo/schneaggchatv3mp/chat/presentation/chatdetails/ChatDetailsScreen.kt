package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lerchenflo.taptarget.tapTarget
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventItem
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.ExtensionListSection
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsDivider
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ZoomableProfilePicture
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.common_groups
import schneaggchatv3mp.composeapp.generated.resources.edit_profile_picture
import schneaggchatv3mp.composeapp.generated.resources.groupmembers
import schneaggchatv3mp.composeapp.generated.resources.shared_content_title

@Composable
fun ChatDetailsScreenRoot(
    chatId: String,
    isGroup: Boolean,
) {
    val viewModel = koinViewModel<ChatDetailsViewModel> { parametersOf(chatId, isGroup) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChatDetailsScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

/**
 * Details of a user or group chat: name and ⋮ actions in the top bar, picture, badges, the texts
 * about the chat, map, event, extensions, shared content and the members or common groups.
 */
@Composable
fun ChatDetailsScreen(
    state: ChatDetailsState,
    onAction: (ChatDetailsAction) -> Unit,
) {
    ChatDetailsDialogs(state = state, onAction = onAction)

    Column(modifier = Modifier.fillMaxWidth()) {
        ActivityTitle(
            title = chatDetailsTitle(state.chat),
            onBackClick = { onAction(ChatDetailsAction.OnBackClick) },
            menuItems = chatDetailsMenuItems(
                isGroup = state.isGroup,
                iAmAdmin = state.iAmAdmin,
                hasGroupTimer = state.groupExpiresAt != null,
                isEventCreator = state.isEventCreator,
                onAction = onAction
            ),
            // Tour steps of actions that live in the ⋮: nickname and wake (1:1), and the delete timer while
            // there is no countdown on the page yet (admins only, they are the ones who can set it)
            menuButtonModifier = when {
                !state.isGroup -> Modifier.tapTarget("chat_details_menu")
                state.iAmAdmin && state.groupExpiresAt == null -> Modifier.tapTarget("chat_details_group_timer")
                else -> Modifier
            }
        )

        HorizontalDivider()

        // Every row below is followed by its own divider, so section headers skip their top one
        // and no two dividers ever stack, whichever optional rows are shown.
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            ChatDetailsHeader(
                profilePictureUrl = state.chat.profilePictureUrl,
                showEditPictureBadge = state.isGroup,
                onEditPictureClick = { onAction(ChatDetailsAction.OnEditPictureClick) },
            )

            // Facts as small colored badges: the group's delete timer (every member sees the countdown,
            // admins set, move or clear it in the ⋮) or the friend's birthday
            ChatDetailsBadgeRow(
                groupExpiresAt = state.groupExpiresAt,
                birthDate = state.userDetails?.user?.birthDate,
                onBirthdayClick = { onAction(ChatDetailsAction.OnBirthdayClick) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                timerModifier = Modifier.tapTarget("chat_details_group_timer")
            )

            HorizontalDivider()

            // The friend's status and what their friends write about them, or the group's description
            ChatDetailsAboutCard(
                chat = state.chat,
                isGroup = state.isGroup,
                onEditDescriptionClick = { onAction(ChatDetailsAction.OnEditDescriptionClick) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider()

            // Where the friend is, or the group's members and event (only while someone shares a location)
            state.mapPreview?.let { preview ->
                ChatMapPreview(
                    preview = preview,
                    onClick = { onAction(ChatDetailsAction.OnMapPreviewClick) },
                )
                HorizontalDivider()
            }

            ConnectedEventSection(state = state, onAction = onAction)

            // New extensions are added through the ⋮, so an empty section is left out
            if (state.groupExtensions.isNotEmpty()) {
                ExtensionListSection(
                    extensions = state.groupExtensions,
                    onAction = { onAction(ChatDetailsAction.OnExtensionAction(it)) }
                )
                HorizontalDivider()
            }

            // Everything ever shared in this chat, one part per tab of the shared content screen
            SettingsDivider(
                title = stringResource(Res.string.shared_content_title),
                showTopDivider = false
            )
            SharedContentSplitButton(
                imageCount = state.sharedImageCount,
                linkCount = state.sharedLinkCount,
                messageCount = state.messageCount,
                onClick = { onAction(ChatDetailsAction.OnSharedContentClick(it)) }
            )
            HorizontalDivider()

            GroupMembersSection(state = state, onAction = onAction)
            CommonGroupsSection(state = state, onAction = onAction)
        }
    }
}

/** The event this group was created for, if any; its creator deletes or detaches it through the ⋮. */
@Composable
private fun ConnectedEventSection(
    state: ChatDetailsState,
    onAction: (ChatDetailsAction) -> Unit,
) {
    val event = state.connectedEvent ?: return
    val creatorPictureUrl = state.groupDetails?.members
        ?.find { it.groupMember.userId == event.creatorId }
        ?.user
        ?.profilePictureUrl

    EventItem(
        event = event,
        creatorProfilePictureUrl = creatorPictureUrl,
        isOwnEvent = state.isEventCreator,
        onClick = { onAction(ChatDetailsAction.OnConnectedEventClick) },
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
    )
    HorizontalDivider()
}

/** The group's members; admins (and every member while the group allows it) add new ones with the button in the header. */
@Composable
private fun GroupMembersSection(
    state: ChatDetailsState,
    onAction: (ChatDetailsAction) -> Unit,
) {
    val group = state.groupDetails ?: return
    val ownId = state.ownId ?: return

    SettingsDivider(
        title = stringResource(Res.string.groupmembers, group.members.size),
        showTopDivider = false,
        modifier = Modifier.tapTarget("chat_details_group_members"),
        action = if (state.canAddMembers) {
            { AddMemberButton(onClick = { onAction(ChatDetailsAction.OnAddMembersClick) }) }
        } else {
            null
        }
    )

    GroupMembersView(
        members = group.members,
        ownId = ownId,
        iAmAdmin = state.iAmAdmin,
        changeAdminStatus = { onAction(ChatDetailsAction.OnChangeAdminStatus(it)) },
        removeMember = { onAction(ChatDetailsAction.OnRemoveMember(it)) },
        openUnknownUser = { userId, name -> onAction(ChatDetailsAction.OnOpenUnknownUser(userId, name)) },
    )
    HorizontalDivider()
}

/** The groups the user shares with this friend, only while there are some. */
@Composable
private fun CommonGroupsSection(
    state: ChatDetailsState,
    onAction: (ChatDetailsAction) -> Unit,
) {
    val commonGroups = state.userDetails?.commonGroups.orEmpty()
    if (commonGroups.isEmpty()) return

    SettingsDivider(
        title = stringResource(Res.string.common_groups, commonGroups.size),
        showTopDivider = false
    )
    CommonGroupsView(
        groups = commonGroups,
        onOpenGroup = { onAction(ChatDetailsAction.OnOpenChat(chatId = it, isGroup = true)) }
    )
    HorizontalDivider()
}

/**
 * The group's name, or the friend's name with the own nickname for them: Name ("Nickname"), the
 * nickname smaller and italic.
 */
@Composable
private fun chatDetailsTitle(chat: ChatDetailsChat): AnnotatedString {
    val nickName = (chat as? ChatDetailsChat.UserDetails)?.user?.nickName?.takeIf { it.isNotBlank() }
    val nickNameFontSize = MaterialTheme.typography.titleLarge.fontSize * 0.8f

    return buildAnnotatedString {
        append(chat.name)
        if (nickName != null) {
            append(" (\"")
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontSize = nickNameFontSize)) {
                append(nickName)
            }
            append("\")")
        }
    }
}

/**
 * Centered profile picture (the name is in the top bar); [showEditPictureBadge] adds the same edit
 * badge as the own profile in the user settings, and an edit button to the big picture a tap on the
 * picture opens.
 */
@Composable
private fun ChatDetailsHeader(
    profilePictureUrl: String,
    showEditPictureBadge: Boolean,
    onEditPictureClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(160.dp)) {
            ZoomableProfilePicture(
                filepath = profilePictureUrl,
                modifier = Modifier.fillMaxSize(),
                onEdit = if (showEditPictureBadge) onEditPictureClick else null,
            )

            if (showEditPictureBadge) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .clickable { onEditPictureClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.edit_profile_picture),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatDetailsScreenPreview() {
    SchneaggchatTheme {
        ChatDetailsScreen(
            state = ChatDetailsState(
                isGroup = false,
                ownId = "me",
                chat = ChatDetailsChat.UserDetails(
                    user = User(
                        id = "emma",
                        name = "emma",
                        nickName = "Emmi",
                        description = "Kommt zu jeder Skitour, aber nie vor 11 😄",
                        status = "Immer auf der Suche nach dem besten Kaffee ☕",
                        friendshipStatus = FriendshipStatus.ACCEPTED,
                        birthDate = "2000-03-20",
                        emailVerifiedAt = null,
                        createdAt = 0L,
                        profilePicUpdatedAt = 0L,
                    )
                ),
                sharedLinkCount = 1,
                messageCount = 6,
            ),
            onAction = {}
        )
    }
}

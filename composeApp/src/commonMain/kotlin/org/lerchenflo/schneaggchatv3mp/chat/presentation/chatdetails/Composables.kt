package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton

@Composable
fun GroupMembersView(
    members: List<GroupMemberWithUser>,
    onUserClick: (String) -> Unit //Returns the user id
) {
    Column {
        members.forEach { (groupMember, user) ->
            Text(user?.name ?: "Kenni ned")
            //USer nullable, kann si dass der ned in da lokala db isch
            //TODO: Show users (Fabi neuer userbutton würdi macha ohne scetchy selectedchat)
        }
    }
}
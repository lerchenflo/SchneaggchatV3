package org.lerchenflo.schneaggchatv3mp.chatauswahl.Presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.database.User

@Preview
@Composable
fun addUserButton(user: User){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable{
                openChat(user)
            }
    ){
        Text(
            text = user.name.toString(),
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}


fun openChat(user: User){
    println("Chat geöffnet: Userid:${user.id}")
}


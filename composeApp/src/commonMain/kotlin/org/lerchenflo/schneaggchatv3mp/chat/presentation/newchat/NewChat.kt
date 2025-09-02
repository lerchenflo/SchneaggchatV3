package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.lerchenflo.schneaggchatv3mp.theme.SchneaggchatTheme

@Composable
fun NewChat(){
    SchneaggchatTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
        ){
            Text("new Chat activity")
        }

    }

}
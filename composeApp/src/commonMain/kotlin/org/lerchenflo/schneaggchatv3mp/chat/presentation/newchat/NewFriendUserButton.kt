package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.common_friends
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer

@Preview(
    showBackground = true
)
@Composable
fun NewFriendUserButton(
    username: String = "New user",
    commonFriendCount: Int = 4,
    highlightedLetterCount: Int = 0,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
        .padding(4.dp)
){
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable{
                onClick()
            }

    ) {

        Icon(
            imageResource(Res.drawable.icon_nutzer),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.padding(horizontal = 8.dp))

        Column {
            val annotatedUsername = buildAnnotatedString {
                val highlightCount = highlightedLetterCount.coerceIn(0, username.length)

                if (highlightCount > 0) {
                    // Highlighted part
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(username.take(highlightCount))
                    }
                    // Rest of the text
                    append(username.drop(highlightCount))
                } else {
                    append(username)
                }
            }

            Text(
                text = annotatedUsername,
            )

            if (commonFriendCount != 0){
                Text(
                    text = stringResource(Res.string.common_friends, commonFriendCount.toString()),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp) //TODO Fabi: Eventuell die unterschiede zwüschat da textsizes kle größer macha es luagt alls glich us?
                )
            }

        }


    }
}
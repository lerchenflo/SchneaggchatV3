package org.lerchenflo.schneaggchatv3mp.chat.Presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.descriptors.PrimitiveKind
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.database.User
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.*
import kotlin.random.Random

@Composable
fun Chatauswahlscreen(
    onChatSelected: (Int) -> Unit,  // navigation callback
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val viewModel = koinViewModel<SharedViewModel>()

    val users by viewModel.getAllUsers().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(true) {
        val userlist = listOf<User>(
            User(1, 0, "Ben"),
            User(3, 0, "flo"),
            User(4, 0, "fabi")
        )

        userlist.forEach {
            viewModel.upsertUser(it)
        }
    }

    //Hauptlayout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)

    ) {
        //Obere Zeile für Buttons
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                fontSize = 26.sp,
                text = stringResource(Res.string.app_name),
                modifier = Modifier
                    .padding(16.dp)


            )

            //TODO: Schneaggmap button usw
        }

        //Zweite Zeile für Freund suchen
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {
                    //TODO: Suchen
                }
            )

            //TODO: Filtersymbol

            // new chat button
            Button(

                onClick = { onNewChatClick() },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    painterResource(Res.drawable.new_chat),
                    null,
                    tint = LocalContentColor.current
                )
            }
        }

        //Gegneranzeige
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(users) { user ->
                AddUserButton(
                    user = user,
                    onClick = {onChatSelected(user.id)}
                )
                HorizontalDivider(
                    thickness = 2.dp
                )
            }
        }
    }
}



@Preview
@Composable
fun AddUserButton(
    user: User?,
    onClick: () -> Unit = {}  // Add click handler
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min), // Use minimal intrinsic height
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile picture
        Image(
            painter = painterResource(Res.drawable.icon_nutzer),
            contentDescription = stringResource(Res.string.profile_picture),
            modifier = Modifier
                .size(48.dp) // Use square aspect ratio
                .padding(end = 8.dp) // Right padding only
                .clip(CircleShape) // Circular image
        )

        // User info column
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {onClick()} // onclick to open chat
        ) {
            // Username
            Text(
                text = user?.name ?: "Unknown User",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (Random.nextBoolean()) { // todo: check if lastmessage isch existent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last message preview
                    Text(
                        text = "Placeholder message text",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Time indicator
                    Text(
                        text = "16:15",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
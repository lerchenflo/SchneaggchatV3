package org.lerchenflo.schneaggchatv3mp.todolist.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.content.MediaType.Companion.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugPriority
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.noti_bell
import schneaggchatv3mp.composeapp.generated.resources.notification_bell

@Composable
fun TodoEntryUI(
    todoEntry: TodoEntry,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(
            top = 4.dp,
            start = 4.dp,
            end = 4.dp
        ),
    onClick: () -> Unit,
) {

    Column(
        modifier = modifier
            .clickable{
                onClick()
            }
    ) {
        //TODO: DO am afang s profilbild ine


        Row {
            Text(
                text = todoEntry.title,
                maxLines = 1,
                modifier = Modifier
                    .padding(5.dp)
                    .weight(1f)
            )

            when(todoEntry.priority){
                BugPriority.High.value -> {
                    Image(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = "highlighted",
                        modifier = Modifier
                            .padding(5.dp)
                    )
                }

                BugPriority.Extreme.value -> {
                    Image(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "highlighted",
                        modifier = Modifier
                            .padding(5.dp)
                    )
                }


            }

        }

        Row {
            Text(
                text = todoEntry.content,
                maxLines = 1,
                modifier = Modifier
                    .padding(5.dp)
            )
        }
    }

}
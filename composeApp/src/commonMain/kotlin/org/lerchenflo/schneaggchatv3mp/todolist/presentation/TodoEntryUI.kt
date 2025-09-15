package org.lerchenflo.schneaggchatv3mp.todolist.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.content.MediaType.Companion.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugPriority
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugStatus
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugType
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
    profilepicfilepath : String,
) {

    Row(
        modifier = modifier
            .clickable{
                onClick()
            }
            .background(when (todoEntry.status) {
                BugStatus.InProgress.value -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)else -> MaterialTheme.colorScheme.tertiaryContainer
            },
                shape = RoundedCornerShape(15.dp)
            )
            
            .alpha(if(todoEntry.status == BugStatus.Finished.value) 0.6f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {

        ProfilePictureView(
            filepath = profilepicfilepath,
            modifier = Modifier
                .padding(8.dp)
                .size(40.dp) // Use square aspect ratio
                .clip(CircleShape) // Circular image

        )

        Column {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {


                Text(
                    text = todoEntry.title,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(5.dp)
                        .weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )

                when(todoEntry.priority){
                    BugPriority.Low.value -> {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "outlined star",
                            modifier = Modifier.padding(5.dp).size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    BugPriority.High.value -> {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = "outlined star",
                            modifier = Modifier.padding(5.dp).size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    BugPriority.Extreme.value -> {

                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "filled star",
                            modifier = Modifier.padding(5.dp).size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }


                }

            }

            Row(
                verticalAlignment = Alignment.CenterVertically

            ) {
                Text(
                    text = todoEntry.content,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(5.dp)
                        .weight(1f),
                    style = MaterialTheme.typography.bodySmall
                )

                when (todoEntry.type) {
                    BugType.BugReport.value -> {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "bug report",
                            modifier = Modifier.padding(end = 9.dp).size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    BugType.FeatureRequest.value -> {
                        Icon(
                            imageVector = Icons.Default.EmojiObjects,
                            contentDescription = "feature request",
                            modifier = Modifier.padding(end = 9.dp).size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    BugType.Todo.value -> {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "todo",
                            modifier = Modifier.padding(
                                end = 9.dp).size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

        }



    }

}
package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.app_name
import schneaggchatv3mp.composeapp.generated.resources.enter_username
import schneaggchatv3mp.composeapp.generated.resources.poll_creator_title
import schneaggchatv3mp.composeapp.generated.resources.poll_options_title
import schneaggchatv3mp.composeapp.generated.resources.poll_title
import schneaggchatv3mp.composeapp.generated.resources.search_user

@Preview
@Composable
fun PollDialog(
    onDismiss: () -> Unit = {},
    onCreatePoll: (

    ) -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var customOption by remember { mutableStateOf(false) }
    var multipleAnswers by remember { mutableStateOf(false) }
    var visibility by remember { mutableStateOf(PollVisibility.PUBLIC) }

    val options = remember {
        mutableStateListOf("")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ){
            Column(
                modifier = Modifier
            ){
                Text(
                    text = stringResource(Res.string.poll_creator_title)
                )

                OutlinedTextField(
                    value = title,
                    maxLines = 1,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.poll_title)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Text(
                    text = stringResource(Res.string.poll_options_title)
                )

                options.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { options[index] = it },
                        label = { Text("Option ${index + 1}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }


            }
        }
    }

}

enum class PollVisibility{
    PUBLIC,
    PRIVATE,
    ANONYMOUS
}
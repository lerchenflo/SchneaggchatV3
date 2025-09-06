package org.lerchenflo.schneaggchatv3mp.todolist.presentation

import androidx.compose.foundation.content.MediaType.Companion.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.lerchenflo.schneaggchatv3mp.todolist.domain.TodoEntry

@Composable
fun TodoEntryUI(
    todoEntry: TodoEntry,
    modifier: Modifier = Modifier
        .fillMaxWidth()
) {

    Column(
        modifier = modifier
    ) {
        BasicText(
            text = todoEntry.title
        )
    }

}
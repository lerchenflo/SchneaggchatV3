package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message

@Composable
fun TextMessageContentView(
    useMD: Boolean = false,
    message: Message,
    myMessage: Boolean,
    modifier: Modifier = Modifier
){

    if(useMD){ // get setting if if md is enabled
        // Cache markdown rendering to avoid re-parsing on every recomposition
        val content = remember(message.content) {
            message.content
        }
        Markdown(
            content = content,
            modifier = modifier,
            //TODO: Nocham neua theme do die message text color flicka

            colors = DefaultMarkdownColors(
                text = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                inlineCodeBackground = MaterialTheme.colorScheme.error,
                dividerColor = MaterialTheme.colorScheme.onPrimary,
                tableBackground = MaterialTheme.colorScheme.onSurface,
                codeBackground = MaterialTheme.colorScheme.onSurfaceVariant,
            )


        )
    }else{
        Text(
            text = message.content,
            color = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }



}
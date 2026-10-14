package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.Reaction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction

@Composable
fun ReactionView(
    reactions: List<Reaction>,
    myMessage: Boolean,
    messageId: String,
    onAction: (MessageAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return
    
    // Group reactions by emoji and count them
    val groupedReactions by produceState(
        initialValue = emptyList(),
        key1 = reactions
    ) {
        value = withContext(Dispatchers.Default) {
            reactions.groupBy { it.content }
                .map { (emoji, reactionList) ->
                    ReactionBadge(
                        reaction = emoji,
                        count = reactionList.size,
                        hasReacted = reactionList.any {
                            it.userId == SessionCache.requireLoggedIn()?.userId
                        }
                    )
                }
                .sortedBy { it.reaction }
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (myMessage) 40.dp else 0.dp,
                end = if (myMessage) 0.dp else 40.dp
            ),
        horizontalArrangement = if (myMessage) Arrangement.End else Arrangement.Start
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy((2).dp),
            verticalArrangement = Arrangement.spacedBy((2).dp)
        ) {
            groupedReactions.forEach { reactionBadge ->
                ReactionBadgeItem(
                    reactionBadge = reactionBadge,
                    onClick = {
                        onAction(MessageAction.ToggleReaction(
                            messageId = messageId,
                            reaction = reactionBadge.reaction
                        ))
                    }
                )
            }
        }
    }
}

@Composable
internal fun ReactionBadgeItem(
    reactionBadge: ReactionBadge,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (reactionBadge.hasReacted) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClick = {onClick()})
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = reactionBadge.reaction,
                fontSize = 14.sp
            )
            
            if (reactionBadge.count > 1) {
                Text(
                    text = reactionBadge.count.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (reactionBadge.hasReacted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

internal data class ReactionBadge(
    val reaction: String,
    val count: Int,
    val hasReacted: Boolean
)
package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.poll

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoter

/**
 * Sample data for poll previews: a poll five levels deep (the maximum), with sub polls on several
 * options per level (Weekend trip? -> Yes -> Where to? -> Mountains -> Which hut? -> Frassenhütte ->
 * How do we get up? -> Hike -> Which route?).
 */
internal object PollPreviewData {

    const val OWN_ID = "me"
    private const val CREATOR_ID = "anna"

    private val readerMap = mapOf(OWN_ID to "Me", CREATOR_ID to "Anna", "ben" to "Ben", "clara" to "Clara")

    private fun option(id: String, text: String, voterIds: List<String>, subPoll: PollMessage? = null) = PollVoteOption(
        id = id,
        text = text,
        custom = false,
        creatorId = CREATOR_ID,
        voters = voterIds.mapIndexed { index, userId -> PollVoter(userId = userId, votedAt = 1_000L + index) },
        subPoll = subPoll,
    )

    private fun poll(
        title: String,
        description: String? = null,
        maxAnswers: Int? = 1,
        visibleToAll: Boolean = true,
        options: List<PollVoteOption>,
    ) = PollMessage(
        creatorId = CREATOR_ID,
        title = title,
        description = description,
        maxAnswers = maxAnswers,
        customAnswersEnabled = false,
        maxAllowedCustomAnswers = null,
        visibility = PollVisibility.PUBLIC,
        expiresAt = null,
        voteOptions = options,
        visibleToAll = visibleToAll,
    )

    /**
     * The sent poll, five levels deep (the maximum) with sub polls on several options per level.
     * "me" picked Yes -> Mountains + Lake -> Frassenhütte -> Hike -> Via Bludenz, plus Swimming.
     */
    val samplePoll: PollMessage = poll(
        title = "Weekend trip?",
        description = "Saturday + Sunday, leaving Friday evening",
        options = listOf(
            option(
                id = "yes", text = "Yes", voterIds = listOf(OWN_ID, "ben", CREATOR_ID),
                subPoll = poll(
                    title = "Where to?",
                    maxAnswers = null,
                    options = listOf(
                        option(
                            id = "mountains", text = "Mountains", voterIds = listOf(OWN_ID, "ben"),
                            subPoll = poll(
                                title = "Which hut?",
                                options = listOf(
                                    option(
                                        id = "frassen", text = "Frassenhütte", voterIds = listOf(OWN_ID),
                                        subPoll = poll(
                                            title = "How do we get up?",
                                            options = listOf(
                                                option(
                                                    id = "hike", text = "Hike", voterIds = listOf(OWN_ID),
                                                    subPoll = poll(
                                                        title = "Which route?",
                                                        description = "All around 3h",
                                                        visibleToAll = false,
                                                        options = listOf(
                                                            option(id = "bludenz", text = "Via Bludenz", voterIds = listOf(OWN_ID)),
                                                            option(id = "nenzing", text = "Via Nenzing", voterIds = emptyList()),
                                                            option(id = "raggal", text = "Via Raggal", voterIds = emptyList()),
                                                        )
                                                    )
                                                ),
                                                option(id = "cablecar", text = "Cable car", voterIds = emptyList()),
                                                option(
                                                    id = "bike", text = "Mountain bike", voterIds = emptyList(),
                                                    subPoll = poll(
                                                        title = "Rent e-bikes?",
                                                        options = listOf(
                                                            option(id = "ebike_yes", text = "Yes, rent", voterIds = emptyList()),
                                                            option(id = "ebike_no", text = "Own bikes", voterIds = emptyList()),
                                                        )
                                                    )
                                                ),
                                            )
                                        )
                                    ),
                                    option(
                                        id = "freschen", text = "Freschenhaus", voterIds = listOf("ben"),
                                        subPoll = poll(
                                            title = "Sleep where?",
                                            options = listOf(
                                                option(id = "dorm", text = "Dormitory", voterIds = listOf("ben")),
                                                option(id = "room", text = "Private room", voterIds = emptyList()),
                                            )
                                        )
                                    ),
                                    option(id = "lindauer", text = "Lindauer Hütte", voterIds = emptyList()),
                                )
                            )
                        ),
                        option(
                            id = "lake", text = "Lake Constance", voterIds = listOf(OWN_ID, CREATOR_ID),
                            subPoll = poll(
                                title = "What to do at the lake?",
                                maxAnswers = null,
                                options = listOf(
                                    option(id = "swim", text = "Swimming", voterIds = listOf(OWN_ID, CREATOR_ID)),
                                    option(id = "sup", text = "Stand-up paddling", voterIds = listOf(CREATOR_ID)),
                                    option(
                                        id = "boat", text = "Boat tour", voterIds = emptyList(),
                                        subPoll = poll(
                                            title = "Which boat?",
                                            options = listOf(
                                                option(id = "ferry", text = "Ferry to Lindau", voterIds = emptyList()),
                                                option(id = "sail", text = "Rent a sailboat", voterIds = emptyList()),
                                            )
                                        )
                                    ),
                                )
                            )
                        ),
                        option(
                            id = "city", text = "City trip", voterIds = emptyList(),
                            subPoll = poll(
                                title = "Which city?",
                                options = listOf(
                                    option(id = "zurich", text = "Zurich", voterIds = emptyList()),
                                    option(id = "munich", text = "Munich", voterIds = emptyList()),
                                    option(id = "milan", text = "Milan", voterIds = emptyList()),
                                )
                            )
                        ),
                    )
                )
            ),
            option(
                id = "no", text = "No", voterIds = listOf("clara"),
                subPoll = poll(
                    title = "Why not?",
                    options = listOf(
                        option(id = "work", text = "Work", voterIds = emptyList()),
                        option(
                            id = "expensive", text = "Too expensive", voterIds = listOf("clara"),
                            subPoll = poll(
                                title = "What budget would work?",
                                options = listOf(
                                    option(id = "budget50", text = "Under 50 €", voterIds = listOf("clara")),
                                    option(id = "budget100", text = "Under 100 €", voterIds = emptyList()),
                                )
                            )
                        ),
                        option(id = "other", text = "Other plans", voterIds = emptyList()),
                    )
                )
            ),
            option(id = "maybe", text = "Maybe", voterIds = emptyList()),
        )
    )

    fun sampleMessage(poll: PollMessage = samplePoll) = Message(
        id = "preview_poll",
        msgType = MessageType.POLL,
        poll = poll,
        senderId = CREATOR_ID,
        receiverId = OWN_ID,
        sendDate = "1704067200000",
        myMessage = false,
        readByMe = true,
        readers = emptyList(),
    )

    val sampleReaderMap: Map<String, String> = readerMap

    /** Draft level with sub polls switched on whenever any option carries one. */
    private fun draft(
        title: String,
        vararg options: Pair<String, PollDraft?>,
        configure: PollDraft.() -> Unit = {},
    ) = PollDraft(
        title = title,
        options = options.map { (text, subPoll) -> PollOptionInput(TextFieldValue(text), subPoll = subPoll) }
    ).apply {
        subPollsEnabled = options.any { it.second != null }
        configure()
    }

    /** The same five-level poll as an unsent draft in the creator. */
    fun sampleDraft(): PollDraft {
        val routeDraft = draft(
            "Which route?",
            "Via Bludenz" to null,
            "Via Nenzing" to null,
            "Via Raggal" to null,
        ) { visibleToAll = false }

        val ascentDraft = draft(
            "How do we get up?",
            "Hike" to routeDraft,
            "Cable car" to null,
            "Mountain bike" to draft("Rent e-bikes?", "Yes, rent" to null, "Own bikes" to null),
        )

        val hutDraft = draft(
            "Which hut?",
            "Frassenhütte" to ascentDraft,
            "Freschenhaus" to draft("Sleep where?", "Dormitory" to null, "Private room" to null),
            "Lindauer Hütte" to null,
        )

        val lakeDraft = draft(
            "What to do at the lake?",
            "Swimming" to null,
            "Stand-up paddling" to null,
            "Boat tour" to draft("Which boat?", "Ferry to Lindau" to null, "Rent a sailboat" to null),
        ) {
            allowMultipleAnswers = true
            allowedAnswerCount = 10
        }

        val whereDraft = draft(
            "Where to?",
            "Mountains" to hutDraft,
            "Lake Constance" to lakeDraft,
            "City trip" to draft("Which city?", "Zurich" to null, "Munich" to null, "Milan" to null),
        ) {
            allowMultipleAnswers = true
            allowedAnswerCount = 10
        }

        val whyNotDraft = draft(
            "Why not?",
            "Work" to null,
            "Too expensive" to draft("What budget would work?", "Under 50 €" to null, "Under 100 €" to null),
            "Other plans" to null,
        )

        return draft(
            "Weekend trip?",
            "Yes" to whereDraft,
            "No" to whyNotDraft,
            "Maybe" to null,
        ) {
            description = TextFieldValue("Saturday + Sunday, leaving Friday evening")
        }
    }
}

@Composable
private fun PollPreviewBubble(ownId: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        PollMessageContentView(
            ownId = ownId,
            message = PollPreviewData.sampleMessage(),
            useMD = false,
            myMessage = false,
            readerMap = PollPreviewData.sampleReaderMap,
        )
    }
}

/** "me" picked the whole path, so all three levels are open and answerable. */
@Preview(showBackground = true, heightDp = 2400)
@Composable
private fun PollSubPollsVoterPreview() {
    PollPreviewBubble(ownId = PollPreviewData.OWN_ID)
}

/** Someone who picked "No": the sub polls are collapsed and only viewable read-only. */
@Preview(showBackground = true, heightDp = 2400)
@Composable
private fun PollSubPollsNonVoterPreview() {
    PollPreviewBubble(ownId = "clara")
}

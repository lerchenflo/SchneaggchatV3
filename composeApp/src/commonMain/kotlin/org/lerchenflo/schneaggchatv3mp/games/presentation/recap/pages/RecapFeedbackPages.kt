package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.FeedbackVotesUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageTheme
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageThemes
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_confirmed
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_fixed
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_implemented
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_open
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_planned
import schneaggchatv3mp.composeapp.generated.resources.feedback_status_requested
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_didnt_know
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_down
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_not_needed
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_up
import schneaggchatv3mp.composeapp.generated.resources.recap_count_all_time
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_bugs
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_comments
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_confirmed
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_dev_replies
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_fixed
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_ideas
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_implemented
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_outcomes_label
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_planned
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_reported_all_time_label
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_reported_label
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_title
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_top_entry
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_top_entry_label
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_votes_cast_label
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_votes_received_label
import schneaggchatv3mp.composeapp.generated.resources.recap_feedback_votes_title
import schneaggchatv3mp.composeapp.generated.resources.recap_label_count

// The two feedback board pages: what you reported and what became of it, then your votes and comments.

/** Bugs and ideas you sent, what became of them and your most upvoted entry. */
@Composable
fun RecapFeedbackReportsPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.FeedbackReports
    val feedback = recap.feedback
    // Without reports this year the all-time numbers lead
    val thisYear = feedback.reportedThisYear > 0
    val bugs = if (thisYear) feedback.bugsThisYear else feedback.bugsAllTime
    val ideas = if (thisYear) feedback.ideasThisYear else feedback.ideasAllTime

    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_feedback_title), color = theme.accent, maxFontSize = 36.sp)
        }
        Spacer(Modifier.height(24.dp))
        RecapBigNumber(
            target = if (thisYear) feedback.reportedThisYear else feedback.reportedAllTime,
            running = visible,
            color = theme.onBackground
        )
        RevealItem(visible, 1) {
            RecapBody(
                text = stringResource(if (thisYear) Res.string.recap_feedback_reported_label else Res.string.recap_feedback_reported_all_time_label),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 20.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        RevealItem(visible, 2) {
            PillFlow(
                pills = buildList {
                    if (bugs > 0) add(stringResource(Res.string.recap_feedback_bugs, formatCount(bugs)))
                    if (ideas > 0) add(stringResource(Res.string.recap_feedback_ideas, formatCount(ideas)))
                    if (thisYear) add(stringResource(Res.string.recap_count_all_time, formatCount(feedback.reportedAllTime)))
                },
                theme = theme
            )
        }

        val outcomes = buildList {
            if (feedback.ideasImplemented > 0) add(stringResource(Res.string.recap_feedback_implemented, formatCount(feedback.ideasImplemented)))
            if (feedback.ideasPlanned > 0) add(stringResource(Res.string.recap_feedback_planned, formatCount(feedback.ideasPlanned)))
            if (feedback.bugsFixed > 0) add(stringResource(Res.string.recap_feedback_fixed, formatCount(feedback.bugsFixed)))
            if (feedback.bugsConfirmed > 0) add(stringResource(Res.string.recap_feedback_confirmed, formatCount(feedback.bugsConfirmed)))
        }
        if (outcomes.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            RevealItem(visible, 3) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecapEyebrow(text = stringResource(Res.string.recap_feedback_outcomes_label), color = theme.accent.copy(alpha = 0.8f))
                    PillFlow(pills = outcomes, theme = theme)
                }
            }
        }

        feedback.topEntry?.let { entry ->
            Spacer(Modifier.height(24.dp))
            RevealItem(visible, 4) {
                RecapLabeledCard(
                    label = stringResource(Res.string.recap_feedback_top_entry_label),
                    text = stringResource(
                        Res.string.recap_feedback_top_entry,
                        entry.title,
                        formatCount(entry.upVotes),
                        feedbackStatusLabel(entry.status)?.let { stringResource(it) } ?: entry.status
                    ),
                    theme = theme
                )
            }
        }
    }
}

/** Votes you cast and got on the feedback board, comments you wrote and the devs' replies to you. */
@Composable
fun RecapFeedbackVotesPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.FeedbackVotes
    val feedback = recap.feedback

    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_feedback_votes_title), color = theme.accent)
        }
        if (feedback.votesCast.total > 0) {
            Spacer(Modifier.height(24.dp))
            RecapBigNumber(target = feedback.votesCast.total, running = visible, color = theme.onBackground)
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(Res.string.recap_feedback_votes_cast_label),
                    color = theme.onBackground.copy(alpha = 0.8f),
                    fontSize = 20.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            RevealItem(visible, 2) {
                VotePills(votes = feedback.votesCast, theme = theme)
            }
        }
        if (feedback.votesReceived.total > 0) {
            Spacer(Modifier.height(24.dp))
            RevealItem(visible, 3) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecapEyebrow(
                        text = stringResource(Res.string.recap_feedback_votes_received_label, formatCount(feedback.votesReceived.total)),
                        color = theme.accent.copy(alpha = 0.8f)
                    )
                    VotePills(votes = feedback.votesReceived, theme = theme)
                }
            }
        }

        val notes = buildList {
            if (feedback.commentsAllTime > 0) {
                add(stringResource(Res.string.recap_feedback_comments, formatCount(feedback.commentsThisYear), formatCount(feedback.commentsAllTime)))
            }
            if (feedback.devRepliesAllTime > 0) {
                add(stringResource(Res.string.recap_feedback_dev_replies, formatCount(feedback.devRepliesThisYear), formatCount(feedback.devRepliesAllTime)))
            }
        }
        if (notes.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            RevealItem(visible, 4) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.forEach { note ->
                        RecapBody(text = note, color = theme.onBackground.copy(alpha = 0.85f), fontSize = 17.sp)
                    }
                }
            }
        }
    }
}

/** One pill per vote kind that was used, e.g. "Upvote: 12". */
@Composable
private fun VotePills(votes: FeedbackVotesUi, theme: RecapPageTheme) {
    val kinds = listOf(
        Res.string.feedback_vote_up to votes.up,
        Res.string.feedback_vote_down to votes.down,
        Res.string.feedback_vote_didnt_know to votes.didntKnow,
        Res.string.feedback_vote_not_needed to votes.notNeeded,
    )
    PillFlow(
        pills = kinds.filter { (_, count) -> count > 0 }
            .map { (label, count) -> stringResource(Res.string.recap_label_count, stringResource(label), formatCount(count)) },
        theme = theme
    )
}

/** The feedback board's own label for a server status name, null for an unknown one. */
private fun feedbackStatusLabel(status: String): StringResource? = when (status) {
    "REQUESTED" -> Res.string.feedback_status_requested
    "PLANNED" -> Res.string.feedback_status_planned
    "IMPLEMENTED" -> Res.string.feedback_status_implemented
    "OPEN" -> Res.string.feedback_status_open
    "CONFIRMED" -> Res.string.feedback_status_confirmed
    "FIXED" -> Res.string.feedback_status_fixed
    else -> null
}

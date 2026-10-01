package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.poll

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.ComboInputField
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.poll_create
import schneaggchatv3mp.composeapp.generated.resources.poll_create_description
import schneaggchatv3mp.composeapp.generated.resources.poll_create_description_placeholder
import schneaggchatv3mp.composeapp.generated.resources.poll_create_screentitle
import schneaggchatv3mp.composeapp.generated.resources.poll_create_title
import schneaggchatv3mp.composeapp.generated.resources.poll_create_title_placeholder
import schneaggchatv3mp.composeapp.generated.resources.poll_expiry_title
import schneaggchatv3mp.composeapp.generated.resources.poll_expiry_title_info
import schneaggchatv3mp.composeapp.generated.resources.poll_expiry_title_withdate
import schneaggchatv3mp.composeapp.generated.resources.poll_option_maxvoters_withcount
import schneaggchatv3mp.composeapp.generated.resources.poll_options_error
import schneaggchatv3mp.composeapp.generated.resources.poll_options_placeholder
import schneaggchatv3mp.composeapp.generated.resources.poll_options_title
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowcustom
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowcustom_info
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowcustom_withcount
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowmultiple
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowmultiple_info
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowmultiple_withcount
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowdeleteoptions
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_allowdeleteoptions_info
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_infinite_custom_and_selected_answers_warning
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_infinite_custom_answers_warning
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_limitperentry
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_limitperentry_info
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_showcheckboxes
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_showcheckboxes_info
import schneaggchatv3mp.composeapp.generated.resources.poll_visibility_title
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_subpoll_visible_to_all
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_subpoll_visible_to_all_info
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_subpolls
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_subpolls_info
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_add
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_done
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_edit
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_error
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_option_count
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_remove
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_screentitle
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_shown_for
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_too_many_options
import schneaggchatv3mp.composeapp.generated.resources.poll_subpoll_untitled
import sh.calvin.reorderable.ReorderableColumn
import kotlin.time.Clock

/**
 * Mutable holder for a single poll option row so text, its per-entry vote limit and its sub poll
 * survive drag-to-reorder (identity-based, not index-based).
 */
class PollOptionInput(
    text: TextFieldValue = TextFieldValue(""),
    maxVoters: Int? = null,
    subPoll: PollDraft? = null,
) {
    var text by mutableStateOf(text)
    var maxVoters by mutableStateOf(maxVoters)
    var subPoll by mutableStateOf(subPoll)

    //Set by validation when this option's sub poll is incomplete, re-checked when leaving its editor
    var subPollError by mutableStateOf(false)
}

//Keep in sync with the server-side limits in ValidationUtils / MessageService (schneaggchatv3server)
private const val POLL_TITLE_MAX_LENGTH = 200
private const val POLL_DESCRIPTION_MAX_LENGTH = 500
private const val POLL_MAX_VOTE_OPTIONS = 20
private const val POLL_MAX_DEPTH = 5 //Poll levels in one message, the root poll included
private const val POLL_MAX_TREE_OPTIONS = 60 //Options of the root poll and all sub polls together

/**
 * Editable state of one poll level in the creator. A sub poll is another [PollDraft] hanging off
 * [PollOptionInput.subPoll]. Visibility and close date only exist on the root poll - sub polls
 * inherit them, so they are passed into [toRequest] instead of living here.
 */
class PollDraft(
    title: String = "",
    description: String = "",
    options: List<PollOptionInput> = listOf(PollOptionInput(), PollOptionInput()),
) {
    var title by mutableStateOf(TextFieldValue(title))
    var description by mutableStateOf(TextFieldValue(description))

    var allowCustomAnswers by mutableStateOf(false)
    var allowedCustomAnswerCount by mutableStateOf(1)

    var allowMultipleAnswers by mutableStateOf(false)
    var allowedAnswerCount by mutableStateOf(1)

    var limitVotersPerEntry by mutableStateOf(false)

    var showCheckboxes by mutableStateOf(true)
    var allowDeleteOptions by mutableStateOf(false)

    var subPollsEnabled by mutableStateOf(false)

    //Sub polls only: may users who did not pick the parent option still look at this poll
    var visibleToAll by mutableStateOf(true)

    val options = mutableStateListOf<PollOptionInput>().apply { addAll(options) }

    //Errors
    var titleError by mutableStateOf(false)
    var optionsError by mutableStateOf(false)

    /** Sub polls open by picking an option, which a list poll (no checkboxes) can't do. */
    val subPollsActive: Boolean
        get() = subPollsEnabled && showCheckboxes

    fun filledOptions(): List<PollOptionInput> = options.filter { it.text.text.trim().isNotEmpty() }

    fun isTitleValid(): Boolean = title.text.length >= 2

    //Only check for min input of two answers if no custom answers allowed
    fun areOptionsValid(): Boolean = allowCustomAnswers || filledOptions().size >= 2

    /** Options of this poll and every active sub poll below it. */
    fun totalOptionCount(): Int = filledOptions().sumOf { option ->
        1 + (if (subPollsActive) option.subPoll?.totalOptionCount() ?: 0 else 0)
    }

    /**
     * Checks this level and every active sub poll below it. With [markErrors] the problems are
     * flagged on the drafts/options so the editor can highlight them.
     */
    fun validate(markErrors: Boolean = true): Boolean {
        val titleValid = isTitleValid()
        val optionsValid = areOptionsValid()
        if (markErrors) {
            titleError = !titleValid
            optionsError = !optionsValid
        }

        var valid = titleValid && optionsValid
        if (subPollsActive) {
            filledOptions().forEach { option ->
                val subPoll = option.subPoll ?: return@forEach
                val subPollValid = subPoll.validate(markErrors)
                if (markErrors) option.subPollError = !subPollValid
                if (!subPollValid) valid = false
            }
        }
        return valid
    }

    fun toRequest(visibility: PollVisibility, closeDate: Long?): NetworkUtils.PollCreateRequest {
        val filledOptions = filledOptions()

        return NetworkUtils.PollCreateRequest(
            title = title.text,
            description = description.text.ifEmpty { null },
            //A list-mode poll (no checkboxes) never votes, so maxAnswers must stay null - the server rejects it otherwise
            maxAnswers = if (!showCheckboxes) null else if (allowMultipleAnswers) {
                if (allowedAnswerCount == 10) {
                    null
                } else if (allowCustomAnswers) {
                    //Custom answers add possibilities beyond the predefined options, so no need to clamp
                    allowedAnswerCount
                } else {
                    //Without custom answers, can't exceed the actual number of options, or the server rejects the poll
                    allowedAnswerCount.coerceAtMost(filledOptions.size)
                }
            } else 1,
            customAnswersEnabled = allowCustomAnswers,
            //Only meaningful (and only accepted by the server) when custom answers are enabled
            maxAllowedCustomAnswers = if (allowCustomAnswers) {
                if (allowedCustomAnswerCount == 10) null else allowedCustomAnswerCount
            } else null,
            visibility = visibility,
            closeDate = closeDate,
            voteOptions = filledOptions
                .map {
                    NetworkUtils.PollVoteOptionCreateRequest(
                        text = it.text.text,
                        //maxVoters is a voting concept - never sent on a list-mode poll
                        maxVoters = if (showCheckboxes && limitVotersPerEntry) it.maxVoters else null,
                        //Sub polls stay in the draft while the switch is off, but are only sent while it's on
                        subPoll = if (subPollsActive) it.subPoll?.toRequest(visibility, closeDate) else null,
                    )
                },
            allowDeleteOptions = allowDeleteOptions,
            showCheckboxes = showCheckboxes,
            visibleToAll = visibleToAll,
        )
    }
}

/** One step down the sub poll tree while editing: the sub poll [draft] of [option]. */
private class SubPollEditStep(val option: PollOptionInput, val draft: PollDraft)

/** What the creator currently shows - [depth] 0 is the root poll. */
private data class PollEditorLevel(val draft: PollDraft, val depth: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PollDialog(
    onDismiss: () -> Unit = {},
    onCreatePoll: (NetworkUtils.PollCreateRequest) -> Unit = {},
    initialDraft: PollDraft? = null, //Prefilled draft, used by previews
) {
    val rootDraft = remember { initialDraft ?: PollDraft() }

    var visibility by remember { mutableStateOf(PollVisibility.PUBLIC) }

    var expiresAt by remember { mutableStateOf<LocalDateTime?>(null) }
    var showExpiresAtDatePickerDialog by remember { mutableStateOf(false) }

    var tooManyOptionsError by remember { mutableStateOf(false) }

    //Sub polls being edited, outermost first. Empty = editing the root poll
    val editPath = remember { mutableStateListOf<SubPollEditStep>() }

    fun closeSubPoll() {
        val step = editPath.removeAt(editPath.lastIndex)
        //Clear the chip's error once the sub poll got fixed
        if (step.option.subPollError) {
            step.option.subPollError = !step.draft.validate(markErrors = false)
        }
    }

    fun goBack() {
        if (editPath.isNotEmpty()) closeSubPoll() else onDismiss()
    }

    fun openSubPoll(option: PollOptionInput) {
        val subPoll = option.subPoll ?: PollDraft().also { option.subPoll = it }
        editPath.add(SubPollEditStep(option, subPoll))
    }

    LaunchedEffect(rootDraft.totalOptionCount()) {
        if (tooManyOptionsError && rootDraft.totalOptionCount() <= POLL_MAX_TREE_OPTIONS) {
            tooManyOptionsError = false
        }
    }

    Dialog(
        onDismissRequest = { goBack() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)

        ) {
            val currentLevel = PollEditorLevel(
                draft = editPath.lastOrNull()?.draft ?: rootDraft,
                depth = editPath.size
            )

            //Opening a sub poll slides in from the right, going back slides out to the right
            AnimatedContent(
                targetState = currentLevel,
                transitionSpec = {
                    if (targetState.depth > initialState.depth) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                    }
                },
            ) { level ->
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {

                    //Title to navigate back for ios users
                    ActivityTitle(
                        title = stringResource(
                            if (level.depth == 0) Res.string.poll_create_screentitle else Res.string.poll_subpoll_screentitle
                        ),
                        onBackClick = { goBack() }
                    )

                    if (level.depth > 0) {
                        val steps = editPath.take(level.depth)

                        SubPollBreadcrumb(
                            rootTitle = rootDraft.title.text,
                            optionTexts = steps.map { it.option.text.text },
                            onCrumbClick = { depth ->
                                while (editPath.size > depth) closeSubPoll()
                            }
                        )

                        steps.lastOrNull()?.let { step ->
                            Spacer(modifier = Modifier.height(8.dp))
                            SubPollInfoBanner(optionText = step.option.text.text)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PollDraftEditor(
                        draft = level.draft,
                        isSubPoll = level.depth > 0,
                        canAddSubPolls = level.depth + 1 < POLL_MAX_DEPTH,
                        onEditSubPoll = { option -> openSubPoll(option) }
                    )

                    if (level.depth == 0) {
                        //Answer visibility - shared by all sub polls
                        var dropDownMenuShown by remember { mutableStateOf(false) }

                        NormalButton(
                            text = stringResource(Res.string.poll_visibility_title, visibility.toUiText().asString()),
                            onClick = {
                                dropDownMenuShown = true
                            },
                            primary = false,
                            showOutline = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column {
                            DropdownMenu(
                                expanded = dropDownMenuShown,
                                onDismissRequest = {dropDownMenuShown = false},
                                modifier = Modifier.fillMaxWidth(),
                                scrollState = rememberScrollState(),
                            ) {
                                PollVisibility.entries.forEach { entry ->
                                    DropdownMenuItem(
                                        onClick = {
                                            visibility = entry
                                            dropDownMenuShown = false
                                        },
                                        text = {
                                            Text(
                                                text = entry.toUiText().asString()
                                            )
                                        },
                                    )
                                }
                            }
                        }


                        Spacer(modifier = Modifier.height(16.dp))


                        //Timer for poll expiration - shared by all sub polls
                        SettingsSwitch(
                            modifier = Modifier.fillMaxWidth(),
                            titletext = if (expiresAt == null) {stringResource(Res.string.poll_expiry_title)} else {
                                stringResource(Res.string.poll_expiry_title_withdate, "${expiresAt!!.day.toString().padStart(2, '0')}.${expiresAt!!.month.ordinal.toString().padStart(2, '0')}.${expiresAt!!.year} ${expiresAt!!.hour.toString().padStart(2, '0')}:${expiresAt!!.minute.toString().padStart(2, '0')}")
                            },
                            infotext = stringResource(Res.string.poll_expiry_title_info),
                            switchchecked = expiresAt != null,
                            onSwitchChange = {
                                if (expiresAt != null) {
                                    expiresAt = null
                                } else {
                                    showExpiresAtDatePickerDialog = true
                                }
                                             },
                            icon = null
                        )
                        //Popup for date picker
                        if (showExpiresAtDatePickerDialog) {
                            AlertDialog(
                                onDismissRequest = { showExpiresAtDatePickerDialog = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        // Handle confirm action
                                        showExpiresAtDatePickerDialog = false
                                    }) {
                                        Text(stringResource(Res.string.ok))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showExpiresAtDatePickerDialog = false
                                        expiresAt = null
                                    }) {
                                        Text(stringResource(Res.string.cancel))
                                    }
                                },
                                text = {
                                    WheelDateTimePicker(
                                        modifier = Modifier.fillMaxWidth(),
                                        rowCount = 3,
                                        minDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                                        textColor = MaterialTheme.colorScheme.onSurface,
                                        selectorProperties = WheelPickerDefaults.selectorProperties(
                                            enabled = true,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        ),
                                        onSnappedDateTime = {
                                            expiresAt = it
                                        }
                                    )
                                }
                            )
                        }


                        Spacer(modifier = Modifier.height(16.dp))

                        if (tooManyOptionsError) {
                            Text(
                                text = stringResource(Res.string.poll_subpoll_too_many_options, POLL_MAX_TREE_OPTIONS.toString()),
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        //Submit / Cancel row
                        Row(
                            verticalAlignment = Alignment.CenterVertically

                        ) {
                            NormalButton(
                                onClick = { onDismiss() },
                                text = stringResource(Res.string.cancel),
                                primary = false,
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            NormalButton(
                                onClick = {
                                    if (!rootDraft.validate()) return@NormalButton
                                    if (rootDraft.totalOptionCount() > POLL_MAX_TREE_OPTIONS) {
                                        tooManyOptionsError = true
                                        return@NormalButton
                                    }

                                    onCreatePoll(
                                        rootDraft.toRequest(
                                            visibility = visibility,
                                            closeDate = expiresAt?.toInstant(TimeZone.currentSystemDefault())
                                                ?.toEpochMilliseconds(),
                                        )
                                    )

                                    onDismiss()
                                },
                                text = stringResource(Res.string.poll_create),
                            )
                        }
                    } else {
                        //Sub poll: back to the level above (the whole tree is sent from the root)
                        Row {
                            Spacer(modifier = Modifier.weight(1f))
                            NormalButton(
                                onClick = { goBack() },
                                text = stringResource(Res.string.poll_subpoll_done),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Title, description, options and settings of one poll level. Used for the root poll and, with
 * [isSubPoll], for every sub poll (which then shows its visibility switch instead of the root-only
 * settings the caller adds).
 */
@Composable
private fun PollDraftEditor(
    draft: PollDraft,
    isSubPoll: Boolean,
    canAddSubPolls: Boolean,
    onEditSubPoll: (PollOptionInput) -> Unit,
) {
    //Reset errors with launchedeffect
    LaunchedEffect(draft.title.text, draft.options.map { it.text.text }, draft.options.size) {
        if (draft.titleError && draft.isTitleValid()) {
            draft.titleError = false
        }

        if (draft.optionsError && draft.areOptionsValid()) {
            draft.optionsError = false
        }
    }

    Column {
        //Title text input
        ComboInputField(
            value = draft.title,
            onValueChange = { if (it.text.length <= POLL_TITLE_MAX_LENGTH) draft.title = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {Text(stringResource(Res.string.poll_create_title_placeholder))},
            label = {Text(stringResource(Res.string.poll_create_title))},
            isError = draft.titleError
        )

        Spacer(modifier = Modifier.height(16.dp))


        //Description text input
        Text(stringResource(Res.string.poll_create_description))

        ComboInputField(
            value = draft.description,
            onValueChange = { if (it.text.length <= POLL_DESCRIPTION_MAX_LENGTH) draft.description = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.poll_create_description_placeholder)) },
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))


        //Poll options
        Text(text = stringResource(Res.string.poll_options_title))

        if (draft.optionsError) {
            Text(text = stringResource(Res.string.poll_options_error),
                color = MaterialTheme.colorScheme.error)
        }


        val hapticFeedback = LocalHapticFeedback.current
        val options = draft.options

        ReorderableColumn(
            list = options.toList(),
            onSettle = { from, to ->
                options.add(to, options.removeAt(from))
                hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            },
        ) { _, value, _ ->
            Spacer(modifier = Modifier.height(8.dp))

            ReorderableItem(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ComboInputField(
                            value = value.text,
                            onValueChange = { value.text = it },
                            placeholder = { Text(stringResource(Res.string.poll_options_placeholder)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (draft.limitVotersPerEntry && value.text.text.isNotEmpty()) {
                            val sliderValue = value.maxVoters ?: 10

                            Text(
                                text = stringResource(
                                    Res.string.poll_option_maxvoters_withcount,
                                    if (sliderValue in 1..9) sliderValue.toString() else "∞"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Slider(
                                value = sliderValue.toFloat(),
                                onValueChange = { value.maxVoters = it.toInt().let { count -> if (count == 10) null else count } },
                                valueRange = 1f..10f,
                                steps = 9,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, bottom = 4.dp)
                            )
                        }

                        //Sub poll of this option: a chip once there is one, otherwise an add button
                        if (draft.subPollsActive && value.text.text.isNotBlank()) {
                            val subPoll = value.subPoll
                            if (subPoll != null) {
                                SubPollDraftChip(
                                    subPoll = subPoll,
                                    isError = value.subPollError,
                                    onEdit = { onEditSubPoll(value) },
                                    onRemove = {
                                        value.subPoll = null
                                        value.subPollError = false
                                    },
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            } else if (canAddSubPolls) {
                                TextButton(
                                    onClick = { onEditSubPoll(value) },
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SubdirectoryArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(Res.string.poll_subpoll_add))
                                }
                            }
                        }
                    }

                    IconButton(
                        modifier = Modifier.draggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            },
                            onDragStopped = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            },
                        ),
                        onClick = {},
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "Reorder",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        //Auto add new entry if last entry is not empty
        if (options.size < POLL_MAX_VOTE_OPTIONS && options[options.size-1].text.text.isNotEmpty()) {
            options.add(PollOptionInput())
        }


        Spacer(modifier = Modifier.height(24.dp))


        //settings

        //Sub poll only: whether people who did not pick the parent option can look at it
        if (isSubPoll) {
            SettingsSwitch(
                modifier = Modifier.fillMaxWidth(),
                titletext = stringResource(Res.string.poll_settings_subpoll_visible_to_all),
                infotext = stringResource(Res.string.poll_settings_subpoll_visible_to_all_info),
                switchchecked = draft.visibleToAll,
                onSwitchChange = { draft.visibleToAll = it },
                icon = null
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        //Whether the poll shows checkboxes/radio buttons and accepts votes, or is a plain read-only list
        SettingsSwitch(
            modifier = Modifier.fillMaxWidth(),
            titletext = stringResource(Res.string.poll_settings_showcheckboxes),
            infotext = stringResource(Res.string.poll_settings_showcheckboxes_info),
            switchchecked = draft.showCheckboxes,
            onSwitchChange = {
                draft.showCheckboxes = it
                if (!draft.showCheckboxes) {
                    //Voting-only settings are meaningless without checkboxes
                    draft.allowMultipleAnswers = false
                    draft.limitVotersPerEntry = false
                }
            },
            icon = null
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (draft.showCheckboxes) {
            SettingsSwitch(
                titletext = if (!draft.allowMultipleAnswers) {
                    stringResource(Res.string.poll_settings_allowmultiple)
                }
                else if (draft.allowedAnswerCount in 1..9) {
                    stringResource(
                        Res.string.poll_settings_allowmultiple_withcount, //TODO: Quantity string?? für 1 answer: https://developer.android.com/guide/topics/resources/string-resource
                        draft.allowedAnswerCount.toString()
                    )
                }
                else {
                    stringResource(Res.string.poll_settings_allowmultiple_withcount, "∞")
                },
                infotext = stringResource(Res.string.poll_settings_allowmultiple_info),
                switchchecked = draft.allowMultipleAnswers,
                onSwitchChange = { draft.allowMultipleAnswers = it},
                icon = null,
                modifier = Modifier.fillMaxWidth()
            )
            if (draft.allowMultipleAnswers) {
                Slider(
                    value = draft.allowedAnswerCount.toFloat(),
                    onValueChange = {draft.allowedAnswerCount = it.toInt()},
                    valueRange = 1f..10f,
                    steps = 9,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        SettingsSwitch(
            modifier = Modifier.fillMaxWidth(),
            titletext = if (!draft.allowCustomAnswers) {
                stringResource(Res.string.poll_settings_allowcustom)
            }
            else if (draft.allowedCustomAnswerCount in 1..9) {
                stringResource(
                    Res.string.poll_settings_allowcustom_withcount,
                    draft.allowedCustomAnswerCount.toString()
                )
            }
            else {
                stringResource(Res.string.poll_settings_allowcustom_withcount, "∞")
            },
            infotext = stringResource(Res.string.poll_settings_allowcustom_info),
            switchchecked = draft.allowCustomAnswers,
            onSwitchChange = {
                draft.allowCustomAnswers = it
                if (draft.allowCustomAnswers) {
                    draft.allowedCustomAnswerCount = 1
                }
                             },
            icon = null
        )

        if (draft.allowCustomAnswers) {
            Slider(
                value = draft.allowedCustomAnswerCount.toFloat(),
                onValueChange = {draft.allowedCustomAnswerCount = it.toInt()},
                valueRange = 1f..10f,
                steps = 9,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        //Warning field if user selects both options
        if (draft.allowMultipleAnswers && draft.allowedCustomAnswerCount == 10 || draft.allowedCustomAnswerCount == 10) {

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .background(
                        color = Color(red = 255, green = 165, blue = 0),
                        shape = RoundedCornerShape(15.dp)
                    )
            ) {
                Text(
                    text = if (draft.allowMultipleAnswers) {
                        stringResource(Res.string.poll_settings_infinite_custom_and_selected_answers_warning)
                    } else stringResource(Res.string.poll_settings_infinite_custom_answers_warning),
                    textAlign = TextAlign.Center,
                )
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        if (draft.showCheckboxes) {
            SettingsSwitch(
                modifier = Modifier.fillMaxWidth(),
                titletext = stringResource(Res.string.poll_settings_limitperentry),
                infotext = stringResource(Res.string.poll_settings_limitperentry_info),
                switchchecked = draft.limitVotersPerEntry,
                onSwitchChange = { draft.limitVotersPerEntry = it },
                icon = null
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        //Whether the creator/option-creators may delete options after the poll is sent
        SettingsSwitch(
            modifier = Modifier.fillMaxWidth(),
            titletext = stringResource(Res.string.poll_settings_allowdeleteoptions),
            infotext = stringResource(Res.string.poll_settings_allowdeleteoptions_info),
            switchchecked = draft.allowDeleteOptions,
            onSwitchChange = { draft.allowDeleteOptions = it },
            icon = null
        )

        Spacer(modifier = Modifier.height(16.dp))

        //Follow-up polls per option - only on a votable poll, and not past the depth limit. Kept
        //visible while this level already has sub polls, so they can still be switched off
        if (draft.showCheckboxes && (canAddSubPolls || draft.options.any { it.subPoll != null })) {
            SettingsSwitch(
                modifier = Modifier.fillMaxWidth(),
                titletext = stringResource(Res.string.poll_settings_subpolls),
                infotext = stringResource(Res.string.poll_settings_subpolls_info),
                switchchecked = draft.subPollsEnabled,
                onSwitchChange = { draft.subPollsEnabled = it },
                icon = null
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Path from the root poll to the sub poll being edited; tapping an earlier step jumps back there. */
@Composable
private fun SubPollBreadcrumb(
    rootTitle: String,
    optionTexts: List<String>,
    onCrumbClick: (depth: Int) -> Unit,
) {
    val rootLabel = rootTitle.ifBlank { stringResource(Res.string.poll_create_screentitle) }
    val crumbs = listOf(rootLabel) + optionTexts

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        crumbs.forEachIndexed { index, crumb ->
            val isCurrent = index == crumbs.lastIndex

            if (index > 0) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = crumb,
                style = MaterialTheme.typography.labelLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 140.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = !isCurrent) { onCrumbClick(index) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/** Tells the creator who gets to see the sub poll they are editing. */
@Composable
private fun SubPollInfoBanner(optionText: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SubdirectoryArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = stringResource(Res.string.poll_subpoll_shown_for, optionText),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/** Compact summary of an option's sub poll in the creator, opens it for editing on tap. */
@Composable
private fun SubPollDraftChip(
    subPoll: PollDraft,
    isError: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onEdit)
            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SubdirectoryArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subPoll.title.text.ifBlank { stringResource(Res.string.poll_subpoll_untitled) },
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isError) {
                    stringResource(Res.string.poll_subpoll_error)
                } else {
                    stringResource(Res.string.poll_subpoll_option_count, subPoll.filledOptions().size.toString())
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(Res.string.poll_subpoll_edit),
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.poll_subpoll_remove),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PollDialogSubPollsPreview() {
    PollDialog(initialDraft = PollPreviewData.sampleDraft())
}

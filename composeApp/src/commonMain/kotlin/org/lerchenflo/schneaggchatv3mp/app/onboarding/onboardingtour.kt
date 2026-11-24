package org.lerchenflo.schneaggchatv3mp.app.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.lerchenflo.taptarget.FreeRoamBarPosition
import io.github.lerchenflo.taptarget.TapTargetTour
import io.github.lerchenflo.taptarget.TourControls
import io.github.lerchenflo.taptarget.TourDescription
import io.github.lerchenflo.taptarget.TourTitle
import io.github.lerchenflo.taptarget.tapTargetTour
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.ttt_controls_skip_section
import schneaggchatv3mp.composeapp.generated.resources.ttt_controls_skip_section_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_controls_skip_tour
import schneaggchatv3mp.composeapp.generated.resources.ttt_controls_skip_tour_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_question
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_question_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_show
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_skip
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_again
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_misc_reset_tour
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_misc_reset_tour_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_friend_chat
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_friend_chat_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_details_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_details_about_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_feedback_requests
import schneaggchatv3mp.composeapp.generated.resources.ttt_feedback_requests_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_feedback_create
import schneaggchatv3mp.composeapp.generated.resources.ttt_feedback_create_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_details
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_details_about
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_message_long_press
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_message_long_press_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_message_swipe
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_message_swipe_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_polls
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_polls_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_details_nickname
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_details_nickname_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_details_wake
import schneaggchatv3mp.composeapp.generated.resources.ttt_chat_details_wake_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_group_members
import schneaggchatv3mp.composeapp.generated.resources.ttt_group_members_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_group_timer
import schneaggchatv3mp.composeapp.generated.resources.ttt_group_timer_description
import schneaggchatv3mp.composeapp.generated.resources.notifications
import schneaggchatv3mp.composeapp.generated.resources.ttt_appearance_settings_go_back
import schneaggchatv3mp.composeapp.generated.resources.ttt_appearance_settings_language
import schneaggchatv3mp.composeapp.generated.resources.ttt_appearance_settings_language_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_appearance_settings_language_freeroam
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_events
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_events_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_feedback
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_feedback_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_games
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_map
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_map_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_continue
import schneaggchatv3mp.composeapp.generated.resources.ttt_events_create_button
import schneaggchatv3mp.composeapp.generated.resources.ttt_events_create_button_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_feedback_quiz
import schneaggchatv3mp.composeapp.generated.resources.ttt_feedback_quiz_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_difficulty
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_difficulty_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_global_highscore
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_global_highscore_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_random
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_random_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_initscreen
import schneaggchatv3mp.composeapp.generated.resources.ttt_initscreen_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_new_chat_create_group
import schneaggchatv3mp.composeapp.generated.resources.ttt_new_chat_create_group_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_new_chat_go_back
import schneaggchatv3mp.composeapp.generated.resources.ttt_new_chat_search_friends
import schneaggchatv3mp.composeapp.generated.resources.ttt_new_chat_search_friends_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_new_chat_search_friends_freeroam
import schneaggchatv3mp.composeapp.generated.resources.ttt_notification_settings_go_back
import schneaggchatv3mp.composeapp.generated.resources.ttt_schneaggmap_locations
import schneaggchatv3mp.composeapp.generated.resources.ttt_schneaggmap_locations_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_schneaggmap_locations_freeroam
import schneaggchatv3mp.composeapp.generated.resources.ttt_schneaggmap_settings
import schneaggchatv3mp.composeapp.generated.resources.ttt_schneaggmap_settings_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_schneaggmap_snailtrail_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_appearance
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_appearance_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_misc
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_misc_app_broken
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_misc_app_broken_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_notifications_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_privacy
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_privacy_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_user
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_user_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_wake
import schneaggchatv3mp.composeapp.generated.resources.ttt_user_settings_go_back
import schneaggchatv3mp.composeapp.generated.resources.ttt_user_settings_phone_number
import schneaggchatv3mp.composeapp.generated.resources.ttt_user_settings_phone_number_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_user_settings_phone_number_freeroam
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_wake_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_start_chatting
import schneaggchatv3mp.composeapp.generated.resources.ttt_start_chatting_description

/**
 * Tags of the steps that start by themselves the first time their target shows (new features, UI
 * that only exists later). One tag per screen part, so a part that is not there yet (e.g. no
 * message, not an admin) does not hold back the rest. Steps without tags only run in the full tour.
 * A step added to an existing screen later gets that screen's tag plus its own new tag.
 */
private object TourTags {
    const val CHAT = "chat"
    const val CHAT_MESSAGES = "chat_messages"
    const val USER_DETAILS = "user_details"
    const val GROUP_MEMBERS = "group_members"
    const val GROUP_TIMER = "group_timer"
    const val FEEDBACK_REQUESTS = "feedback_requests"
    const val FEEDBACK_CREATE = "feedback_create"
}

/**
 * Tags whose steps start by themselves on their screen. Off until the full tour was finished or
 * skipped once (a new user gets the start popup first) and until the seen tags are loaded.
 */
fun pendingTourTags(tour: TapTargetTour, onboardingSeen: Boolean, seenTags: Set<String>?): Set<String> =
    if (onboardingSeen && seenTags != null) tour.allTags - seenTags else emptySet()

/**
 * Color of the tour's spotlight outline and rings: the brightest of primary, tertiary and
 * inversePrimary, because the scrim is dark and a dark primary (e.g. the flos theme) hardly shows on it.
 */
@Composable
@ReadOnlyComposable
fun tourHighlightColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return listOf(scheme.primary, scheme.tertiary, scheme.inversePrimary).maxBy { it.luminance() }
}

@Composable
fun rememberOnboardingTour(isAndroid: Boolean, isDesktop: Boolean): TapTargetTour {
    return remember {
        tapTargetTour {

            //Show chatselector first
            infoStep(
                title = Res.string.ttt_initscreen,
                description = Res.string.ttt_initscreen_description,
                route = Route.ChatSelector
            )

            //The tour's own controls: end the tour, jump to the next part (taps on them do nothing here)
            highlightStep(
                id = TourControls.SKIP_TOUR,
                title = Res.string.ttt_controls_skip_tour,
                description = Res.string.ttt_controls_skip_tour_description,
            )

            highlightStep(
                id = TourControls.SKIP_SECTION,
                title = Res.string.ttt_controls_skip_section,
                description = Res.string.ttt_controls_skip_section_description,
            )

            section("add_friends") {
                //New chat button
                tapStep(
                    id = "chatselector_new_chat_button",
                    title = Res.string.ttt_start_chatting,
                    description = Res.string.ttt_start_chatting_description,
                    route = Route.ChatSelector
                )

                //Navigate to new chat screen to show features
                //The tap reaches the search field, so it is focused for the free roam step
                tapStep(
                    id = "new_chat_search_friends",
                    title = Res.string.ttt_new_chat_search_friends,
                    description = Res.string.ttt_new_chat_search_friends_description,
                    route = Route.NewChat,
                    passTapToTarget = true,
                )

                freeRoamStep(
                    title = Res.string.ttt_new_chat_search_friends_freeroam,
                    position = FreeRoamBarPosition.Bottom,
                    continueButtonText = Res.string.ttt_continue,
                    route = Route.NewChat
                )

                //Only informs about the button: "Got it" or a tap next to it continues, a tap on it does nothing
                highlightStep(
                    id = "new_chat_create_group",
                    title = Res.string.ttt_new_chat_create_group,
                    description = Res.string.ttt_new_chat_create_group_description,
                    route = Route.NewChat, //Set route in case the user navigated from the screen in the free roam
                )

                //Point to the back button to navigate back
                tapStep(
                    id = "new_chat_back_button",
                    title = Res.string.ttt_new_chat_go_back,
                    route = Route.NewChat
                )
            }

            section("chat") {
                //Open a chat with a friend for the chat and chat details steps. Only the first accepted
                //friend chat with messages is a target; without one, the full tour passes over these steps
                tapStep(
                    id = "chatselector_friend_chat",
                    title = Res.string.ttt_chatselector_friend_chat,
                    description = Res.string.ttt_chatselector_friend_chat_description,
                    route = Route.ChatSelector,
                    passTapToTarget = true, //The tap opens the chat
                    //Short wait: the list may still be coming back from the new chat screen. Without a
                    //friend chat, all chat steps below are passed over at once after it (dependsOn)
                    targetTimeout = 500.milliseconds,
                )

                // Chat and chat details: no route; without a friend chat they are passed over at once
                // (dependsOn). Outside the full tour they start by themselves the first time their target shows
                highlightStep(
                    id = "chat_message",
                    title = Res.string.ttt_chat_message_long_press,
                    description = Res.string.ttt_chat_message_long_press_description,
                    tags = setOf(TourTags.CHAT_MESSAGES),
                    dependsOn = "chatselector_friend_chat",
                    targetTimeout = 2.seconds, //The chat is still opening and loading its messages
                )

                highlightStep(
                    id = "chat_message",
                    title = Res.string.ttt_chat_message_swipe,
                    description = Res.string.ttt_chat_message_swipe_description,
                    tags = setOf(TourTags.CHAT_MESSAGES),
                    dependsOn = "chatselector_friend_chat",
                )

                highlightStep(
                    id = "chat_add_media",
                    title = Res.string.ttt_chat_polls,
                    description = Res.string.ttt_chat_polls_description,
                    tags = setOf(TourTags.CHAT),
                    dependsOn = "chatselector_friend_chat",
                )

                //The tap opens the chat details, where the next steps are
                tapStep(
                    id = "chat_partner",
                    title = Res.string.ttt_chat_details,
                    description = Res.string.ttt_chat_details_description,
                    tags = setOf(TourTags.CHAT),
                    passTapToTarget = true, //The tap opens the chat details
                    dependsOn = "chatselector_friend_chat",
                )

                highlightStep(
                    id = "chat_details_menu", //The ⋮ of the chat details, nickname and wake are in it
                    title = Res.string.ttt_chat_details_nickname,
                    description = Res.string.ttt_chat_details_nickname_description,
                    tags = setOf(TourTags.USER_DETAILS),
                    dependsOn = "chat_partner",
                    targetTimeout = 2.seconds, //The chat details are still opening
                )

                highlightStep(
                    id = "chat_details_description",
                    title = Res.string.ttt_chat_details_about,
                    description = Res.string.ttt_chat_details_about_description,
                    tags = setOf(TourTags.USER_DETAILS),
                    dependsOn = "chat_partner",
                )

                highlightStep(
                    id = "chat_details_menu", //The ⋮ of the chat details, nickname and wake are in it
                    title = Res.string.ttt_chat_details_wake,
                    description = Res.string.ttt_chat_details_wake_description,
                    tags = setOf(TourTags.USER_DETAILS),
                    dependsOn = "chat_partner",
                )
            }

            section("feedback") {
                //Open the feedback board from the chat selector
                tapStep(
                    id = "chatselector_feedback_button",
                    title = Res.string.ttt_chatselector_feedback,
                    description = Res.string.ttt_chatselector_feedback_description,
                    route = Route.ChatSelector
                )

                highlightStep(
                    id = "feedback_requests_tab",
                    title = Res.string.ttt_feedback_requests,
                    description = Res.string.ttt_feedback_requests_description,
                    route = Route.Feedback(),
                    tags = setOf(TourTags.FEEDBACK_REQUESTS),
                )

                //Only shown once the board has loaded, so it gets its own tag
                highlightStep(
                    id = "feedback_create_button",
                    title = Res.string.ttt_feedback_create,
                    description = Res.string.ttt_feedback_create_description,
                    route = Route.Feedback(),
                    tags = setOf(TourTags.FEEDBACK_CREATE),
                )

                highlightStep(
                    id = "feedback_feature_quiz_button",
                    title = Res.string.ttt_feedback_quiz,
                    description = Res.string.ttt_feedback_quiz_description,
                    route = Route.Feedback()
                )
            }

            section("map") {
                //Map, mobile only
                if (!isDesktop) {
                    //Go to map via bottom nav
                    tapStep(
                        id = "bottombar_map_button",
                        title = Res.string.ttt_chatselector_map,
                        description = Res.string.ttt_chatselector_map_description,
                        route = Route.ChatSelector
                    )

                    //Places and snail trails both live in this dropdown, so one step explains both
                    highlightStep(
                        id = "schneaggmap_location_dropdown",
                        route = Route.Schneaggmap(),
                    ) {
                        TourTitle(stringResource(Res.string.ttt_schneaggmap_locations))
                        TourDescription(stringResource(Res.string.ttt_schneaggmap_locations_description))
                        TourDescription(stringResource(Res.string.ttt_schneaggmap_snailtrail_description))
                    }

                    freeRoamStep(
                        title = Res.string.ttt_schneaggmap_locations_freeroam,
                        position = FreeRoamBarPosition.Bottom,
                        continueButtonText = Res.string.ttt_continue,
                        route = Route.Schneaggmap()
                    )

                    highlightStep(
                        id = "schneaggmap_settings_button",
                        title = Res.string.ttt_schneaggmap_settings,
                        description = Res.string.ttt_schneaggmap_settings_description,
                        route = Route.Schneaggmap(), //Navigate to map in case the user navigated back while freeroaming
                    )
                }
            }

            section("events") {
                //Straight to the events tab (no detour over the chat list, the tab switches made the
                //pager jump back and forth); points at its bar button there
                highlightStep(
                    id = "bottombar_events_button",
                    title = Res.string.ttt_chatselector_events,
                    description = Res.string.ttt_chatselector_events_description,
                    route = Route.Events()
                )

                highlightStep(
                    id = "events_create_button",
                    title = Res.string.ttt_events_create_button,
                    description = Res.string.ttt_events_create_button_description,
                    route = Route.Events()
                )
            }

            section("settings") {
                //Show the settings
                tapStep(
                    id = "chatselector_settings_button",
                    title = Res.string.ttt_settings,
                    route = Route.ChatSelector
                )

                tapStep(
                    id = "settings_user",
                    title = Res.string.ttt_settings_user,
                    description = Res.string.ttt_settings_user_description,
                    route = Route.SettingsScreen
                )

                highlightStep(
                    id = "user_settings_phone_number",
                    title = Res.string.ttt_user_settings_phone_number,
                    description = Res.string.ttt_user_settings_phone_number_description,
                    route = Route.UserSettings
                )

                freeRoamStep(
                    title = Res.string.ttt_user_settings_phone_number_freeroam,
                    position = FreeRoamBarPosition.Top, //At the bottom it would cover the field while typing
                    continueButtonText = Res.string.ttt_continue,
                    route = Route.UserSettings
                )

                //Point to back button so we return to the settings list before the next step
                tapStep(
                    id = "user_settings_back_button",
                    title = Res.string.ttt_user_settings_go_back,
                    route = Route.UserSettings
                )

                highlightStep(
                    id = "settings_privacy",
                    title = Res.string.ttt_settings_privacy,
                    description = Res.string.ttt_settings_privacy_description,
                    route = Route.SettingsScreen
                )

                if (isAndroid) {
                    tapStep(
                        id = "settings_notifications",
                        title = Res.string.notifications,
                        description = Res.string.ttt_settings_notifications_description,
                        route = Route.SettingsScreen
                    )

                    highlightStep(
                        id = "settings_notifications_wake",
                        title = Res.string.ttt_settings_wake,
                        description = Res.string.ttt_settings_wake_description,
                        route = Route.NotificationSettings
                    )

                    //Point to back button
                    tapStep(
                        id = "notification_settings_back_button",
                        title = Res.string.ttt_notification_settings_go_back,
                        route = Route.NotificationSettings
                    )
                } else {
                    highlightStep(
                        id = "settings_notifications",
                        title = Res.string.notifications,
                        description = Res.string.ttt_settings_notifications_description,
                        route = Route.SettingsScreen
                    )
                }

                tapStep(
                    id = "settings_appearance",
                    title = Res.string.ttt_settings_appearance,
                    description = Res.string.ttt_settings_appearance_description,
                    route = Route.SettingsScreen,
                )

                highlightStep(
                    id = "appearance_settings_language",
                    title = Res.string.ttt_appearance_settings_language,
                    description = Res.string.ttt_appearance_settings_language_description,
                    route = Route.AppearanceSettings
                )

                freeRoamStep(
                    title = Res.string.ttt_appearance_settings_language_freeroam,
                    position = FreeRoamBarPosition.Bottom,
                    continueButtonText = Res.string.ttt_continue,
                    route = Route.AppearanceSettings
                )

                //Point to back button so we return to the settings list before the next step
                tapStep(
                    id = "appearance_settings_back_button",
                    title = Res.string.ttt_appearance_settings_go_back,
                    route = Route.AppearanceSettings
                )
            }

            //Optional: the user is asked first and can skip the whole part
            optionalSection(
                name = "games",
                title = Res.string.ttt_games_question,
                description = Res.string.ttt_games_question_description,
                showButtonText = Res.string.ttt_games_show,
                skipButtonText = Res.string.ttt_games_skip,
            ) {
                //Mobile opens the games from the bottom bar
                if (!isDesktop) {
                    tapStep(
                        id = "bottombar_games_button",
                        title = Res.string.ttt_chatselector_games,
                        route = Route.ChatSelector
                    )
                }

                highlightStep(
                    id = "games_difficulty_selector",
                    title = Res.string.ttt_games_difficulty,
                    description = Res.string.ttt_games_difficulty_description,
                    route = Route.GamesSelector
                )

                highlightStep(
                    id = "games_global_ranking_button",
                    title = Res.string.ttt_games_global_highscore,
                    description = Res.string.ttt_games_global_highscore_description,
                    route = Route.GamesSelector
                )

                highlightStep(
                    id = "games_random_button",
                    title = Res.string.ttt_games_random,
                    description = Res.string.ttt_games_random_description,
                    route = Route.GamesSelector
                )
            }

            section("misc") {
                //Misc settings come last, so the tour ends at "Reset Tour"
                tapStep(
                    id = "chatselector_settings_button",
                    title = Res.string.ttt_settings_again,
                    route = Route.ChatSelector
                )

                tapStep(
                    id = "settings_misc",
                    title = Res.string.ttt_settings_misc,
                    route = Route.SettingsScreen
                )

                highlightStep(
                    id = "settings_misc_app_broken",
                    title = Res.string.ttt_settings_misc_app_broken,
                    description = Res.string.ttt_settings_misc_app_broken_description,
                    route = Route.MiscSettings
                )

                //Last step: thanks, and where the tour can be started again
                highlightStep(
                    id = "settings_misc_reset_tour",
                    title = Res.string.ttt_settings_misc_reset_tour,
                    description = Res.string.ttt_settings_misc_reset_tour_description,
                    route = Route.MiscSettings
                )
            }

            //Group details: the full tour opens a friend chat, so these only start on their own screen
            notInFullTour {
                highlightStep(
                    id = "chat_details_group_members",
                    title = Res.string.ttt_group_members,
                    description = Res.string.ttt_group_members_description,
                    tags = setOf(TourTags.GROUP_MEMBERS),
                )

                //Only shown to admins, or to everyone once a timer is set
                highlightStep(
                    id = "chat_details_group_timer",
                    title = Res.string.ttt_group_timer,
                    description = Res.string.ttt_group_timer_description,
                    tags = setOf(TourTags.GROUP_TIMER),
                )
            }

            // step(id = "settings",  title = "Settings",       description = "Adjust your preferences here")
        }
    }
}
package org.lerchenflo.schneaggchatv3mp.app.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.lerchenflo.taptarget.FreeRoamBarPosition
import io.github.lerchenflo.taptarget.TapTargetTour
import io.github.lerchenflo.taptarget.TourDescription
import io.github.lerchenflo.taptarget.TourTitle
import io.github.lerchenflo.taptarget.tapTargetTour
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.notifications
import schneaggchatv3mp.composeapp.generated.resources.ttt_appearance_settings_go_back
import schneaggchatv3mp.composeapp.generated.resources.ttt_appearance_settings_language
import schneaggchatv3mp.composeapp.generated.resources.ttt_appearance_settings_language_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_appearance_settings_language_freeroam
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_events
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_events_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_games
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_map
import schneaggchatv3mp.composeapp.generated.resources.ttt_chatselector_map_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_continue
import schneaggchatv3mp.composeapp.generated.resources.ttt_events_create_button
import schneaggchatv3mp.composeapp.generated.resources.ttt_events_create_button_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_difficulty
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_difficulty_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_global_highscore
import schneaggchatv3mp.composeapp.generated.resources.ttt_games_global_highscore_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_initscreen
import schneaggchatv3mp.composeapp.generated.resources.ttt_initscreen_description
import schneaggchatv3mp.composeapp.generated.resources.ttt_misc_settings_go_back
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
import schneaggchatv3mp.composeapp.generated.resources.ttt_settings_misc_bugreport
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
import schneaggchatv3mp.composeapp.generated.resources.ttt_thank_you
import schneaggchatv3mp.composeapp.generated.resources.ttt_thank_you_description

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
                position = FreeRoamBarPosition.Bottom,
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

            tapStep(
                id = "settings_misc",
                title = Res.string.ttt_settings_misc,
                route = Route.SettingsScreen
            )

            highlightStep(
                id = "settings_misc_bugreport",
                description = Res.string.ttt_settings_misc_bugreport,
                route = Route.MiscSettings
            )

            highlightStep(
                id = "settings_misc_app_broken",
                title = Res.string.ttt_settings_misc_app_broken,
                description = Res.string.ttt_settings_misc_app_broken_description,
                route = Route.MiscSettings
            )

            //Point to back button
            tapStep(
                id = "misc_settings_back_button",
                title = Res.string.ttt_misc_settings_go_back,
                route = Route.MiscSettings
            )

            //Go to events via bottom nav
            tapStep(
                id = "bottombar_events_button",
                title = Res.string.ttt_chatselector_events,
                description = Res.string.ttt_chatselector_events_description,
                route = Route.ChatSelector
            )

            highlightStep(
                id = "events_create_button",
                title = Res.string.ttt_events_create_button,
                description = Res.string.ttt_events_create_button_description,
                route = Route.Events()
            )

            //Mobile only
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



                tapStep(
                    id = "bottombar_games_button",
                    title = Res.string.ttt_chatselector_games,
                    route = Route.Schneaggmap()
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

            infoStep(
                title = Res.string.ttt_thank_you,
                description = Res.string.ttt_thank_you_description,
                route = Route.ChatSelector
            )

            // step(id = "settings",  title = "Settings",       description = "Adjust your preferences here")
        }
    }
}
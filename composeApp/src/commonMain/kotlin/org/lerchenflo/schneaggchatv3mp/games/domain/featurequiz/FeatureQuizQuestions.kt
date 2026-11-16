package org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz

import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_annotations_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_annotations_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_annotations_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_annotations_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_annotations_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_annotations_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_birthdays_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_birthdays_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_birthdays_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_birthdays_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_birthdays_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_birthdays_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_c_challenge_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_c_challenge_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_c_challenge_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_c_challenge_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_c_challenge_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_c_challenge_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_changelog_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_changelog_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_changelog_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_changelog_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_changelog_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_changelog_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_coin_flip_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_coin_flip_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_coin_flip_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_coin_flip_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_coin_flip_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_coin_flip_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_crossword_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_crossword_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_crossword_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_crossword_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_crossword_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_crossword_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_drafts_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_drafts_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_drafts_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_drafts_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_drafts_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_drafts_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feature_tour_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feature_tour_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feature_tour_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feature_tour_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feature_tour_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feature_tour_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feedback_board_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feedback_board_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feedback_board_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feedback_board_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feedback_board_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_feedback_board_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_finger_picker_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_finger_picker_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_finger_picker_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_finger_picker_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_finger_picker_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_finger_picker_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_fuel_mix_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_fuel_mix_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_fuel_mix_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_fuel_mix_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_fuel_mix_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_fuel_mix_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_game_2048_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_game_2048_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_game_2048_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_game_2048_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_game_2048_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_game_2048_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_games_random_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_games_random_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_games_random_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_games_random_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_games_random_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_games_random_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_grid_rush_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_grid_rush_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_grid_rush_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_grid_rush_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_grid_rush_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_grid_rush_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_groups_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_groups_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_groups_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_groups_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_groups_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_groups_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_image_messages_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_image_messages_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_image_messages_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_image_messages_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_image_messages_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_image_messages_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_location_sharing_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_location_sharing_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_location_sharing_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_location_sharing_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_location_sharing_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_location_sharing_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_map_places_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_map_places_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_map_places_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_map_places_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_map_places_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_map_places_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_message_search_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_message_search_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_message_search_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_message_search_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_message_search_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_message_search_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_morse_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_morse_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_morse_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_morse_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_morse_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_morse_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_notifications_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_notifications_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_notifications_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_notifications_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_notifications_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_notifications_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_odd_one_out_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_odd_one_out_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_odd_one_out_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_odd_one_out_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_odd_one_out_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_odd_one_out_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_online_status_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_online_status_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_online_status_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_online_status_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_online_status_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_online_status_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_phone_number_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_phone_number_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_phone_number_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_phone_number_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_phone_number_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_phone_number_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_read_receipts_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_read_receipts_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_read_receipts_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_read_receipts_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_read_receipts_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_read_receipts_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_rodeo_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_rodeo_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_rodeo_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_rodeo_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_rodeo_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_rodeo_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_schneaggahus_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_schneaggahus_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_schneaggahus_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_schneaggahus_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_schneaggahus_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_schneaggahus_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_settings_sync_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_settings_sync_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_settings_sync_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_settings_sync_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_settings_sync_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_settings_sync_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_shared_content_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_shared_content_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_shared_content_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_shared_content_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_shared_content_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_shared_content_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_system_messages_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_system_messages_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_system_messages_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_system_messages_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_system_messages_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_system_messages_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tetris_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tetris_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tetris_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tetris_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tetris_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tetris_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_themes_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_themes_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_themes_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_themes_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_themes_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_themes_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_undercover_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_undercover_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_undercover_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_undercover_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_undercover_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_undercover_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_user_descriptions_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_user_descriptions_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_user_descriptions_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_user_descriptions_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_user_descriptions_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_user_descriptions_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_voice_messages_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_voice_messages_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_voice_messages_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_voice_messages_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_voice_messages_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_voice_messages_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_yahtzee_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_yahtzee_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_yahtzee_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_yahtzee_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_yahtzee_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_yahtzee_question

/**
 * The questions of the feature quiz. The first answer is the right one. [FeatureQuizQuestion.seedKey]
 * must be a `seedKey` of the server's feedback seed file (`seed/feedback_features.json`), or the
 * question is left out of every round.
 */
object FeatureQuizQuestions {
    val ALL: List<FeatureQuizQuestion> = listOf(
        FeatureQuizQuestion(
            seedKey = "polls-custom-answers",
            question = Res.string.featurequiz_polls_subpoll_question,
            answers = listOf(Res.string.featurequiz_polls_subpoll_answer_1, Res.string.featurequiz_polls_subpoll_answer_2, Res.string.featurequiz_polls_subpoll_answer_3),
            noFeatureAnswer = Res.string.featurequiz_polls_subpoll_none,
            explanation = Res.string.featurequiz_polls_subpoll_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "language-vorarlbergerisch",
            question = Res.string.featurequiz_language_question,
            answers = listOf(Res.string.featurequiz_language_answer_1, Res.string.featurequiz_language_answer_2, Res.string.featurequiz_language_answer_3),
            noFeatureAnswer = Res.string.featurequiz_language_none,
            explanation = Res.string.featurequiz_language_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "wake-feature",
            question = Res.string.featurequiz_wake_question,
            answers = listOf(Res.string.featurequiz_wake_answer_1, Res.string.featurequiz_wake_answer_2, Res.string.featurequiz_wake_answer_3),
            noFeatureAnswer = Res.string.featurequiz_wake_none,
            explanation = Res.string.featurequiz_wake_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "events",
            question = Res.string.featurequiz_event_reminders_question,
            answers = listOf(Res.string.featurequiz_event_reminders_answer_1, Res.string.featurequiz_event_reminders_answer_2, Res.string.featurequiz_event_reminders_answer_3),
            noFeatureAnswer = Res.string.featurequiz_event_reminders_none,
            explanation = Res.string.featurequiz_event_reminders_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "events",
            question = Res.string.featurequiz_event_title_question,
            answers = listOf(Res.string.featurequiz_event_title_answer_1, Res.string.featurequiz_event_title_answer_2, Res.string.featurequiz_event_title_answer_3),
            noFeatureAnswer = Res.string.featurequiz_event_title_none,
            explanation = Res.string.featurequiz_event_title_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-wordle",
            question = Res.string.featurequiz_wordle_question,
            answers = listOf(Res.string.featurequiz_wordle_answer_1, Res.string.featurequiz_wordle_answer_2, Res.string.featurequiz_wordle_answer_3),
            noFeatureAnswer = Res.string.featurequiz_wordle_none,
            explanation = Res.string.featurequiz_wordle_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-tictactoe",
            question = Res.string.featurequiz_tictactoe_question,
            answers = listOf(Res.string.featurequiz_tictactoe_answer_1, Res.string.featurequiz_tictactoe_answer_2, Res.string.featurequiz_tictactoe_answer_3),
            noFeatureAnswer = Res.string.featurequiz_tictactoe_none,
            explanation = Res.string.featurequiz_tictactoe_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-dartcounter",
            question = Res.string.featurequiz_dartcounter_question,
            answers = listOf(Res.string.featurequiz_dartcounter_answer_1, Res.string.featurequiz_dartcounter_answer_2, Res.string.featurequiz_dartcounter_answer_3),
            noFeatureAnswer = Res.string.featurequiz_dartcounter_none,
            explanation = Res.string.featurequiz_dartcounter_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "app-broken-button",
            question = Res.string.featurequiz_app_broken_question,
            answers = listOf(Res.string.featurequiz_app_broken_answer_1, Res.string.featurequiz_app_broken_answer_2, Res.string.featurequiz_app_broken_answer_3),
            noFeatureAnswer = Res.string.featurequiz_app_broken_none,
            explanation = Res.string.featurequiz_app_broken_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "nicknames",
            question = Res.string.featurequiz_nicknames_question,
            answers = listOf(Res.string.featurequiz_nicknames_answer_1, Res.string.featurequiz_nicknames_answer_2, Res.string.featurequiz_nicknames_answer_3),
            noFeatureAnswer = Res.string.featurequiz_nicknames_none,
            explanation = Res.string.featurequiz_nicknames_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "group-extensions",
            question = Res.string.featurequiz_group_extensions_question,
            answers = listOf(Res.string.featurequiz_group_extensions_answer_1, Res.string.featurequiz_group_extensions_answer_2, Res.string.featurequiz_group_extensions_answer_3),
            noFeatureAnswer = Res.string.featurequiz_group_extensions_none,
            explanation = Res.string.featurequiz_group_extensions_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "message-reactions",
            question = Res.string.featurequiz_reactions_question,
            answers = listOf(Res.string.featurequiz_reactions_answer_1, Res.string.featurequiz_reactions_answer_2, Res.string.featurequiz_reactions_answer_3),
            noFeatureAnswer = Res.string.featurequiz_reactions_none,
            explanation = Res.string.featurequiz_reactions_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "quick-reactions",
            question = Res.string.featurequiz_quick_reactions_question,
            answers = listOf(Res.string.featurequiz_quick_reactions_answer_1, Res.string.featurequiz_quick_reactions_answer_2, Res.string.featurequiz_quick_reactions_answer_3),
            noFeatureAnswer = Res.string.featurequiz_quick_reactions_none,
            explanation = Res.string.featurequiz_quick_reactions_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "user-descriptions",
            question = Res.string.featurequiz_user_descriptions_question,
            answers = listOf(Res.string.featurequiz_user_descriptions_answer_1, Res.string.featurequiz_user_descriptions_answer_2, Res.string.featurequiz_user_descriptions_answer_3),
            noFeatureAnswer = Res.string.featurequiz_user_descriptions_none,
            explanation = Res.string.featurequiz_user_descriptions_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "birthdays",
            question = Res.string.featurequiz_birthdays_question,
            answers = listOf(Res.string.featurequiz_birthdays_answer_1, Res.string.featurequiz_birthdays_answer_2, Res.string.featurequiz_birthdays_answer_3),
            noFeatureAnswer = Res.string.featurequiz_birthdays_none,
            explanation = Res.string.featurequiz_birthdays_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "location-sharing",
            question = Res.string.featurequiz_location_sharing_question,
            answers = listOf(Res.string.featurequiz_location_sharing_answer_1, Res.string.featurequiz_location_sharing_answer_2, Res.string.featurequiz_location_sharing_answer_3),
            noFeatureAnswer = Res.string.featurequiz_location_sharing_none,
            explanation = Res.string.featurequiz_location_sharing_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "schneaggmap-places",
            question = Res.string.featurequiz_map_places_question,
            answers = listOf(Res.string.featurequiz_map_places_answer_1, Res.string.featurequiz_map_places_answer_2, Res.string.featurequiz_map_places_answer_3),
            noFeatureAnswer = Res.string.featurequiz_map_places_none,
            explanation = Res.string.featurequiz_map_places_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "annotations",
            question = Res.string.featurequiz_annotations_question,
            answers = listOf(Res.string.featurequiz_annotations_answer_1, Res.string.featurequiz_annotations_answer_2, Res.string.featurequiz_annotations_answer_3),
            noFeatureAnswer = Res.string.featurequiz_annotations_none,
            explanation = Res.string.featurequiz_annotations_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "games",
            question = Res.string.featurequiz_games_random_question,
            answers = listOf(Res.string.featurequiz_games_random_answer_1, Res.string.featurequiz_games_random_answer_2, Res.string.featurequiz_games_random_answer_3),
            noFeatureAnswer = Res.string.featurequiz_games_random_none,
            explanation = Res.string.featurequiz_games_random_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-schneagg-rodeo",
            question = Res.string.featurequiz_rodeo_question,
            answers = listOf(Res.string.featurequiz_rodeo_answer_1, Res.string.featurequiz_rodeo_answer_2, Res.string.featurequiz_rodeo_answer_3),
            noFeatureAnswer = Res.string.featurequiz_rodeo_none,
            explanation = Res.string.featurequiz_rodeo_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-schneaggahus",
            question = Res.string.featurequiz_schneaggahus_question,
            answers = listOf(Res.string.featurequiz_schneaggahus_answer_1, Res.string.featurequiz_schneaggahus_answer_2, Res.string.featurequiz_schneaggahus_answer_3),
            noFeatureAnswer = Res.string.featurequiz_schneaggahus_none,
            explanation = Res.string.featurequiz_schneaggahus_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-c-challenge",
            question = Res.string.featurequiz_c_challenge_question,
            answers = listOf(Res.string.featurequiz_c_challenge_answer_1, Res.string.featurequiz_c_challenge_answer_2, Res.string.featurequiz_c_challenge_answer_3),
            noFeatureAnswer = Res.string.featurequiz_c_challenge_none,
            explanation = Res.string.featurequiz_c_challenge_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-crossword",
            question = Res.string.featurequiz_crossword_question,
            answers = listOf(Res.string.featurequiz_crossword_answer_1, Res.string.featurequiz_crossword_answer_2, Res.string.featurequiz_crossword_answer_3),
            noFeatureAnswer = Res.string.featurequiz_crossword_none,
            explanation = Res.string.featurequiz_crossword_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-2048",
            question = Res.string.featurequiz_game_2048_question,
            answers = listOf(Res.string.featurequiz_game_2048_answer_1, Res.string.featurequiz_game_2048_answer_2, Res.string.featurequiz_game_2048_answer_3),
            noFeatureAnswer = Res.string.featurequiz_game_2048_none,
            explanation = Res.string.featurequiz_game_2048_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-tetris",
            question = Res.string.featurequiz_tetris_question,
            answers = listOf(Res.string.featurequiz_tetris_answer_1, Res.string.featurequiz_tetris_answer_2, Res.string.featurequiz_tetris_answer_3),
            noFeatureAnswer = Res.string.featurequiz_tetris_none,
            explanation = Res.string.featurequiz_tetris_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-grid-rush",
            question = Res.string.featurequiz_grid_rush_question,
            answers = listOf(Res.string.featurequiz_grid_rush_answer_1, Res.string.featurequiz_grid_rush_answer_2, Res.string.featurequiz_grid_rush_answer_3),
            noFeatureAnswer = Res.string.featurequiz_grid_rush_none,
            explanation = Res.string.featurequiz_grid_rush_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-odd-one-out",
            question = Res.string.featurequiz_odd_one_out_question,
            answers = listOf(Res.string.featurequiz_odd_one_out_answer_1, Res.string.featurequiz_odd_one_out_answer_2, Res.string.featurequiz_odd_one_out_answer_3),
            noFeatureAnswer = Res.string.featurequiz_odd_one_out_none,
            explanation = Res.string.featurequiz_odd_one_out_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-morse",
            question = Res.string.featurequiz_morse_question,
            answers = listOf(Res.string.featurequiz_morse_answer_1, Res.string.featurequiz_morse_answer_2, Res.string.featurequiz_morse_answer_3),
            noFeatureAnswer = Res.string.featurequiz_morse_none,
            explanation = Res.string.featurequiz_morse_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-yahtzee",
            question = Res.string.featurequiz_yahtzee_question,
            answers = listOf(Res.string.featurequiz_yahtzee_answer_1, Res.string.featurequiz_yahtzee_answer_2, Res.string.featurequiz_yahtzee_answer_3),
            noFeatureAnswer = Res.string.featurequiz_yahtzee_none,
            explanation = Res.string.featurequiz_yahtzee_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-undercover",
            question = Res.string.featurequiz_undercover_question,
            answers = listOf(Res.string.featurequiz_undercover_answer_1, Res.string.featurequiz_undercover_answer_2, Res.string.featurequiz_undercover_answer_3),
            noFeatureAnswer = Res.string.featurequiz_undercover_none,
            explanation = Res.string.featurequiz_undercover_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "tool-coinflip",
            question = Res.string.featurequiz_coin_flip_question,
            answers = listOf(Res.string.featurequiz_coin_flip_answer_1, Res.string.featurequiz_coin_flip_answer_2, Res.string.featurequiz_coin_flip_answer_3),
            noFeatureAnswer = Res.string.featurequiz_coin_flip_none,
            explanation = Res.string.featurequiz_coin_flip_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "tool-fingerpicker",
            question = Res.string.featurequiz_finger_picker_question,
            answers = listOf(Res.string.featurequiz_finger_picker_answer_1, Res.string.featurequiz_finger_picker_answer_2, Res.string.featurequiz_finger_picker_answer_3),
            noFeatureAnswer = Res.string.featurequiz_finger_picker_none,
            explanation = Res.string.featurequiz_finger_picker_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "tool-fuel-mix",
            question = Res.string.featurequiz_fuel_mix_question,
            answers = listOf(Res.string.featurequiz_fuel_mix_answer_1, Res.string.featurequiz_fuel_mix_answer_2, Res.string.featurequiz_fuel_mix_answer_3),
            noFeatureAnswer = Res.string.featurequiz_fuel_mix_none,
            explanation = Res.string.featurequiz_fuel_mix_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "voice-messages",
            question = Res.string.featurequiz_voice_messages_question,
            answers = listOf(Res.string.featurequiz_voice_messages_answer_1, Res.string.featurequiz_voice_messages_answer_2, Res.string.featurequiz_voice_messages_answer_3),
            noFeatureAnswer = Res.string.featurequiz_voice_messages_none,
            explanation = Res.string.featurequiz_voice_messages_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "image-messages",
            question = Res.string.featurequiz_image_messages_question,
            answers = listOf(Res.string.featurequiz_image_messages_answer_1, Res.string.featurequiz_image_messages_answer_2, Res.string.featurequiz_image_messages_answer_3),
            noFeatureAnswer = Res.string.featurequiz_image_messages_none,
            explanation = Res.string.featurequiz_image_messages_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "shared-content",
            question = Res.string.featurequiz_shared_content_question,
            answers = listOf(Res.string.featurequiz_shared_content_answer_1, Res.string.featurequiz_shared_content_answer_2, Res.string.featurequiz_shared_content_answer_3),
            noFeatureAnswer = Res.string.featurequiz_shared_content_none,
            explanation = Res.string.featurequiz_shared_content_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "message-search",
            question = Res.string.featurequiz_message_search_question,
            answers = listOf(Res.string.featurequiz_message_search_answer_1, Res.string.featurequiz_message_search_answer_2, Res.string.featurequiz_message_search_answer_3),
            noFeatureAnswer = Res.string.featurequiz_message_search_none,
            explanation = Res.string.featurequiz_message_search_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "drafts",
            question = Res.string.featurequiz_drafts_question,
            answers = listOf(Res.string.featurequiz_drafts_answer_1, Res.string.featurequiz_drafts_answer_2, Res.string.featurequiz_drafts_answer_3),
            noFeatureAnswer = Res.string.featurequiz_drafts_none,
            explanation = Res.string.featurequiz_drafts_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "groups",
            question = Res.string.featurequiz_groups_question,
            answers = listOf(Res.string.featurequiz_groups_answer_1, Res.string.featurequiz_groups_answer_2, Res.string.featurequiz_groups_answer_3),
            noFeatureAnswer = Res.string.featurequiz_groups_none,
            explanation = Res.string.featurequiz_groups_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "system-messages",
            question = Res.string.featurequiz_system_messages_question,
            answers = listOf(Res.string.featurequiz_system_messages_answer_1, Res.string.featurequiz_system_messages_answer_2, Res.string.featurequiz_system_messages_answer_3),
            noFeatureAnswer = Res.string.featurequiz_system_messages_none,
            explanation = Res.string.featurequiz_system_messages_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "read-receipts",
            question = Res.string.featurequiz_read_receipts_question,
            answers = listOf(Res.string.featurequiz_read_receipts_answer_1, Res.string.featurequiz_read_receipts_answer_2, Res.string.featurequiz_read_receipts_answer_3),
            noFeatureAnswer = Res.string.featurequiz_read_receipts_none,
            explanation = Res.string.featurequiz_read_receipts_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "online-status",
            question = Res.string.featurequiz_online_status_question,
            answers = listOf(Res.string.featurequiz_online_status_answer_1, Res.string.featurequiz_online_status_answer_2, Res.string.featurequiz_online_status_answer_3),
            noFeatureAnswer = Res.string.featurequiz_online_status_none,
            explanation = Res.string.featurequiz_online_status_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "themes",
            question = Res.string.featurequiz_themes_question,
            answers = listOf(Res.string.featurequiz_themes_answer_1, Res.string.featurequiz_themes_answer_2, Res.string.featurequiz_themes_answer_3),
            noFeatureAnswer = Res.string.featurequiz_themes_none,
            explanation = Res.string.featurequiz_themes_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "phone-number-sharing",
            question = Res.string.featurequiz_phone_number_question,
            answers = listOf(Res.string.featurequiz_phone_number_answer_1, Res.string.featurequiz_phone_number_answer_2, Res.string.featurequiz_phone_number_answer_3),
            noFeatureAnswer = Res.string.featurequiz_phone_number_none,
            explanation = Res.string.featurequiz_phone_number_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "settings-sync",
            question = Res.string.featurequiz_settings_sync_question,
            answers = listOf(Res.string.featurequiz_settings_sync_answer_1, Res.string.featurequiz_settings_sync_answer_2, Res.string.featurequiz_settings_sync_answer_3),
            noFeatureAnswer = Res.string.featurequiz_settings_sync_none,
            explanation = Res.string.featurequiz_settings_sync_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "native-notifications",
            question = Res.string.featurequiz_notifications_question,
            answers = listOf(Res.string.featurequiz_notifications_answer_1, Res.string.featurequiz_notifications_answer_2, Res.string.featurequiz_notifications_answer_3),
            noFeatureAnswer = Res.string.featurequiz_notifications_none,
            explanation = Res.string.featurequiz_notifications_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "changelog-popup",
            question = Res.string.featurequiz_changelog_question,
            answers = listOf(Res.string.featurequiz_changelog_answer_1, Res.string.featurequiz_changelog_answer_2, Res.string.featurequiz_changelog_answer_3),
            noFeatureAnswer = Res.string.featurequiz_changelog_none,
            explanation = Res.string.featurequiz_changelog_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "feature-tour",
            question = Res.string.featurequiz_feature_tour_question,
            answers = listOf(Res.string.featurequiz_feature_tour_answer_1, Res.string.featurequiz_feature_tour_answer_2, Res.string.featurequiz_feature_tour_answer_3),
            noFeatureAnswer = Res.string.featurequiz_feature_tour_none,
            explanation = Res.string.featurequiz_feature_tour_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "feedback-board",
            question = Res.string.featurequiz_feedback_board_question,
            answers = listOf(Res.string.featurequiz_feedback_board_answer_1, Res.string.featurequiz_feedback_board_answer_2, Res.string.featurequiz_feedback_board_answer_3),
            noFeatureAnswer = Res.string.featurequiz_feedback_board_none,
            explanation = Res.string.featurequiz_feedback_board_explanation,
        ),
    )
}

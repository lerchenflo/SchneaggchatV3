package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.PolymorphicSerializer
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.ChatScreen
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.Chatauswahlscreen


@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        println("hello world")
        NavHost(
            navController = navController,
            startDestination = Route.ChatGraph
        ) {
            navigation<Route.ChatGraph>(
                startDestination = Route.ChatSelector
            ) {
                composable<Route.ChatSelector>{
                    Chatauswahlscreen(
                        onChatSelected = { userId ->
                            // Navigate to Chat screen with user ID
                            println("opening chat with $userId")
                            navController.navigate(Route.Chat(userId))
                        }
                    )
                }

                composable<Route.Chat>{ backStackEntry ->
                    val userId = backStackEntry.toRoute<Route.Chat>().id

                    ChatScreen(userId)

                }
            }
        }


    }
}
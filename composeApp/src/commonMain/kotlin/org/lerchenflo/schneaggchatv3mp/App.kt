package org.lerchenflo.schneaggchatv3mp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.database.User
import org.lerchenflo.schneaggchatv3mp.database.UserDao
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.compose_multiplatform

@Composable
@Preview
fun App(userDao: UserDao) {
    MaterialTheme {
        val users by userDao.getallusers().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()

        LaunchedEffect(true) {
            val userlist = listOf<User>(
                User(3, 0, "flo"),
                User(4, 0, "fabi")
            )

            userlist.forEach {
                userDao.upsert(it)
            }
        }


        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(users) { person ->
                Text(
                    text = person.name.toString(),
                    modifier = Modifier.fillMaxWidth()
                        .padding(16.dp)

                )
            }
        }

    }
}
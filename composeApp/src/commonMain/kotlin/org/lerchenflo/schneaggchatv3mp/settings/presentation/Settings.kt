package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.app_broken
import schneaggchatv3mp.composeapp.generated.resources.are_you_sure_you_want_to_logout
import schneaggchatv3mp.composeapp.generated.resources.logout
import schneaggchatv3mp.composeapp.generated.resources.markdownInfo
import schneaggchatv3mp.composeapp.generated.resources.no
import schneaggchatv3mp.composeapp.generated.resources.settings
import schneaggchatv3mp.composeapp.generated.resources.useMarkdown
import schneaggchatv3mp.composeapp.generated.resources.version
import schneaggchatv3mp.composeapp.generated.resources.yes

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    toLoginNavigator: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .safeContentPadding()
){
    val viewModel = koinViewModel<SettingsViewModel>()
    val appRepository = koinInject<AppRepository>()
    val appVersion = koinInject<AppVersion>()

    LaunchedEffect(Unit){
        viewModel.init()
    }

    val ownuser by viewModel.getOwnuser().collectAsStateWithLifecycle(null)

    Column(
        modifier = modifier
    ){
        ActivityTitle(
            title = stringResource(Res.string.settings),
            onBackClick = onBackClick
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Section Userinfo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ){

            ProfilePictureView(
                filepath = ownuser?.profilePicture ?: "",
                modifier = Modifier
                    .size(120.dp)
                    .clickable { SnackbarManager.showMessage("Bald kann ma profilbild ändern") }
                    .padding(8.dp)
            )


            Column(
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.Start
            ){

                Text(
                    //TODO: Style besser macha
                    text = ownuser?.name ?: "",
                    modifier = Modifier
                        .clickable{viewModel.changeUsername()},

                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 10.sp,
                        maxFontSize = 30.sp
                    ),

                )
                Text(
                    text = "Userid : ${ownuser?.id}"
                )
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        //---- App Einstellungen ----


        // Markdown Formatting
        SettingsSwitch(
            titletext = stringResource(Res.string.useMarkdown),
            infotext = stringResource(Res.string.markdownInfo),
            switchchecked = viewModel.markdownEnabeled,
            onSwitchChange = { viewModel.updateMarkdownSwitch(it) },
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        SettingsOption(
            Icons.Default.Delete,
            "Dekete app data",
            "Deletes all app data and destroys ur phone"
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        Button(
            onClick = {viewModel.deleteAllAppData()},
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = 2.dp,
                    bottom = 2.dp
                ),
        ){
            Text(
                text = stringResource(Res.string.app_broken),
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Logout icon", // or null if purely decorative
                modifier = Modifier
                    .padding(start = 8.dp)
            )
        }


        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        
        
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        // Account section
        LogoutButton(
            viewModel = viewModel,
            toLoginNavigator = toLoginNavigator
        )

        Text(
            text = stringResource(Res.string.version, appVersion.getversionName()) + " Buildnr: " + appVersion.getversionCode(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )

    }
}


@Composable
fun LogoutButton(
    toLoginNavigator: () -> Unit = {},
    viewModel: SettingsViewModel
)
{
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    Button(
        onClick = {showLogoutDialog = true},
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 10.dp,
                end = 10.dp,
                top = 2.dp,
                bottom = 2.dp
            ),
    ){
        Text(
            text = stringResource(Res.string.logout),
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Logout icon", // or null if purely decorative
            modifier = Modifier
                .padding(start = 8.dp)
        )
    }

    // Confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = stringResource(Res.string.logout)) },
            text = { Text(text = stringResource(Res.string.are_you_sure_you_want_to_logout)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    toLoginNavigator()
                }) {
                    Text(text = stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = stringResource(Res.string.no))
                }
            }
        )
    }
}


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
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
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
import schneaggchatv3mp.composeapp.generated.resources.app_broken_desc
import schneaggchatv3mp.composeapp.generated.resources.are_you_sure_you_want_to_logout
import schneaggchatv3mp.composeapp.generated.resources.logout
import schneaggchatv3mp.composeapp.generated.resources.markdownInfo
import schneaggchatv3mp.composeapp.generated.resources.markdown_24px
import schneaggchatv3mp.composeapp.generated.resources.no
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap
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
                    text = ownuser?.name ?: "Username",
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


        // Markdown Formatting
        SettingsSwitch(
            titletext = stringResource(Res.string.useMarkdown),
            infotext = stringResource(Res.string.markdownInfo),
            switchchecked = viewModel.markdownEnabeled,
            onSwitchChange = { viewModel.updateMarkdownSwitch(it) },
            icon = vectorResource(Res.drawable.markdown_24px) //Gibts uf da icons no ned aber uf da website scho
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        SettingsOption(
            Icons.Default.Delete,
            stringResource(Res.string.app_broken),
            stringResource(Res.string.app_broken_desc),
            onClick = {viewModel.deleteAllAppData()}
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
        SettingsOption(
            icon = Icons.AutoMirrored.Default.ExitToApp,
            text = stringResource(Res.string.logout),
            onClick = {
                showLogoutDialog = true
            }
        )

        //Logoutdialog
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

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        Text(
            text = stringResource(Res.string.version, appRepository.appVersion.getVersionName()) + " Buildnr: " + appRepository.appVersion.getVersionCode(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(
                    top = 16.dp
                )
                .fillMaxWidth()
        )

    }
}
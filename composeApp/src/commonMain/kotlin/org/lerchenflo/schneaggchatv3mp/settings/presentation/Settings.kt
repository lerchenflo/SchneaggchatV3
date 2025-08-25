package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.OWNID
import org.lerchenflo.schneaggchatv3mp.USERNAME
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.*

@Composable
@Preview
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .safeContentPadding()
){
    val viewModel = koinViewModel<SettingsViewModel>()

    viewModel.init() // load all settings to show


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
        ){

            Image(
                painterResource(Res.drawable.icon_nutzer),
                contentDescription = stringResource(Res.string.tools_and_games),
                modifier = Modifier
                    .size(80.dp)
                    .clickable { SnackbarManager.showMessage("Bald kann ma profilbild ändern") }
                    .padding(8.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
            ){
                Text(
                    text = USERNAME,
                    modifier = Modifier
                        .clickable{viewModel.changeUsername()}

                )
                Text(
                    text = "Userid : $OWNID"
                )
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        //---- App Einstellungen ----

        // Marddown Formatting
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(
                    text = stringResource(Res.string.useMarkdown),
                    maxLines = 2,
                    modifier = Modifier
                        .wrapContentHeight(align = Alignment.CenterVertically),
                )

                Spacer(modifier = Modifier.width(8.dp))

                val mdInfo = stringResource(Res.string.markdownInfo) // pfusch wegs composable
                IconButton(
                    onClick = { SnackbarManager.showMessage(mdInfo)},
                    modifier = Modifier
                        .padding(top = 5.dp, start = 5.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(Res.string.markdownInfo),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = viewModel.markdownEnabeled,
                onCheckedChange = { viewModel.updateMarkdownSwitch(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = androidx.compose.ui.graphics.Color.Green,
                    uncheckedThumbColor = androidx.compose.ui.graphics.Color.Gray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }



        NormalButton(
            text = stringResource(Res.string.app_broken),
            onClick = {viewModel.deleteAllAppData()},
            disabled = false,
            isLoading = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = 2.dp,
                    bottom = 2.dp
                )
        )


        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Account section
        LogoutButton(
            viewModel = viewModel
        )

        Text(
            text = stringResource(Res.string.version, "3.x.x"),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )

    }
}

@Composable
fun LogoutButton(
    viewModel: SettingsViewModel
)
{
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    NormalButton(
        text = stringResource(Res.string.logout),
        onClick = {showLogoutDialog = true},
        disabled = false,
        isLoading = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 10.dp,
                end = 10.dp,
                top = 2.dp,
                bottom = 2.dp
            )
    )

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


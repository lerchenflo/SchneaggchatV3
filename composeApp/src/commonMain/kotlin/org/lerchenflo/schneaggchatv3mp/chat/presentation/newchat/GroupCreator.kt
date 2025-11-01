package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.common_friends
import schneaggchatv3mp.composeapp.generated.resources.info
import schneaggchatv3mp.composeapp.generated.resources.members
import schneaggchatv3mp.composeapp.generated.resources.new_group

@Preview
@Composable
fun GroupCreator(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .safeContentPadding()
){
    val viewModel = koinViewModel<NewChatViewModel>()
    val users by viewModel.groupCreatorState.collectAsStateWithLifecycle(emptyList())

    Column(
        modifier = modifier
    ){
        ActivityTitle(
            title = stringResource(Res.string.new_group),
            onBackClick = onBackClick
        )


        MultiChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = 0,
                    count = 2
                ),
                onCheckedChange = { viewModel.groupCreatorStage = GroupCreatorStage.MEMBERSEL },
                checked = viewModel.groupCreatorStage == GroupCreatorStage.MEMBERSEL || viewModel.groupCreatorStage == GroupCreatorStage.GROUPDETAILS,
                label = {
                    Row() {
                        Text(stringResource(Res.string.members))
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(Res.string.members)
                        )
                    }
                }

            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = 1,
                    count = 2
                ),
                onCheckedChange = { viewModel.groupCreatorStage = GroupCreatorStage.GROUPDETAILS },
                checked = viewModel.groupCreatorStage == GroupCreatorStage.GROUPDETAILS,
                label = {
                    Row {
                        Text(stringResource(Res.string.info))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(Res.string.info)
                        )
                    }
                }

            )
        }

        if(viewModel.groupCreatorStage == GroupCreatorStage.MEMBERSEL){
            Column(
                modifier = Modifier.fillMaxWidth()
            ){
                viewModel.selectedUsers.forEach{
                    user -> Text(text = user.name) // todo coole ui mit profilbild für jeden user und so
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
            ) {
                items(users) { user ->
                    UserButton(
                        selectedChat = user,
                        useOnClickGes = true,
                        lastMessage = null,
                        bottomTextOverride = "",
                        onClickGes = {
                            viewModel.selectedUsers.add(user)
                        }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp
                    )
                }
            }

            Text(text = "test 1")
        }else if(viewModel.groupCreatorStage == GroupCreatorStage.GROUPDETAILS){
            Text(text = "test 2")
        }

    }


}
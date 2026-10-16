package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionOwnerType
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.SplitScreenRoot
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_extension_not_found
import schneaggchatv3mp.composeapp.generated.resources.group_extension_under_construction

@Composable
fun GroupExtensionScreenRoot(
    extensionId: String,
) {
    val viewModel = koinViewModel<GroupExtensionViewModel> {
        parametersOf(extensionId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Types with their own screen take over once the container is loaded
    when (state.extension?.type) {
        GroupExtensionType.SPLIT -> SplitScreenRoot(extensionId = extensionId)
        else -> GroupExtensionScreen(
            state = state,
            onAction = viewModel::onAction
        )
    }
}

@Composable
fun GroupExtensionScreen(
    state: GroupExtensionState,
    onAction: (GroupExtensionAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = state.extension?.title.orEmpty(),
            onBackClick = { onAction(GroupExtensionAction.OnBackClick) }
        )

        HorizontalDivider()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.extension == null -> Text(stringResource(Res.string.group_extension_not_found))
                // todo: dispatch the remaining types to their own screens in GroupExtensionScreenRoot (GROUP_EXTENSIONS_PLAN.md phases 3-5)
                else -> Text(
                    stringResource(
                        Res.string.group_extension_under_construction,
                        state.extension.type.toUiText().asString()
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupExtensionScreenPreview() {
    SchneaggchatTheme {
        GroupExtensionScreen(
            state = GroupExtensionState(
                extension = GroupExtension(
                    id = "preview",
                    ownerType = ExtensionOwnerType.GROUP,
                    ownerId = "group",
                    type = GroupExtensionType.SPLIT,
                    title = "Italien 2026",
                    createdBy = "user",
                    createdAt = 0L,
                    updatedBy = "user",
                    updatedAt = 0L,
                ),
                isLoading = false,
            ),
            onAction = {}
        )
    }
}

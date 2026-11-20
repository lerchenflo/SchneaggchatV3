package org.lerchenflo.schneaggchatv3mp.sharedUi.picture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.profile_picture
import schneaggchatv3mp.composeapp.generated.resources.show_picture_big

/**
 * Which picture the big picture dialog shows, null while it is closed. A screen with many small
 * pictures (a list) keeps one of these and one [BigPictureHost]; a single picture uses
 * [ZoomableProfilePicture], which brings its own.
 */
@Stable
class BigPictureState {
    var filepath: String? by mutableStateOf(null)
        private set

    /** Opens [filepath] big. An empty path means no picture (only the placeholder), so nothing opens. */
    fun show(filepath: String) {
        if (filepath.isNotBlank()) this.filepath = filepath
    }

    fun dismiss() {
        filepath = null
    }
}

@Composable
fun rememberBigPictureState(): BigPictureState = remember { BigPictureState() }

/**
 * Shows the big picture dialog while [state] holds a picture. [onEdit] adds an edit button, which
 * closes the dialog before it runs.
 */
@Composable
fun BigPictureHost(state: BigPictureState, onEdit: (() -> Unit)? = null) {
    val filepath = state.filepath ?: return
    ProfilePictureBigDialog(
        filepath = filepath,
        onDismiss = state::dismiss,
        showEditButton = onEdit != null,
        onEdit = {
            state.dismiss()
            onEdit?.invoke()
        },
    )
}

/**
 * A round profile or group picture that opens big on a tap. Without a picture the placeholder
 * does not react. [onEdit] adds an edit button to the big view.
 */
@Composable
fun ZoomableProfilePicture(
    filepath: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(Res.string.profile_picture),
    placeholder: Painter = rememberProfilePicturePlaceholder(),
    onEdit: (() -> Unit)? = null,
) {
    val bigPicture = rememberBigPictureState()
    ProfilePictureView(
        filepath = filepath,
        contentDescription = contentDescription,
        placeholder = placeholder,
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                enabled = filepath.isNotBlank(),
                onClickLabel = stringResource(Res.string.show_picture_big),
            ) { bigPicture.show(filepath) },
    )
    BigPictureHost(state = bigPicture, onEdit = onEdit)
}

package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer

@Composable
fun ProfilePictureView(
    filepath: String, //Absolute filepath
    modifier: Modifier = Modifier
        .size(40.dp)
        .padding(end = 8.dp)
        .clip(CircleShape)
) {

    AsyncImage(
        model = filepath,
        contentDescription = "Profile picture",
        error = painterResource(Res.drawable.icon_nutzer),
        modifier = modifier.clip(CircleShape)
    )
}
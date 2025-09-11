package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.go_back
import schneaggchatv3mp.composeapp.generated.resources.settings

@Preview
@Composable
fun ActivityTitle(
    title: String = "ActivityTitle",
    onBackClick: () -> Unit = {},
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ){
        // Backbutton
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(top = 5.dp, start = 5.dp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.go_back),
            )
        }


        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .align(alignment = Alignment.CenterVertically)
                .padding(start = 10.dp),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 20.sp,
                maxFontSize = 30.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
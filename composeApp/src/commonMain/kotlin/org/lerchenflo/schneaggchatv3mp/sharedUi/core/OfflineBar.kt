package org.lerchenflo.schneaggchatv3mp.sharedUi.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.offline

@Composable
fun OfflineBar(
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ),
        horizontalArrangement = Arrangement.Center
    ){
        Text(
            text = stringResource(Res.string.offline),
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(vertical = 2.dp) //Abstand oba und unta
        )
    }
}
package org.lerchenflo.schneaggchatv3mp.sharedUi.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.TooltipIconButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.test_mode_banner
import schneaggchatv3mp.composeapp.generated.resources.test_mode_banner_tooltip

@Composable
fun TestModeBar(
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ){
        Text(
            text = stringResource(Res.string.test_mode_banner),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        TooltipIconButton(tooltip = stringResource(Res.string.test_mode_banner_tooltip))
    }
}

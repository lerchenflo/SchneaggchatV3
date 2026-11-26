package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind

/**
 * The point's sign, the same as its map marker: the number of a stop, the icon of an accommodation
 * or sight, a small dot for a drive-through point.
 */
@Composable
fun RoutePointBadge(
    kind: RoutePointKind,
    stopNumber: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    val colors = kind.colors()
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (kind == RoutePointKind.WAYPOINT) {
            Box(
                modifier = Modifier
                    .size(size * 0.4f)
                    .background(colors.container, CircleShape)
                    .border(2.dp, colors.content, CircleShape)
            )
            return@Box
        }
        Box(
            modifier = Modifier
                .size(size)
                .background(colors.container, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (stopNumber != null) {
                Text(
                    text = stopNumber.toString(),
                    color = colors.content,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Icon(
                    imageVector = kind.icon(),
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}

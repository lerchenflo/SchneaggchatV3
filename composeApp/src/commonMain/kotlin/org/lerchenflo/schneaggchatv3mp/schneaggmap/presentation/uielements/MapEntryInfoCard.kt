package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry

@Composable
fun MapEntryInfoCard(
    entry: MapEntry,
    mainTypeDisplayName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.width(220.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mainTypeDisplayName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }
            if (entry.description.isNotBlank()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            val attributesList = when (val data = entry.locationData) {
                is LocationData.Radar -> listOf(
                    "Speed Limit" to data.speedLimit.value.toString(),
                    "Radar Type" to data.radarType.name
                )
                is LocationData.Street -> listOfNotNull(
                    data.mautFee?.let { "Maut Fee" to it.value.toString() },
                    data.heightLimit?.let { "Height Limit" to it.value.toString() },
                    data.closedInWinter?.let { "Closed in Winter" to it.value.toString() },
                    data.wheeliesAllowed?.let { "Wheelies Allowed" to it.value.toString() }
                )
                is LocationData.Camping -> listOf(
                    "Official" to data.official.value.toString()
                )
                is LocationData.SightSeeing -> listOfNotNull(
                    data.entryFee?.let { "Entry Fee" to it.value.toString() }
                )
                is LocationData.SwimmingLocation -> listOfNotNull(
                    data.indoor?.let { "Indoor" to it.value.toString() }
                )
                is LocationData.PartyLocation -> listOfNotNull(
                    data.entryFee?.let { "Entry Fee" to it.value.toString() }
                )
                is LocationData.Food -> listOfNotNull(
                    "Food Type" to data.foodType.name,
                    data.allYouCanEat?.let { "All You Can Eat" to it.value.toString() }
                )
            }

            attributesList.take(3).forEach { (key, value) ->
                Text(
                    text = "$key: $value",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}


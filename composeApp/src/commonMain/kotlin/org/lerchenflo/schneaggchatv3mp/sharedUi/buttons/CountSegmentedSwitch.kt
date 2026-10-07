@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.sharedUi.buttons

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme

/**
 * Full width single-choice switch between a few tabs, each label optionally followed by a count
 * ("Images (12)"). A null count leaves the label bare, e.g. while the count is still loading.
 */
@Composable
fun <T> CountSegmentedSwitch(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    count: (T) -> Int?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    showLeadingIcon: Boolean = true
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            val optionCount = count(option)

            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                icon = { if (showLeadingIcon) SegmentedButtonDefaults.Icon(option == selected) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(
                    text = if (optionCount != null) "${label(option)} ($optionCount)" else label(option),
                    maxLines = 1
                )
            }
        }
    }
}

@Preview
@Composable
private fun CountSegmentedSwitchPreview() {
    SchneaggchatTheme {
        CountSegmentedSwitch(
            options = listOf("Features", "Bugs"),
            selected = "Features",
            label = { it },
            count = { if (it == "Features") 12 else null },
            onSelect = {}
        )
    }
}

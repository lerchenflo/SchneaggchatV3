package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue

@Composable
fun KeyValueView(
    key: String,
    value: AttributeValue,
    onValueChange: (AttributeValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key
        )

        when (value) {

            is AttributeValue.BoolValue -> {
                Switch(
                    checked = value.value,
                    onCheckedChange = {
                        onValueChange(
                            AttributeValue.BoolValue(it)
                        )
                    }
                )
            }

            is AttributeValue.StringValue -> {
                OutlinedTextField(
                    value = value.value,
                    onValueChange = {
                        onValueChange(
                            AttributeValue.StringValue(it)
                        )
                    },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
            }

            is AttributeValue.IntValue -> {
                OutlinedTextField(
                    value = value.value.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { intValue ->
                            onValueChange(
                                AttributeValue.IntValue(intValue)
                            )
                        }
                    },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            }

            is AttributeValue.DoubleValue -> {
                OutlinedTextField(
                    value = value.value.toString(),
                    onValueChange = {
                        it.toDoubleOrNull()?.let { doubleValue ->
                            onValueChange(
                                AttributeValue.DoubleValue(doubleValue)
                            )
                        }
                    },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }
        }
    }
}
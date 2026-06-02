package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeDefinition
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.BoolValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.DoubleValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.IntValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.StringValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.label
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.uiTextFromEnumValue

@Composable
fun KeyValueView(
    value: AttributeValue?,
    definition: AttributeDefinition,
    onValueChange: (AttributeValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = definition.label() + if (definition.required) " *" else ""
        )

        Spacer(modifier = Modifier.width(4.dp))

        when (definition) {
            is AttributeDefinition.BoolDef -> {
                Switch(
                    checked = (value as? BoolValue)?.value ?: false,
                    onCheckedChange = {
                        onValueChange(BoolValue(it))
                    }
                )
            }

            is AttributeDefinition.DoubleDef -> {
                var rawText by remember(value) {
                    mutableStateOf(((value as? DoubleValue)?.value ?: "").toString())
                }

                val error = rawText.toDoubleOrNull().let { parsed ->
                    when {
                        rawText.isBlank() && definition.required -> "Required"
                        parsed == null && rawText.isNotBlank()   -> "Invalid number"
                        parsed != null && definition.min != null && parsed < definition.min -> "Min: ${definition.min}"
                        parsed != null && definition.max != null && parsed > definition.max -> "Max: ${definition.max}"
                        else -> null
                    }
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { input ->
                        rawText = input
                        input.toDoubleOrNull()?.let { parsed ->
                            val clamped = when {
                                definition.min != null && parsed < definition.min -> definition.min
                                definition.max != null && parsed > definition.max -> definition.max
                                else -> parsed
                            }
                            onValueChange(DoubleValue(clamped))
                        }
                    },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            is AttributeDefinition.IntDef -> {
                var rawText by remember(value) {
                    mutableStateOf(((value as? IntValue)?.value ?: "").toString())
                }

                val error = rawText.toIntOrNull().let { parsed ->
                    when {
                        rawText.isBlank() && definition.required              -> "Required"
                        parsed == null && rawText.isNotBlank()                -> "Invalid number"
                        parsed != null && definition.min != null && parsed < definition.min -> "Min: ${definition.min}"
                        parsed != null && definition.max != null && parsed > definition.max -> "Max: ${definition.max}"
                        else -> null
                    }
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { input ->
                        rawText = input
                        input.toIntOrNull()?.let { parsed ->
                            val clamped = when {
                                definition.min != null && parsed < definition.min -> definition.min
                                definition.max != null && parsed > definition.max -> definition.max
                                else -> parsed
                            }
                            onValueChange(IntValue(clamped))
                        }
                    },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            is AttributeDefinition.StringDef -> {
                var rawText by remember(value) {
                    mutableStateOf((value as? StringValue)?.value ?: "")
                }

                val error = when {
                    rawText.isBlank() && definition.required                                    -> "Required"
                    definition.maxLength != null && rawText.length > definition.maxLength -> "Max ${definition.maxLength} chars"
                    else -> null
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { input ->
                        val clamped = if (definition.maxLength != null) input.take(definition.maxLength) else input
                        rawText = clamped
                        onValueChange(StringValue(clamped))
                    },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
            }

            is AttributeDefinition.EnumDef -> {
                var expanded by remember { mutableStateOf(false) }


                Button(
                    onClick = {expanded = true}
                ) {
                    Text(
                        text = uiTextFromEnumValue((value as? AttributeValue.EnumValue)?.value ?: "").asString()
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    scrollState = rememberScrollState(),
                ){
                    definition.options.forEach {
                        Text(
                            text = uiTextFromEnumValue(it).asString()
                        )
                    }
                }
            }
        }
    }
}
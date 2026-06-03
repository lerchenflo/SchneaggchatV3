package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeDefinition
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.BoolValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.DoubleValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.IntValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.StringValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.label
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.uiTextFromEnumValue
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton

@Composable
fun KeyValueView(
    value: AttributeValue?,
    definition: AttributeDefinition,
    onValueChange: (AttributeValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {


        Text(
            text = definition.label() + if (definition.required) " *" else "",
        )

        Spacer(modifier = Modifier.weight(1f))

        when (definition) {
            is AttributeDefinition.BoolDef -> {
                Switch(
                    checked = (value as? BoolValue)?.value ?: false,
                    onCheckedChange = {
                        onValueChange(BoolValue(it))
                    },
                    modifier = Modifier.width(120.dp)

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

                TextField(
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(120.dp)
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

                TextField(
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp)
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

                TextField(
                    value = rawText,
                    onValueChange = { input ->
                        val clamped = if (definition.maxLength != null) input.take(definition.maxLength) else input
                        rawText = clamped
                        onValueChange(StringValue(clamped))
                    },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.width(120.dp)
                )
            }

            is AttributeDefinition.EnumDef -> {
                var expanded by remember { mutableStateOf(false) }

                Box{
                    NormalButton(
                        text = uiTextFromEnumValue((value as? AttributeValue.EnumValue)?.value ?: "").asString(),
                        onClick = {expanded = true},
                        primary = false,
                        modifier = Modifier.width(120.dp)

                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        scrollState = rememberScrollState(),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ){
                        definition.options.forEach {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = uiTextFromEnumValue(it).asString()
                                    )
                                },
                                onClick = {
                                    onValueChange(AttributeValue.EnumValue(it))
                                    expanded = false
                                }

                            )

                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, heightDp = 600)
@Composable
private fun AllKeyValueTypesPreview() {
    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KeyValueView(
                value = BoolValue(true),
                definition = AttributeDefinition.BoolDef(
                    key = "official",
                    required = true
                ),
                onValueChange = {}
            )

            KeyValueView(
                value = IntValue(80),
                definition = AttributeDefinition.IntDef(
                    key = "speedLimit",
                    required = true,
                    min = 10,
                    max = 130
                ),
                onValueChange = {}
            )

            KeyValueView(
                value = DoubleValue(15.5),
                definition = AttributeDefinition.DoubleDef(
                    key = "entryFee",
                    required = false,
                    min = 0.0,
                    max = 100.0
                ),
                onValueChange = {}
            )

            KeyValueView(
                value = StringValue("Pizza"),
                definition = AttributeDefinition.StringDef(
                    key = "foodType",
                    required = true,
                    maxLength = 30
                ),
                onValueChange = {}
            )

            KeyValueView(
                value = AttributeValue.EnumValue("FIXED"),
                definition = AttributeDefinition.EnumDef(
                    key = "radarType",
                    required = true,
                    options = listOf(
                        "FIXED",
                        "MOBILE",
                        "SECTION_CONTROL"
                    )
                ),
                onValueChange = {}
            )
        }
    }
}
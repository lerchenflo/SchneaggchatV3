package org.lerchenflo.schneaggchatv3mp.tools.presentation.fuelcalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.tools.domain.FUEL_RATIO_PRESETS
import org.lerchenflo.schneaggchatv3mp.tools.domain.FuelMixBasis
import org.lerchenflo.schneaggchatv3mp.tools.domain.MAX_FUEL_RATIO
import org.lerchenflo.schneaggchatv3mp.tools.domain.MIN_FUEL_RATIO
import org.lerchenflo.schneaggchatv3mp.tools.domain.formatLitres
import org.lerchenflo.schneaggchatv3mp.tools.domain.formatMillilitres
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_amount_error
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_amount_mixture
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_amount_oil
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_amount_petrol
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_basis_mixture
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_basis_oil
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_basis_petrol
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_basis_title
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_clear
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_empty_hint
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_explanation
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_ratio_custom
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_ratio_error
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_ratio_preset
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_ratio_title
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_result_oil
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_result_petrol
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_result_total
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_title
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_unit_litres
import schneaggchatv3mp.composeapp.generated.resources.tools_fuel_unit_millilitres

@Composable
fun FuelCalculatorScreenRoot(
    onBackClick: () -> Unit,
    viewModel: FuelCalculatorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    FuelCalculatorScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
fun FuelCalculatorScreen(
    state: FuelCalculatorState,
    onAction: (FuelCalculatorAction) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = stringResource(Res.string.tools_fuel_title),
            onBackClick = onBackClick,
        )

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.tools_fuel_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            BasisSelector(
                selected = state.basis,
                onSelect = { onAction(FuelCalculatorAction.OnBasisSelect(it)) },
            )

            OutlinedTextField(
                value = state.amountInput,
                onValueChange = { onAction(FuelCalculatorAction.OnAmountChange(it)) },
                label = { Text(stringResource(state.basis.amountLabelRes())) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.amountError,
                supportingText = if (state.amountError) {
                    { Text(stringResource(Res.string.tools_fuel_amount_error)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            RatioPicker(
                ratioInput = state.ratioInput,
                ratioError = state.ratioError,
                onRatioChange = { onAction(FuelCalculatorAction.OnRatioChange(it)) },
                onPresetSelect = { onAction(FuelCalculatorAction.OnRatioPresetSelect(it)) },
            )

            val result = state.result
            if (result == null) {
                Text(
                    text = stringResource(Res.string.tools_fuel_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // The entered quantity is shown again too, so the card reads as a recipe
                        ResultRow(
                            label = stringResource(Res.string.tools_fuel_result_petrol),
                            value = stringResource(
                                Res.string.tools_fuel_unit_litres,
                                formatLitres(result.petrolLitres)
                            ),
                        )
                        ResultRow(
                            label = stringResource(Res.string.tools_fuel_result_oil),
                            value = stringResource(
                                Res.string.tools_fuel_unit_millilitres,
                                formatMillilitres(result.oilLitres)
                            ),
                            emphasized = true,
                        )
                        HorizontalDivider()
                        ResultRow(
                            label = stringResource(Res.string.tools_fuel_result_total),
                            value = stringResource(
                                Res.string.tools_fuel_unit_litres,
                                formatLitres(result.totalLitres)
                            ),
                        )
                    }
                }
            }

            if (state.amountInput.isNotEmpty()) {
                TextButton(onClick = { onAction(FuelCalculatorAction.OnClear) }) {
                    Text(stringResource(Res.string.tools_fuel_clear))
                }
            }
        }
    }
}

@Composable
private fun BasisSelector(
    selected: FuelMixBasis,
    onSelect: (FuelMixBasis) -> Unit,
) {
    val bases = FuelMixBasis.entries

    Column {
        Text(
            text = stringResource(Res.string.tools_fuel_basis_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            bases.forEachIndexed { index, basis ->
                SegmentedButton(
                    selected = basis == selected,
                    onClick = { onSelect(basis) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = bases.size),
                ) {
                    Text(text = stringResource(basis.labelRes()), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun RatioPicker(
    ratioInput: String,
    ratioError: Boolean,
    onRatioChange: (String) -> Unit,
    onPresetSelect: (Int) -> Unit,
) {
    Column {
        Text(
            text = stringResource(Res.string.tools_fuel_ratio_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // Wraps rather than scrolls, so all six presets stay reachable without a swipe and
        // "1:100" keeps its own width instead of being squeezed into a sixth of the screen
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            FUEL_RATIO_PRESETS.forEach { preset ->
                FilterChip(
                    selected = ratioInput.trim().toIntOrNull() == preset,
                    onClick = { onPresetSelect(preset) },
                    label = {
                        Text(
                            text = stringResource(Res.string.tools_fuel_ratio_preset, preset),
                            maxLines = 1,
                        )
                    },
                )
            }
        }

        OutlinedTextField(
            value = ratioInput,
            onValueChange = onRatioChange,
            label = { Text(stringResource(Res.string.tools_fuel_ratio_custom)) },
            prefix = { Text("1:") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = ratioError,
            supportingText = if (ratioError) {
                {
                    Text(
                        stringResource(
                            Res.string.tools_fuel_ratio_error,
                            MIN_FUEL_RATIO,
                            MAX_FUEL_RATIO
                        )
                    )
                }
            } else null,
            singleLine = true,
            modifier = Modifier.width(180.dp),
        )
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleLarge
            else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (emphasized) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun FuelMixBasis.labelRes(): StringResource = when (this) {
    FuelMixBasis.PETROL -> Res.string.tools_fuel_basis_petrol
    FuelMixBasis.MIXTURE -> Res.string.tools_fuel_basis_mixture
    FuelMixBasis.OIL -> Res.string.tools_fuel_basis_oil
}

private fun FuelMixBasis.amountLabelRes(): StringResource = when (this) {
    FuelMixBasis.PETROL -> Res.string.tools_fuel_amount_petrol
    FuelMixBasis.MIXTURE -> Res.string.tools_fuel_amount_mixture
    FuelMixBasis.OIL -> Res.string.tools_fuel_amount_oil
}

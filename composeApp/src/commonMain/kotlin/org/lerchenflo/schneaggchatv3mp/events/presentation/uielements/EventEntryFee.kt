package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.events.domain.EVENT_ENTRY_FEE_CURRENCY
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.utilities.currencySymbol
import org.lerchenflo.schneaggchatv3mp.utilities.formatMoney
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.decimal_separator
import schneaggchatv3mp.composeapp.generated.resources.error_event_entry_fee_invalid
import schneaggchatv3mp.composeapp.generated.resources.event_entry_fee_amount_label
import schneaggchatv3mp.composeapp.generated.resources.event_entry_fee_info
import schneaggchatv3mp.composeapp.generated.resources.event_entry_fee_label

/** "12,50 €" in the current language's decimal format. */
@Composable
fun Event.entryFeeText(): String =
    formatMoney(entryFeeCents, EVENT_ENTRY_FEE_CURRENCY, stringResource(Res.string.decimal_separator))

/**
 * Entry fee part of the event edit popup: a switch and, while it is on, the amount field.
 * Stateless: the popup keeps the toggle, the raw text and the error, like the participant limit.
 */
@Composable
fun EventEntryFeeSection(
    chargeEntryFee: Boolean,
    amountText: String,
    isError: Boolean,
    onChargeEntryFeeChange: (Boolean) -> Unit,
    onAmountTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SettingsSwitch(
            modifier = Modifier.fillMaxWidth(),
            titletext = stringResource(Res.string.event_entry_fee_label),
            infotext = stringResource(Res.string.event_entry_fee_info),
            switchchecked = chargeEntryFee,
            onSwitchChange = onChargeEntryFeeChange,
            icon = null
        )

        if (chargeEntryFee) {
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountTextChange,
                label = { Text(text = stringResource(Res.string.event_entry_fee_amount_label)) },
                suffix = { Text(text = currencySymbol(EVENT_ENTRY_FEE_CURRENCY)) },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text(text = stringResource(Res.string.error_event_entry_fee_invalid)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

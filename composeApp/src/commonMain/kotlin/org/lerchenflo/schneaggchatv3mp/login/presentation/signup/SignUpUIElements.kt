@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.InputTextField
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.TooltipIconButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.accept_agb_pt1
import schneaggchatv3mp.composeapp.generated.resources.accept_agb_pt2
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.create_account
import schneaggchatv3mp.composeapp.generated.resources.create_account_subtitle
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.password
import schneaggchatv3mp.composeapp.generated.resources.password_again
import schneaggchatv3mp.composeapp.generated.resources.profile_picture
import schneaggchatv3mp.composeapp.generated.resources.select_gebi_date
import schneaggchatv3mp.composeapp.generated.resources.select_profile_pic
import schneaggchatv3mp.composeapp.generated.resources.tooltip_birthdate
import schneaggchatv3mp.composeapp.generated.resources.tooltip_email
import schneaggchatv3mp.composeapp.generated.resources.tooltip_password
import schneaggchatv3mp.composeapp.generated.resources.tooltip_password_repeat
import schneaggchatv3mp.composeapp.generated.resources.tooltip_profile_picture
import schneaggchatv3mp.composeapp.generated.resources.tooltip_terms
import schneaggchatv3mp.composeapp.generated.resources.tooltip_username
import schneaggchatv3mp.composeapp.generated.resources.username
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@Composable
fun SignUpHeaderText(
    alignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
){
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        ActivityTitle(
            title = stringResource(Res.string.create_account),
            onBackClick = {
                onBackClick()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.create_account_subtitle),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun BirthdatePickerPopup(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    defaultDate: LocalDate? = null
) {
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    val endOfCurrentYear = remember(today) {
        LocalDate(year = today.year, month = 12, day = 31)
    }

    // Compute default safely: avoid .minus(DateTimeUnit.YEAR) which is unstable on iOS
    // Manually subtract 18 years, clamping Feb 29 → Feb 28 on non-leap years
    val safeDefaultDate = remember(defaultDate, today) {
        defaultDate ?: run {
            val targetYear = today.year - 18
            val targetDay = if (today.month == Month.FEBRUARY &&
                today.day == 29 &&
                !LocalDate.isLeapYear(targetYear)
            ) 28 else today.day
            LocalDate(targetYear, today.month, targetDay)
        }
    }

    var selectedDate by remember { mutableStateOf(safeDefaultDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                // Clamp to valid range before confirming — guards against picker edge cases on iOS
                val clamped = when {
                    selectedDate < LocalDate(1900, 1, 1) -> LocalDate(1900, 1, 1)
                    selectedDate > endOfCurrentYear -> endOfCurrentYear
                    else -> selectedDate
                }
                onDateSelected(clamped)
            }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        text = {
            WheelDatePicker(
                modifier = Modifier.fillMaxWidth(),
                startDate = selectedDate,
                minDate = LocalDate(1900, 1, 1),
                maxDate = endOfCurrentYear,
                rowCount = 5,
                textColor = MaterialTheme.colorScheme.onSurface,
                selectorProperties = WheelPickerDefaults.selectorProperties(
                    enabled = true,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ),
                onSnappedDate = { snappedDate: LocalDate ->
                    // Guard against the picker delivering out-of-range dates on iOS
                    if (snappedDate >= LocalDate(1900, 1, 1) && snappedDate <= endOfCurrentYear) {
                        selectedDate = snappedDate
                    }
                }
            )
        }
    )
}

// Helper — kotlinx-datetime doesn't expose this directly
private fun LocalDate.Companion.isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)


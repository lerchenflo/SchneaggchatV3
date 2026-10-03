package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.groupextentions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.graphics.vector.ImageVector
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.ext_money_split
import schneaggchatv3mp.composeapp.generated.resources.ext_quote
import schneaggchatv3mp.composeapp.generated.resources.ext_text_document
import schneaggchatv3mp.composeapp.generated.resources.ext_tour_planner

enum class ExtensionType
{
    MONEYSPLIT,
    QUOTE,
    TOURPLANNER,
    TEXTDOCUMENT;

    fun toUiText(): UiText = when (this) {
        MONEYSPLIT -> UiText.StringResourceText(Res.string.ext_money_split)
        QUOTE -> UiText.StringResourceText(Res.string.ext_quote)
        TOURPLANNER   -> UiText.StringResourceText(Res.string.ext_tour_planner)
        TEXTDOCUMENT -> UiText.StringResourceText(Res.string.ext_text_document)
    }

    fun getIcon(): ImageVector = when (this) {

        MONEYSPLIT -> Icons.Default.Money
        QUOTE -> Icons.Default.FormatQuote
        TOURPLANNER   -> Icons.Default.Route
        TEXTDOCUMENT -> Icons.Default.TextFields
    }
}
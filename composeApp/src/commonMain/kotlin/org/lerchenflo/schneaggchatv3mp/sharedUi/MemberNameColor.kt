package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.ui.graphics.Color

/**
 * The color a group member's name is shown in, from the ARGB value in `GroupMember.color`.
 * 0 means no color is set (e.g. a former member) and shows red, the same as the sender name in the chat.
 */
fun memberNameColor(color: Int): Color =
    if (color == 0) Color.Red else Color(color.toLong() and 0xFFFFFFFFL)

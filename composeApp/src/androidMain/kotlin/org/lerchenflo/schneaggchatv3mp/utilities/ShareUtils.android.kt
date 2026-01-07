package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK

actual class ShareUtils(private val context: Context) {
    
    actual fun shareString(string: String) {
        val sendIntent = Intent().apply {
            action = ACTION_SEND
            putExtra(EXTRA_TEXT, string)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        shareIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
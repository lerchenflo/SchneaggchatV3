package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri

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
    
    actual fun openMailClient(recipient: String, subject: String, body: String) {
        val mailtoUri = "mailto:$recipient".let { uri ->
            val params = mutableListOf<String>()
            if (subject.isNotEmpty()) params.add("subject=${Uri.encode(subject)}")
            if (body.isNotEmpty()) params.add("body=${Uri.encode(body)}")
            if (params.isNotEmpty()) "$uri?${params.joinToString("&")}" else uri
        }
        
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(mailtoUri)
            addFlags(FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: try generic email intent
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                println("No mail client available on this device: ${e2.message}")
            }
        }
    }
    
    actual fun copyToClipboard(text: String, clipboard: Any) {
        val manager = clipboard as ClipboardManager
        val clip = ClipData.newPlainText("text", text)
        manager.setPrimaryClip(clip)
    }
}
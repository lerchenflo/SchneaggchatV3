package org.lerchenflo.schneaggchatv3mp.utilities

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

actual class ShareUtils {
    actual fun shareString(string: String) {
        try {
            val stringSelection = StringSelection(string)
            val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(stringSelection, null)
            println("Text copied to clipboard: $string")
        } catch (e: Exception) {
            println("Failed to copy to clipboard: ${e.message}")
        }
    }
    
    actual fun openMailClient(recipient: String, subject: String, body: String) {
        try {
            val mailtoUri = "mailto:$recipient".let { uri ->
                val params = mutableListOf<String>()
                if (subject.isNotEmpty()) params.add("subject=${URLEncoder.encode(subject, "UTF-8").replace("+", "%20")}")
                if (body.isNotEmpty()) params.add("body=${URLEncoder.encode(body, "UTF-8").replace("+", "%20")}")
                if (params.isNotEmpty()) "$uri?${params.joinToString("&")}" else uri
            }
            
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
                Desktop.getDesktop().mail(URI(mailtoUri))
            } else {
                // Fallback: try to open with default browser
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(mailtoUri))
                } else {
                    println("No mail client or browser available to handle mailto URL")
                    println("Mailto link: $mailtoUri")
                }
            }
        } catch (e: Exception) {
            println("Failed to open mail client: ${e.message}")
            // As a last resort, print the mailto link for manual use
            val mailtoUri = "mailto:$recipient".let { uri ->
                val params = mutableListOf<String>()
                if (subject.isNotEmpty()) params.add("subject=$subject")
                if (body.isNotEmpty()) params.add("body=$body")
                if (params.isNotEmpty()) "$uri?${params.joinToString("&")}" else uri
            }
            println("Please use this link manually: $mailtoUri")
        }
    }
    
    actual fun copyToClipboard(text: String) {
        val systemClipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        systemClipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
    }

    actual fun openLocationInMaps(lat: Double, long: Double, label: String) {
        try {
            val url = "https://www.google.com/maps/search/?api=1&query=$lat,$long"
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                println("No browser available to open location: $url")
            }
        } catch (e: Exception) {
            println("Failed to open location in maps: ${e.message}")
        }
    }

    actual fun openPhoneDialer(phoneNumber: String) {
        try {
            val url = "tel:$phoneNumber"
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                println("No app available to open dialer: $url")
            }
        } catch (e: Exception) {
            println("Failed to open phone dialer: ${e.message}")
        }
    }

    actual fun addEventToCalendar(title: String, description: String, location: String, startDateMillis: Long, endDateMillis: Long?) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
            val start = formatter.format(Instant.ofEpochMilli(startDateMillis))
            val end = formatter.format(Instant.ofEpochMilli(endDateMillis ?: (startDateMillis + 3_600_000L)))

            val icsContent = buildString {
                appendLine("BEGIN:VCALENDAR")
                appendLine("VERSION:2.0")
                appendLine("BEGIN:VEVENT")
                appendLine("SUMMARY:${icsEscape(title)}")
                if (description.isNotEmpty()) appendLine("DESCRIPTION:${icsEscape(description)}")
                if (location.isNotEmpty()) appendLine("LOCATION:${icsEscape(location)}")
                appendLine("DTSTART:$start")
                appendLine("DTEND:$end")
                appendLine("END:VEVENT")
                appendLine("END:VCALENDAR")
            }

            val file = File.createTempFile("event", ".ics")
            file.writeText(icsContent)
            file.deleteOnExit()

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file)
            } else {
                println("No app available to open calendar file: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            println("Failed to add event to calendar: ${e.message}")
        }
    }
}

private fun icsEscape(text: String): String {
    return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
}
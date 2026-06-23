package io.github.lerchenflo.voicemessages.internal

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.write

internal fun ensureDirectoryExists(path: String) {
    val dir = Path(path)
    if (!SystemFileSystem.exists(dir)) {
        SystemFileSystem.createDirectories(dir)
    }
}

internal fun ensureParentDirectoryExists(filePath: String) {
    val parent = Path(filePath).parent ?: return
    if (!SystemFileSystem.exists(parent)) {
        SystemFileSystem.createDirectories(parent)
    }
}

internal fun readVoiceMessageBytes(filePath: String): ByteArray {
    return SystemFileSystem.source(Path(filePath)).buffered().use { source ->
        source.readByteArray()
    }
}

internal fun writeVoiceMessageBytes(filePath: String, bytes: ByteArray) {
    SystemFileSystem.sink(Path(filePath)).buffered().use { sink ->
        sink.write(bytes)
    }
}

internal fun deleteVoiceMessageFile(filePath: String) {
    SystemFileSystem.delete(Path(filePath), mustExist = false)
}

private val idChars = "abcdefghijklmnopqrstuvwxyz0123456789"

internal fun randomFileName(prefix: String): String =
    "$prefix-" + (1..12).map { idChars.random() }.joinToString("")

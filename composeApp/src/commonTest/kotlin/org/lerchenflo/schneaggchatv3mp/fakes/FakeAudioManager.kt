package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager

/** In-memory [AudioManager]: "files" live in [storedAudios], paths are `audio/<name>`. No sound. */
class FakeAudioManager(var mediaDurationMillis: Long = 1_000L) : AudioManager {

    val storedAudios = mutableMapOf<String, ByteArray>()

    val initializeCalls = CallLog<Unit>()
    val deletedAudios = CallLog<String>()

    override fun initializeAudio() = initializeCalls.record(Unit)

    override fun getRecordingPath(filename: String): String = "audio/$filename"

    override suspend fun saveAudioToStorage(audioBytes: ByteArray, filename: String): String {
        storedAudios[filename] = audioBytes
        return getRecordingPath(filename)
    }

    override suspend fun deleteAudio(filename: String): Boolean {
        deletedAudios.record(filename)
        return storedAudios.remove(filename) != null
    }

    override suspend fun deleteAllStoredAudio(): Int = storedAudios.size.also { storedAudios.clear() }

    override fun checkAudioExists(filePath: String): Boolean = storedAudios.keys.any { getRecordingPath(it) == filePath }

    override suspend fun getMediaDuration(path: String): Long = mediaDurationMillis

    override fun prepareBeep(frequencyHz: Int) = Unit
    override fun startBeep(frequencyHz: Int) = Unit
    override fun stopBeep() = Unit
    override fun releaseBeep() = Unit
}

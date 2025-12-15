package org.lerchenflo.schneaggchatv3mp.utilities

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class CryptoUtil {

    companion object {
        private val provider = CryptographyProvider.Default
        private val aesGcm = provider.get(AES.GCM)
        private val sha256 = provider.get(SHA256)

        /**
         * Encrypts a string using AES-GCM
         * @param plainText The text to encrypt
         * @param key The encryption key (your JWT token)
         * @return Base64 encoded encrypted string
         */
        suspend fun encrypt(plainText: String, key: String): String {
            val aesKey = deriveAESKey(key)

            val cipher = aesKey.cipher()

            val encryptedBytes = cipher.encrypt(
                plaintext = plainText.encodeToByteArray()
            )

            return Base64.encode(encryptedBytes)
        }

        /**
         * Decrypts an encrypted string
         * @param encryptedText Base64 encoded encrypted string
         * @param key The decryption key (your JWT token)
         * @return Decrypted plain text
         */
        suspend fun decrypt(encryptedText: String, key: String): String {
            val aesKey = deriveAESKey(key)

            val cipher = aesKey.cipher()

            val decryptedBytes = cipher.decrypt(
                ciphertext = Base64.decode(encryptedText)
            )

            return decryptedBytes.decodeToString()
        }

        /**
         * Derives a deterministic AES-256 key from a string (JWT)
         * using SHA-256
         */
        private suspend fun deriveAESKey(key: String): AES.GCM.Key {
            val hasher = sha256.hasher()
            val keyHash = hasher.hash(key.encodeToByteArray()) // 32 bytes

            return aesGcm
                .keyDecoder()
                .decodeFromByteArray(
                    format = AES.Key.Format.RAW,
                    bytes = keyHash
                )
        }
    }
}

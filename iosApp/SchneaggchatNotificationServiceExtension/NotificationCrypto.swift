import CryptoKit
import Foundation

enum NotificationCrypto {
    static func decrypt(base64Ciphertext: String, key: String) -> String? {
        guard let combined = Data(base64Encoded: base64Ciphertext),
              let keyBytes = key.data(using: .utf8) else {
            return nil
        }
        let derived = SymmetricKey(data: SHA256.hash(data: keyBytes))
        do {
            let sealedBox = try AES.GCM.SealedBox(combined: combined)
            let plaintext = try AES.GCM.open(sealedBox, using: derived)
            return String(data: plaintext, encoding: .utf8)
        } catch {
            return nil
        }
    }
}

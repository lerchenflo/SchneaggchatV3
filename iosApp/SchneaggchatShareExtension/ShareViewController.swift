import UIKit
import Social
import MobileCoreServices
import UniformTypeIdentifiers
import os

extension Bundle {
    var appGroupID: String {
        return "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtension"
    }
}

class ShareViewController: UIViewController {

    let logger = Logger(subsystem: Bundle.main.bundleIdentifier!, category: "sharing");

    override func viewDidLoad() {
        super.viewDidLoad()

        if let item = extensionContext?.inputItems.first as? NSExtensionItem,
           let attachment = item.attachments?.first {

            let typeURL = kUTTypeURL as String
            let typeText = kUTTypePlainText as String
            let typeImage = UTType.image.identifier

            if attachment.hasItemConformingToTypeIdentifier(typeURL) {
                attachment.loadItem(forTypeIdentifier: typeURL, options: nil) { [weak self] (data, error) in
                    // Handle URL
                    let text = (data as? URL)?.absoluteString ?? (data as? String)
                    // CALL SAVE INSIDE THE BLOCK
                    DispatchQueue.main.async {
                        self?.saveAndRedirect(text: text)
                    }
                }
            } else if attachment.hasItemConformingToTypeIdentifier(typeText) {
                attachment.loadItem(forTypeIdentifier: typeText, options: nil) { [weak self] (data, error) in
                    // Handle Text
                    let text = data as? String
                    // CALL SAVE INSIDE THE BLOCK
                    DispatchQueue.main.async {
                        self?.saveAndRedirect(text: text)
                    }
                }
            } else if attachment.hasItemConformingToTypeIdentifier(typeImage) {
                attachment.loadItem(forTypeIdentifier: typeImage, options: nil) { [weak self] (data, error) in
                    var imageData: Data?
                    if let url = data as? URL {
                        imageData = try? Data(contentsOf: url)
                    } else if let image = data as? UIImage {
                        imageData = image.jpegData(compressionQuality: 0.8)
                    } else if let rawData = data as? Data {
                        imageData = rawData
                    }
                    DispatchQueue.main.async {
                        self?.saveImageAndRedirect(imageData: imageData)
                    }
                }
            }
        }
    }

    private func saveAndRedirect(text: String?) {
        NSLog("DEBUG: Share Extension started")
        logger.info("DEBUG: Share Extension started")
        // 2. Save to the App Group so the main app can read it
        let suiteName = Bundle.main.appGroupID
        NSLog("suite name: " + suiteName)
        logger.info("DEBUG: shareExt suite name: \(suiteName, privacy: .public)")

        if let containerURL = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: suiteName) {
            logger.info("DEBUG: shareExt container URL: \(containerURL.path, privacy: .public)")
        } else {
            logger.error("DEBUG: shareExt container URL is NIL (App Group is invalid/not provisioned correctly)")
        }

        if let userDefaults = UserDefaults(suiteName: suiteName) {
            userDefaults.set(text, forKey: "sharedTextKey")
            let success = userDefaults.synchronize()
            NSLog("DEBUG: Saved text: \(text ?? "nil") - Success: \(success)")
            logger.info("DEBUG: Saved text: \(text ?? "nil", privacy: .public)) - Success: \(success, privacy: .public)")
        } else {
            NSLog("DEBUG: ERROR - Could not initialize UserDefaults with suiteName")
            logger.info("DEBUG: ERROR - Could not initialize UserDefaults with suiteName")
        }

        // 3. Open the Main App via URL Scheme
        // Note: Extensions cannot use UIApplication.shared.open
        let url = URL(string: "schneaggchat://share")!
        var responder: UIResponder? = self
        while responder != nil {
            if let application = responder as? UIApplication {
                application.open(url, options: [:], completionHandler: nil)
                break
            }
            responder = responder?.next
        }

        // 4. Close the extension
        self.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }

    private func saveImageAndRedirect(imageData: Data?) {
        guard let imageData = imageData else {
            extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
            return
        }

        let suiteName = Bundle.main.appGroupID
        guard let containerURL = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: suiteName
        ) else {
            extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
            return
        }

        // Create shared images directory
        let imagesDir = containerURL.appendingPathComponent("SharedImages")
        try? FileManager.default.createDirectory(at: imagesDir, withIntermediateDirectories: true)

        // Clean old files
        if let files = try? FileManager.default.contentsOfDirectory(at: imagesDir, includingPropertiesForKeys: nil) {
            for file in files { try? FileManager.default.removeItem(at: file) }
        }

        // Save image with timestamp filename
        let filename = "\(Int(Date().timeIntervalSince1970 * 1000)).jpg"
        let fileURL = imagesDir.appendingPathComponent(filename)

        try? imageData.write(to: fileURL)

        if let userDefaults = UserDefaults(suiteName: suiteName) {
            userDefaults.set([filename], forKey: "sharedImageKeys")
            userDefaults.synchronize()
        }

        // Open main app
        let url = URL(string: "schneaggchat://share")!
        var responder: UIResponder? = self
        while responder != nil {
            if let application = responder as? UIApplication {
                application.open(url, options: [:], completionHandler: nil)
                break
            }
            responder = responder?.next
        }

        extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}


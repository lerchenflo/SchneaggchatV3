import UIKit
import Social
import MobileCoreServices
import os

extension Bundle {
    var appGroupID: String {
        return "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtention"
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
}


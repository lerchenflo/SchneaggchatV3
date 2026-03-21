import Foundation
import UIKit

public class AppUpdateChecker {
    public static func checkForUpdate() {
        guard let info = Bundle.main.infoDictionary,
              let currentVersion = info["CFBundleShortVersionString"] as? String,
              let bundleId = Bundle.main.bundleIdentifier else { return }
        
        let urlString = "https://itunes.apple.com/lookup?bundleId=\(bundleId)&t=\(Int(Date().timeIntervalSince1970))"
        guard let url = URL(string: urlString) else { return }
        
        URLSession.shared.dataTask(with: url) { data, response, error in
            if let error = error {
                return
            }
            
            guard let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200,
                  let data = data else { return }
            
            guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let results = json["results"] as? [[String: Any]],
                  results.count > 0 else { return }
            
            let firstResult = results[0]
            
            guard let latestVersion = firstResult["version"] as? String,
                  let appStoreURL = firstResult["trackViewUrl"] as? String else { return }
            
            let comparisonResult = latestVersion.compare(currentVersion, options: .numeric)
            print("🔍 [AppUpdateChecker] Version comparison - Latest: '\(latestVersion)' vs Current: '\(currentVersion)' - Result: \(comparisonResult)")
            
            if comparisonResult == .orderedDescending {
                showUpdateSuggestionPopUp(appStoreURL: appStoreURL)
            }
        }.resume()
    }

    private static func showUpdateSuggestionPopUp(appStoreURL: String) {
        DispatchQueue.main.async {
            let alert = UIAlertController(
                title: "Update Required",
                message: "A new version of the app is required to continue. Please update now.",
                preferredStyle: .alert
            )
            
            alert.addAction(UIAlertAction(title: "Close App", style: .destructive) { _ in
                UIApplication.shared.perform(#selector(NSXPCConnection.suspend))
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    exit(0)
                }
            })
            
            alert.addAction(UIAlertAction(title: "Update", style: .default) { _ in
                openAppStore(appStoreURL: appStoreURL)
            })
            
            if let scene = UIApplication.shared.connectedScenes
                .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
               let rootVC = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController {
                rootVC.present(alert, animated: true)
            }
        }
    }

    private static func openAppStore(appStoreURL: String) {
        if let url = URL(string: appStoreURL) {
            UIApplication.shared.open(url)
        }
    }
}
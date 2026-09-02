import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
            // IOSKEYBOARDFIX: explicitly ignore the keyboard safe area too, so SwiftUI never
            // resizes/repositions the hosting view when the keyboard shows - Compose already
            // handles the keyboard itself via WindowInsets.ime. Without this, on some devices
            // the two keyboard-avoidance mechanisms fight, leaving a stale gap under the input.
            .ignoresSafeArea(.keyboard, edges: .bottom)
    }
}




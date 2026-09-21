import SwiftUI
import SharedApp
import WidgetKit

extension NSNotification.Name {
    static let deviceDidShakeNotification = NSNotification.Name("deviceDidShakeNotification")
}

extension UIWindow {
    open override func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        super.motionEnded(motion, with: event)
        if motion == .motionShake {
            NotificationCenter.default.post(name: .deviceDidShakeNotification, object: nil)
        }
    }
}

@main
struct iOSApp: App {
    init() {
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("ReloadWidgetsNotification"),
            object: nil,
            queue: .main
        ) { _ in
            WidgetCenter.shared.reloadAllTimelines()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
    }
}

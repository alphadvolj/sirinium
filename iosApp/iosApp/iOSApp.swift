import SwiftUI
import SharedApp
import WidgetKit

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

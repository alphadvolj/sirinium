import SwiftUI
import SharedApp

@main
struct iOSApp: App {
    init() {
        MainViewControllerKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
    }
}

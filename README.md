# RequestLite

A lightweight, local-first REST API client for Android and iOS. It allows developers, QA engineers, students, support engineers, and technical operators to build, send, save, inspect, and repeat HTTP requests from a phone.

## Screenshots

<p align="center">
  <img src="images/android_screenshot.png" width="300" alt="Android Screenshot" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="images/ios_screenshot.png" width="300" alt="iOS Screenshot" />
</p>

## Features

- **Mobile First Design**: Touch-friendly request builder, large targets, and a progressive request editor tailored for a phone screen.
- **Quick Run & cURL Paste**: Start with a blank GET request or directly paste a cURL command from your clipboard to run it quickly.
- **Safe Environments**: Manage environments (Local, Staging, Production) without revealing protected secrets. Environment locators ensure you always know where your requests are heading.
- **History & Collections**: Treat history as a debugging journal with notes and immutable snapshots. Pin favorite requests for one-tap reruns.
- **Advanced Response Viewer**: Inspect formatted JSON with tree, raw, search, wrap, and copy-path tools built natively for mobile.
- **Drafts & Offline Mode**: Preserve every unfinished request as a recoverable draft. The app operates locally without requiring any account or internet connection for organizing collections.
- **Multiplatform**: Powered by Kotlin Multiplatform and Compose Multiplatform for a native-like experience with shared business logic and a unified "Quiet Signal" design system.

## Build and Run

### Android
To install and run on an Android emulator:
```bash
./gradlew :androidApp:installDebug
```

### iOS
To build and run on an iOS simulator:
```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16 Pro' build
```

# POS App Prototype

A mobile/tablet-optimized Point of Sale (POS) application prototype designed for SUNMI devices. This project consists of a high-fidelity web frontend prototype and a native Android wrapper that bridges web actions with the device's native capabilities, such as the built-in thermal printer and status bar control.

---

## 🛠️ Technology Stack

The project is built using a lightweight and efficient tech stack to guarantee fast loading speeds, ease of deployment, and smooth native integration:

### 1. Web Frontend
*   **HTML5 & Vanilla JavaScript**: Core logic and structures are built with clean, native JavaScript (no heavy frontend frameworks like React or Vue), minimizing bundle size and execution overhead.
*   **Vanilla CSS3**: Tailored layout designs, responsive grid/flexbox layouts, custom animations, and a simulated smartphone/tablet shell frame.
*   **Typography**: [Poppins](https://fonts.google.com/specimen/Poppins) font integrated via Google Fonts.
*   **Icons**: [Tabler Icons](https://tabler.io/icons) loaded dynamically via CDN.
*   **Receipt Styling**: Customized layout and print stylesheets in `bon.html` specifically calibrated for 80mm thermal receipt printers.

### 2. Android Wrapper
*   **Language**: Java (Targeting Android SDK 34, Min SDK 26, Java 17).
*   **Web Engine**: Native Android `WebView` configuration supporting DOM storage, hardware acceleration, and offline databases.
*   **Hybrid Bridge**: Native Android `@JavascriptInterface` bindings:
    *   `AndroidPrinter`: Bridges the web app to the SUNMI printer SDK for printing text, columns, bitmaps, wrapping, and paper cutting.
    *   `AndroidStatusBar`: Allows the web application to dynamically adjust the device's system status bar color and icon brightness.
*   **Dependencies**: [SUNMI Printer Library](https://github.com/sunmidev/SunmiPrinterSDK) (`com.sunmi:printerlibrary:1.0.24`) to communicate with SUNMI terminal thermal printers.

### 3. CI/CD & Hosting
*   **Hosting**: [Netlify](https://www.netlify.com/) configured via `netlify.toml` for static web hosting.
*   **CI/CD**: GitHub Actions workflow (`build-apk.yml`) that triggers on changes to compile, sign, and build the debug APK using Gradle, uploading it directly as a release asset.

---

## 📂 Project Structure

```
pos-app-prototype/
├── .github/workflows/
│   └── build-apk.yml       # GitHub Actions workflow for automatic Android APK builds
├── android-app/            # Native Android shell project
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/opining/pos/MainActivity.java  # WebView shell & JS bridge
│   │   │   ├── AndroidManifest.xml                    # App configuration and permissions
│   │   │   └── res/                                   # App resources (icons, themes)
│   │   └── build.gradle    # App module gradle build configuration
│   └── build.gradle        # Project-level gradle build configuration
├── bon.html                # 80mm thermal receipt layout preview
├── index.html              # Core POS web application (570KB+ single-file prototype)
├── manifest.json           # Progressive Web App (PWA) manifest
├── netlify.toml            # Netlify hosting configurations
└── logo.png / icons        # Web app and PWA icon assets
```

---

## 🚀 How to Run the Project

### 1. Running the Web Prototype Locally

Since the frontend is a standalone static site, there is no build process required. You have a few options to run it:

#### Option A: Direct Open (Easiest)
Simply double-click the [index.html](file:///d:/laragon/www/pos-app-prototype/index.html) file to open it directly in any web browser.

#### Option B: Live Server (Recommended for PWA/Manifest testing)
To test features that require an HTTP context, run a local web server:

*   **Using Python (3.x)**:
    Run the following command in your terminal inside the project directory:
    ```bash
    python -m http.server 8000
    ```
    Then, open [http://localhost:8000](http://localhost:8000) in your browser.

*   **Using Node.js**:
    If you have Node.js installed, run:
    ```bash
    npx serve .
    ```
    Then, open [http://localhost:3000](http://localhost:3000) in your browser.

*   **Using PHP**:
    If you use Laragon or local PHP installation:
    ```bash
    php -S localhost:8000
    ```

---

### 2. Building & Running the Android App

The Android wrapper compiles into an APK that runs on Android tablets and SUNMI POS hardware.

#### Prerequisites
*   [Android Studio](https://developer.android.com/studio) installed.
*   Java Development Kit (JDK) 17 installed.
*   An Android device or emulator (SUNMI hardware required to test physical print features).

#### Steps
1.  Launch **Android Studio**.
2.  Choose **Open an Existing Project** and select the [android-app](file:///d:/laragon/www/pos-app-prototype/android-app) folder inside this repository.
3.  Wait for Android Studio to sync the Gradle dependencies.
4.  Enable **USB Debugging** on your target Android device:
    *   Go to *Settings* -> *About Device* -> Tap *Build Number* 7 times.
    *   Go to *System* -> *Developer Options* -> Enable *USB Debugging*.
5.  Connect your device to your computer via USB.
6.  Select your device in the toolbar dropdown list and click the green **Run (Play)** button, or press `Shift + F10`.
7.  The application will build, deploy, and launch on your device.

#### Building APK via Command Line
To build a debug APK manually using Gradle from your command line:
1.  Navigate into the `android-app` directory:
    ```bash
    cd android-app
    ```
2.  Run the Gradle build task:
    *   **On Windows (PowerShell/CMD)**:
        ```powershell
        .\gradlew.bat assembleDebug
        ```
    *   **On macOS/Linux**:
        ```bash
        ./gradlew assembleDebug
        ```
3.  The generated APK will be available at:
    `android-app/app/build/outputs/apk/debug/app-debug.apk`

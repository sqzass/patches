# 🧩 sqz patches

[![Release](https://img.shields.io/github/v/release/sqzass/patches?logo=github&color=blue)](https://github.com/sqzass/patches/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Add to Morphe](https://img.shields.io/badge/Morphe-Add_Source-7C4DFF?logo=android&logoColor=white)](https://morphe.software/add-source?github=sqzass/patches)

Universal and customized patches for Android apps, compatible with [Morphe](https://morphe.software).

## ❓ About

sqz patches is a custom patches bundle for use with the Morphe patcher. It includes universal patches such as permission stripping for enhanced privacy.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=sqzass/patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.3.0](https://github.com/sqzass/patches/releases/tag/v1.3.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;12 patches total
<details open>
<summary>🌐 Universal&nbsp;&nbsp;•&nbsp;&nbsp;12 patches</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Allow screenshots and screen recording](#allow-screenshots-and-screen-recording) | Removes recent-apps preview hiding and screen capture restrictions across application activities. | • Enable Recent App Previews<br>• Remove Capture Restrictions |
| [Bypass root and debugger checks](#bypass-root-and-debugger-checks) | Reports the debugger as disconnected and hides root indicator files from existence checks, defeating common detection heuristics. | • Bypass Debugger Checks<br>• Bypass Root File Checks |
| [Bypass screenshot detection](#bypass-screenshot-detection) | Disables screenshot detection callbacks so the app is not notified when a screenshot is taken. |  |
| [Disable boot start-up](#disable-boot-start-up) | Removes boot intent filters so the app cannot start itself when the device boots. | • Remove Boot Intent Filters |
| [Disable tracker SDK calls](#disable-tracker-sdk-calls) | Replaces void-returning calls into known tracking SDKs with no-op instructions so analytics, attribution, and advertising code never runs at runtime. |  |
| [Enable debugging and inspection](#enable-debugging-and-inspection) | Enables application debugging flags, permits cleartext traffic for proxy inspection, and strips test-only restrictions in the application manifest. | • Enable Debugging<br>• Allow Cleartext Traffic<br>• Remove Test-Only Flag |
| [Open links in browser](#open-links-in-browser) | Stops http and https links from opening inside the app by removing app link verification and link intent filters. | • Remove App Link Verification<br>• Remove Link Intent Filters |
| [Remove FLAG_SECURE](#remove-flag-secure) | Removes FLAG_SECURE from window flag operations so secure applications allow screenshots and screen recording. |  |
| [Strip package visibility](#strip-package-visibility) | Removes package visibility declarations so the app cannot enumerate other installed applications. | • Remove Queries Declarations<br>• Remove QUERY_ALL_PACKAGES Permission |
| [Strip sensitive permissions](#strip-sensitive-permissions) | Strips sensitive Android permissions (such as camera and microphone access) from the application manifest to enhance privacy. | • Strip Camera<br>• Strip Microphone<br>• Strip Location<br>• Strip Sensors<br>• Strip Contacts<br>• Strip Phone & SMS<br>• Strip Calendar<br>• Strip Storage & Media<br>• Strip Nearby Wi-Fi<br>• Strip Overlay Access<br>• Strip Special Access |
| [Strip trackers and telemetry](#strip-trackers-and-telemetry) | Strips known tracking, analytics, and advertising SDK components (such as Google Analytics, Firebase, AppsFlyer, Adjust, Facebook, and Crashlytics) from the application manifest. | • Strip Google & Firebase Analytics<br>• Strip Facebook Analytics<br>• Strip Attribution Trackers<br>• Strip Crash Reporting Telemetry<br>• Strip Advertising ID Permissions |
| [Trust user certificates](#trust-user-certificates) | Installs a network security config that trusts user-installed CA certificates so HTTPS traffic can be inspected with proxy tools. | • Trust User Certificates<br>• Permit Cleartext Traffic |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop) like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

sqz patches are licensed under the [GNU General Public License v3.0](LICENSE)

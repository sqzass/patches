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
> **[v1.1.0](https://github.com/sqzass/patches/releases/tag/v1.1.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>🌐 Universal&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Strip sensitive permissions](#strip-sensitive-permissions) | Strips sensitive Android permissions (such as camera and microphone access) from the application manifest to enhance privacy. | • Strip Camera<br>• Strip Microphone<br>• Strip Location<br>• Strip Sensors |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop) like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

sqz patches are licensed under the [GNU General Public License v3.0](LICENSE)

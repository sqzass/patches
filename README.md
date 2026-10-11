# 🧩 sqz patches

Universal and customized patches for Android apps, compatible with [Morphe](https://morphe.software).

## ❓ About

sqz patches is a custom patches bundle for use with the Morphe patcher. It includes universal patches such as permission stripping for enhanced privacy.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=sqzass/patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.1](https://github.com/sqzass/patches/releases/tag/v1.0.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;2 patches total
<details open>
<summary>📦 XYZ app&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 2.0.0 | 1.0.2 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Example Patch](#example-patch) | Example patch to start with. |  |

</details>

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

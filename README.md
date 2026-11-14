# 🧩 Lustx Patches

> **Lustx Patches by sqzass**  
> Custom patches for use with the Morphe app patcher.

## ❓ About

**Lustx Patches** is a custom patch bundle by **sqzass**, built for compatibility with the [Morphe](https://morphe.software) patcher ecosystem and based on prior work of the Morphe and [ReVanced](https://github.com/ReVanced) projects.

### How to use these patches

Add this repository source to Morphe Manager:
`https://github.com/sqzass/lustx-patches`

---

## 🛠️ Tools & Development

This repository includes a local helper tool for decompiling Android apps and split packages (`.apk`, `.apks`, `.xapk`, `.apkm`, `.aab`, `.zip`) to inspect Java sources, Smali bytecode, and Android resources when developing custom patches:

- **Decompile all app packages in `apks/`**:
  ```bash
  python scripts/decompile.py
  ```

_Note: All app files placed in `apks/` are processed and automatically deleted once decompilation successfully completes._

&nbsp;

## 🩹 Patches list

<!-- PATCHES_START -->
> **[v1.10.2](https://github.com/sqzass/lustx-patches/releases/tag/v1.10.2)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>📦 Pawxy - Fast VPN & Web Browser&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 1.15.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Unlock premium](#unlock-premium) | Unlocks Pawxy premium features (VPN servers, removes paywall and upsell badges). |  |

</details>

<!-- PATCHES_END -->

## Contributing

We currently accept pull requests for fixes/improvements to the Pawxy patch.
If you have developed patches for other apps, you are encouraged to release your patches in a third party repository.
See the [Patches template](https://github.com/morpheapp/morphe-patches-template) for more information.

Before opening a pull request, read the [contribution guidelines](CONTRIBUTING.md).

## 📜 License

Lustx Patches are licensed under the [GNU General Public License v3.0](LICENSE), with additional conditions under GPLv3 Section 7 preserving upstream notices and disclaimers.

- **Attribution (7b):** Any use of this code, including derivative works, must preserve all original notices and disclaimers.
- **Name & Branding Restrictions (7c & 7e):** Derivative works must use their own distinct branding. The **"Morphe"** name, logos, and trademarks may not be used for the branding or title of derivative works (e.g., names like _"Morphe Plus"_, _"Morphe Expanded"_, or _"Morphe UserXYZ"_ are strictly prohibited).

See the [LICENSE](LICENSE) file for full GPLv3 terms and the [NOTICE](NOTICE) file for full conditions of GPLv3 Section 7 and upstream attributions.

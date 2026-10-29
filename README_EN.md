[简体中文](README.md) | English

<p align="center">
  <img src="docs/assets/foxsu-logo.png" width="132" alt="FoxSu" />
</p>

<h1 align="center">FoxSu</h1>

<p align="center">
  A kernel-based root solution for Android<br />
  <sub>KernelSU inside, a completely rewritten manager on top</sub>
</p>

<p align="center">
  <a href="https://github.com/Cmry-mlc/FoxSu/actions/workflows/build-manager.yml"><img src="https://img.shields.io/github/actions/workflow/status/Cmry-mlc/FoxSu/build-manager.yml?branch=main&label=build&style=flat-square" alt="Build" /></a>
  <a href="https://github.com/Cmry-mlc/FoxSu/releases"><img src="https://img.shields.io/badge/releases-view%20all-blue?style=flat-square" alt="Releases" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-orange?style=flat-square" alt="License" /></a>
  <img src="https://img.shields.io/badge/Android-12%2B%20(API%2031)-3DDC84?style=flat-square" alt="Android" />
  <img src="https://img.shields.io/badge/Kernel-GKI%205.10%2B%20%7C%204.14%2B-1E88E5?style=flat-square" alt="Kernel" />
</p>

---

## What is FoxSu

FoxSu is a fork of [KernelSU](https://github.com/tiann/KernelSU). Root stays in the kernel — `su`, the allowlist, app profiles, module mounting and sucompat all come from the kernel module and `ksud`. What changed is the manager: new package name (`com.foxsu.manager`), rewritten screens, a new install flow, five skins and a deeply customisable dock.

> Root in the kernel, experience in the manager.

## Manager features

### Five skins

Same features, five layout languages. Switch in Settings → Interface style; every screen follows.

| Skin | Style | Notes |
| --- | --- | --- |
| **Redesign** (Miuix) | Glass cards + capsule dock | Default. Densest layout: one hero card plus four KPI cards |
| **Editorial** | Magazine layout | Paper background, ink rules, serif headings, native dock (capsule optional) |
| **Pages** (Fresh) | Generous whitespace | Large titles and sectioned lists for one-handed use |
| **Glass** | Liquid glass | Translucent surfaces with highlights; pairs well with a wallpaper |
| **Nocturne** | Dark neon | Dark-first, with glowing accents |

### Dock: 13 capsule styles

The dock is decoupled from the skin and selectable globally. Side-drawer mode reserves left space on every page so content is never covered.

`Original capsule` · `Expanding pill` · `Side drawer` · `Flowing cursor` · `Neon outline` · `Block cards` · `Solid gradient` · `Top indicator` · `Floating disc` · `Editorial ink` · `White pill` · `Liquid pill` · `Nocturne neon`

Corner radius, opacity, icon shape (rounded square / circle / squircle / rainbow gradient), immersive glow, liquid glass, rainbow icons and Monet tinting are all adjustable.

### Customisation centre

Every option is wired to real settings and applies immediately:

- **Wallpaper**: pick, crop, zoom, dim; the global background can be disabled
- **Card artwork**: separate pictures for the hero card, the four KPI cards and each module card, with per-item opacity
- **Card colours**: a global tint plus per-card overrides, independent opacity, one-tap rainbow cards
- **Fonts**: system default, serif, monospace, rounded handwriting, or your own font file
- **Title**: custom home title
- **Launcher icon**: four fox faces that follow light/dark mode

### Root and permissions

- **Allowlist**: only granted apps can reach `su`; everything else cannot detect root
- **App profile**: per-uid root and non-root behaviour — su uid / gid / groups / capabilities, SELinux context, module unmounting
- **Profile templates**: store a policy once and apply it to many apps
- **Umount modules by default** for apps without a profile
- **Classic SU command** so legacy apps can keep using `/system/bin/su`
- **Kernel umount**: modules are unmounted in the kernel, nothing visible from userspace
- **Hide SELinux modification**
- **SU Log**: root grants and denials recorded and readable inside the manager

### Modules

- Install from a local zip, with module info and conflict warnings before flashing
- **Module repository**: built-in online index to browse, search, inspect and install
- Metamodule detection and highlighting, with a warning before uninstall
- Run module action scripts in one tap, or run arbitrary shell scripts
- Update checks, enable / disable, sorting by name, stars or update time
- Undo an uninstall to bring a module back, and create home-screen shortcuts for modules

### Install and flash

- One-screen install wizard: kernel and KMI info, install method, partition selection and advanced options without a second page
- Direct install, a local LKM file, or hand-off to another manager
- Optional shell access, ADB, and a forced backup before flashing
- Dedicated flash and script screens with live output

### Diagnostics

- **Crash log**: uncaught exceptions go to `files/crash/last-crash.txt` and are mirrored to external storage
- **Soft reboot** instead of a full reboot when a change needs one
- Update checks for the manager and the LKM
- Temporary / permanent unroot, restore stock image

## Compatibility

FoxSu officially supports **GKI 2.0** devices (kernel 5.10+). Older kernels (4.14+) work too, but the kernel has to be built manually.

The manager requires **Android 12 (API 31)** or newer; it targets API 37 and builds with JDK 21 and the NDK.

## Installation

> Installing the APK alone grants no root. The device kernel must already contain the FoxSu / KernelSU driver.

1. Download the latest manager APK from [Releases](https://github.com/Cmry-mlc/FoxSu/releases) and install it.
2. Prepare a kernel with the FoxSu driver: use a GKI kernel matching your device / KMI, or build the kernel or an LKM yourself.
3. In the manager's Install screen, pick a method and partition, flash, then reboot.
4. The first launch shows an onboarding tour covering granting, modules, app profiles and logs.

The kernel identifies its manager by package name and signing fingerprint, so third-party apps cannot impersonate the FoxSu manager and take over root grants.

## Building from source

Manager:

```bash
cd manager
./gradlew assembleRelease \
  -PKEYSTORE_FILE=/path/to/your.jks \
  -PKEY_ALIAS=your_alias \
  -PKEYSTORE_PASSWORD=****** \
  -PKEY_PASSWORD=******
```

The version code is `30000 + git commit count`; the version name comes from `git describe`.

Kernel and userspace:

- `kernel/` — kernel module, built against a device kernel tree or the DDK; after changing the package name or signature constants, the kernel or LKM must be rebuilt for it to take effect
- `userspace/ksud/` — Rust daemon, `cargo ndk -t arm64-v8a build`
- `.github/workflows/` — manager, ksud, LKM and full release pipelines

## Relationship to KernelSU

FoxSu is based on [KernelSU](https://github.com/tiann/KernelSU) (GPL-3.0). The kernel and `ksud` capabilities come from upstream and its community; the manager is maintained here. Please report FoxSu-specific issues to [this repository](https://github.com/Cmry-mlc/FoxSu/issues) rather than upstream.

## Credits

- [KernelSU](https://github.com/tiann/KernelSU) — the kernel root solution, module system and translations
- Everyone who filed feedback, tested builds or sent suggestions

## License

[GPL-3.0](LICENSE)

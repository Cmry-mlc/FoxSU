[简体中文](README.md) | English

<p align="center">
  <img src="docs/assets/foxsu-logo.png" width="132" alt="FoxSu" />
</p>

<h1 align="center">FoxSu</h1>

<p align="center">
  A kernel-based root solution for Android<br />
  <sub>KernelSU underneath, a manager rewritten from scratch</sub>
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

FoxSu is a fork of [KernelSU](https://github.com/tiann/KernelSU). Root stays in the kernel — `su`, the allowlist, app profiles, module mounting and sucompat all come from the kernel module and `ksud`. What changed is the manager: new package name (`com.foxsu.manager`), rewritten screens and install flow, plus five skins, 21 dock styles, 40 dock effects, 20 dock icon sets and 21 home background effects on top.

> Root in the kernel, experience in the manager.

## At a glance

| Layer | What you get |
| --- | --- |
| Skins | 5, applied to every screen at once |
| Dock | 21 capsule styles, 10 button shapes, decoupled from the skin |
| Dock effects | 20 inside the dock + 20 on its outline, each optional and stackable |
| Dock icons | 20 shape sets, recoloured by the theme instead of hard-coded |
| Background | Paw ambience + 20 full-screen effects, pick one of 21, own master switch |
| Customisation | Wallpaper, card artwork, card colours, fonts, title, launcher icon |
| Root | Allowlist, app profiles, templates, classic SU, kernel umount, SELinux hide, SU log |
| Modules | Local zip, online repo, metamodule detection, action scripts, undo uninstall, shortcuts |
| Install | One-screen wizard, direct / inactive slot / local image / download, live output |

## Manager features

### Five skins

Same features, five layout languages. Switch in Settings → Interface style; every screen follows.

| Skin | Style | Notes |
| --- | --- | --- |
| **Miuix** | Glass cards + capsule dock | Default. Densest layout: one status card plus stats / device / security cards |
| **Editorial** | Magazine layout | Paper background, ink rules, serif headings, native dock (capsule optional) |
| **Pages** | Generous whitespace | Large titles and sectioned lists for one-handed use |
| **Glass** | Liquid glass | Translucent surfaces with highlights; pairs well with a wallpaper |
| **Nocturne** | Dark neon | Dark-first, with glowing accents |

### Dock

The dock is decoupled from the skin and selected globally — switching skins no longer changes how you navigate. Metrics stay fixed (78dp items, 60dp tall) so the bar never jumps when you switch styles.

**21 capsule styles**

`Coral bubble` · `Film strip` · `Neon tube` · `Glass stack` · `Single focus` · `Cloud sticker` · `Slide track` · `Original capsule` · `Expanding pill` · `Side drawer` · `Flowing cursor` · `Neon outline` · `Block cards` · `Solid gradient` · `Top indicator` · `Floating disc` · `Centre avatar` · `Editorial ink` · `White pill` · `Liquid pill` · `Nocturne neon`

The first seven use a different grid: 48dp square hit areas, 6dp inset, and some variants keep a fox avatar in the middle as the donation entry. `Side drawer` docks left or right and the page content makes room for it.

**10 button shapes**

`Pill` · `Circle` · `Squircle` · `Outline` · `Double` · `Cut` · `Keycap` · `Card` · `Glass` · `Glow`

Selected and unselected are two states of the same shape, so a switch swaps the whole row rather than mixing old and new.

**40 tap effects, two scopes**

The inside of the dock and its outline are two independent pickers. Use one, or stack them — "breathing outline + bouncing icons", for example.

- **20 inner effects** (icons and the selected capsule): jelly slide, bounce landing, jelly shake, spin-in, heartbeat, wave, shine sweep, halo ring, bubbles, stardust, capsule pop, wiggle, 3D flip, gradient flow, label lift, magnet pull, dot trail, ripple, elastic overshoot, mini firework
- **20 outline effects** (the border of the whole dock): breathing glow, marquee dot, border flow, rainbow stroke, dashed rotation, flowing glow, tap ripple, selection flash, stroke breathing, electric stroke, follow halo, sweep, shockwave, selection spot, selection orbit, glint, elastic frame, shadow lift, edge sparks, soundwave

Outline effects split into two groups: always-on ones loop, interactive ones play once on a page change or press. Inside the dock only `Gradient flow` loops; the rest are one-shot timelines that stop when finished, so nothing keeps burning frames.

**20 dock icon sets**

`Architectural section` · `Origami` · `Map top-down` · `Pixel 8-bit` · `Sticky note` · `Molecular` · `Gauge ticks` · `Overlapping rings` · `Fault slice` · `Crayon doodle` · `Block grid` · `Contour lines` · `Soundwave` · `Building blocks` · `Stamp` · `Document folder` · `Wireframe` · `Folding fan` · `Ripple` · `Star trail`

Only the shape changes; colouring still follows the accent colour and selection state, so swapping icons never breaks the palette.

**Dock behaviour**

- **Water-drop indicator**: the selection stretches, squashes and springs to the new tab; it suspends while idle instead of spinning every frame
- **Page push**: content follows a page change slightly, independent of the water-drop switch
- **Auto-hide**: the capsule sinks after N seconds of inactivity and slides back on a tap
- **Appearance**: corner radius, opacity, immersive glow, liquid glass, rainbow icons, Monet tinting

### Ambience and motion

- **21 background effects**: `Paw ambience` plus 20 full-screen effects, mutually exclusive, one master switch. Drawn above content but never taking pointer events, so taps pass through
- **The 20**: breathing glows · drifting dust · aurora bands · ripples · soft grid · diagonal sweep · atmospheric fog · constellation · liquid gradient · rain · sparkles · orbit rings · colour drift · sonar · falling petals · slow stripes · parallax glows · vertical breathing · particle vortex · dawn glow
- **Paw ambience**: cat paws, small hearts and four-point stars drift up from the bottom of the screen on the home screen, the donate page and the customisation header card; it sits first in the same picker
- **Motion level**: `Off` / `Restrained` / `Full`, controlling card particle edges, 3D touch tilt and dock tap ripples together
- **Page swipe gesture**: `Default` / `Cross-axis` / `iOS style` / `Off`; choosing Off also disables swiping the dock
- Predictive back and edge swipe-back can be toggled separately

### Customisation centre

Every option is wired to real settings and applies immediately, no restart:

- **Wallpaper**: pick, crop, zoom, dim; the global background can be disabled
- **Card artwork**: separate pictures for the hero card, the stat cards and each module card, with per-item opacity
- **Card colours**: a global tint plus per-card overrides, independent opacity, one-tap rainbow cards
- **Fonts**: system default, Noto Serif SC, JetBrains Mono, LXGW WenKai, or your own font file
- **Title**: custom home title text and colour
- **Launcher icon**: five fox faces plus the original KernelSU icon, following light/dark mode
- Any of the above can be reset to defaults in one tap

### Root and permissions

- **Allowlist**: only granted apps can reach `su`; everything else cannot detect root
- **App profile**: per-uid root and non-root behaviour — su uid / gid / groups / capabilities, SELinux context, module unmounting
- **Profile templates**: store a policy once and apply it to many apps
- **Umount modules by default** for apps without a profile
- **Classic SU command** so legacy apps can keep using `/system/bin/su`
- **Kernel umount**: modules are unmounted in the kernel, nothing visible from userspace
- **Hide SELinux modification**
- **SU Log**: root grants and denials recorded and readable inside the manager
- **Shell hardening**: optionally allow shell root only when the shell was started by adbd or a KernelSU root process

### Modules

- Install from a local zip, with module info and conflict warnings before flashing
- **Module repository**: built-in online index to browse, search, inspect and install, with size and SHA-256 verification on download
- **Bundled modules**: Magic Mount-rs, Zygisk Next and Oh My Keymint ship inside the APK — install, update, toggle and uninstall from a detail page, and open a module's own webroot UI when it has one
- Metamodule detection and highlighting, with a warning before uninstall
- Run module action scripts in one tap, or run arbitrary shell scripts
- Update checks, enable / disable, sorting by name, stars or update time
- Undo an uninstall to bring a module back, and create home-screen shortcuts for modules
- Module cards are painted from the current theme: an accent rail and an initial badge when enabled, a muted card when disabled
- Descriptions collapse after a configurable number of lines and expand on tap

### Install and flash

- One-screen install wizard: kernel and KMI info, install method, partition selection and advanced options without a second page
- Four methods: direct install, install to the inactive slot (after an OTA), patch a local image, download and patch an image
- Optional shell access, ADB, and a forced backup before flashing; direct flashing is disabled while in jailbreak mode
- Dedicated flash and script screens with live output

### Diagnostics

- **Crash log**: uncaught exceptions go to `files/crash/last-crash.txt` and are mirrored to external storage
- **Soft reboot** instead of a full reboot when a change needs one
- Update checks for the manager and the LKM, with custom LKMs flagged separately
- Temporary / permanent unroot, restore stock image

## Compatibility

FoxSu officially supports **GKI 2.0** devices (kernel 5.10+). Older kernels (4.14+) work too, but the kernel has to be built manually.

The manager requires **Android 12 (API 31)** or newer; it targets API 37 and builds with JDK 21 and the NDK.

## Installation

> Installing the APK alone grants no root. The device kernel must already contain the FoxSu / KernelSU driver.

1. Download the latest manager APK from [Releases](https://github.com/Cmry-mlc/FoxSu/releases) and install it.
2. Prepare a kernel with the FoxSu driver: use a GKI kernel matching your device / KMI, or build the kernel or an LKM yourself.
3. In the manager's Install screen, pick a method and partition, flash, then reboot.
4. The first launch shows a five-page tour covering granting, modules, app profiles and logs.

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

The version comes from the `VERSION` file at the repository root (e.g. `3.0.53`): `versionCode = major*10000 + minor*100 + patch`, and `versionName` is the string itself. The manager, `ksud` and the kernel module all read that one file, so the three can never disagree — which is what makes "does the LKM need an update" answerable at all, and what stops local and CI builds from refusing to install over each other.

Release builds (tag-triggered) and signing-key setup are documented in [RELEASE.md](RELEASE.md).

Kernel and userspace:

- `kernel/` — kernel module, built against a device kernel tree or the DDK; after changing the package name or signature constants, the kernel or LKM must be rebuilt for it to take effect
- `userspace/ksud/` — Rust daemon, `cargo ndk -t arm64-v8a build`
- `.github/workflows/` — manager, ksud, LKM and full release pipelines

### Changing the package name / signing key

The kernel only recognises a manager when both conditions hold: the package under `/data/app` equals `KSU_PACKAGE_NAME`, and the APK's v2 signing certificate length and SHA-256 equal `KSU_EXPECTED_SIZE` / `KSU_EXPECTED_HASH` (see `kernel/manager/throne_tracker.c`). Those constants live in `kernel/Kbuild`, `manager/app/build.gradle.kts` and `userspace/ksud/build.rs`; touching only one of them, or rebuilding the APK without the LKM, shows up as **"not installed" in the manager, no root prompt, and no way to grant access**.

One script keeps them in sync and derives the signature constants from a keystore:

```bash
./scripts/manager-identity.sh set com.your.package          # sync all three defaults
KEYSTORE_PASSWORD=****** ./scripts/manager-identity.sh sign /path/to/your.jks your_alias
./scripts/manager-identity.sh show                          # are the three consistent?
./scripts/manager-identity.sh check app-release.apk         # will this APK be recognised?
```

Then rebuild in order: `ksud` → manager APK → `kernelsu.ko`/LKM, reflash the boot image, and reinstall the manager. Allowlist entries from the old package stay keyed by UID and are harmless, but you will need to grant access again.

## Relationship to KernelSU

FoxSu is based on [KernelSU](https://github.com/tiann/KernelSU) (GPL-3.0). The kernel and `ksud` capabilities come from upstream and its community; the manager is maintained here. Please report FoxSu-specific issues to [this repository](https://github.com/Cmry-mlc/FoxSu/issues) rather than upstream.

## Credits

FoxSu did not grow from nothing. The tables below group every upstream project by **what its code actually does here** — source we adapted directly, binaries shipped inside the APK, and the parts running on the kernel side. Licences are listed per row, nothing folded away.

### Root and kernel

| Project | Where it sits in FoxSu | Licence |
|---|---|---|
| [KernelSU](https://github.com/tiann/KernelSU) | The origin of the whole project. Kernel module, `ksud`, module system, su and App Profile all come from here; FoxSu is a fork | GPL-3.0 |
| [KernelPatch](https://github.com/bmax121/KernelPatch) | `libkpatch.so` ships in the APK; KPM load, unload and `ctl0` calls go through its supercall | GPL-2.0 |
| [susfs4ksu](https://github.com/ShirkNeko/susfs4ksu) | The kernel-side contract for path hiding and mount spoofing; the SusFs screen issues config against its interface. Upstream `simonpunk` repo is gone, link points at a community mirror | GPL-3.0 |
| [Magic Mount-rs](https://github.com/Tools-cx-app/meta-magic_mount-rs) | `builtin/magic_mount_rs-arm64.zip` ships in the APK and takes over system partition mounting as a meta-module | GPL-3.0 |
| [Zygisk Next](https://github.com/LSPosed/ZygiskNext) | `builtin/zygisk_next.zip` ships in the APK: a standalone Zygisk implementation that lets Zygisk modules run | GPL-3.0 |
| [Oh My Keymint](https://github.com/qwq233/OhMyKeymint) | `builtin/oh_my_keymint-arm64.zip` ships in the APK: replaces the system KeyMint with its own daemon | Apache-2.0 |

### Interface and visuals

| Project | Where it sits in FoxSu | Licence |
|---|---|---|
| [Miuix](https://github.com/compose-miuix-ui/miuix) | Component foundation for several skins (`miuix-ui` / `miuix-nav` / `miuix-preference` / `miuix-blur`) | Apache-2.0 |
| [Jetpack Compose](https://github.com/androidx/androidx) | Declarative UI, animation and gesture system | Apache-2.0 |
| [MaterialKolor](https://github.com/jordond/materialkolor) | Wallpaper colour extraction for the dynamic theme | MIT |
| [AppIconLoader](https://github.com/zhanghai/AppIconLoader) | Loads installed app icons at the right size | Apache-2.0 |
| [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) | The liquid glass drawing under `ui/component/liquid/` (`Lens`, `Vibrancy`, `InnerShadow`, `CombinedBackdrop`) is adapted from this project | Apache-2.0 |

### Capability components

| Project | Where it sits in FoxSu | Licence |
|---|---|---|
| [libsu](https://github.com/topjohnwu/libsu) | Root shell and daemon — every command goes through it | Apache-2.0 |
| [OkHttp](https://github.com/lysine-dev/okhttp) | Downloading ad-block rules, update checks | Apache-2.0 |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Coroutine scheduling; the timeline behind all async work and animation | Apache-2.0 |
| [Protocol Buffers](https://github.com/protocolbuffers/protobuf) | Binary serialisation for module info and app lists | BSD-3-Clause |
| [RikkaX](https://github.com/RikkaApps/RikkaX) (ParcelableList module) | Passing installed app lists across processes | MIT |
| [commonmark-java](https://github.com/commonmark/commonmark-java) | Markdown rendering on module description pages | BSD-2-Clause |
| [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) | Reading internal system state past the hidden-API block | Apache-2.0 |
| [Commons Compress](https://github.com/apache/commons-compress) · [XZ for Java](https://github.com/tukaani-project/xz-java) | Unpacking module archives and images | Apache-2.0 / 0BSD |

### Data and rules

| Project | Where it sits in FoxSu | Licence |
|---|---|---|
| [anti-AD](https://github.com/privacy-protection-tools/anti-AD) | Ad-domain rules for Chinese apps and carriers, updated daily | MIT |
| [AdGuard Chinese filters](https://github.com/AdguardTeam/AdguardFilters) | Extra ad domains for Chinese sites and apps | GPL-3.0 |

### Build, tooling and docs

| Project | Where it sits in FoxSu | Licence |
|---|---|---|
| [android_bootimg](https://github.com/5ec1cff/android_bootimg) | `ksud` parsing and repacking boot images | GPL-3.0 |
| [ksu_props](https://github.com/KernelSU2/ksu_props) · [java-properties](https://github.com/KernelSU2/java-properties) | Reading and writing `module.prop` and system properties | no licence file / MIT |
| [kernlog.rs](https://github.com/kstep/kernlog.rs) | Kernel log output in `ksuinit` | MIT |
| [libc](https://github.com/rust-lang/libc) | Patched fork, pinned by commit in `Cargo.toml` | MIT / Apache-2.0 |
| [Magisk](https://github.com/topjohnwu/Magisk) | Reference for `su` daemon semantics (attributed in `userspace/ksud/src/su.rs`) | GPL-3.0 |
| [VitePress](https://github.com/vuejs/vitepress) | Build and theme for the [website](https://cmry-mlc.github.io/FoxSu/zh_CN/) | MIT |
| KernelSU community | Translations (zh-CN, zh-TW, en, ja, vi, id, ru, pt-BR) | — |

> Licensing: FoxSu is distributed under GPL-3.0. The parts coming from KernelSU, susfs4ksu, Magic Mount-rs, Zygisk Next, KernelPatch and android_bootimg are GPL too, and that copyleft applies to this project. Everything else keeps its own licence, unmodified.

Something missing? [Open an issue](https://github.com/Cmry-mlc/FoxSu/issues). The manager carries the same list under **Settings → About → FoxSu Manager → Open-source credits**.

## License

[GPL-3.0](LICENSE)

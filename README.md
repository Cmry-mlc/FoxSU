# KuaiMaoSU

> 一个基于 [BakaSU](https://github.com/Baka-SU/BakaSU) 的 Android 内核级 root 管理器：内核态 `su`、模块系统、SUSFS，加一套重新设计的管理器界面。

[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](./LICENSE)
[![Release](https://img.shields.io/github/v/release/Cmry-mlc/KuaiMaoSU?label=release)](https://github.com/Cmry-mlc/KuaiMaoSU/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3ddc84?logo=android&logoColor=white)](#兼容性)
[![Build Manager](https://github.com/Cmry-mlc/KuaiMaoSU/actions/workflows/build-manager.yml/badge.svg)](https://github.com/Cmry-mlc/KuaiMaoSU/actions/workflows/build-manager.yml)

---

## 这是什么

KuaiMaoSU 是在 **BakaSU（原 ReSukiSU）** 的基础上继续开发的 KernelSU 分支，包含内核模块、`ksud` 用户态守护进程和 Android 管理器三部分。

上游提供的是 root 能力本身；本项目在保留全部内核功能的前提下，把管理器从头重做了一遍，并补上了几个上游没有的开关（内置挂载控制器、SUSFS LKM 管理、重启面板、界面个性化等）。相对上游的完整改动见应用内 **关于 → 修改清单**，或 [`CHANGELOG.md`](./CHANGELOG.md)。

## 特性

**内核 / root**

- 基于内核的 `su` 与授权管理，App Profile 把 root 权限关进笼子
- 基于 metamodules 的模块系统；支持非 GKI 与 GKI 1.0 内核
- SUSFS 集成：路径屏蔽、内核态隐藏，附带守卫 LKM 的加载 / 卸载 / 开机自启管理
- **内置挂载控制器**：OverlayFS 与 Magic Mount 在设置里直接切换生效，不需要额外安装挂载模块
- 沿用上游的三种 Hook 模式：Tracepoint Syscall Redirect / Manual Hook / SUSFS Inline Hook

**管理器**

- 全新的视觉与交互（Loom 设计系统）：统一的配色、圆角、字重与动效
- 主页大卡片跟随触摸做 3D 透视倾斜、径向流光与漂浮尘埃
- 首屏信息流骨架屏，与真实内容同形，不闪白、不跳动
- 底栏三形态（传统 / 胶囊 / 侧边栏）、三档页面转场、全局横滑换页
- 个性化：在线字体、背景特效、DPI、顶栏诗词、**流光轮廓**（ColorOS 17 风格边缘凝光）
- 工具页：重启面板（重启 / 软重启 / Recovery / Bootloader / Download / EDL）
- 合规页：开源致谢（作者、仓库、协议）、修改清单、完整依赖协议清单

## 兼容性

| 项目 | 说明 |
| --- | --- |
| Android | 8.0 及以上（`minSdk 26`，`targetSdk 37`） |
| 内核 | 官方支持 GKI 2.0（5.10+）；3.4+ 需自行编译 |
| ABI | `arm64-v8a`、`armeabi-v7a`、`x86_64`、`riscv64` |
| SUSFS | 本项目仅支持 backport 到 4.3+ 的内核 |

## 安装

1. 从 [Releases](https://github.com/Cmry-mlc/KuaiMaoSU/releases/latest) 下载 `KuaiMaoSU-*.apk` 安装；
2. 内核侧需先刷入带 KuaiMaoSU / BakaSU / KernelSU 支持的 boot 镜像（LKM 或 GKI 方式），否则管理器只会显示「未激活」；
3. 刷机前务必备份 `boot` 分区，并确认机型与内核版本匹配。

> 管理器支持与上游内核混用：官方 KernelSU / KOWSU 的内核也可以作为后端。

## 从源码构建

环境：JDK 21、Android SDK（`compileSdk 37`）、NDK（版本见 `manager/gradle/libs.versions.toml`）。

```bash
cd manager

# 方式一：命令行传签名参数
./gradlew :app:assembleRelease \
  -PKEYSTORE_FILE=/path/to/release.jks \
  -PKEYSTORE_PASSWORD=****** \
  -PKEY_ALIAS=your_alias \
  -PKEY_PASSWORD=******

# 方式二：在 manager/gradle.properties 里写同名属性（CI 就是这样做的）
# KEYSTORE_FILE=key.jks
# KEYSTORE_PASSWORD=******
# KEY_ALIAS=your_alias
# KEY_PASSWORD=******
```

产物：`manager/app/build/outputs/apk/release/*.apk`。

- 不传签名参数时构建出的 release 包不可用于分发（也无法覆盖安装已有版本）；
- 内核模块（LKM）与 `ksud`、`ksuinit` 由 `.github/workflows/` 下的流水线分别构建，再由 `repack_apk*.py` 按 ABI 合并进 APK；
- 本地只编管理器时，需要 `--offline` 或已缓存依赖。

## 目录结构

```
kernel/      内核侧代码（GPL-2.0-only）
manager/     Android 管理器（Kotlin + Jetpack Compose）
userspace/   ksud 与 ksuinit（Rust / C）
uapi/        内核与用户态共享头文件
scripts/     构建与发布脚本
docs/        文档
js/          WebUI 相关资源
```

## CI

- `build-manager.yml`：编译 LKM、ksud（四个 ABI）、ksuinit 与管理器，并合并出可分发的 APK
- `release.yml`：推送 `v*` 标签时创建 GitHub Release
- 发布用的签名密钥通过仓库 Secrets 注入（`KEYSTORE`（base64）、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`），仓库内不含任何私钥

## 上游与致谢

本项目不是从零写起的，管理器、`ksud` 与内核方案都来自以下项目，完整清单见应用内 **关于 → 开源致谢**。

| 项目 | 作者 | 协议 | 作用 |
| --- | --- | --- | --- |
| [BakaSU](https://github.com/Baka-SU/BakaSU) | BakaSU Team（原 ReSukiSU） | GPL-3.0 | 本项目的直接上游 |
| [KernelSU](https://github.com/tiann/KernelSU) | tiann | GPL-3.0 | 内核级 root 方案的起点 |
| [SUSFS](https://github.com/simonpunk/susfs4ksu) | simonpunk | GPL-2.0 | 内核态隐藏与路径屏蔽 |
| [libsu](https://github.com/topjohnwu/libsu) | topjohnwu | Apache-2.0 | root shell 与守护进程通信 |
| [miuix-kmp](https://github.com/YuKongA/miuix-kmp) | YuKongA | Apache-2.0 | 实时模糊与转场动画 |
| [AboutLibraries](https://github.com/mikepenz/AboutLibraries) | mikepenz | Apache-2.0 | 依赖与协议清单 |

界面图标由 **尘梦如烟** 绘制。

## 许可证

- `kernel/` 目录下的文件为 **GPL-2.0-only**；
- 其余部分为 **GPL-3.0-or-later**，全文见 [LICENSE](./LICENSE)。

按 GPL-3.0 的要求：你可以自由使用、修改与再分发本项目，但**分发修改版时必须同样以 GPL-3.0 开源、附上完整源码，并保留原作者版权声明**。本项目按「现状」提供，不附带任何担保。

## 免责声明

刷写内核、修改系统分区、获取 root 均属高风险操作，可能导致设备变砖、数据丢失或保修失效。请自行评估风险并提前备份；因使用本项目造成的一切后果由使用者自行承担。

## 链接

- 源码：<https://github.com/Cmry-mlc/KuaiMaoSU>
- 问题反馈：<https://github.com/Cmry-mlc/KuaiMaoSU/issues>
- Telegram：<https://t.me/KuaiMaoSU>

---

<details>
<summary>English</summary>

KuaiMaoSU is a KernelSU-based root solution for Android, forked from [BakaSU](https://github.com/Baka-SU/BakaSU) (formerly ReSukiSU). It keeps the kernel-side `su`, metamodule system and SUSFS integration, and ships a completely redesigned manager (Compose) with a built-in mount controller (OverlayFS / Magic Mount), SUSFS LKM management, a reboot panel, and extensive theming.

Licensed under GPL-3.0-or-later (kernel directory: GPL-2.0-only). Redistributing a modified version requires publishing the source under the same license and keeping the original copyright notices.

</details>

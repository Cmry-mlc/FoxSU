简体中文 | [English](README_EN.md)

<p align="center">
  <img src="docs/assets/foxsu-logo.png" width="132" alt="FoxSu" />
</p>

<h1 align="center">FoxSu</h1>

<p align="center">
  适用于安卓设备的、基于内核的 root 解决方案<br />
  <sub>内核沿用 KernelSU，管理器整套重写</sub>
</p>

<p align="center">
  <a href="https://github.com/Cmry-mlc/FoxSu/actions/workflows/build-manager.yml"><img src="https://img.shields.io/github/actions/workflow/status/Cmry-mlc/FoxSu/build-manager.yml?branch=main&label=build&style=flat-square" alt="Build" /></a>
  <a href="https://github.com/Cmry-mlc/FoxSu/releases"><img src="https://img.shields.io/badge/releases-view%20all-blue?style=flat-square" alt="Releases" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-orange?style=flat-square" alt="License" /></a>
  <img src="https://img.shields.io/badge/Android-12%2B%20(API%2031)-3DDC84?style=flat-square" alt="Android" />
  <img src="https://img.shields.io/badge/Kernel-GKI%205.10%2B%20%7C%204.14%2B-1E88E5?style=flat-square" alt="Kernel" />
</p>

---

## 简介

FoxSu 是 [KernelSU](https://github.com/tiann/KernelSU) 的衍生分支。root 能力依旧留在内核里——`su`、白名单、App Profile、模块挂载、sucompat 全部由内核模块与 `ksud` 提供；改变的是管理器：包名 `com.foxsu.manager`，界面、交互、安装流程被整套重写，并在其上加了五套皮肤与一个可深度定制的底栏。

> Root 在内核，体验在管理器。

## 管理器特性

### 五套界面皮肤

同一份功能，五种排版语言，在「设置 → 界面风格」里切换，全部页面同步生效。

| 皮肤 | 风格 | 说明 |
| --- | --- | --- |
| **Redesign**（Miuix） | 玻璃卡片 + 胶囊底栏 | 默认皮肤，信息密度最高，主页为一张主卡 + 四张 KPI 卡 |
| **Editorial** | 杂志排版 | 纸面底色、墨线分隔、衬线标题，自带原生底栏（可选换成胶囊） |
| **Pages**（Fresh） | 大留白 | 大标题 + 分区块列表，适合单手浏览 |
| **Glass** | 液态玻璃 | 通透卡面、浅色高光，配壁纸观感最好 |
| **Nocturne** | 暗夜霓虹 | 深色优先，霓虹描边与发光强调色 |

### 底栏：13 种胶囊样式

底栏不再跟皮肤绑死，而是全局可选。侧边抽屉模式会自动为每个页面让出左侧空间，内容不会被胶囊压住。

`原有胶囊` · `展开药丸` · `侧边抽屉` · `流光光标` · `描边霓虹` · `方块卡片` · `渐变实底` · `顶指示条` · `浮起圆片` · `纸面墨线` · `白色药丸` · `液态药丸` · `暗夜霓虹`

另外可单独调节：圆角、不透明度、图标造型（圆角方 / 正圆 / 超椭圆 / 七彩渐变）、沉浸光感、液态玻璃、七彩图标、莫奈取色。

### 定制中心

所有外观项都接真实设置并即时生效，不需要重启：

- **壁纸**：选取、拖动裁剪、缩放、压暗，可关掉全局背景图
- **卡面配图**：主页大卡、四张 KPI 卡、每个模块卡都能单独配图并调透明度
- **卡面配色**：全局取色 + 单卡覆盖，透明度独立可调，另有一键七彩卡片
- **字体**：系统默认 / 衬线 / 等宽 / 手写圆体，也可导入自己的字体文件
- **标题**：主页顶部标题可自定义
- **启动器图标**：四套狐狸形象，随系统主题切换深浅

### Root 与权限

- **白名单授权**：只有被允许的应用能拿到 `su`，其余应用感知不到 root 存在
- **应用配置文件**：按 uid 分别设定 root 与非 root 行为——`su` 的 uid / gid / groups / capabilities、SELinux 上下文、是否卸载模块
- **配置模板**：把一套策略存为模板，批量套用到多个应用
- **默认卸载模块**：未单独配置的应用统一按全局默认值处理
- **Classic SU 命令**：让老应用通过 `/system/bin/su` 正常提权
- **Kernel umount**：在内核层卸载模块，用户态看不到挂载痕迹
- **隐藏 SELinux 修改**：阻止应用探测到 SELinux 改动
- **SU Log**：把授权、拒绝等 root 事件写入 sulog，可在管理器内直接翻阅

### 模块

- 本地 zip 安装，安装前展示模块信息与冲突提示
- **模块仓库**：内置在线索引，可浏览、搜索、查看详情并直接安装
- 元模块（metamodule）识别与高亮，卸载前给出警告
- 模块 action 脚本一键执行，也支持手动运行 shell 脚本
- 模块更新检查、启用 / 禁用，可按名称、星标、更新时间排序
- 卸载后可撤销，误删的模块能直接恢复；也能给模块创建桌面快捷方式

### 安装与刷写

- 单屏安装向导：当前内核与 KMI 信息、安装方式、分区选择、高级选项全部在一页内完成，没有二级页面
- 支持直接安装、使用本地 LKM 文件、交给其他管理器刷写
- 可选允许 shell、开启 ADB、刷写前强制备份
- 独立刷写页与脚本执行页，带实时输出

### 诊断与维护

- **崩溃日志**：未捕获异常写入 `files/crash/last-crash.txt`，同时镜像到外部目录，方便反馈问题
- **软重启**：用软重启代替整机重启，改完设置不必等开机动画
- 管理器与 LKM 的更新检查
- 临时 / 永久卸载 root、恢复原厂镜像

## 兼容状态

FoxSu 官方支持 **GKI 2.0** 设备（内核 5.10 及以上）。较旧的内核（4.14 及以上）同样兼容，但需要自行编译内核。

管理器要求 **Android 12（API 31）及以上**；编译目标 API 37，使用 JDK 21 与 NDK 构建。

## 安装

> 只安装 APK 不会获得 root。设备内核必须先包含 FoxSu / KernelSU 驱动。

1. 从 [Releases](https://github.com/Cmry-mlc/FoxSu/releases) 下载最新的管理器 APK 并安装；
2. 为设备准备带 FoxSu 驱动的内核：使用与机型 / KMI 匹配的 GKI 内核，或自行编译内核、构建 LKM；
3. 在管理器的「安装」页选择安装方式与分区，按提示刷写并重启；
4. 首次启动会进入欢迎页，逐页介绍授权、模块、配置文件与日志的用法。

内核侧通过包名与签名指纹识别管理器，因此第三方应用无法伪装成 FoxSu 管理器接管 root 授权。

## 从源码构建

管理器：

```bash
cd manager
./gradlew assembleRelease \
  -PKEYSTORE_FILE=/path/to/your.jks \
  -PKEY_ALIAS=your_alias \
  -PKEYSTORE_PASSWORD=****** \
  -PKEY_PASSWORD=******
```

版本号取自 git 提交数（`30000 + commit count`），版本名取自 `git describe`。
正式包（tag 触发）的密钥、Secrets 与发版步骤见 [RELEASE.md](RELEASE.md)。


内核与用户态：

- `kernel/`：内核模块，按设备内核树或 DDK 编译；改动包名 / 签名常量后必须重新编译内核或 LKM 才会生效
- `userspace/ksud/`：Rust 用户态守护进程，`cargo ndk -t arm64-v8a build`
- `.github/workflows/`：管理器、ksud、LKM 与整包发布流程

### 改包名 / 换签名

内核只按两个条件认管理器：`/data/app` 里的包名等于 `KSU_PACKAGE_NAME`，且该 APK 的 v2 签名证书长度与 SHA-256 等于 `KSU_EXPECTED_SIZE` / `KSU_EXPECTED_HASH`（见 `kernel/manager/throne_tracker.c`）。两处常量分布在 `kernel/Kbuild`、`manager/app/build.gradle.kts`、`userspace/ksud/build.rs`，只改一处、或者只重打 APK 不重编 LKM，表现都是**管理器显示"未安装"、请求 root 不弹窗、点了也授不了权**。

用脚本一次改齐，并从密钥库生成签名常量：

```bash
./scripts/manager-identity.sh set com.your.package          # 同步三处默认包名
KEYSTORE_PASSWORD=****** ./scripts/manager-identity.sh sign /path/to/your.jks your_alias
./scripts/manager-identity.sh show                          # 检查三处是否一致
./scripts/manager-identity.sh check app-release.apk         # 检查 APK 能否被内核认成管理器
```

改完按 `ksud` → 管理器 APK → `kernelsu.ko`/LKM 的顺序重编并重刷启动镜像，再覆盖安装管理器。旧包名留下的白名单条目按 UID 记录，不会影响新包名，但需要重新授权一次。

## 与 KernelSU 的关系

FoxSu 基于 [KernelSU](https://github.com/tiann/KernelSU)（GPL-3.0），内核与 `ksud` 侧的能力来自上游及其社区贡献，管理器为 FoxSu 自行维护。FoxSu 特有的问题请提到[本仓库的 Issues](https://github.com/Cmry-mlc/FoxSu/issues)，不要打扰上游。

## 致谢

- [KernelSU](https://github.com/tiann/KernelSU)——内核 root 方案、模块体系与多语言翻译
- 以及所有提交反馈、测试与建议的朋友

## 许可证

[GPL-3.0](LICENSE)

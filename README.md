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

FoxSu 是 [KernelSU](https://github.com/tiann/KernelSU) 的衍生分支。root 能力依旧留在内核里——`su`、白名单、App Profile、模块挂载、sucompat 全部由内核模块与 `ksud` 提供；改变的是管理器：包名 `com.foxsu.manager`，界面、交互、安装流程被整套重写，并在其上叠了五套皮肤、21 种胶囊样式与 21 套通栏结构、40 套底栏特效、20 套底栏图标与 21 套主页背景特效。

> Root 在内核，体验在管理器。

## 特性一览

| 层 | 内容 |
| --- | --- |
| 皮肤 | 5 套，全部页面同步生效 |
| 底栏 | 21 种胶囊样式 + 21 套通栏结构 · 10 种按钮造型，样式与皮肤解耦 |
| 底栏特效 | 栏内 20 套 + 外圈 20 套，两个作用域各自可选、可叠加 |
| 底栏图标 | 20 套造型，只换形状不换配色 |
| 背景特效 | 猫爪氛围 + 20 套整屏特效，21 选 1，独立总开关 |
| 定制 | 壁纸、卡面配图、卡面配色、字体、标题、启动器图标 |
| Root | 白名单、App Profile、模板、Classic SU、内核级卸载、SELinux 隐藏、SU 日志 |
| 模块 | 本地 zip、在线仓库、元模块识别、action 脚本、撤销卸载、桌面快捷方式 |
| 安装 | 单屏向导、直接刷入 / 非活动槽 / 本地镜像 / 在线下载、实时输出 |

## 管理器特性

### 五套界面皮肤

同一份功能，五种排版语言，在「设置 → 界面风格」里切换，全部页面同步生效。

| 皮肤 | 风格 | 说明 |
| --- | --- | --- |
| **Miuix** | 玻璃卡片 + 胶囊底栏 | 默认皮肤，信息密度最高。主页为一张状态主卡 + 统计 / 设备 / 安全三组卡 |
| **Editorial** | 杂志排版 | 纸面底色、墨线分隔、衬线标题，自带原生底栏（可选换成胶囊） |
| **Pages** | 大留白 | 大标题 + 分区块列表，适合单手浏览 |
| **Glass** | 液态玻璃 | 通透卡面、浅色高光，配壁纸观感最好 |
| **Nocturne** | 暗夜霓虹 | 深色优先，霓虹描边与发光强调色 |

### 底栏

底栏与皮肤解耦，样式全局可选——换皮肤不再改变操作习惯。切样式时尺寸恒定（项宽 78dp、项高 60dp），底栏不会忽长忽短。

**21 种胶囊样式**

`珊瑚气泡` · `胶片孔` · `霓虹管` · `玻璃叠层` · `单点聚焦` · `云朵贴纸` · `滑轨卡片` · `原有胶囊` · `展开药丸` · `侧边抽屉` · `流光光标` · `描边霓虹` · `方块卡片` · `渐变实底` · `顶指示条` · `浮起圆片` · `中央头像` · `纸面墨线` · `白色药丸` · `液态药丸` · `暗夜霓虹`

前 7 套是另一套版式：48dp 方形热区、6dp 内衬，部分变体中央留一枚狐狸头像作为捐赠入口。`侧边抽屉` 可左右停靠，页面内容自动让位。

**传统底栏 21 套结构**

打开「传统底栏」开关后，切换的是另一条轴：不是胶囊的造型，而是整条贴底栏的结构语言——齿边、星形、竖槽、棱柱、光环、切角。

`经典` · `锯齿顶边` · `不对称圆角` · `四角星绽放` · `温度计竖槽` · `旋转棱柱` · `呼吸光环` · `分体胶囊` · `舱门开启` · `序号角标` · `折纸页角` · `蜂窝六边` · `弧形塌陷` · `三角齿` · `滑轨信号灯` · `堆叠卡` · `侧锚引线` · `穿孔带` · `镜像折叠` · `浮雕凸面` · `折叠屏中缝`

全部取全局强调色与它的渐变，换强调色、换皮肤时整条栏跟着走，不存在写死的第二种配色。

**10 种按钮造型**

`胶囊` · `正圆` · `超椭圆` · `描边框` · `双层` · `切角` · `立体键` · `浮片` · `玻璃` · `光晕`

选中与未选中是同一造型的两种状态，切换时整排一起换，不会新旧混搭。

**40 套点击特效，分两个作用域**

栏内与边框是两条独立的选择器，可以只开一个，也可以叠着用——例如「描边呼吸 + 图标弹跳」。

- **栏内 20 套**（作用在图标与选中胶囊上）：滑动果冻胶囊、弹跳落地、果冻抖动、旋转入场、心跳双脉冲、波浪起伏、光泽扫过、扩散光环、气泡上浮、星屑迸发、胶囊膨胀、左右摇摆、立体翻转、渐变流转、文字弹射、磁吸拉伸、圆点滑轨、水波涟漪、弹性超调、迷你烟花
- **外圈 20 套**（作用在整条底栏的边框上）：呼吸光晕、跑马光点、边框流光、彩虹描边、虚线旋转、流动光晕、点击扩散波纹、选中闪框、描边呼吸、电流描边、跟随光环、旋转扫光、外圈冲击波、选中光斑、选中环绕、光点掠过、弹性外框、投影抬升、边缘火星、声波外扩

外圈分两类：常驻类持续循环，交互类只在切页或按下时播一次。栏内只有「渐变流转」常驻，其余都是一次性时间线，播完即停，不会持续占用帧。

**20 套底栏图标**

`建筑剖面` · `折纸` · `地图俯视` · `像素 8-bit` · `便签贴纸` · `分子结构` · `仪表刻度` · `圆环叠加` · `断层切片` · `童画涂鸦` · `街区网格` · `等高线` · `声波` · `积木拼块` · `印章` · `公文夹` · `线框解构` · `折扇` · `涟漪` · `星轨`

只替换形状，着色仍跟随主题强调色与选中状态，所以换图标不会打乱配色。

**底栏行为**

- **水滴流动指示器**：选中色块像水滴流到新 Tab，带拉长、压扁与回弹；静止时挂起，不空转
- **页面轻推动效**：切页时内容轻微跟随，与水滴开关互不影响
- **自动隐藏**：无操作 N 秒后胶囊沉底，点一下屏幕滑回
- **外观**：圆角、不透明度、沉浸光感、液态玻璃、七彩图标、莫奈取色

### 氛围与动效

- **背景特效 21 套**：`猫爪氛围` + 20 套整屏特效，互斥单选，一个总开关控制生效；画在内容之上但不接管指针事件，不挡点击
- **20 套清单**：呼吸光斑 · 漂浮微粒 · 极光流带 · 波纹涟漪 · 柔和网格 · 斜向流光 · 大气雾层 · 光点连线 · 液态渐变 · 雨滴下落 · 星芒微闪 · 光晕环旋 · 色彩漂移 · 同心声波 · 飘落花瓣 · 斜纹缓扫 · 视差光斑 · 上下呼吸 · 粒子涡旋 · 晨昏暖光
- **猫爪氛围**：猫爪、爱心、四角星从屏幕底部缓缓升起，主页、捐赠页与定制页头卡三处同时生效，在选择器里排第 1 项
- **动效强度**：`关闭` / `克制` / `全开` 三档，统一控制卡片粒子边、3D 触摸倾斜与底栏点击涟漪
- **页面切换手势**：`默认` / `跨轴拦截` / `iOS 风格` / `关闭` 四档，选「关闭」后底栏横滑一并停用
- 另可单独开关预测性返回手势与边缘横滑返回

### 定制中心

所有外观项都接真实设置并即时生效，不需要重启：

- **壁纸**：选取、拖动裁剪、缩放、压暗，可关掉全局背景图
- **卡面配图**：主页大卡、统计卡、每个模块卡都能单独配图并调透明度
- **卡面配色**：全局取色 + 单卡覆盖，透明度独立可调，另有一键七彩卡片
- **字体**：系统默认 / 思源宋体 / JetBrains Mono 等宽 / 霞鹜文楷，也可导入自己的字体文件
- **标题**：主页顶部标题文字与颜色可自定义
- **启动器图标**：5 套狐狸形象 + KernelSU 原版图标，随系统主题切换深浅
- 以上任意项都能一键恢复默认

### Root 与权限

- **白名单授权**：只有被允许的应用能拿到 `su`，其余应用感知不到 root 存在
- **应用配置文件**：按 uid 分别设定 root 与非 root 行为——`su` 的 uid / gid / groups / capabilities、SELinux 上下文、是否卸载模块
- **配置模板**：把一套策略存为模板，批量套用到多个应用
- **默认卸载模块**：未单独配置的应用统一按全局默认值处理
- **Classic SU 命令**：让老应用通过 `/system/bin/su` 正常提权
- **Kernel umount**：在内核层卸载模块，用户态看不到挂载痕迹
- **隐藏 SELinux 修改**：阻止应用探测到 SELinux 改动
- **SU Log**：把授权、拒绝等 root 事件写入 sulog，可在管理器内直接翻阅
- **Shell 收紧**：可限定 Shell 仅由 adbd 或 KernelSU root 进程启动时才能提权

### 模块

- 本地 zip 安装，安装前展示模块信息与冲突提示
- **模块仓库**：内置在线索引，可浏览、搜索、查看详情并直接安装，下载走大小与 SHA-256 校验
- **内置模块**：Magic Mount-rs、Zygisk Next、Oh My Keymint 三颗安装包随 APK 打包，不用去 GitHub 找 release；详情页可安装、更新、启停、卸载，自家带 webroot 的模块还能直接打开它的原始界面
- 元模块（metamodule）识别与高亮，卸载前给出警告
- 模块 action 脚本一键执行，也支持手动运行 shell 脚本
- 模块更新检查、启用 / 禁用，可按名称、星标、更新时间排序
- 卸载后可撤销，误删的模块能直接恢复；也能给模块创建桌面快捷方式
- 模块卡按当前主题配色渲染：启用态一条主题渐变竖条 + 首字徽章，停用态整卡降级，状态一眼可辨
- 描述超过设定行数自动折叠，点卡片展开全文

### 安装与刷写

- 单屏安装向导：当前内核与 KMI 信息、安装方式、分区选择、高级选项全部在一页内完成，没有二级页面
- 四种安装方式：直接刷入当前槽、安装到未使用的槽位（OTA 后）、选择本地镜像修补、下载镜像并修补
- 可选允许 shell、开启 ADB、刷写前强制备份；越狱模式下会自动禁用直接刷写入口
- 独立刷写页与脚本执行页，带实时输出

### 诊断与维护

- **崩溃日志**：未捕获异常写入 `files/crash/last-crash.txt`，同时镜像到外部目录，方便反馈问题
- **软重启**：用软重启代替整机重启，改完设置不必等开机动画
- 管理器与 LKM 的更新检查，自定义 LKM 会单独标注
- 临时 / 永久卸载 root、恢复原厂镜像

## 兼容状态

FoxSu 官方支持 **GKI 2.0** 设备（内核 5.10 及以上）。较旧的内核（4.14 及以上）同样兼容，但需要自行编译内核。

管理器要求 **Android 12（API 31）及以上**；编译目标 API 37，使用 JDK 21 与 NDK 构建。

## 安装

> 只安装 APK 不会获得 root。设备内核必须先包含 FoxSu / KernelSU 驱动。

1. 从 [Releases](https://github.com/Cmry-mlc/FoxSu/releases) 下载最新的管理器 APK 并安装；
2. 为设备准备带 FoxSu 驱动的内核：使用与机型 / KMI 匹配的 GKI 内核，或自行编译内核、构建 LKM；
3. 在管理器的「安装」页选择安装方式与分区，按提示刷写并重启；
4. 首次启动会进入 5 页欢迎页，逐页介绍授权、模块、配置文件与日志的用法。

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

版本号来自仓库根的 `VERSION` 文件（形如 `3.0.53`）：`versionCode = major*10000 + minor*100 + patch`，`versionName` 直接取该字符串。管理器、`ksud`、内核模块读的是同一个文件，因此三者的版本号永远一致——这样才能正确判断「LKM 是否需要更新」，也不会再出现本地包与 CI 包互相装不上的情况。

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

FoxSu 不是从零长出来的。下面按「这些代码在项目里承担什么」分组，列出全部上游项目——包括直接改编进来的源码、打包进 APK 的二进制，以及跑在设备内核侧的那部分。许可证逐行写明，不做折叠。

### 根与内核

| 项目 | 在 FoxSu 里的位置 | 许可 |
|---|---|---|
| [KernelSU](https://github.com/tiann/KernelSU) | 整个项目的源头。内核模块、`ksud`、模块体系、su 与 App Profile 都来自这里，FoxSu 是它的分支 | GPL-3.0 |
| [KernelPatch](https://github.com/bmax121/KernelPatch) | APK 内置 `libkpatch.so`，KPM 模块的加载、卸载与 `ctl0` 调用走它的 supercall | GPL-2.0 |
| [susfs4ksu](https://github.com/ShirkNeko/susfs4ksu) | 路径隐藏与挂载伪装的内核补丁约定，管理器的 SusFs 页面按它的接口下发配置。上游 `simonpunk` 仓库已下线，链接指向社区镜像 | GPL-3.0 |
| [Magic Mount-rs](https://github.com/Tools-cx-app/meta-magic_mount-rs) | APK 内置 `builtin/magic_mount_rs-arm64.zip`，作为元模块接管系统分区挂载 | GPL-3.0 |
| [Zygisk Next](https://github.com/LSPosed/ZygiskNext) | APK 内置 `builtin/zygisk_next.zip`，不依赖 Magisk 的 Zygisk 实现，为 Zygisk 模块提供注入 | GPL-3.0 |
| [Oh My Keymint](https://github.com/qwq233/OhMyKeymint) | APK 内置 `builtin/oh_my_keymint-arm64.zip`，用自带 keymint 守护进程替换系统 KeyMint | Apache-2.0 |

### 界面与视觉

| 项目 | 在 FoxSu 里的位置 | 许可 |
|---|---|---|
| [Miuix](https://github.com/compose-miuix-ui/miuix) | 多套皮肤的组件底座（`miuix-ui` / `miuix-nav` / `miuix-preference` / `miuix-blur`） | Apache-2.0 |
| [Jetpack Compose](https://github.com/androidx/androidx) | 声明式 UI、动画与手势系统 | Apache-2.0 |
| [MaterialKolor](https://github.com/jordond/materialkolor) | 从壁纸取色，生成整套动态主题 | MIT |
| [AppIconLoader](https://github.com/zhanghai/AppIconLoader) | 以正确尺寸加载已安装应用的图标 | Apache-2.0 |
| [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) | `ui/component/liquid/` 下的液体玻璃绘制（`Lens`、`Vibrancy`、`InnerShadow`、`CombinedBackdrop`）改编自该项目 | Apache-2.0 |

### 能力组件

| 项目 | 在 FoxSu 里的位置 | 许可 |
|---|---|---|
| [libsu](https://github.com/topjohnwu/libsu) | root shell 与守护进程，所有命令都从这里走 | Apache-2.0 |
| [OkHttp](https://github.com/lysine-dev/okhttp) | 下载去广告规则、检查更新 | Apache-2.0 |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | 协程调度，整套异步与动效的时间线 | Apache-2.0 |
| [Protocol Buffers](https://github.com/protocolbuffers/protobuf) | 模块信息与应用列表的二进制序列化 | BSD-3-Clause |
| [RikkaX](https://github.com/RikkaApps/RikkaX)（ParcelableList 模块） | 跨进程传递已安装应用列表 | MIT |
| [commonmark-java](https://github.com/commonmark/commonmark-java) | 模块说明页的 Markdown 渲染 | BSD-2-Clause |
| [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) | 绕过隐藏 API 限制，读取系统内部状态 | Apache-2.0 |
| [Commons Compress](https://github.com/apache/commons-compress) · [XZ for Java](https://github.com/tukaani-project/xz-java) | 模块包与镜像的压缩格式解包 | Apache-2.0 / 0BSD |

### 数据与规则

| 项目 | 在 FoxSu 里的位置 | 许可 |
|---|---|---|
| [anti-AD](https://github.com/privacy-protection-tools/anti-AD) | 国内 App 与运营商广告域名规则库，每日更新 | MIT |
| [AdGuard 中文过滤](https://github.com/AdguardTeam/AdguardFilters) | 补充中文站点与 App 的广告域名 | GPL-3.0 |

### 构建、工具与文档

| 项目 | 在 FoxSu 里的位置 | 许可 |
|---|---|---|
| [android_bootimg](https://github.com/5ec1cff/android_bootimg) | `ksud` 解析与重打包 boot 镜像 | GPL-3.0 |
| [ksu_props](https://github.com/KernelSU2/ksu_props) · [java-properties](https://github.com/KernelSU2/java-properties) | 读写模块 `module.prop` 与系统属性 | 仓库未附许可证 / MIT |
| [kernlog.rs](https://github.com/kstep/kernlog.rs) | `ksuinit` 的内核日志输出 | MIT |
| [libc](https://github.com/rust-lang/libc) | 打了补丁的分支，`Cargo.toml` 里按 commit 锁定 | MIT / Apache-2.0 |
| [Magisk](https://github.com/topjohnwu/Magisk) | `su` 守护进程的语义参考（`userspace/ksud/src/su.rs` 有出处标注） | GPL-3.0 |
| [VitePress](https://github.com/vuejs/vitepress) | [官网](https://cmry-mlc.github.io/FoxSu/zh_CN/)的构建与主题 | MIT |
| KernelSU 社区 | 多语言翻译（简繁中、英、日、越、印尼、俄、葡） | — |

> 许可证：FoxSu 整体以 GPL-3.0 分发。上表中来自 KernelSU、susfs4ksu、Magic Mount-rs、Zygisk Next、KernelPatch、android_bootimg 的部分同为 GPL 授权，其传染性适用于本项目；其余组件按各自许可证保留原条款，未做任何删改。

如果你发现某个项目漏了，欢迎[提 Issue](https://github.com/Cmry-mlc/FoxSu/issues)补上。管理器里也有同一份清单：**设置 → 关于 → FoxSu Manager → 开源致谢**。

## 许可证

[GPL-3.0](LICENSE)

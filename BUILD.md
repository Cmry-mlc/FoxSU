# KuaiMaoSU 打包配置

本文档记录 manager（APK）的打包配置，便于换机或其它环境复现构建。

> 签名密钥库文件（`*.jks`）与真实口令**不入库**，见 `.gitignore`。口令请从私有渠道获取。

## 一、基本信息

| 项目 | 值 |
|---|---|
| 包名 (applicationId) | `com.kuaimaosu.manager` |
| 应用名 | KuaiMaoSU |
| 版本名 / 版本号 | 见 `manager/build.gradle.kts` |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 |
| Build Tools | 36.1.0 |
| NDK | 30.0.16248370 |
| Gradle / AGP / Kotlin | 9.8.0 / 9.4.1 / 2.4.20 |
| JDK | 21 |
| Compose BOM | 2026.09.00 |

## 二、签名

通过 `apksign` 插件，从 Gradle 属性读取：

| 属性 | 说明 |
|---|---|
| `KEYSTORE_FILE` | 密钥库路径 |
| `KEYSTORE_PASSWORD` | 密钥库口令 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥口令 |

> **签名方案必须为纯 v2（无 v1 / v3）。** 启用 v3 会导致动态管理器功能失效。

## 三、构建类型

| 配置 | debug | release |
|---|---|---|
| 代码混淆 (R8) | 关 | 开 |
| 资源裁剪 | 关 | 开 |
| 优化档位 | `-Og` | `-Oz` + LTO |
| proguard | — | `app/proguard-rules.pro` |

release 的 native 编译：LTO + `--gc-sections` + `--icf=all` + strip，C 标准 C2x、C++ C++2b，`ANDROID_STL=none`。

## 四、ABI 与产物

- debug：仅 `arm64-v8a`
- release：splits 生成 `arm64-v8a` / `armeabi-v7a` / `x86_64` / `riscv64` + `universal`
- 产物命名：`KuaiMaoSU_{版本名}_{版本号}-{ABI}-release.apk`
- 产物目录：`manager/app/build/outputs/apk/release/`

## 五、资源与打包

- 语言白名单：`en`、`zh-rCN`、`zh-rHK`、`zh-rTW`
- `useLegacyPackaging = true`（so 不压缩）
- 启用 Compose / AIDL / BuildConfig / Prefab

## 六、构建命令

```bash
cd <repo>
export JAVA_HOME=/path/to/jdk21
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"

./manager/gradlew -p manager :app:assembleRelease \
  -PKEYSTORE_FILE=/path/to/release.jks \
  -PKEYSTORE_PASSWORD=****** \
  -PKEY_ALIAS=****** \
  -PKEY_PASSWORD=****** \
  --offline
```

## 七、改版本号

`manager/build.gradle.kts` 中的：

```kotlin
extra["managerVersionCode"] = 40003
extra["managerVersionName"] = "4.0.3"
```

改完此处，APK 文件名、应用内版本号、一键越狱页副标题显示的版本号会全部自动同步。

## 八、Gradle 全局参数

`manager/gradle.properties`：

```properties
android.experimental.enableNewResourceShrinker.preciseShrinking=true
android.enableAppCompileTimeRClass=true
android.useAndroidX=true
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
org.gradle.parallel=true
org.gradle.vfs.watch=true
android.r8.maxWorkers=1
org.gradle.configuration-cache=true
```

## 九、内置提权引擎组件

| 文件 | 作用 |
|---|---|
| `jniLibs/arm64-v8a/libkmengine.so` | Rust runner，编排放置与执行 |
| `jniLibs/arm64-v8a/libkmplant.so` | 静态链接的 plant 可执行（写原语核心） |
| `jniLibs/arm64-v8a/libksud.so` | late-load 用的 ksud |
| `assets/KuaiMaoroot/kernel_profiles.txt` | 内核偏移表，按 `uname -r` 精确匹配 |

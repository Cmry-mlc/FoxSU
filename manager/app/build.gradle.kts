@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.aboutLibraries)
    id("kotlin-parcelize")
}

val androidCompileSdkVersion = rootProject.extra["androidCompileSdkVersion"] as Int
val androidCompileNdkVersion = rootProject.extra["androidCompileNdkVersion"] as String
val androidBuildToolsVersion = rootProject.extra["androidBuildToolsVersion"] as String
val androidMinSdkVersion = rootProject.extra["androidMinSdkVersion"] as Int
val androidTargetSdkVersion = rootProject.extra["androidTargetSdkVersion"] as Int
val androidSourceCompatibility = rootProject.extra["androidSourceCompatibility"] as JavaVersion
val androidTargetCompatibility = rootProject.extra["androidTargetCompatibility"] as JavaVersion
val managerVersionCode = rootProject.extra["managerVersionCode"] as Int
val managerVersionName = rootProject.extra["managerVersionName"] as String
val managerPackageName = rootProject.extra["managerPackageName"] as String
val managerName = rootProject.extra["managerName"] as String

apksign {
    storeFileProperty = "KEYSTORE_FILE"
    storePasswordProperty = "KEYSTORE_PASSWORD"
    keyAliasProperty = "KEY_ALIAS"
    keyPasswordProperty = "KEY_PASSWORD"
}

val baseCFlags = listOf(
    "-Wall", "-Qunused-arguments", "-fvisibility=hidden", "-fvisibility-inlines-hidden",
    "-fno-exceptions", "-fno-stack-protector", "-fomit-frame-pointer",
    "-Wno-builtin-macro-redefined", "-Wno-unused-value", "-D__FILE__=__FILE_NAME__",
)
val baseCppFlags = baseCFlags + "-fno-rtti"

val isReleaseTask =
    project.gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

android {
    namespace = "com.loomsu.manager"

    buildTypes {
        debug {
            // 测试包关闭 R8 与资源裁剪：开启后每次构建要跑全量混淆，
            // 单次 5~10 分钟且容易因内存不足卡死。体积靠 arm64 / 语言白名单控制即可。
            isMinifyEnabled = false
            isShrinkResources = false
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DCMAKE_CXX_FLAGS_DEBUG=-Og", "-DCMAKE_C_FLAGS_DEBUG=-Og")
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            externalNativeBuild {
                cmake {
                    arguments += "-DDEBUG_SYMBOLS_PATH=${layout.buildDirectory.get().asFile.absolutePath}/symbols"
                    arguments += "-DCMAKE_BUILD_TYPE=Release"

                    val releaseFlags = listOf(
                        "-flto",
                        "-ffunction-sections",
                        "-fdata-sections",
                        "-Wl,--gc-sections",
                        "-fno-unwind-tables",
                        "-fno-asynchronous-unwind-tables",
                        "-Wl,--exclude-libs,ALL",
                    )
                    val configFlags = listOf("-Oz", "-DNDEBUG").joinToString(" ")

                    cppFlags += releaseFlags
                    cFlags += releaseFlags

                    arguments += listOf(
                        "-DCMAKE_CXX_FLAGS_RELEASE=$configFlags",
                        "-DCMAKE_C_FLAGS_RELEASE=$configFlags",
                        "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,--gc-sections -Wl,--exclude-libs,ALL -Wl,--icf=all -s -Wl,--hash-style=sysv -Wl,-z,norelro",
                    )
                }
            }
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        resValues = true
        compose = true
        prefab = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            if (isReleaseTask) {
                // https://stackoverflow.com/a/58956288
                // It will break Layout Inspector, but it's unused for release build.
                excludes += "META-INF/*.version"
            }
            // https://github.com/Kotlin/kotlinx.coroutines?tab=readme-ov-file#avoiding-including-the-debug-infrastructure-in-the-resulting-apk
            excludes += "DebugProbesKt.bin"
            // https://issueantenna.com/repo/kotlin/kotlinx.coroutines/issues/3158
            excludes += "kotlin-tooling-metadata.json"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        generateLocaleConfig = true
        // 测试包只保留中文（简/繁）与英文，去掉其余数十种翻译。
        // 注意：这里必须写完整的 locale 目录名（zh-rCN 等），只写 "zh" 不会匹配 values-zh-rCN，
        // 会把中文资源整体裁掉，导致界面全部回落到英文。
        localeFilters += listOf("en", "zh-rCN", "zh-rHK", "zh-rTW")
    }

    compileSdk = androidCompileSdkVersion
    ndkVersion = androidCompileNdkVersion
    buildToolsVersion = androidBuildToolsVersion

    defaultConfig {
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = managerVersionCode
        versionName = managerVersionName
        applicationId = managerPackageName

        val isPrBuild = rootProject.extra["isPrBuild"] as Boolean
        buildConfigField("boolean", "IS_PR_BUILD", isPrBuild.toString())
        resValue("string", "app_name", managerName)

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=none"
                cFlags += baseCFlags + "-std=c2x"
                cppFlags += baseCppFlags + "-std=c++2b"
            }
        }

        ndk {
            // 测试包只留 arm64：libksud 的 late-load 也只有这一份 so
            abiFilters += listOf("arm64-v8a")
        }
    }

    splits {
        abi {
            isEnable = isReleaseTask
            reset()
            include("arm64-v8a", "x86_64", "armeabi-v7a", "riscv64")
            isUniversalApk = true
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = androidSourceCompatibility
        targetCompatibility = androidTargetCompatibility
    }
}

baselineProfile {
    mergeIntoMain = true
    saveInSrc = true
    automaticGenerationDuringBuild = false
}

base {
    archivesName.set(
        "LoomSU_${managerVersionName}_$managerVersionCode",
    )
}

aboutLibraries {
    library {
        // Enable the duplication mode, allows to merge, or link dependencies which relate
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
        // Configure the duplication rule, to match "duplicates" with
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    }
}

dependencies {
    lintChecks(project(":lint-rules"))
    baselineProfile(project(":baselineprofile"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.compose.viewmodel)

    implementation(libs.gson)
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.foundation)

    implementation(libs.androidx.compose.runtime.tracing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.miuix.blur)
    implementation(libs.miuix.nav)

    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)

    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.github.topjohnwu.libsu.service)
    implementation(libs.com.github.topjohnwu.libsu.io)

    implementation(libs.material.kolor)
    implementation(libs.monet.compat)
    implementation(libs.material.components)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.capsule)

    implementation(libs.dev.rikka.rikkax.parcelablelist)

    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.ucrop)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.me.zhanghai.android.appiconloader)
    implementation(libs.me.zhanghai.android.appiconloader.coil)
    implementation(libs.org.lsposed.hiddenapibypass)

    implementation(libs.markdown)
    implementation(libs.markwon.tables)
    implementation(libs.androidx.webkit)

    implementation(libs.lsposed.cxx)

    implementation(libs.accompanist.drawablepainter)
}

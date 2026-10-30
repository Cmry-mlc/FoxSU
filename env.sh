#!/usr/bin/env bash
# FoxSu 编译环境变量
# 用法: source /workspace/FoxSu/env.sh

export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export ANDROID_NDK_ROOT=/opt/android/android-ndk-r29
export ANDROID_NDK_HOME=/opt/android/android-ndk-r29
export JAVA_HOME=/opt/jdk21
export GRADLE_HOME=/opt/gradle-9.7.1

export PATH="$JAVA_HOME/bin:$GRADLE_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# rustup 组件下载走 USTC 镜像
export RUSTUP_DIST_SERVER=https://mirrors.ustc.edu.cn/rust-static

echo "ANDROID_NDK_ROOT = $ANDROID_NDK_ROOT"
echo "ANDROID_HOME     = $ANDROID_HOME"
echo "JAVA_HOME        = $JAVA_HOME ($($JAVA_HOME/bin/java -version 2>&1 | head -1))"
echo "GRADLE_HOME      = $GRADLE_HOME"

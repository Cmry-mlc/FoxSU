#!/usr/bin/env bash
# 一键构建 FoxSu：ksud(arm64/x86_64) + Manager APK
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

echo "==> [1/3] 编译 ksud (aarch64-linux-android)"
cargo build --target aarch64-linux-android --release --manifest-path userspace/ksud/Cargo.toml

echo "==> [2/3] 编译 ksud (x86_64-linux-android)"
cargo build --target x86_64-linux-android --release --manifest-path userspace/ksud/Cargo.toml

echo "==> 复制到 jniLibs"
mkdir -p manager/app/src/main/jniLibs/arm64-v8a manager/app/src/main/jniLibs/x86_64
cp target/aarch64-linux-android/release/ksud manager/app/src/main/jniLibs/arm64-v8a/libksud.so
cp target/x86_64-linux-android/release/ksud  manager/app/src/main/jniLibs/x86_64/libksud.so

echo "==> [3/3] 构建 Manager APK"
cd manager
gradle assembleRelease --console=plain

echo
echo "完成："
ls -lh app/build/outputs/apk/release/*.apk

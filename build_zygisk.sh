#!/usr/bin/env bash
# 一键构建 KuaiMaoSU（含内置 FolkZygisk 引擎）
# 用法：./build_zygisk.sh [manager|engine|all]  默认 all
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

NDK="${ANDROID_NDK_HOME:-/opt/android-sdk/ndk/30.0.16248370}"
export JAVA_HOME="${JAVA_HOME:-/opt/jdk21}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
export ANDROID_NDK_HOME="$NDK"
export PATH="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH"
# FoxSU 的 ksud 用了 #![feature(decl_macro)]，稳定版需 RUSTC_BOOTSTRAP 放行
export RUSTC_BOOTSTRAP=1

KEYSTORE="${KEYSTORE_FILE:-/workspace/loomsu-release.jks}"
KSPASS="${KEYSTORE_PASSWORD:-loomsu123456}"
ALIAS="${KEY_ALIAS:-loomsu}"
KEYPASS="${KEY_PASSWORD:-loomsu123456}"
OUT_DIR="${OUT_DIR:-$ROOT/dist}"

STEP="${1:-all}"

log() { printf '\033[1;36m[*]\033[0m %s\n' "$*"; }

build_engine() {
  log "编译 FolkZygisk (arm64 + arm32, API 26)"
  make -C userspace/folkzygisk BUILD_TYPE=release \
    ARCHS="arm64-v8a armeabi-v7a" NDK_PATH="$NDK" API_LEVEL=26
  log "打包引擎到 ksud assets"
  rm -rf userspace/ksud/bin/aarch64/folkzygisk
  python3 userspace/folkzygisk/package.py \
    --stage userspace/folkzygisk/build/stage/release \
    --assets userspace/ksud/bin/aarch64 --target aarch64
}

build_ksud() {
  log "编译 ksud aarch64 release"
  ( cd userspace/ksud && cargo clean -p ksud && \
      cargo build --target=aarch64-linux-android --release --offline )
  mkdir -p "$ROOT/target/aarch64-linux-android/release"
  cp userspace/ksud/target/aarch64-linux-android/release/ksud \
     "$ROOT/target/aarch64-linux-android/release/ksud"
}

build_apk() {
  log "构建 manager release APK"
  ./manager/gradlew -p manager :app:assembleRelease \
    -PKEYSTORE_FILE="$KEYSTORE" -PKEYSTORE_PASSWORD="$KSPASS" \
    -PKEY_ALIAS="$ALIAS" -PKEY_PASSWORD="$KEYPASS" --offline
}

repack() {
  log "注入 ksud 并重签名"
  # repack_apk.py 用「mtime 最新的 APK」当输入，assemble 后各 ABI 时间几乎相同，
  # 容易误选 riscv64。这里临时只留 arm64-v8a，确保输入 ABI 正确。
  local apk_dir="$ROOT/manager/app/build/outputs/apk/release"
  local bak="$ROOT/.apk-bak-zygisk"
  mkdir -p "$bak"
  local f base
  for f in "$apk_dir"/*.apk; do
    [ -f "$f" ] || continue
    base="$(basename "$f")"
    case "$base" in
      *arm64-v8a-release.apk) ;;
      *) mv "$f" "$bak"/ ;;
    esac
  done
  python3 repack_apk.py repack -b release -t release -a arm64-v8a \
    -K "$KEYSTORE" -A "$ALIAS" -P "$KSPASS" -S "$KEYPASS" \
    --strip -n KuaiMaoSU_4.0.0 -o "$OUT_DIR"
  for f in "$bak"/*.apk; do
    [ -f "$f" ] && mv "$f" "$apk_dir"/
  done
  rmdir "$bak" 2>/dev/null || true
}

case "$STEP" in
  engine)  build_engine ;;
  ksud)    build_engine; build_ksud ;;
  manager) build_apk ;;
  repack)  repack ;;
  all)     build_engine; build_ksud; build_apk; repack ;;
  *) echo "用法: $0 [manager|engine|ksud|repack|all]" >&2; exit 1 ;;
esac

log "完成"

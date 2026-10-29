#!/usr/bin/env bash
# 生成内核侧「管理器签名」常量（kernel/Kbuild 的 KSU_EXPECTED_SIZE / KSU_EXPECTED_HASH）。
#
# KernelSU 认管理器的方式是「包名 + APK 签名」双条件：包名不符连候选都不会考虑，
# 签名不符候选会被丢弃。换签名（或换发布密钥）后必须同步这两个值并重编内核/LKM，
# 否则 FoxSu 在管理器里会一直显示"未安装"，只能靠动态管理器（需 CONFIG_KSU_DEBUG）绕。
#
# 用法：
#   ./scripts/manager-signature.sh /path/to/release.jks <alias>
# 输出：
#   size = 0x....（证书 DER 字节数）
#   hash = sha256(DER)
set -euo pipefail

KS="${1:?usage: manager-signature.sh <keystore> <alias>}"
ALIAS="${2:?usage: manager-signature.sh <keystore> <alias>}"
PASS="${KEYSTORE_PASSWORD:-}"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

keytool -exportcert \
  -keystore "$KS" -alias "$ALIAS" \
  ${PASS:+-storepass "$PASS"} \
  -file "$TMP/cert.der" >/dev/null

SIZE=$(stat -c %s "$TMP/cert.der" 2>/dev/null || stat -f %z "$TMP/cert.der")
HASH=$(openssl dgst -sha256 "$TMP/cert.der" | awk '{print $NF}')

printf 'KSU_EXPECTED_SIZE ?= 0x%04x\n' "$SIZE"
printf 'KSU_EXPECTED_HASH ?= %s\n' "$HASH"

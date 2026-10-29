#!/usr/bin/env bash
# 生成正式版（release）签名密钥，并打印需要配置的 GitHub Secrets。
#
#   ./scripts/create-release-key.sh [输出目录] [别名]
#
# 默认输出到 ./release-key/，别名 foxsu。
# 生成的是 PKCS12 密钥库（RSA 4096，有效期 30 年），口令随机生成并写在同目录。
# 口令和 .jks 都要离线备份：Android 不允许换签名升级，丢了就只能让用户卸载重装。
set -euo pipefail

OUT_DIR="${1:-release-key}"
ALIAS="${2:-foxsu}"

command -v keytool >/dev/null || { echo "缺少 keytool，请先安装 JDK" >&2; exit 1; }
command -v openssl >/dev/null || { echo "缺少 openssl" >&2; exit 1; }

KEYSTORE="$OUT_DIR/foxsu-release.jks"
if [ -e "$KEYSTORE" ]; then
    echo "$KEYSTORE 已存在，拒绝覆盖（换密钥会让老用户无法升级）" >&2
    exit 1
fi

mkdir -p "$OUT_DIR"
chmod 700 "$OUT_DIR"

PASSWORD="$(openssl rand -base64 32 | tr -d '/+=\n' | cut -c1-24)"
printf '%s' "$PASSWORD" >"$OUT_DIR/keystore-password.txt"
chmod 600 "$OUT_DIR/keystore-password.txt"

keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -storetype PKCS12 \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 4096 \
    -validity 10950 \
    -storepass "$PASSWORD" -keypass "$PASSWORD" \
    -dname "CN=FoxSu, OU=Android, O=FoxSu, C=CN" >/dev/null 2>&1

base64 -w0 "$KEYSTORE" >"$OUT_DIR/keystore.b64" 2>/dev/null || base64 "$KEYSTORE" | tr -d '\n' >"$OUT_DIR/keystore.b64"

SIZE="$(keytool -exportcert -keystore "$KEYSTORE" -alias "$ALIAS" -storepass "$PASSWORD" -file "$OUT_DIR/cert.der" >/dev/null 2>&1; wc -c <"$OUT_DIR/cert.der" | tr -d ' ')"
HASH="$(openssl dgst -sha256 "$OUT_DIR/cert.der" | awk '{print $NF}')"

cat <<EOF
已生成签名密钥
  密钥库   : $KEYSTORE
  别名     : $ALIAS
  口令     : 见 $OUT_DIR/keystore-password.txt
  证书长度 : $SIZE 字节
  证书指纹 : $HASH

下一步（仓库需 admin 权限）：

  gh secret set KEYSTORE          --repo <owner>/<repo> < $OUT_DIR/keystore.b64
  gh secret set KEYSTORE_PASSWORD --repo <owner>/<repo> --body '$(cat "$OUT_DIR/keystore-password.txt")'
  gh secret set KEY_ALIAS         --repo <owner>/<repo> --body '$ALIAS'
  gh secret set KEY_PASSWORD      --repo <owner>/<repo> --body '$(cat "$OUT_DIR/keystore-password.txt")'

把证书指纹写进内核常量（必须提交，LKM 按它认管理器）：

  KEYSTORE_PASSWORD='$(cat "$OUT_DIR/keystore-password.txt")' \\
    ./scripts/manager-identity.sh sign $KEYSTORE $ALIAS

然后备份 $KEYSTORE 和 keystore-password.txt，并从 git 工作区排除：
  echo '$OUT_DIR/' >> .gitignore
EOF

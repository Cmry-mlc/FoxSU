#!/usr/bin/env bash
# 统一维护「管理器包名 + 签名指纹」，并校验 APK 与内核常量是否一致。
#
# 背景：FoxSu/KernelSU 的内核（kernelsu.ko）在 throne_tracker.c 里按
#   包名 == KSU_PACKAGE_NAME  且  APK 签名证书的 size/sha256 == KSU_EXPECTED_*
# 来认管理器。三处默认值必须同步，否则内核永远不会把 App 认成管理器：
# 表现就是管理器显示"未安装"、请求 root 没有弹窗、点了也授不了权。
#
# 用法:
#   ./scripts/manager-identity.sh show
#       打印当前三处包名与签名常量，指出不一致的地方。
#
#   ./scripts/manager-identity.sh set <包名>
#       同步修改 kernel/Kbuild、manager/app/build.gradle.kts、
#       userspace/ksud/build.rs 的默认包名（PR 构建自动用 <包名>.pr）。
#
#   ./scripts/manager-identity.sh sign <keystore> <alias>
#       从密钥库导出证书，写回 KSU_EXPECTED_SIZE / KSU_EXPECTED_HASH。
#       口令走环境变量 KEYSTORE_PASSWORD（PKCS12 与 key 口令相同）。
#
#   ./scripts/manager-identity.sh check <apk>
#       读取 APK 的真实包名与 v2 签名证书，和当前源码常量比对。
#
# 改完记得三样一起重编并重新刷入：ksud -> 管理器 APK -> kernelsu.ko（LKM）。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KBUILD="$ROOT/kernel/Kbuild"
GRADLE="$ROOT/manager/app/build.gradle.kts"
KSUD_BUILD="$ROOT/userspace/ksud/build.rs"
MAX_PKG_LEN=127

die() { printf '错误: %s\n' "$*" >&2; exit 1; }
note() { printf '%s\n' "$*"; }

require_file() {
    [ -f "$1" ] || die "找不到文件: $1"
}

kbuild_pkg() {
    grep -m1 -E '^KSU_PACKAGE_NAME \?=' "$KBUILD" | awk '{print $3}'
}

kbuild_size() {
    grep -m1 -E '^KSU_EXPECTED_SIZE \?=' "$KBUILD" | awk '{print $3}'
}

kbuild_hash() {
    grep -m1 -E '^KSU_EXPECTED_HASH \?=' "$KBUILD" | awk '{print $3}'
}

gradle_pkg() {
    sed -n 's/.*if (isPrBuild) "[^"]*" else "\([^"]*\)".*/\1/p' "$GRADLE" | head -n1
}

ksud_pkg() {
    sed -n 's/.*rustc-env=KSU_PACKAGE_NAME=\([^"]*\)".*/\1/p' "$KSUD_BUILD" | head -n1
}

cmd_show() {
    require_file "$KBUILD"
    require_file "$GRADLE"
    require_file "$KSUD_BUILD"

    local pkg size hash gp kp
    pkg="$(kbuild_pkg)"
    size="$(kbuild_size)"
    hash="$(kbuild_hash)"
    gp="$(gradle_pkg)"
    kp="$(ksud_pkg)"

    note "管理器身份（源码默认值）"
    note "--------------------------------------------------------------"
    note "kernel/Kbuild            : $pkg"
    note "manager/build.gradle.kts : $gp"
    note "ksud/build.rs            : $kp"
    note "签名证书 size            : $size"
    note "签名证书 sha256          : $hash"

    local bad=0
    if [ "$pkg" != "$gp" ]; then
        note ""
        note "✗ 内核包名与 Gradle 默认包名不一致：内核在找 $pkg，APK 装的是 $gp"
        bad=1
    fi
    if [ "$pkg" != "$kp" ]; then
        note ""
        note "✗ 内核包名与 ksud 默认包名不一致：/data/user_de/<包名> 备份目录、启动管理器都会落空"
        bad=1
    fi
    if [ "$size" = "0x0360" ] && [ "$hash" = "436cd0842ed146ae35b386c1b2253634944d6741f1a0278b392f74c4063c8b86" ]; then
        note ""
        note "· 签名常量仍是仓库发布证书（/opt/foxsu-build/foxsu.jks）。"
        note "  如果你用别的密钥签管理器，必须执行 sign 子命令，否则内核认不出管理器。"
    fi
    if [ "$bad" -eq 0 ]; then
        note ""
        note "✓ 三处包名一致"
    fi
    return "$bad"
}

cmd_set() {
    local pkg="${1:-}"
    [ -n "$pkg" ] || die "用法: manager-identity.sh set <包名>"

    if ! printf '%s' "$pkg" | grep -Eq '^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$'; then
        die "不是合法的 Android 包名: $pkg"
    fi
    if [ "${#pkg}" -gt "$MAX_PKG_LEN" ]; then
        die "包名过长（内核上限 $MAX_PKG_LEN 字节）: $pkg"
    fi
    case "$pkg" in
        *.pr) die "包名不要以 .pr 结尾，PR 构建会自动加后缀" ;;
    esac

    require_file "$KBUILD"
    require_file "$GRADLE"
    require_file "$KSUD_BUILD"

    python3 - "$KBUILD" "$GRADLE" "$KSUD_BUILD" "$pkg" <<'PY'
import re
import sys

kbuild, gradle, ksud_build, pkg = sys.argv[1:5]

# kernel/Kbuild: 第一条是正式构建，第二条是 PR 构建
text = open(kbuild, encoding="utf-8").read()
out, seen = [], 0
for line in text.splitlines(keepends=True):
    m = re.match(r"^(KSU_PACKAGE_NAME \?= )(\S+)(\s*)$", line)
    if m:
        seen += 1
        line = m.group(1) + (pkg if seen == 1 else pkg + ".pr") + m.group(3)
    out.append(line)
open(kbuild, "w", encoding="utf-8").write("".join(out))

# manager/app/build.gradle.kts
text = open(gradle, encoding="utf-8").read()
text, n = re.subn(
    r'val defaultManagerPackageName = if \(isPrBuild\) "[^"]*" else "[^"]*"',
    f'val defaultManagerPackageName = if (isPrBuild) "{pkg}.pr" else "{pkg}"',
    text,
)
if n != 1:
    sys.exit("build.gradle.kts 里没找到 defaultManagerPackageName，请手动确认")
open(gradle, "w", encoding="utf-8").write(text)

# userspace/ksud/build.rs
text = open(ksud_build, encoding="utf-8").read()
text, n = re.subn(
    r'(rustc-env=KSU_PACKAGE_NAME=)[^"]*',
    lambda m: m.group(1) + pkg,
    text,
)
if n != 1:
    sys.exit("ksud/build.rs 里没找到 KSU_PACKAGE_NAME 默认值，请手动确认")
open(ksud_build, "w", encoding="utf-8").write(text)
PY

    note "已把默认包名改为: $pkg"
    note "  kernel/Kbuild            -> $pkg（PR: $pkg.pr）"
    note "  manager/build.gradle.kts -> $pkg（PR: $pkg.pr）"
    note "  ksud/build.rs            -> $pkg"
    note ""
    note "下一步：重编 ksud -> 重打管理器 APK -> 重编 kernelsu.ko 并重刷 boot 镜像。"
}

cmd_sign() {
    local keystore="${1:-}" alias="${2:-}"
    if [ -z "$keystore" ] || [ -z "$alias" ]; then
        die "用法: manager-identity.sh sign <keystore> <alias>"
    fi
    [ -f "$keystore" ] || die "找不到密钥库: $keystore"
    command -v keytool >/dev/null || die "缺少 keytool（安装 JDK）"
    command -v openssl >/dev/null || die "缺少 openssl"

    local tmp
    tmp="$(mktemp -d)"

    keytool -exportcert -keystore "$keystore" -alias "$alias" \
        ${KEYSTORE_PASSWORD:+-storepass "$KEYSTORE_PASSWORD"} \
        -file "$tmp/cert.der" >"$tmp/keytool.log" 2>&1 || {
        cat "$tmp/keytool.log" >&2
        rm -rf "$tmp"
        die "导出证书失败（口令或别名不对？）"
    }

    local size hash
    size="$(wc -c <"$tmp/cert.der" | tr -d ' ')"
    hash="$(openssl dgst -sha256 "$tmp/cert.der" | awk '{print $NF}')"

    python3 - "$KBUILD" "$(printf '0x%04x' "$size")" "$hash" <<'PY'
import re
import sys

path, size, digest = sys.argv[1:4]
text = open(path, encoding="utf-8").read()
text, n1 = re.subn(r"(?m)^KSU_EXPECTED_SIZE \?= .*$", f"KSU_EXPECTED_SIZE ?= {size}", text)
text, n2 = re.subn(r"(?m)^KSU_EXPECTED_HASH \?= .*$", f"KSU_EXPECTED_HASH ?= {digest}", text)
if n1 != 1 or n2 != 1:
    sys.exit("Kbuild 里的 KSU_EXPECTED_SIZE / KSU_EXPECTED_HASH 没匹配到")
open(path, "w", encoding="utf-8").write(text)
PY

    note "已写入签发证书指纹:"
    note "  KSU_EXPECTED_SIZE ?= $(printf '0x%04x' "$size")   # $size 字节"
    note "  KSU_EXPECTED_HASH ?= $hash"
    rm -rf "$tmp"
    note ""
    note "提醒：这正是「签名不匹配 → 管理器显示未安装」的常量，改完必须重编 kernelsu.ko 并重刷。"
}

cmd_check() {
    local apk="${1:-}"
    [ -n "$apk" ] || die "用法: manager-identity.sh check <apk>"
    [ -f "$apk" ] || die "找不到 APK: $apk"

    local json
    json="$(python3 "$ROOT/scripts/apk_identity.py" "$apk" --json)"

    local apkg asize ahash kpkg ksize khash
    apkg="$(printf '%s' "$json" | python3 -c 'import json,sys;print(json.load(sys.stdin)["package"] or "")')"
    asize="$(printf '%s' "$json" | python3 -c 'import json,sys;print(json.load(sys.stdin)["cert_size"] or 0)')"
    ahash="$(printf '%s' "$json" | python3 -c 'import json,sys;print(json.load(sys.stdin)["cert_sha256"] or "")')"
    kpkg="$(kbuild_pkg)"
    ksize="$(kbuild_size)"
    khash="$(kbuild_hash)"

    note "APK      包名: ${apkg:-<未能解析>}"
    note "内核期望包名: $kpkg"
    if [ "$asize" = "0" ] || [ -z "$ahash" ]; then
        note "APK      证书: <没有 v2 签名块，内核无法校验>"
    else
        note "APK      证书: $asize 字节 / $ahash"
    fi
    note "内核期望证书: $((ksize)) 字节 / $khash"
    note ""

    local bad=0
    [ "$apkg" = "$kpkg" ] || { note "✗ 包名不匹配：内核不会把这个 APK 当管理器"; bad=1; }
    [ "$asize" = "$((ksize))" ] || { note "✗ 证书长度不匹配：签名校验会失败"; bad=1; }
    [ "$ahash" = "$khash" ] || { note "✗ 证书指纹不匹配：签名校验会失败"; bad=1; }

    if [ "$bad" -eq 0 ]; then
        note "✓ 这个 APK 能被当前内核认成管理器"
    else
        note ""
        note "修复：./scripts/manager-identity.sh set <包名> / sign <keystore> <alias>，重编 kernelsu.ko 后重刷。"
    fi
    return "$bad"
}

case "${1:-}" in
    show) cmd_show ;;
    set) shift; cmd_set "$@" ;;
    sign) shift; cmd_sign "$@" ;;
    check) shift; cmd_check "$@" ;;
    ""|-h|--help|help)
        sed -n '2,30p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
        ;;
    *) die "未知子命令: $1（可用: show / set / sign / check）" ;;
esac

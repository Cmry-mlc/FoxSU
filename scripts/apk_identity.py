#!/usr/bin/env python3
"""读取 APK 的包名与签名证书（仅用标准库）。

KernelSU/FoxSu 内核按「包名 + 签名证书」认管理器，这个脚本用来自查：
输出的 size / sha256 与 kernel/Kbuild 的 KSU_EXPECTED_SIZE / KSU_EXPECTED_HASH 对应。

用法:
    python3 scripts/apk_identity.py <apk> [--json] [--kbuild kernel/Kbuild]

带 --kbuild 时会和内核常量比对，不一致返回非 0（CI 用它拦住"签名不对还发版"）。
"""

import hashlib
import json
import struct
import sys
import zipfile


def _u32(buf, off):
    return struct.unpack_from("<I", buf, off)[0]


def _u64(buf, off):
    return struct.unpack_from("<Q", buf, off)[0]


# ---------------------------------------------------------------- manifest

def _decode_string_pool(buf, off):
    """解析 AXML string pool，返回字符串列表。"""
    header_size = struct.unpack_from("<H", buf, off + 2)[0]
    chunk_size = _u32(buf, off + 4)
    string_count = _u32(buf, off + 8)
    flags = _u32(buf, off + 16)
    strings_start = _u32(buf, off + 20)
    utf8 = bool(flags & 0x100)

    offsets = [_u32(buf, off + header_size + 4 * i) for i in range(string_count)]
    base = off + strings_start
    end = off + chunk_size
    out = []
    for rel in offsets:
        p = base + rel
        if p >= end:
            out.append("")
            continue
        try:
            if utf8:
                # utf16 length (varint), utf8 length (varint), bytes, 0x00
                n = buf[p]
                p += 2 if n & 0x80 else 1
                n = buf[p]
                if n & 0x80:
                    n = ((n & 0x7F) << 8) | buf[p + 1]
                    p += 2
                else:
                    p += 1
                out.append(buf[p:p + n].decode("utf-8", "replace"))
            else:
                n = struct.unpack_from("<H", buf, p)[0]
                p += 2
                if n & 0x8000:
                    n = ((n & 0x7FFF) << 16) | struct.unpack_from("<H", buf, p)[0]
                    p += 2
                out.append(buf[p:p + n * 2].decode("utf-16-le", "replace"))
        except (IndexError, struct.error):
            out.append("")
    return out


def apk_package(path):
    """从二进制 AndroidManifest.xml 的 <manifest package="..."> 取包名。"""
    with zipfile.ZipFile(path) as zf:
        buf = zf.read("AndroidManifest.xml")

    strings = None
    off = 8  # 跳过 XML chunk 头
    total = len(buf)
    while off + 8 <= total:
        ctype = struct.unpack_from("<H", buf, off)[0]
        hsize = struct.unpack_from("<H", buf, off + 2)[0]
        csize = _u32(buf, off + 4)
        if csize <= 0:
            break
        if ctype == 0x0001:  # string pool
            strings = _decode_string_pool(buf, off)
        elif ctype == 0x0102 and strings:  # start element
            name_idx = _u32(buf, off + 20)
            attr_start = struct.unpack_from("<H", buf, off + 24)[0]
            attr_size = struct.unpack_from("<H", buf, off + 26)[0]
            attr_count = struct.unpack_from("<H", buf, off + 28)[0]
            name = strings[name_idx] if name_idx < len(strings) else ""
            attr_off = off + 16 + attr_start
            if name == "manifest":
                for i in range(attr_count):
                    a = attr_off + i * attr_size
                    aname = _u32(buf, a + 4)
                    raw = _u32(buf, a + 8)
                    dtype = buf[a + 15]
                    data = _u32(buf, a + 16)
                    if aname < len(strings) and strings[aname] == "package":
                        if raw != 0xFFFFFFFF and raw < len(strings):
                            return strings[raw]
                        if dtype == 0x03 and data < len(strings):
                            return strings[data]
                return None
        off += csize
    return None


# --------------------------------------------------------------- signature

def _lp(buf, off):
    """读取 u32 长度前缀的块，返回 (data, next_off)。"""
    if off + 4 > len(buf):
        raise ValueError("truncated length prefix")
    size = _u32(buf, off)
    start = off + 4
    if start + size > len(buf):
        raise ValueError("truncated length-prefixed value")
    return buf[start:start + size], start + size


def apk_signing_cert(path):
    """取 APK v2 签名块里第一张证书的 DER，返回 (size, sha256_hex)。"""
    with open(path, "rb") as fp:
        data = fp.read()

    eocd_sig = b"PK\x05\x06"
    idx = data.rfind(eocd_sig, max(0, len(data) - 65557))
    if idx < 0:
        return None
    cd_offset = _u32(data, idx + 16)

    block_magic = b"APK Sig Block 42"
    if data[cd_offset - 16:cd_offset] != block_magic:
        return None
    footer_size = _u64(data, cd_offset - 24)
    header_off = cd_offset - footer_size - 8

    pairs_off = header_off + 8
    pairs_end = cd_offset - 24
    while pairs_off + 12 <= pairs_end:
        pair_len = _u64(data, pairs_off)
        pair_id = _u32(data, pairs_off + 8)
        value_off = pairs_off + 12
        value_end = pairs_off + 8 + pair_len
        if pair_id == 0x7109871A:  # APK Signature Scheme v2
            value = data[value_off:value_end]
            signers, _ = _lp(value, 0)
            signer, _ = _lp(signers, 0)
            signed_data, _ = _lp(signer, 0)
            _, next_off = _lp(signed_data, 0)  # digests
            certs, _ = _lp(signed_data, next_off)
            cert, _ = _lp(certs, 0)
            return len(cert), hashlib.sha256(cert).hexdigest()
        pairs_off = value_end
    return None


def read_kbuild(path):
    """从 kernel/Kbuild 读 KSU_PACKAGE_NAME / KSU_EXPECTED_SIZE / KSU_EXPECTED_HASH。"""
    import re

    text = open(path, encoding="utf-8").read()
    pkg = re.search(r"(?m)^KSU_PACKAGE_NAME \?= (\S+)", text)
    size = re.search(r"(?m)^KSU_EXPECTED_SIZE \?= (\S+)", text)
    digest = re.search(r"(?m)^KSU_EXPECTED_HASH \?= (\S+)", text)
    if not (pkg and size and digest):
        raise ValueError(f"{path} 里没有 KSU_PACKAGE_NAME / KSU_EXPECTED_*")
    return pkg.group(1), int(size.group(1), 0), digest.group(1)


def main():
    if len(sys.argv) < 2:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    apk = sys.argv[1]
    result = {"apk": apk}
    try:
        result["package"] = apk_package(apk)
    except Exception as exc:  # noqa: BLE001
        result["package"] = None
        result["package_error"] = str(exc)
    try:
        cert = apk_signing_cert(apk)
    except Exception as exc:  # noqa: BLE001
        cert = None
        result["cert_error"] = str(exc)
    if cert:
        result["cert_size"] = cert[0]
        result["cert_sha256"] = cert[1]
    else:
        result["cert_size"] = None
        result["cert_sha256"] = None

    if "--json" in sys.argv[2:]:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print(f"package      : {result['package']}")
        print(f"cert size    : {result['cert_size']}")
        print(f"cert sha256  : {result['cert_sha256']}")

    if "--kbuild" in sys.argv[2:]:
        kbuild = sys.argv[sys.argv.index("--kbuild") + 1]
        pkg, size, digest = read_kbuild(kbuild)
        problems = []
        if result["package"] != pkg:
            problems.append(f"包名: APK={result['package']} 内核={pkg}")
        if result["cert_size"] != size:
            problems.append(f"证书长度: APK={result['cert_size']} 内核={size}")
        if result["cert_sha256"] != digest:
            problems.append(f"证书指纹: APK={result['cert_sha256']} 内核={digest}")
        if problems:
            print("\n".join(problems), file=sys.stderr)
            print("内核不会把这个 APK 认成管理器（显示未安装、无法授权）", file=sys.stderr)
            return 1
        print("✓ 与 kernel/Kbuild 的管理器身份一致")
    return 0



if __name__ == "__main__":
    sys.exit(main())

# 内置 Zygisk 引擎来源与许可声明

KuaiMaoSU 内置了 FolkZygisk 引擎（无需安装模块即可使用 Zygisk）。引擎以**独立 native 二进制**形式编译进 `ksud`，再由 `ksud` 在运行时释放到 `/data/adb/ksu/bin/folkzygisk/`。本文件说明其来源与许可，代码与资产各自的许可证相互独立、互不覆盖。

## 来源链路

| 组件 | 来源 | 许可 | 用途 |
| :--- | :--- | :--- | :--- |
| **FolkZygisk** | [FinaSU](https://github.com/LyraVoid/FinaSU) `userspace/folkzygisk` | AGPL-3.0 | FinaSU 基于 ReZygisk 的适配版本，本项目的直接移植来源 |
| **ReZygisk** | [PerformanC/ReZygisk](https://github.com/PerformanC/ReZygisk) commit `5b0a02b` | AGPL-3.0 | loader / zygiskd / 注入逻辑的上游 |
| **CSOLoader** | [ThePedroo/CSOLoader](https://github.com/ThePedroo/CSOLoader) commit `3762c52` | AGPL-3.0 | zygote 注入所需的 SO 加载器，vendored 于 loader 内 |
| **PLTI** | [PerformanC/PLTI](https://github.com/PerformanC/PLTI) commit `3e7cc55` | BSD-3-Clause | PLT 内联钩子系统，vendored 于 loader 内 |

完整上游溯源与贡献者声明见 `userspace/folkzygisk/UPSTREAM.md`。

## 许可说明

- FolkZygisk、ReZygisk 与 CSOLoader 采用 **GNU Affero General Public License v3.0 (AGPL-3.0)**，许可全文随引擎一并嵌入，位于：
  - `userspace/folkzygisk/LICENSE`（上游 AGPL-3.0 全文）
  - 嵌入资源中的 `folkzygisk/AGPL-3.0.txt`（CSOLoader 的 AGPL-3.0 全文）
  - `userspace/folkzygisk/loader/src/external/csoloader/LICENSE`
- PLTI 采用 **BSD-3-Clause**，全文见 `userspace/folkzygisk/loader/src/external/plti/LICENSE`。
- `NOTICE` 与 `UPSTREAM.md` 保留上游署名与来源链接。

## 与本项目其它许可证的关系

- 本项目的 `ksud`、管理器及其它代码维持 **GPL-3.0**。
- 引擎以独立二进制形式嵌入并由 `ksud` 在运行时释放，其 AGPL-3.0 许可独立适用，**不改变** ksud 与管理器的 GPL-3.0 授权。
- 分发包含内置引擎的 KuaiMaoSU 时，须保留上述全部许可证与署名文件——这些文件已随引擎资源一并编入最终 APK。

## 行为说明

- 当系统内已安装外部 Zygisk 提供者（ZygiskNext / ReZygisk / NeoZygisk 等）时，内置引擎会自动让位，不干预模块安装，避免冲突。
- 移除外部提供者后，下一次启动会重新接管。
- 引擎开关在「设置 → 内置引擎」中控制，所有变更重启后生效。

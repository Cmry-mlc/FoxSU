# 发布正式包

正式包（Release）由 `.github/workflows/release.yml` 在推 `v*` tag 时触发：先构建 LKM（8 个 KMI）、ksud、ksuinit 与管理器，再用仓库 secret 里的密钥重新签名 APK，最后把 APK、`kernelsu.ko`、`ksud`、`ksuinit` 一起挂到 GitHub Release。

发版能否顺利跑起来，只取决于三件事：**签名密钥**、**仓库 Secrets**、**内核里的签名常量**。三者任意一处不一致，产出的包刷进设备后会显示"未安装"、请求 root 无弹窗、也无法授权。

## 一、生成签名密钥（一次性）

```bash
./scripts/create-release-key.sh            # 输出到 ./release-key/，别名 foxsu
```

脚本生成 PKCS12 密钥库（RSA 4096，有效期 30 年），随机口令写在同目录的 `keystore-password.txt`，并打印后续要执行的命令。

要点：

- **别名、口令、`.jks` 三者要一起备份**。Android 不允许换签名升级，密钥丢失或口令遗忘后，老用户只能卸载重装。
- 有效期至少 25 年；Google Play 要求密钥有效期覆盖到 2033 年以后。
- 用 Android Studio 生成也可以（Build → Generate Signed Bundle / APK → Create new），务必选 **PKCS12**、RSA ≥ 2048、有效期 30 年。
- `release-key/` 与 `*.jks` 已在 `.gitignore` 里，别提交进仓库。

## 二、配置仓库 Secrets

需要仓库 admin 权限，四个 secret：

| Secret | 内容 |
| --- | --- |
| `KEYSTORE` | `.jks` 的 base64（单行，`base64 -w0 foxsu-release.jks`） |
| `KEYSTORE_PASSWORD` | 密钥库口令 |
| `KEY_ALIAS` | 别名，如 `foxsu` |
| `KEY_PASSWORD` | key 口令（PKCS12 与上面相同） |

命令行配置：

```bash
R=release-key
gh secret set KEYSTORE          --repo <owner>/<repo> < $R/keystore.b64
gh secret set KEYSTORE_PASSWORD --repo <owner>/<repo> --body "$(cat $R/keystore-password.txt)"
gh secret set KEY_ALIAS         --repo <owner>/<repo> --body foxsu
gh secret set KEY_PASSWORD      --repo <owner>/<repo> --body "$(cat $R/keystore-password.txt)"
gh secret list --repo <owner>/<repo>
```

`build-manager.yml` 会在 tag 构建时检查 `KEYSTORE` 是否存在，缺失直接失败，避免发出一批每次签名都不同的包。

## 三、把证书指纹写进内核

内核只按「包名 + APK 签名证书的 size/sha256」认管理器（`kernel/manager/throne_tracker.c`），这两个常量编译进 `kernelsu.ko`：

```bash
KEYSTORE_PASSWORD="$(cat release-key/keystore-password.txt)" \
  ./scripts/manager-identity.sh sign release-key/foxsu-release.jks foxsu
./scripts/manager-identity.sh show
```

输出会写入 `kernel/Kbuild` 的 `KSU_EXPECTED_SIZE` / `KSU_EXPECTED_HASH`，**必须提交到 `main`**，否则 Release 里编出的 LKM 认不出用新密钥签的 APK。

包名同理：改包名用 `./scripts/manager-identity.sh set com.your.package`，它会同步 `kernel/Kbuild`、`manager/app/build.gradle.kts`、`userspace/ksud/build.rs` 三处（PR 构建自动用 `.pr` 后缀）。

## 四、发版

```bash
git tag -a v1.2.0 -m "FoxSu v1.2.0"
git push origin v1.2.0
```

- 版本号：versionCode = `30000 + git 提交数`，versionName 取自 `git describe`，所以先打 tag 再构建才能拿到正确版本名。
- 流程：`Release` → `build-manager`（LKM ×8、ksud ×3、ksud-extra ×5、ksuinit、管理器重签）→ `release`。
- 产物：`manager/*.apk`（已用正式密钥重签）、`lkm/lkm-*_kernelsu.ko`、`ksud/*`、`ksuinit/*`。

发布前本地自检：

```bash
./scripts/manager-identity.sh show
python3 scripts/apk_identity.py app-release.apk --kbuild kernel/Kbuild
```

CI 里 `repack-manager` 也会跑同一条校验，APK 与内核常量对不上就直接失败，不会把坏包发出去。

## 五、常见坑

| 现象 | 原因 |
| --- | --- |
| 管理器显示"未安装"、点授权没反应 | APK 包名或签名与 LKM 里的 `KSU_*` 常量不一致；刷的镜像不是用当前源码编的 |
| 每次发的包签名都不同 | 没配 `KEYSTORE` secret，CI 用了临时密钥 |
| Release 构建在签名步骤失败 | tag 构建缺 `KEYSTORE`（现在会明确报错） |
| 老用户装不上新包 | 换了签名密钥；只能卸载重装，且内核常量要同步更新 |
| PR 构建的包在设备上不认 | PR 用临时密钥 + `.pr` 包名，本来就不能配正式内核 |

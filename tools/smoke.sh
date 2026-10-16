#!/usr/bin/env bash
# 에뮬레이터에서 APK 를 설치·실행하고 주요 화면을 찍는다 (smoke.yml 에서 부름).
set -u
APK="$1"
OUT=shots
PKG=com.earlln.history
mkdir -p "$OUT"

shot() { sleep 2; adb exec-out screencap -p > "$OUT/$1.png"; }

# 화면에서 글자가 text 인 요소의 가운데를 누른다
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null
  local b
  b=$(python3 - "$1" <<'PY'
import re, sys
xml = open('/tmp/ui.xml', encoding='utf-8').read()
want = sys.argv[1]
for m in re.finditer(r'<node [^>]*>', xml):
    n = m.group(0)
    t = re.search(r' text="([^"]*)"', n)
    d = re.search(r' content-desc="([^"]*)"', n)
    if (t and t.group(1).startswith(want)) or (d and d.group(1) == want):
        x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  if [ -z "$b" ]; then echo "못 찾음: $1"; return 1; fi
  adb shell input tap $b
}

adb install -r "$APK"
adb logcat -c
adb shell am start -n $PKG/.MainActivity
sleep 6
shot 01_home_korean_eras

tap_text "핵심 키워드" && shot 02_korean_keywords
tap_text "인물" && shot 03_korean_keywords_people
tap_text "연대별 흐름"
tap_text "고려" && shot 04_era_goryeo
tap_text "고려의 성립과 통치 체제" && shot 05_topic_goryeo_early
tap_text "광종" && shot 06_key_gwangjong
adb shell input keyevent KEYCODE_BACK; adb shell input keyevent KEYCODE_BACK; adb shell input keyevent KEYCODE_BACK
sleep 1
tap_text "세계사" && shot 07_world_eras
tap_text "핵심 키워드" && shot 08_world_keywords
tap_text "검색" && adb shell input text "napoleon" ; sleep 1
adb shell input keyevent KEYCODE_BACK

if adb shell pidof $PKG >/dev/null; then echo "앱 살아 있음"; else echo "앱이 죽었음"; fi
adb logcat -d -b crash > "$OUT/crash.txt"
adb logcat -d | grep -E "AndroidRuntime|FATAL" > "$OUT/fatal.txt" || true
if [ -s "$OUT/crash.txt" ]; then echo "크래시 기록:"; cat "$OUT/crash.txt"; exit 1; fi
echo "크래시 없음"

"""웹앱(PWA)을 _site/ 로 묶는다 — GitHub Pages 배포용.

    python tools/build_content.py   # 먼저 history.json 을 만든다
    python tools/build_web.py       # web/ + history.json + 아이콘 → _site/
    python tools/build_web.py --icons-only   # 아이콘만 다시 그림 (web/icons)

버전은 app/build.gradle.kts 의 versionName 을 그대로 쓴다 (안드로이드 앱과 같은 번호).
"""
import pathlib
import re
import shutil
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parent.parent
WEB = ROOT / "web"
SITE = ROOT / "_site"
JSON = ROOT / "app" / "src" / "main" / "assets" / "history.json"


def make_icons():
    """안드로이드 런처 아이콘과 같은 그림: 갈색 바탕에 펼친 책 + 시간 축."""
    from PIL import Image, ImageDraw

    out = WEB / "icons"
    out.mkdir(exist_ok=True)
    big = 1024
    s = big / 108  # 안드로이드 벡터(108 단위)를 그대로 옮긴다
    img = Image.new("RGB", (big, big), "#7A3E1D")
    d = ImageDraw.Draw(img)

    def pts(seq):
        return [(x * s, y * s) for x, y in seq]

    d.polygon(pts([(30, 38), (38, 35.5), (46, 35.5), (53, 39), (53, 72), (46, 68.5), (38, 68.5), (30, 71)]), fill="#FBF7F1")
    d.polygon(pts([(55, 39), (62, 35.5), (70, 35.5), (78, 38), (78, 71), (70, 68.5), (62, 68.5), (55, 72)]), fill="#F1E4D2")
    d.rectangle([38 * s, 51 * s, 70 * s, 53 * s], fill="#D9A441")
    for cx in (38, 54, 70):
        d.ellipse([(cx - 3) * s, 49 * s, (cx + 3) * s, 55 * s], fill="#D9A441")

    for name, size in (("apple-touch-icon.png", 180), ("icon-192.png", 192), ("icon-512.png", 512)):
        img.resize((size, size), Image.LANCZOS).save(out / name)
    print(f"아이콘 → {out.relative_to(ROOT)}")


def main():
    if "--icons-only" in sys.argv:
        make_icons()
        return
    version = re.search(r'versionName = "([^"]+)"', (ROOT / "app" / "build.gradle.kts").read_text(encoding="utf-8")).group(1)
    if not JSON.exists():
        sys.exit("history.json 이 없습니다 — 먼저 python tools/build_content.py")
    if SITE.exists():
        shutil.rmtree(SITE)
    shutil.copytree(WEB, SITE)
    shutil.copy(JSON, SITE / "history.json")
    build = time.strftime("%Y%m%d%H%M%S")
    for f in ("index.html", "sw.js"):
        p = SITE / f
        p.write_text(p.read_text(encoding="utf-8").replace("__VERSION__", version).replace("__BUILD__", build), encoding="utf-8")
    (SITE / ".nojekyll").write_text("")
    print(f"_site/ 준비 (v{version}, build {build})")


if __name__ == "__main__":
    main()

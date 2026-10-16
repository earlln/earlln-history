"""content/ 의 역사 원고(.txt)를 앱이 읽는 app/src/main/assets/history.json 으로 바꾼다.

원고 형식
    @subject korean | 한국사
    @era     k01 | 선사 시대 | 약 70만 년 전 ~
    @topic   k01_paleo | 구석기 시대 | 약 70만 년 전 ~
    @key     k01_hand_axe | 주먹도끼 | 문화·유산 | (시기, 생략 가능)
    머리줄 다음 줄부터 '---' 까지가 요약(한두 문장), '---' 뒤부터 다음 '@' 까지가 본문.
    본문에서 '## ' 는 소제목, '- ' 는 글머리표, [[id]] · [[id|보이는 글자]] 는 다른 항목으로 가는 링크.
    '//' 로 시작하는 줄은 주석.

subject → era → topic → key 순으로 앞에 나온 윗 단계 아래에 붙는다.
깨진 링크 · 중복 id · 모르는 분류가 있으면 실패한다 (CI 에서도 돈다).

    python tools/build_content.py           # 만들기
    python tools/build_content.py --check   # 검사만
"""
import json
import pathlib
import re
import sys
from datetime import date

ROOT = pathlib.Path(__file__).resolve().parent.parent
CONTENT = ROOT / "content"
OUT = ROOT / "app" / "src" / "main" / "assets" / "history.json"
SUBJECTS = ["korean", "world"]
LEVELS = ["subject", "era", "topic", "key"]
CATEGORIES = {"인물", "사건·전쟁", "제도·정책", "국가·단체", "사상·종교", "문화·유산", "경제·사회", "조약·문서"}
LINK = re.compile(r"\[\[([^\]|]+)(?:\|([^\]]+))?]]")
HEAD = re.compile(r"^@(subject|era|topic|key)\s+(.+)$")


def parse_file(path, nodes, order, errors, stack):
    cur = None
    in_body = False
    for lineno, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.rstrip()
        if line.startswith("//"):
            continue
        m = HEAD.match(line)
        if m:
            kind = m.group(1)
            parts = [p.strip() for p in m.group(2).split("|")]
            nid, title = parts[0], parts[1] if len(parts) > 1 else ""
            where = f"{path.relative_to(ROOT)}:{lineno}"
            if not re.fullmatch(r"[a-z0-9_]+", nid):
                errors.append(f"{where} id 는 영소문자·숫자·_ 만: {nid}")
            if nid in nodes:
                errors.append(f"{where} id 중복: {nid}")
            level = LEVELS.index(kind)
            parent = stack[level - 1] if level > 0 else None
            if level > 0 and parent is None:
                errors.append(f"{where} 윗 단계 없이 @{kind}")
            node = {"type": kind, "title": title, "period": "", "category": "",
                    "summary": [], "body": [], "parent": parent, "children": [], "_where": where}
            if kind == "key":
                node["category"] = parts[2] if len(parts) > 2 else ""
                node["period"] = parts[3] if len(parts) > 3 else ""
                if node["category"] not in CATEGORIES:
                    errors.append(f"{where} 모르는 분류 '{node['category']}' ({nid})")
            elif kind != "subject":
                node["period"] = parts[2] if len(parts) > 2 else ""
            nodes[nid] = node
            order.append(nid)
            if parent:
                nodes[parent]["children"].append(nid)
            stack[level] = nid
            for deeper in range(level + 1, len(LEVELS)):
                stack[deeper] = None
            cur, in_body = node, False
            continue
        if cur is None:
            if line.strip():
                errors.append(f"{path.relative_to(ROOT)}:{lineno} 머리줄(@...) 앞에 글이 있음")
            continue
        if line.strip() == "---" and not in_body:
            in_body = True
            continue
        (cur["body"] if in_body else cur["summary"]).append(line)


def build():
    nodes, order, errors = {}, [], []
    for subject in SUBJECTS:
        stack = [None] * len(LEVELS)
        files = sorted((CONTENT / subject).glob("*.txt"))
        if not files:
            errors.append(f"content/{subject} 에 원고가 없음")
        for f in files:
            parse_file(f, nodes, order, errors, stack)
        if subject not in nodes:
            errors.append(f"@subject {subject} 가 없음")

    for nid in order:
        n = nodes[nid]
        n["summary"] = " ".join(s.strip() for s in n["summary"] if s.strip())
        n["body"] = "\n".join(n["body"]).strip("\n")
        if not n["summary"]:
            errors.append(f"{n['_where']} 요약 없음: {nid}")
        for m in LINK.finditer(n["summary"] + "\n" + n["body"]):
            target = m.group(1).strip()
            if target not in nodes:
                errors.append(f"{n['_where']} 깨진 링크 [[{target}]] ({nid})")
        if n["type"] in ("era", "topic") and not n["children"]:
            errors.append(f"{n['_where']} 아래 항목 없음: {nid}")

    return nodes, order, errors


def main():
    nodes, order, errors = build()
    counts = {t: sum(1 for n in nodes.values() if n["type"] == t) for t in LEVELS}
    links = sum(len(LINK.findall(n["summary"] + n["body"])) for n in nodes.values())
    print(f"시대 {counts['era']} · 주제 {counts['topic']} · 키워드 {counts['key']} · 링크 {links}")
    if errors:
        print(f"\n오류 {len(errors)}건:")
        for e in errors:
            print("  " + e)
        sys.exit(1)
    if "--check" in sys.argv:
        return
    out = {
        "version": date.today().isoformat(),
        "subjects": SUBJECTS,
        "nodes": {nid: {k: v for k, v in nodes[nid].items() if not k.startswith("_")} for nid in order},
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(out, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"→ {OUT.relative_to(ROOT)} ({OUT.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()

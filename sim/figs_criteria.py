"""Judging-criteria visuals for the README (plain SVG, stdlib only).

    python3 sim/figs_criteria.py   # writes docs/img/crit_*.svg, badge_*.svg, link_*.svg
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IMG = ROOT / "docs" / "img"

FONT = "Apple SD Gothic Neo, Noto Sans KR, Malgun Gothic, sans-serif"
INK, MUTED, GRID, CARD = "#1F2340", "#6B6E86", "#E3E4EE", "#F8FAFC"

# 24x24 line glyphs (stroke 2, round caps)
ICONS = {
    "car": ['<path d="M5 12.5l1.8-4.6A2 2 0 0 1 8.7 6.6h6.6a2 2 0 0 1 1.9 1.3L19 12.5"/>',
            '<rect x="3" y="12.5" width="18" height="5" rx="1.6"/>',
            '<circle cx="7.5" cy="17.5" r="1.8"/>', '<circle cx="16.5" cy="17.5" r="1.8"/>'],
    "bulb": ['<path d="M12 3a6 6 0 0 0-3.6 10.8c.7.5 1.1 1.3 1.1 2.1V16h5v-.1c0-.8.4-1.6 1.1-2.1A6 6 0 0 0 12 3z"/>',
             '<path d="M9.5 19h5"/>', '<path d="M10.5 21.5h3"/>'],
    "chip": ['<rect x="6.5" y="6.5" width="11" height="11" rx="2"/>', '<rect x="10" y="10" width="4" height="4" rx=".8"/>',
             '<path d="M10 3.5v3M14 3.5v3M10 17.5v3M14 17.5v3M3.5 10h3M3.5 14h3M17.5 10h3M17.5 14h3"/>'],
    "chart": ['<path d="M4 20.5h16"/>', '<path d="M7 20.5v-4M12 20.5v-6.5M17 20.5v-9"/>',
              '<path d="M4.5 11l5-4 3 2.2L19 4"/>', '<path d="M15.5 4H19v3.5"/>'],
    "play": ['<circle cx="12" cy="12" r="9"/>', '<path d="M10 8.5l5.5 3.5-5.5 3.5z"/>'],
    "down": ['<path d="M12 4v11"/>', '<path d="M7 10.5l5 5 5-5"/>', '<path d="M5 20h14"/>'],
    "box": ['<path d="M12 3l8 4.5v9L12 21l-8-4.5v-9z"/>', '<path d="M4 7.5l8 4.5 8-4.5"/>',
            '<path d="M12 12v9"/>', '<path d="M8 5.2l8 4.6"/>'],
}

CRITERIA = [
    dict(key="vehicle", name="차량특화성", c="#1E40AF", t="#DBEAFE", icon="car",
         sub=["모바일 대비 차량만의 가치", "주행 · 정차 맥락 · 차량 기능"],
         answer="차 신호로 계산하고, 차가 직접 공조 · 창문을 바꿈",
         chips=["핸들 각도로 1–2초 먼저", "외기 · 22°C 닫힌 고리", "버튼 하나로 뒷창문 20%"],
         pills=["§4", "§5", "§6"]),
    dict(key="creativity", name="창의성", c="#C2410C", t="#FFEDD5", icon="bulb",
         sub=["기존에 없던 아이디어", "Pain Point · 해결 방식"],
         answer="동승자는 다음 움직임을 모름 → 계산 · 공기 · 창문 · 학습을 한 고리로",
         chips=["4–13세에서 가장 흔함", "같은 조합의 양산 서비스 없음 (조사 범위)", "버튼 = 학습 데이터"],
         pills=["§1", "§2", "§3", "§6"]),
    dict(key="tech", name="기술 활용성", c="#047857", t="#D1FAE5", icon="chip",
         sub=["Pleos Playground SDK · API", "핵심 기능에 활용"],
         answer="Vehicle (HVAC · Window · SteeringWheel · TurnSignal) · NaviHelper · TTS",
         chips=["에뮬레이터 실제 SDK 11항목 점검 · 9 확인", "파이썬 = Kotlin 2,162개 샘플 일치"],
         pills=["§4", "§5"]),
    dict(key="business", name="사업성", c="#6D28D9", t="#EDE9FE", icon="chart",
         sub=["타깃 · 예상 MAU", "Retention · 확대 계획"],
         answer="아이가 있는 가족 운전자 · 출시 1년 MAU 약 1,800명",
         chips=["Retention: 누를수록 내 기준 정확", "무료 + IP 테마 · 음성 팩", "택시 24만 · 통학 4만 대"],
         pills=["§6", "§7"]),
    dict(key="bonus", name="가산점", badge="가산점 개발 산출물", c="#374151", t="#E5E7EB", icon="box",
         sub=["시연 영상", "GitHub 저장소 + APK"],
         answer="에뮬레이터 실제 실행 시연 + 저장소 + 서명된 APK",
         chips=["시연 애니메이션 · 영상 v3 예정", "APK: GitHub Releases", "CI 점검 · 테스트 · 빌드"],
         pills=["맨 위", "§8"]),
]


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def tw(s, size, bold=False):
    """Rough text width (Hangul ~1 em, Latin ~0.56 em) to lay out chips and wrap lines."""
    w = 0.0
    for ch in s:
        o = ord(ch)
        if 0xAC00 <= o <= 0xD7A3 or 0x3130 <= o <= 0x318F:
            w += 0.9
        elif ch == " ":
            w += 0.28
        elif ch in "il.,:;'|()·":
            w += 0.32
        elif ch.isupper() or ch in "mwMW%→–":
            w += 0.68
        else:
            w += 0.55
    return w * size * (1.06 if bold else 1.0)


def wrap(s, size, width, bold=False):
    words, lines, cur = s.split(" "), [], ""
    for wd in words:
        cand = (cur + " " + wd).strip()
        if cur and tw(cand, size, bold) > width:
            lines.append(cur)
            cur = wd
        else:
            cur = cand
    lines.append(cur)
    return lines


class Svg:
    def __init__(self, w, h, rx=16, stroke=GRID):
        self.w, self.h = w, h
        self.parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}" '
                      f'font-family="{FONT}" font-size="14">',
                      f'<rect x="0.75" y="0.75" width="{w - 1.5}" height="{h - 1.5}" rx="{rx}" fill="#FFFFFF" '
                      f'stroke="{stroke}" stroke-width="1.5"/>']

    def add(self, s):
        self.parts.append(s)

    def text(self, x, y, s, size=14, fill=INK, anchor="start", weight=400):
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{fill}" text-anchor="{anchor}" '
                 f'font-weight="{weight}">{esc(s)}</text>')

    def rect(self, x, y, w, h, fill, rx=0, stroke=None, sw=1):
        s = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        self.add(f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{rx}" fill="{fill}"{s}/>')

    def icon(self, name, cx, cy, d, circle, glyph="#FFFFFF", glyph_px=24):
        self.add(f'<circle cx="{cx}" cy="{cy}" r="{d / 2}" fill="{circle}"/>')
        k = glyph_px / 24
        self.add(f'<g transform="translate({cx - glyph_px / 2:.2f},{cy - glyph_px / 2:.2f}) scale({k:.4f})" '
                 f'fill="none" stroke="{glyph}" stroke-width="{2 / k if k < 1 else 2:.2f}" '
                 f'stroke-linecap="round" stroke-linejoin="round">' + "".join(ICONS[name]) + '</g>')

    def save(self, name):
        IMG.mkdir(parents=True, exist_ok=True)
        (IMG / name).write_text("\n".join(self.parts + ["</svg>"]) + "\n", encoding="utf-8")
        print(IMG / name)


# criteria tiles: one SVG per criterion so each can be wrapped in its own link
W = 960
X0, X1 = 2, 958
LEFT_W = 214
MID_X, MID_R = X0 + LEFT_W + 22, 806
PILL_X, PILL_R = 822, 950
CHIP_H, CHIP_FS = 28, 13.5
ANS_FS = 17


def chip_rows(cr):
    rows, cur, x = [], [], 0
    for ch in cr["chips"]:
        w = tw(ch, CHIP_FS) + 28
        if cur and x + w > MID_R - MID_X:
            rows.append(cur)
            cur, x = [], 0
        cur.append((ch, x, w))
        x += w + 8
    rows.append(cur)
    return rows


def tile(cr):
    rows = chip_rows(cr)
    h = 30 + 24 + 12 + len(rows) * CHIP_H + (len(rows) - 1) * 8 + 24
    h = max(h, 120)
    c, t = cr["c"], cr["t"]
    s = Svg(W, h, rx=14, stroke=c)
    s.add(f'<path d="M{X0 + 14} {X0 - 1.25}H{X0 + LEFT_W}V{h - 0.75}H{X0 + 14}'
          f'A13.25 13.25 0 0 1 0.75 {h - 14}V14A13.25 13.25 0 0 1 {X0 + 14} 0.75Z" fill="{t}"/>')
    s.add(f'<line x1="{X0 + LEFT_W}" y1="1" x2="{X0 + LEFT_W}" y2="{h - 1}" stroke="{c}" stroke-opacity=".35"/>')
    cy = h / 2
    s.icon(cr["icon"], 40, cy - 16, 44, c)
    s.text(72, cy - 9, cr["name"], 20, c, weight=700)
    s.text(20, cy + 24, cr["sub"][0], 12.5, MUTED)
    s.text(20, cy + 42, cr["sub"][1], 12.5, MUTED)
    block = 24 + 12 + len(rows) * CHIP_H + (len(rows) - 1) * 8
    by = (h - block) / 2
    s.text(MID_X, by + 18, cr["answer"], ANS_FS, INK, weight=700)
    y0 = by + 24 + 12
    for r, chips in enumerate(rows):
        yy = y0 + r * (CHIP_H + 8)
        for ch, x, w in chips:
            s.rect(MID_X + x, yy, w, CHIP_H, CARD, 14, GRID, 1)
            s.add(f'<circle cx="{MID_X + x + 12}" cy="{yy + CHIP_H / 2}" r="3" fill="{c}"/>')
            s.text(MID_X + x + 20, yy + 19, ch, CHIP_FS, INK)
    s.add(f'<line x1="{PILL_X - 12}" y1="18" x2="{PILL_X - 12}" y2="{h - 18}" stroke="{GRID}"/>')
    pills = [(p, tw(p, 14, True) + 20) for p in cr["pills"]]
    prow, cur, px = [], [], 0
    for p, w in pills:
        if cur and px + w > PILL_R - PILL_X:
            prow.append(cur)
            cur, px = [], 0
        cur.append((p, px, w))
        px += w + 6
    prow.append(cur)
    ph = len(prow) * 28 + (len(prow) - 1) * 6
    py = (h - ph) / 2
    for i, pr in enumerate(prow):
        for p, x, w in pr:
            yy = py + i * 34
            s.rect(PILL_X + x, yy, w, 28, c, 14)
            s.text(PILL_X + x + w / 2, yy + 19, p, 14, "#FFFFFF", "middle", 700)
    s.save(f"crit_{cr['key']}.svg")


def badges():
    for cr in CRITERIA:
        name = cr.get("badge", cr["name"])
        w = round(50 + tw(name, 17, True) + 20)
        s = Svg(w, 44, rx=22, stroke=cr["c"])
        s.icon(cr["icon"], 22, 22, 36, cr["c"], glyph_px=20)
        s.text(48, 28.5, name, 17, cr["c"], weight=700)
        s.save(f"badge_{cr['key']}.svg")


def link_badge(name, label, sub, color, icon):
    w = round(54 + tw(label, 16, True) + (tw(sub, 13) + 16 if sub else 0) + 24)
    s = Svg(w, 44, rx=22, stroke=color)
    s.icon(icon, 22, 22, 36, color, glyph_px=20)
    s.text(48, 28, label, 16, color, weight=700)
    if sub:
        s.text(48 + tw(label, 16, True) + 12, 27.5, sub, 13, MUTED)
    s.save(name)


if __name__ == "__main__":
    for cr in CRITERIA:
        tile(cr)
    badges()
    link_badge("link_video.svg", "시연 영상 v3", "촬영 예정 · 콘티 보기", "#B91C1C", "play")
    link_badge("link_apk.svg", "APK 받기", "GitHub Releases", "#047857", "down")
    link_badge("link_repo.svg", "실행 방법", "에뮬레이터 4단계", "#374151", "box")

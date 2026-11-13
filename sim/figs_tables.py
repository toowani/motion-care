"""README figures that replace the Markdown tables (plain SVG, stdlib only).

    python3 sim/figs_tables.py      # writes docs/img/<name>.svg

Every figure: white rounded canvas (works on GitHub light and dark), shared colour tokens,
line-glyph icons in tinted circles, no emoji. Numbers come from sim/scenarios/scenarios.json
and sim/out/summary.csv where they exist; the rest is copied from README.md / docs/proposal.md.
"""
import csv
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IMG = ROOT / "docs" / "img"

import crave_style as cs

FONT = cs.FONT
SERIF = "Times New Roman, STIX Two Text, Georgia, serif"
INK, MUTED, LINE = cs.INK, cs.MUTED, cs.LINE
CARD = cs.CARD
FOOT = cs.FOOT
BLUE, BAND = cs.BLUE, cs.BAND
TRACK = cs.TITLE_TRACKING

# variable colours (same in every formula / diagram) -- semantic, kept
V_I, V_I_T = "#2563EB", "#DBEAFE"
V_D, V_D_T = "#7C3AED", "#EDE9FE"
V_TAU, V_TAU_T = "#EA580C", "#FFEDD5"
V_M, V_M_T = "#0D9488", "#CCFBF1"
V_P, V_P_T = "#DC2626", "#FEE2E2"
# old slate decoration -> the deck's blue system
SLATE, SLATE_T = BLUE, BAND
CONST = "#6B7280"

# stage scale (the app's ring colours)
LOW, LOW_T = cs.LOW, cs.LOW_T
MID, MID_T = cs.MID, cs.MID_T
HIGH, HIGH_T = cs.HIGH, cs.HIGH_T
MID_INK = cs.MID_INK

# criteria -> one accent blue
C_CAR, C_CAR_T = BLUE, BAND
C_BIZ, C_BIZ_T = BLUE, BAND

GREY, GREY_T = FOOT, cs.CARD_ALT

# condition badge colours (as in conditions.svg)
COND = {1: (V_I, V_I_T), 2: (V_D, V_D_T), 3: (V_M, V_M_T), 4: (V_TAU, V_TAU_T), "핵심": (BLUE, BAND)}

# ---------------------------------------------------------------- icons (24x24, stroke 2)
ICONS = {
    "speaker": "M4 9h4l5-4v14l-5-4H4z M16 9a4 4 0 0 1 0 6 M18.5 6.5a7.5 7.5 0 0 1 0 11",
    "hand": "M9 12.5V5a1.6 1.6 0 0 1 3.2 0v6 M12.2 10.5V9a1.6 1.6 0 0 1 3.2 0v2 M15.4 11a1.6 1.6 0 0 1 3.2 0v3.5a6.5 6.5 0 0 1-6.5 6.5h-.6a6 6 0 0 1-5-2.6l-2.4-3.6a1.6 1.6 0 0 1 2.6-1.8L9 15",
    "tone": "M3 9.5h3L10 6v12l-4-3.5H3z M13 12c1.2-3.5 2.3-3.5 3.5 0s2.3 3.5 3.5 0",
    "wave": "M2 12c2.5-6 5-6 7.5 0s5 6 7.5 0c1.2-3 3-4 5-3",
    "wind": "M3 8h10a3 3 0 1 0-3-3 M3 12h15a3 3 0 1 1-3 3 M3 16h7",
    "child": "M9.5 5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0 M12 8.5v6.5 M7.5 11.5l4.5-1.5 4.5 1.5 M9 21l3-6 3 6",
    "seat": "M7 3h4l1.5 10H18l1 5H8.5z M9 18v3 M17 18v3",
    "face": "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M9 9.5v1 M15 9.5v1 M8.5 14.5c2 2 5 2 7 0",
    "graph": "M4 4v16h16 M7 15l4-5 3 3 5-6",
    "ring": "M5 12a7 7 0 1 0 14 0a7 7 0 1 0-14 0 M12 2v3",
    "gauge": "M4 17a8 8 0 1 1 16 0 M12 17l4-5",
    "pill": "M7 8h10a4 4 0 0 1 0 8H7a4 4 0 0 1 0-8z M12 8v8",
    "road": "M8 3L5 21 M16 3l3 18 M12 4v3 M12 10v4 M12 17v3",
    "route": "M4 18a2 2 0 1 0 4 0a2 2 0 1 0-4 0 M16 6a2 2 0 1 0 4 0a2 2 0 1 0-4 0 M6 16v-5a3 3 0 0 1 3-3h7",
    "phone": "M8 3h8a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z M11 18h2",
    "car": "M5 12l2-5h10l2 5 M3 12h18v5H3z M6 17v2 M18 17v2 M7 14.5h.01 M17 14.5h.01",
    "clock": "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 7v5l3 2",
    "check": "M5 12.5l4.5 4.5L19 7",
    "x": "M6 6l12 12M18 6L6 18",
    "loop": "M4 12a8 8 0 0 1 14-5 M18 3v4h-4 M20 12a8 8 0 0 1-14 5 M6 21v-4h4",
    "window": "M4 19V10l5-6h11v15z M4 13h16 M12 4v9",
    "power": "M12 3v8 M6.3 6.8a8 8 0 1 0 11.4 0",
    "taxi": "M5 13l2-5h10l2 5 M3 13h18v5H3z M6 18v2 M18 18v2 M9.5 4h5v4h-5z",
    "bus": "M5 3h14v14H5z M5 11h14 M7 17v3 M17 17v3 M8 14h.01 M16 14h.01",
    "shuttle": "M4 8a4 4 0 0 1 4-4h8a4 4 0 0 1 4 4v9H4z M4 12h16 M12 4v8 M7 17v3 M17 17v3",
    "dashboard": "M3 5h18v12H3z M8 21h8 M7 14a5 5 0 0 1 10 0 M12 14l2-3",
    "carpair": "M2 10l1.5-4h7L12 10v3H2z M12 18l1.5-4h7L22 18v3H12z M4 13v1.5 M10 13v1.5 M14 21v1 M20 21v1",
    "star": "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z",
    "badge": "M6 9a6 6 0 1 0 12 0a6 6 0 1 0-12 0 M8.5 14L7 21l5-3 5 3-1.5-7",
    "gear": "M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0 M12 2v3 M12 19v3 M2 12h3 M19 12h3 M4.9 4.9L7 7 M17 17l2.1 2.1 M4.9 19.1L7 17 M17 7l2.1-2.1",
    "palette": "M12 3a9 9 0 1 0 0 18c1.5 0 2-1 2-2s-1-1.5-1-2.5 1-1.5 2-1.5h2a4 4 0 0 0 4-4c0-4.5-4-8-9-8z M7.5 11h.01 M10 7h.01 M15 7h.01",
    "map": "M3 6l6-3 6 3 6-3v15l-6 3-6-3-6 3z M9 3v15 M15 6v15",
    "turn": "M6 20v-8a4 4 0 0 1 4-4h8 M14 4l4 4-4 4",
    "family": "M5 6a2 2 0 1 0 4 0a2 2 0 1 0-4 0 M15 6a2 2 0 1 0 4 0a2 2 0 1 0-4 0 M10.5 12a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0 M4 20v-7a3 3 0 0 1 6 0 M14 13a3 3 0 0 1 6 0v7 M10 20v-3a2 2 0 0 1 4 0v3",
    "person": "M9 7a3 3 0 1 0 6 0a3 3 0 1 0-6 0 M5 21v-2a5 5 0 0 1 5-5h4a5 5 0 0 1 5 5v2",
    "thermo": "M10 14V5a2 2 0 0 1 4 0v9a4 4 0 1 1-4 0z M12 10v6",
    "db": "M4 6a8 3 0 1 0 16 0a8 3 0 1 0-16 0 M4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6 M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3",
    "steering": "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0 M3.5 10H9 M15 10h5.5 M12 15v6",
    "bell": "M6 16v-5a6 6 0 0 1 12 0v5l2 2H4z M10 21a2 2 0 0 0 4 0",
    "mic": "M9 5a3 3 0 0 1 6 0v6a3 3 0 0 1-6 0z M5 11a7 7 0 0 0 14 0 M12 18v3",
    "video": "M3 7h12v10H3z M15 10l6-3v10l-6-3",
    "build": "M12 3v12 M7 10l5 5 5-5 M4 20h16",
    "doc": "M6 3h9l4 4v14H6z M14 3v5h5 M9 13h7 M9 17h5",
    "search": "M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0 M16 16l5 5",
    "lock": "M6 11h12v10H6z M8 11V8a4 4 0 0 1 8 0v3",
    "ruler": "M3 17L17 3l4 4L7 21z M7 13l2 2 M10 10l2 2 M13 7l2 2",
    "gift": "M4 11h16v10H4z M3 7h18v4H3z M12 7v14 M12 7c-2-4-6-3-5 0 M12 7c2-4 6-3 5 0",
    "flag": "M5 21V4 M5 4h12l-2 4 2 4H5",
    "signal": "M5 20V14 M10 20V10 M15 20V6 M20 20V3",
    "noentry": "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M5.6 5.6l12.8 12.8",
}

ESC = {"&": "&amp;", "<": "&lt;", ">": "&gt;"}


def esc(s):
    return "".join(ESC.get(c, c) for c in s)


def tw(s, size, bold=False):
    """Rough text width (px) for layout, tuned for Pretendard (wider Latin than Apple SD Gothic Neo)."""
    w = 0.0
    for c in s:
        o = ord(c)
        if o >= 0x1100 and not (0x2000 <= o <= 0x22FF):
            w += 0.92
        elif c == " ":
            w += 0.28
        elif c in "·":
            w += 0.32
        elif c in "→↑":
            w += 0.9
        elif c in "–—":
            w += 0.55
        elif c in "mwMW%":
            w += 0.82
        elif c.isupper():
            w += 0.66
        elif c == "1":
            w += 0.42
        elif c.isdigit():
            w += 0.58
        elif c in "il.,:;'()|ʼ":
            w += 0.3
        else:
            w += 0.54
    return w * size * (1.05 if bold else 1.0)


def wrap(s, size, width, bold=False):
    words, lines, cur = s.split(" "), [], ""
    for wd in words:
        t = (cur + " " + wd).strip()
        if tw(t, size, bold) <= width or not cur:
            cur = t
        else:
            lines.append(cur)
            cur = wd
    if cur:
        lines.append(cur)
    return lines


def arc_path(cx, cy, r, f0, f1):
    """Arc from fraction f0 to f1 of a full turn, starting at 12 o'clock, clockwise."""
    a0, a1 = 2 * math.pi * f0, 2 * math.pi * f1
    x0, y0 = cx + r * math.sin(a0), cy - r * math.cos(a0)
    x1, y1 = cx + r * math.sin(a1), cy - r * math.cos(a1)
    large = 1 if (f1 - f0) > 0.5 else 0
    return f"M{x0:.2f} {y0:.2f}A{r} {r} 0 {large} 1 {x1:.2f} {y1:.2f}"


class Svg:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.defs = []
        self.parts = [f'<rect x="0.75" y="0.75" width="{w - 1.5}" height="{h - 1.5}" rx="{cs.RADIUS}" fill="{cs.BG}" stroke="{LINE}" stroke-width="1.5"/>']

    def add(self, s):
        self.parts.append(s)

    def text(self, x, y, s, size=14, fill=INK, anchor="start", weight=400, italic=False, family=None, track=None):
        st = ' font-style="italic"' if italic else ""
        fam = f' font-family="{family}"' if family else ""
        if track is None and not family and weight >= 700 and size >= 17:
            track = "-0.02em"          # deck headings / big numbers: slightly tight
        ls = f' letter-spacing="{track}"' if track else ""
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{fill}" text-anchor="{anchor}" font-weight="{weight}"{st}{fam}{ls}>{esc(s)}</text>')

    def grad(self, gid="cg"):
        """Brand gradient (deck hero elements); returns the fill url."""
        if not any(f'id="{gid}"' in d for d in self.defs):
            self.defs.append(cs.grad_def(gid))
        return f"url(#{gid})"

    def rich(self, x, y, segs, size=14, anchor="start", fill=INK, weight=400):
        """segs: list of str or (str, colour[, 'var'|'bold'])."""
        out = []
        for sg in segs:
            if isinstance(sg, str):
                out.append(f"<tspan>{esc(sg)}</tspan>")
                continue
            s, col, *kind = sg
            k = kind[0] if kind else ""
            if k == "var":
                out.append(f'<tspan fill="{col}" font-family="{SERIF}" font-style="italic" font-weight="700" font-size="{size * 1.18:.0f}">{esc(s)}</tspan>')
            elif k == "bold":
                out.append(f'<tspan fill="{col}" font-weight="700">{esc(s)}</tspan>')
            else:
                out.append(f'<tspan fill="{col}">{esc(s)}</tspan>')
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{fill}" font-weight="{weight}" text-anchor="{anchor}">{"".join(out)}</text>')

    def rect(self, x, y, w, h, fill, rx=0, stroke=None, sw=1, dash=None):
        s = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.add(f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{rx}" fill="{fill}"{s}{d}/>')

    def card(self, x, y, w, h, fill=CARD, stroke=LINE, rx=cs.RADIUS, sw=1):
        self.rect(x, y, w, h, fill, rx, stroke, sw)

    def line(self, x1, y1, x2, y2, stroke=LINE, width=1, dash=None, cap="round"):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.add(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{stroke}" stroke-width="{width}" stroke-linecap="{cap}"{d}/>')

    def path(self, d, stroke, width=2, fill="none", cap="round"):
        self.add(f'<path d="{d}" fill="{fill}" stroke="{stroke}" stroke-width="{width}" stroke-linecap="{cap}" stroke-linejoin="round"/>')

    def circle(self, cx, cy, r, fill, stroke=None, sw=1):
        s = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        self.add(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r}" fill="{fill}"{s}/>')

    def glyph(self, name, cx, cy, color, size=24):
        k = size / 24
        self.add(f'<g transform="translate({cx - 12 * k:.1f} {cy - 12 * k:.1f}) scale({k:.3f})" fill="none" stroke="{color}" '
                 f'stroke-width="{2 / k * k:.1f}" stroke-linecap="round" stroke-linejoin="round"><path d="{ICONS[name]}"/></g>')

    def icon(self, name, cx, cy, color, tint, r=22):
        self.circle(cx, cy, r, tint)
        self.glyph(name, cx, cy, color, 24 * r / 22)

    def chip(self, x, y, label, fg=MUTED, bg=CARD, stroke=LINE, size=12, h=24, bold=False, dash=None, anchor="start", pad=10):
        w = tw(label, size, bold) + 2 * pad
        if anchor == "end":
            x -= w
        elif anchor == "middle":
            x -= w / 2
        self.rect(x, y, w, h, bg, h / 2, stroke, 1, dash)
        self.text(x + w / 2, y + h / 2 + size * 0.36, label, size, fg, "middle", 700 if bold else 400)
        return w

    def tag_assume(self, x, y, anchor="start"):
        return self.chip(x, y, "가정", MUTED, "#FFFFFF", FOOT, 12, 22, dash="4 3", anchor=anchor, pad=8)

    def tag_pleos(self, x, y, label="Pleos 협의", anchor="start", size=12, h=22):
        return self.chip(x, y, label, BLUE, "#FFFFFF", BLUE, size, h, bold=True, anchor=anchor, pad=9)

    def status(self, kind, cx, cy, r=11):
        if kind == "ok":
            self.circle(cx, cy, r, LOW)
            self.path(f"M{cx - 5} {cy}l3.5 3.5 6.5-7", "#FFFFFF", 2.2)
        elif kind == "part":
            self.circle(cx, cy, r - 1, "#FFFFFF", MID, 2)
            self.path(f"M{cx} {cy - r + 1}A{r - 1} {r - 1} 0 0 1 {cx} {cy + r - 1}z", MID, 0.1, MID)
        else:
            self.circle(cx, cy, r, HIGH)
            self.path(f"M{cx - 4} {cy - 4}l8 8M{cx + 4} {cy - 4}l-8 8", "#FFFFFF", 2.2)

    def arrow(self, x1, y1, x2, y2, color=MUTED, width=1.6, head=6):
        self.line(x1, y1, x2, y2, color, width)
        a = math.atan2(y2 - y1, x2 - x1)
        p1 = (x2 - head * math.cos(a - 0.5), y2 - head * math.sin(a - 0.5))
        p2 = (x2 - head * math.cos(a + 0.5), y2 - head * math.sin(a + 0.5))
        self.path(f"M{p1[0]:.1f} {p1[1]:.1f}L{x2:.1f} {y2:.1f}L{p2[0]:.1f} {p2[1]:.1f}", color, width)

    def title(self, s, x=28, y=46, size=20):
        self.text(x, y, s, size, INK, "start", cs.TITLE_WEIGHT, track=TRACK)

    def save(self, name, title):
        IMG.mkdir(parents=True, exist_ok=True)
        head = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {self.w} {self.h}" width="{self.w}" height="{self.h}" '
                f'font-family="{FONT}" font-size="14" role="img"><title>{esc(title)}</title>')
        defs = f"<defs>{''.join(self.defs)}</defs>" if self.defs else ""
        (IMG / name).write_text("\n".join([head, defs] + self.parts + ["</svg>"]) + "\n", encoding="utf-8")
        print(IMG / name)


def stage_bar(s, x, y, w, h, fracs, labels, colors):
    """Segmented bar; fracs sum to 1; returns boundary xs."""
    xs, cx = [], x
    s.add(f'<clipPath id="cb{int(x)}{int(y)}"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{h / 2}"/></clipPath>')
    s.add(f'<g clip-path="url(#cb{int(x)}{int(y)})">')
    for f, lab, col in zip(fracs, labels, colors):
        ww = w * f
        s.rect(cx, y, ww, h, col)
        s.text(cx + ww / 2, y + h / 2 + 5, lab, 13, INK if col == MID else "#FFFFFF", "middle", 700)
        cx += ww
        xs.append(cx)
    s.add("</g>")
    for bx in xs[:-1]:
        s.line(bx, y, bx, y + h, "#FFFFFF", 2)
    return xs[:-1]


def var_chip(s, x, y, sym, col, tint, label, h=36):
    w = 44 + tw(label, 16, True) + 18
    s.rect(x, y, w, h, tint, h / 2)
    s.text(x + 22, y + h / 2 + 8, sym, 24, col, "middle", 700, italic=True, family=SERIF)
    s.text(x + 44, y + h / 2 + 6, label, 16, INK, "start", 700)
    return w


# ---------------------------------------------------------------- data
def load():
    sc = {d["name"]: d for d in json.loads((ROOT / "sim/scenarios/scenarios.json").read_text())}
    with open(ROOT / "sim/out/summary.csv", newline="") as f:
        sm = {r["scenario"]: r for r in csv.DictReader(f)}
    return sc, sm


# ---------------------------------------------------------------- figures
def evidence():
    rows = [
        ("핵심", "tone", "100 Hz 소리 1분 → 흔들림 뒤 자세 흔들림 · 멀미 설문 점수 감소", "Gu 외, 2025 (나고야대)",
         "흔들리기 직전 양쪽 귀 · 80–85 dBZ · 그네 · 주행 시뮬레이터 · 실제 차 · 주행 중 재생 효과는 미검증"),
        (1, "speaker", "1초 전 음성 예고 → 멀미 점수 감소", "Kuiper 외, 2020", "확장 ① 예측 음성의 근거 · 앱에서는 꺼 둠"),
        (2, "wave", "0.5 Hz 미만 옆 흔들림이 멀미와 가장 잘 맞음", "Turner & Griffin, 1999", None),
        (2, "wave", "누적량 형태", "ISO 2631-1", "수직 흔들림 기준 → 수평에 응용"),
        (3, "wind", "바람 → 멀미 감소", "D'Amour 외, 2017",
         "실험실, 화면이 일으킨 멀미 · 공조 계수 두 값은 출발값, 동승 조사로 보정"),
        (4, "person", "승객 46%가 5년 안에 차멀미를 겪음 → 누구나 쓰는 앱", "Schmidt 외, 2020", None),
        (4, "seat", "뒤쪽 좌석일수록 흔들림 · 멀미 증가", "Turner & Griffin, 1999", "버스 · 코치 조사 → 승용차 뒷좌석은 유추"),
    ]
    rh, gap, y0 = 58, 10, 28
    H = y0 + len(rows) * rh + (len(rows) - 1) * gap + 28
    s = Svg(960, H)
    for i, (n, ic, txt, cite, sub) in enumerate(rows):
        y = y0 + i * (rh + gap)
        col, tint = COND[n]
        s.card(28, y, 904, rh)
        s.circle(64, y + rh / 2, 18, col)
        s.text(64, y + rh / 2 + (4.5 if n == "핵심" else 6), str(n), 11.5 if n == "핵심" else 17, "#FFFFFF", "middle", 700)
        s.icon(ic, 116, y + rh / 2, col, tint, 20)
        if sub:
            s.text(150, y + 25, txt, 15, INK, "start", 700)
            s.text(150, y + 45, sub, 12.5, MUTED)
        else:
            s.text(150, y + rh / 2 + 5.5, txt, 15, INK, "start", 700)
        s.chip(912, y + rh / 2 - 13, cite, MUTED, "#FFFFFF", LINE, 12, 26, anchor="end")
    s.save("evidence.svg", "조건별 근거")


def low_tone():
    """Core technique: the Nagoya 100 Hz study (left) and when the app plays it from car signals (right)."""
    s = Svg(960, 470)
    s.title("핵심 기술: 차 신호로 때를 잡는 저주파 소리", 28, 48, 22)
    # left: the study
    s.card(28, 72, 420, 330)
    s.icon("tone", 66, 110, "#FFFFFF", s.grad(), 22)
    s.text(100, 106, "나고야대 연구", 17, INK, "start", 800)
    s.text(100, 126, "Gu 외, 2025 · Environ Health Prev Med 30:22", 12.5, SUBT)
    conds = ["100 Hz 순음", "1분", "양쪽 귀", "80–85 dBZ"]
    x = 48
    for c in conds:
        x += s.chip(x, 148, c, BLUE, BAND, BAND, 13, 28, bold=True) + 8
    s.text(48, 202, "흔들리기 직전에 들려주면", 15, INK, "start", 700)
    res = [("그네 · 주행 시뮬레이터 · 실제 차", "자세 흔들림 감소"), ("주행 시뮬레이터", "심박 변이(자율신경) 개선"),
           ("실제 차, 책 읽기", "멀미 설문(MSAQ) 점수 감소"), ("생쥐", "효과 120분 이상 유지")]
    for j, (where, what) in enumerate(res):
        yy = 232 + j * 40
        s.status("ok", 58, yy - 5, 9)
        s.text(76, yy, what, 14.5, INK, "start", 700)
        s.text(76, yy + 17, where, 12.5, SUBT)
    # right: when the app plays it
    s.card(468, 72, 464, 330)
    s.text(490, 106, "멀미 케어가 소리를 트는 때", 17, INK, "start", 800)
    s.text(490, 126, "차가 보내는 신호로 판단 · 한 번에 1분, 차 스피커", 12.5, SUBT)
    when = [("power", "출발할 때", "속도 3 km/h를 처음 넘는 순간 · 연구와 같은 ‘흔들리기 전’"),
            ("wave", "흔들림이 쌓일 때", "흔들림 지수 I가 주의 기준을 10초 넘게 넘으면 · 외기 · 음성 안내와 함께 · 5분 간격"),
            ("hand", "멀미 완화 버튼", "창문 환기 · 음성 안내와 함께")]
    for j, (ic, head, sub) in enumerate(when):
        yy = 150 + j * 80
        s.icon(ic, 510, yy + 16, BLUE, BAND, 20)
        s.text(544, yy + 12, head, 15.5, INK, "start", 700)
        for k, ln in enumerate(wrap(sub, 12.5, 370)):
            s.text(544, yy + 32 + k * 17, ln, 12.5, SUBT)
    # limits
    s.glyph("info", 46, 432, SUBT, 20)
    s.text(70, 437, "주행 중 재생 효과는 연구가 확인하지 않음 · 귀 위치 소리 크기는 차종별 보정 · 특허: 미 · 유럽 · 일 · 중 등록, 한국 출원 없음", 13.5, SUBT)
    s.save("low_tone.svg", "핵심 기술: 차 신호로 때를 잡는 저주파 소리")


def two_values():
    s = Svg(960, 536)
    s.title("두 가지 값")
    cards = [(28, V_I, V_I_T, "I", "흔들림 지수"), (490, V_P, V_P_T, "P", "멀미 가능성")]
    for i, (x, col, tint, sym, name) in enumerate(cards):
        y, w = 64, 442
        s.card(x, y, w, 334)
        var_chip(s, x + 20, y + 20, sym, col, tint, name)
        if i == 0:
            s.text(x + 22, y + 88, "지금 얼마나 흔들리나 (최근 60초)", 14, INK)
            xs = stage_bar(s, x + 22, y + 108, w - 44, 32, [0.45, 0.3, 0.25], ["잔잔", "흔들림", "흔들림 많음"], [LOW, MID, HIGH])
            for bx, v in zip(xs, ["0.9", "1.5"]):
                s.line(bx, y + 142, bx, y + 150, MUTED, 1.2)
                s.rich(bx, y + 166, [v + "·", ("m", V_M, "var")], 13, "middle", MUTED)
            s.text(x + 22, y + 166, "10초 넘게 이어지면 전환", 12, MUTED)
            uses = [("face", "캐릭터 표정 · 둘레 링 색"), ("face", "한 화면 모드 지금 흔들림"), ("graph", "분석 탭 그래프")]
        else:
            s.rich(x + 22, y + 88, ["누적 ", ("D", V_D, "var"), "가 기준선 ", ("τ", V_TAU, "var"), "·", ("m", V_M, "var"), "을 넘을수록 커짐"], 14)
            xs = stage_bar(s, x + 22, y + 108, w - 44, 32, [0.25, 0.25, 0.5], ["낮음", "보통", "높음"], [LOW, MID, HIGH])
            for bx, v in zip(xs, ["25%", "50%"]):
                s.line(bx, y + 142, bx, y + 150, MUTED, 1.2)
                s.text(bx, y + 166, v, 13, MUTED, "middle")
            uses = [("ring", "캐릭터 둘레 링 (차오르는 양)"), ("gauge", "분석 탭 게이지 (% · 색)"), ("pill", "한 화면 % 알약")]
        s.text(x + 22, y + 200, "쓰는 곳", 12, MUTED, "start", 700)
        for k, (ic, lab) in enumerate(uses):
            yy = y + 214 + k * 38
            s.rect(x + 22, yy, w - 44, 32, "#FFFFFF", 16, LINE)
            s.icon(ic, x + 40, yy + 16, col, tint, 13)
            s.text(x + 62, yy + 21, lab, 14, INK)
    # the same three colours, but two measures: face / ring colour follow I, the gauge follows P
    y = 414
    s.card(28, y, 904, 94)
    s.text(50, y + 38, "같은 세 가지 색", 15, INK, "start", 700)
    s.rich(50, y + 74, ["기준은 둘: 캐릭터 표정 · 링 색 = 흔들림 지수 ", ("I", V_I, "var"), " 단계, 분석 게이지 색 = 멀미 가능성 ",
                        ("P", V_P, "var"), " 단계 (링 길이는 ", ("P", V_P, "var"), ")"], 13.5, fill=MUTED)
    cx = 290
    for col, cname, lab in [(LOW, "초록", "낮음"), (MID, "노랑", "보통"), (HIGH, "빨강", "높음")]:
        s.rect(cx, y + 20, 24, 24, col, 7)
        s.text(cx + 34, y + 37, cname, 14, INK, "start", 700)
        s.text(cx + 34 + tw(cname, 14, True) + 6, y + 37, "= " + lab, 14, MUTED)
        cx += 190
    s.save("two_values.svg", "두 가지 값: 흔들림 지수 I와 멀미 가능성 P")


def scenario_cards():
    sc, sm = load()
    km = lambda n: f"{sc[n]['distance_m'] / 1000:.1f} km"
    mins = lambda *ns: "–".join(sorted({sm[n]["minutes"] for n in ns}, key=float)) + "분"
    temp = lambda n: f"출발 {sc[n]['cabin']['temp']:.0f}°C · 내기순환"
    cards = [
        ("도심", ["강남역", "테헤란로", "언주로", "봉은사로", "선릉로"],
         [(str(sc["A_city_rough"]["turns"]), "회전"), (str(sc["A_city_rough"]["stops"]), "신호 정지")],
         [km("A_city_rough"), mins("A_city_smooth", "A_city_rough")], None),
        ("굽은 길", ["북악스카이웨이 (북악산로)"], [("52", "반경 100 m 미만 커브")],
         [km("B_winding_rough"), mins("B_winding_smooth", "B_winding_rough")], temp("B_winding_rough")),
        ("고속도로", ["경부고속도로 판교", "수원신갈IC"], [(f"{sc['C_highway']['distance_m'] / 1000:.1f}", "km")],
         [mins("C_highway")], None),
        ("나들이길", ["동탄", "경부 · 수도권제1순환", "남한산성로"], [(sm["D_family"]["minutes"], "분 긴 주행")],
         [km("D_family")], temp("D_family")),
    ]
    s = Svg(960, 552)
    cw, ch = 444, 200
    for i, (name, route, call, chips, cabin) in enumerate(cards):
        x, y = 28 + (i % 2) * (cw + 16), 28 + (i // 2) * (ch + 16)
        s.card(x, y, cw, ch)
        s.icon("road", x + 40, y + 38, SLATE, SLATE_T, 20)
        s.text(x + 72, y + 44, name, 17, INK, "start", 700)
        # route chips with arrows
        cx, cy = x + 20, y + 70
        for k, r in enumerate(route):
            w = tw(r, 12) + 20
            if cx + w > x + cw - 20:
                cx, cy = x + 20, cy + 30
            s.chip(cx, cy, r, INK, "#FFFFFF", LINE, 12, 24)
            cx += w
            if k < len(route) - 1:
                s.arrow(cx + 3, cy + 12, cx + 15, cy + 12, MUTED, 1.4, 5)
                cx += 18
        # callout
        cx = x + 20
        for num, lab in call:
            s.text(cx, y + 146, num, 28, BLUE, "start", 800)
            cx += tw(num, 28, True) + 6
            s.text(cx, y + 145, lab, 14, MUTED)
            cx += tw(lab, 14) + 22
        cx = x + 20
        for c in chips:
            cx += s.chip(cx, y + 164, c, INK, "#FFFFFF", LINE, 12, 24, bold=True) + 8
        if cabin:
            s.chip(cx, y + 164, cabin, SLATE, SLATE_T, SLATE_T, 12, 24)
    y = 28 + 2 * (ch + 16)
    s.card(28, y, 904, 64)
    s.icon("route", 60, y + 32, SLATE, SLATE_T, 18)
    s.arrow(86, y + 32, 106, y + 32)
    w = s.chip(114, y + 19, "1초 간격 가상 센서 값", INK, "#FFFFFF", LINE, 13, 26, bold=True)
    cx = 114 + w + 12
    s.arrow(cx, y + 32, cx + 20, y + 32)
    cx += 30
    for lab in ["속도", "진행 방향", "실내 온도"]:
        cx += s.chip(cx, y + 19, lab, SLATE, SLATE_T, SLATE_T, 13, 26) + 8
    cx += s.chip(cx + 4, y + 20, "차 신호", SLATE, "#FFFFFF", SLATE, 12, 24, dash="3 2") + 16
    s.text(cx, y + 30, "핸들 각도는 시나리오에 없음", 12, MUTED, "start", 700)
    s.text(cx, y + 47, "(에뮬레이터에서만 확인)", 12, MUTED)
    s.save("scenario_cards.svg", "실제 도로 4곳 · 주행 6회")


def emulator_checks():
    tiles = [
        ("ok", "db", "완화 버튼 학습", "D 32.3 · τ 30.6 기록"),
        ("ok", "loop", "공조 닫힌 고리", "26.0° → 22.0° · 외기"),
        ("ok", "power", "공조 꺼짐", "그대로 · 창문 안내"),
        ("ok", "steering", "핸들 각도", "540° 수신 · CAR_DRIVE"),
        ("ok", "mic", "Pleos 음성", "TTS · STT 서버 연결"),
        ("ok", "turn", "내비 다음 회전", "왼쪽 80 m 수신"),
        ("ok", "map", "내비 앞 화면", "공조 · 안내 그대로"),
        ("ok", "pill", "한 화면 모드", "표정 카드 + % 알약"),
        ("ok", "ring", "지도 탭", "링 초록 → 빨강 · 줌 17"),
        ("part", "window", "창문 명령 수락", "뒷창 위치 2 · 움직임 미확인"),
    ]
    cols, gap, th = 5, 12, 104
    tw_ = (904 - (cols - 1) * gap) / cols
    H = 28 + 2 * th + gap + 76
    s = Svg(960, H)
    for i, (kind, ic, label, detail) in enumerate(tiles):
        x, y = 28 + (i % cols) * (tw_ + gap), 28 + (i // cols) * (th + gap)
        s.card(x, y, tw_, th, CARD, LINE, cs.RADIUS, 1)
        s.icon(ic, x + 34, y + 36, SLATE, SLATE_T, 20)
        s.status(kind, x + tw_ - 26, y + 26, 12)
        s.text(x + 18, y + 72, label, 15 if tw(label, 15, True) < tw_ - 30 else 13.5, INK, "start", 700)
        s.text(x + 18, y + 90, detail, 12.5 if tw(detail, 12.5) < tw_ - 26 else 11.5, MUTED)
    # summary under the tiles
    y = 28 + 2 * th + gap + 18
    s.text(28, y + 20, "Pleos Connect Emulator v2.1 · 10가지 점검", 14, MUTED, "start", 700)
    x = 28 + tw("Pleos Connect Emulator v2.1 · 10가지 점검", 14, True) + 14
    x += s.chip(x, y, "9가지 확인", LOW, LOW_T, LOW_T, 14, 30, bold=True, pad=12) + 8
    s.chip(x, y, "창문은 명령 수락까지 · 실제 움직임은 확인 필요", MID_INK, MID_T, MID_T, 13, 30, bold=True, pad=12)
    s.save("emulator_checks.svg", "에뮬레이터 점검: 10가지 중 9가지 확인, 창문은 명령 수락까지")


def theme_names():
    themes = [("깔끔한 기본", "신호등형 표정", None), ("말랑 3D 친구", "몽글이", None),
              ("애니메이션 친구", "부릉이", None), ("가상 아이돌 콜라보", "루미", "예시 · IP 계약 없음")]
    s = Svg(960, 168)
    cw = (904 - 3 * 12) / 4
    for i, (name, ch, tag) in enumerate(themes):
        x = 28 + i * (cw + 12)
        s.card(x, 24, cw, 66)
        s.text(x + 18, 52, name, 15, INK, "start", 700)
        s.text(x + 18, 74, ch, 13, MUTED)
        if tag:
            s.chip(x + cw - 12, 60, tag, MUTED, "#FFFFFF", MUTED, 11.5, 22, dash="4 3", anchor="end", pad=7)
    cx = 28
    for ic, txt in [("gear", "첫 실행 때 고름 · 주행 중 변경 잠금"), ("palette", "그림 · 말투만 다름"), ("ring", "계산 · 안내는 같음")]:
        w = tw(txt, 13) + 52
        s.rect(cx, 104, w, 36, "#FFFFFF", 18, LINE)
        s.glyph(ic, cx + 20, 122, BLUE, 18)
        s.text(cx + 36, 127, txt, 13, INK)
        cx += w + 10
    s.save("theme_names.svg", "아이콘 테마 4종")


def map_elements():
    s = Svg(960, 446)
    s.title("지도 탭")
    # segmented tab
    x0 = 28 + tw("지도 탭", 20, True) + 18
    s.rect(x0, 26, 210, 30, cs.CARD_ALT, 15, LINE)
    s.rect(x0 + 3, 29, 116, 24, s.grad(), 12)
    s.text(x0 + 61, 46, "지도 (기본)", 13, "#FFFFFF", "middle", 700)
    s.text(x0 + 164, 46, "분석", 13, MUTED, "middle")
    rows = [
        ("map", "지도", ["진행 방향이 위 · 한글 도로 이름 · 줌 고정"]),
        ("route", "경로", ["앞으로 갈 길만 파란 선 (지나온 길 안 그림)"]),
        ("turn", "회전 안내 카드 · 도착 바", ["시연: 경로 파일", "실차: NaviHelper (TBT, DrivingInfo)"]),
        ("face", "캐릭터", ["오른쪽 위, 둘레 링 = 멀미 가능성 (숫자 없음)"]),
    ]
    y = 76
    for i, (ic, ttl, lines) in enumerate(rows):
        h = 72
        s.card(28, y, 540, h)
        s.circle(52, y + h / 2, 12, INK)
        s.text(52, y + h / 2 + 5, str(i + 1), 13, "#FFFFFF", "middle", 700)
        s.icon(ic, 96, y + h / 2, BLUE if ic in ("map", "route", "turn") else V_P, BAND if ic in ("map", "route", "turn") else V_P_T, 20)
        s.text(130, y + 30, ttl, 15, INK, "start", 700)
        if ic == "route":
            s.line(130 + tw(ttl, 15, True) + 14, y + 25, 130 + tw(ttl, 15, True) + 74, y + 25, BLUE, 5)
        if ic == "face":
            s.rich(130, y + 52, ["오른쪽 위, 링 길이 = ", ("멀미 가능성", V_P, "bold"), " · 링 색 = 표정 단계 (숫자 없음)"], 13, fill=MUTED)
        elif len(lines) == 2:
            s.chip(130, y + 38, lines[0], INK, "#FFFFFF", LINE, 12, 24)
            s.chip(136 + tw(lines[0], 12) + 20, y + 38, lines[1], SLATE, SLATE_T, SLATE_T, 12, 24)
        else:
            s.text(130, y + 52, lines[0], 13, MUTED)
        y += h + 8
    # ring diagram
    cx, cy, r = 760, 226, 100
    s.card(588, 76, 344, 312, "#FFFFFF")
    s.add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="{LINE}" stroke-width="20"/>')
    # one colour: the ring is as long as P, its colour is the face's stage (shaking index I)
    s.path(arc_path(cx, cy, r, 0.003, 0.72), MID, 20, cap="butt")
    s.circle(cx, cy, 58, CARD, LINE)
    s.glyph("face", cx, cy - 6, INK, 44)
    s.rich(cx, cy + 40, [("P", V_P, "var"), " 만큼 차오름"], 12, "middle", MUTED)
    # no 25 % / 50 % marks on the ring: those stages colour the analysis gauge, not the ring
    s.line(cx, cy - r - 14, cx, cy - r + 12, INK, 1.5)
    # clockwise arrow
    s.path(arc_path(cx, cy, r + 22, 0.03, 0.12), MUTED, 1.6)
    a = 2 * math.pi * 0.12
    ex, ey = cx + (r + 22) * math.sin(a), cy - (r + 22) * math.cos(a)
    s.path(f"M{ex - 8:.1f} {ey - 4:.1f}L{ex:.1f} {ey:.1f}L{ex - 3:.1f} {ey - 9:.1f}", MUTED, 1.6)
    s.text(cx + 34, cy - r - 30, "12시부터 시계 방향", 12, MUTED)
    s.rich(cx, cy + r + 40, ["색 = 캐릭터 표정 단계 (흔들림 지수 ", ("I", V_I, "var"), ")"], 12, "middle", MUTED)
    s.text(28, 418, "지도: OpenFreeMap 벡터 지도 · 에뮬레이터는 WebGL이 없어 2D", 12, MUTED)
    s.save("map_elements.svg", "지도 탭 요소와 둘레 링")


def phone_vs_car():
    rows = [
        ("저주파 소리", ("person", "직접 찾아 틀어야 함"), ("tone", "차 신호로 때를 잡아 차 스피커로 1분")),
        ("흔들림을 먼저 계산", ("clock", "폰 센서로 뒤늦게"), ("steering", "핸들 각도로 1–2초 먼저")),
        ("실내 온도 · 환기", ("x", "불가"), ("loop", "읽고 바꾸고, 다시 계산")),
        ("창문 열기", ("x", "불가"), ("window", "버튼 하나로 뒷창문 조금")),
        ("매 주행 실행", ("person", "사람이 켜야 함"), ("power", "앱을 한 번 열면 화면을 닫아도 계속", "시동 자동 시작은 Pleos 협의 후")),
    ]
    s = Svg(960, 510)
    lx, lw, mx, mw, rx_, rw = 28, 318, 362, 236, 614, 318
    s.rect(lx, 28, lw, 56, GREY_T, cs.RADIUS)
    s.icon("phone", lx + 40, 56, GREY, "#FFFFFF", 18)
    s.text(lx + 68, 62, "폰 앱", 17, MUTED, "start", 700)
    s.rect(rx_, 28, rw, 56, s.grad(), cs.RADIUS)
    s.icon("car", rx_ + 40, 56, BLUE, "#FFFFFF", 18)
    s.text(rx_ + 68, 62, "멀미 케어", 17, "#FFFFFF", "start", 800)
    s.text(mx + mw / 2, 62, "vs", 15, MUTED, "middle", 700)
    y = 100
    for feat, (pi, pt), (ci, ct, *csub) in rows:
        h = 70
        s.card(lx, y, lw, h, "#FFFFFF")
        s.icon(pi, lx + 36, y + h / 2, GREY, GREY_T, 16)
        s.text(lx + 62, y + h / 2 + 5, pt, 15, MUTED)
        s.rect(mx, y + 16, mw, h - 32, CARD, (h - 32) / 2, LINE)
        s.text(mx + mw / 2, y + h / 2 + 5, feat, 14, INK, "middle", 700)
        s.card(rx_, y, rw, h, "#FFFFFF", "#C9D8F6", cs.RADIUS, 1)
        s.icon(ci, rx_ + 36, y + h / 2, C_CAR, C_CAR_T, 16)
        if csub:
            s.text(rx_ + 62, y + h / 2 - 3, ct, 14, INK, "start", 700)
            s.text(rx_ + 62, y + h / 2 + 16, csub[0], 12.5, MUTED)
        else:
            s.text(rx_ + 62, y + h / 2 + 5, ct, 15, INK, "start", 700)
        s.circle(rx_ + rw - 24, y + h / 2, 10, LOW)
        s.path(f"M{rx_ + rw - 29} {y + h / 2}l3.5 3.5 6-6.5", "#FFFFFF", 2.2)
        y += h + 10
    s.save("phone_vs_car.svg", "폰 앱과 멀미 케어 비교")


def market_compare():
    # Y = yes, N = no, H = partly / research.  Source: research/market.md
    # 멀미 Zero: 공개 자료(HMG 스토리 · 뉴데일리 · EBN, 2026.10) 기준. N = 보도에 없음.
    feats = ["저주파 소리", "차 신호", "미리 계산", "공기 · 바람", "장비 없음", "사람별 기준"]
    lead = {0: "핵심 기술", 3: "조치", 5: "개인 학습"}   # our lead differentiator columns
    rows = [
        ("Apple Motion Cues", "iOS 18, 2024", "NNNNYN", "양산"),
        ("Google Motion Assist · KineStop", "2026 · 2018", "NNNNYN", "양산"),
        ("Citroën Seetroën 안경", "2018", "NNNNNN", "양산"),
        ("현대차 스무스 모드", "아이오닉 6, 2025", "NYNNYN", "양산"),
        ("현대모비스 멀미 저감", "2023", "NYNYNH", "시범"),
        ("JLR 웰니스 점수", "2018 · 2020", "NYNNNY", "연구"),
        ("VW · Bentley 연구", "Diels 외, 2023", "NYHNYN", "연구"),
        ("holoride", "2022", "NYNNNN", "파산"),
        ("Mercedes · BMW", "바람 · 천장 화면", "NYNHYN", "특허"),
        ("DENSO · 나고야대", "100 Hz, 2025", "YNNNNN", "연구"),
        ("멀미 Zero", "2026 사내 공모 최우수상", "NYYNYN", "사내 수상"),
        ("멀미 케어", "Pleos Connect", "YYYYYY", "시제품"),
    ]
    name_w, x_f, fw, x_s = 300, 352, 86, 898
    rh, y0 = 46, 92
    t_end = y0 + len(rows) * rh
    H = t_end + 44 + 148
    s = Svg(960, H)
    for i, (name, *_r) in enumerate(rows):
        y = y0 + i * rh
        if name == "멀미 케어":
            s.card(28, y - 4, 904, rh - 2, BAND, "#BCD0F7", 12, 1)
        elif name == "멀미 Zero":
            s.card(28, y - 4, 904, rh - 2, "#FFFBEB", "#F5D96B", 12, 1)
        elif i % 2 == 0:
            s.rect(28, y - 4, 904, rh - 2, CARD, 10)
    for j, lab in lead.items():
        bx = x_f + j * fw + 6
        s.add(f'<rect x="{bx:.1f}" y="30" width="{fw - 12}" height="{t_end - 30 - 4}" rx="10" fill="{BLUE}" fill-opacity="0.06"/>')
        s.text(bx + (fw - 12) / 2, 46, lab, 11.5, C_CAR, "middle", 700)
    for j, f in enumerate(feats):
        s.text(x_f + j * fw + fw / 2, 64, f, 13.5, INK, "middle", 700)
    s.text(x_s, 64, "상태", 13.5, INK, "middle", 700)
    s.line(28, 78, 932, 78, LINE)
    for i, (name, when, marks, state) in enumerate(rows):
        y = y0 + i * rh
        ours = name == "멀미 케어"
        s.text(40, y + 20, name, 14.5, C_CAR if ours else INK, "start", 700)
        s.text(40 + tw(name, 14.5, True) + 8, y + 20, when, 12, MUTED)
        for j, mk in enumerate(marks):
            cx, cy = x_f + j * fw + fw / 2, y + rh / 2 - 4
            if mk == "Y":
                s.circle(cx, cy, 11, C_CAR if ours else LOW)
                s.path(f"M{cx - 5} {cy}l3.5 3.5 6.5-7", "#FFFFFF", 2.2)
                if ours and j in (2, 5):      # qualified ticks, explained in the footnote
                    s.text(cx + 15, cy - 3, "¹" if j == 2 else "²", 14, C_CAR, "start", 700)
            elif mk == "H":
                s.circle(cx, cy, 10, "#FFFFFF", MID, 2)
                s.path(f"M{cx} {cy - 9}A9 9 0 0 1 {cx} {cy + 9}z", MID, 0.1, MID)
            else:
                s.circle(cx, cy, 4, "#CDD3E0")
        col = {"양산": (LOW, LOW_T, LOW_T), "시제품": ("#FFFFFF", s.grad(), None),
               "사내 수상": (MID_INK, "#FFFFFF", MID)}.get(state, (MUTED, GREY_T, GREY_T))
        s.chip(x_s, y + rh / 2 - 16, state, col[0], col[1], col[2], 12, 24, bold=True, anchor="middle", pad=8)
    # legend
    y = t_end + 18
    cx = 46
    s.circle(cx, y, 8, LOW); s.path(f"M{cx - 4} {y}l2.5 2.5 5-5", "#FFFFFF", 2)
    s.text(cx + 14, y + 5, "있음", 12.5, INK)
    cx += 64
    s.circle(cx, y, 7, "#FFFFFF", MID, 2); s.path(f"M{cx} {y - 6}A6 6 0 0 1 {cx} {y + 6}z", MID, 0.1, MID)
    s.text(cx + 14, y + 5, "일부 · 연구 단계", 12.5, INK)
    cx += 130
    s.circle(cx, y, 4, "#CDD3E0")
    s.text(cx + 12, y + 5, "없음 · 확인 못 함 (멀미 Zero는 공개 자료 기준 보도에 없음)", 12.5, INK)
    # 멀미 Zero note: credit, overlap, difference (public reports only)
    y += 34
    s.line(28, y - 16, 932, y - 16, LINE)
    s.rich(46, y, [("멀미 Zero", INK, "bold"),
                   " · 현대차·기아 2026 아이디어 페스티벌 최우수상 (임직원 팀, 2026-10-19 공개) · OTA로 넣는 차량 기능"], 12.5, fill=MUTED)
    s.rich(46, y + 22, [("겹침", MID_INK, "bold"),
                        "  차 신호(조향각 · 가속도), 움직임을 조명 · 뒷좌석 화면 테두리로 미리 보여 줌, 하드웨어 추가 없음"], 12.5, fill=MUTED)
    s.rich(46, y + 44, [("다름", C_CAR, "bold"),
                        "  멀미 케어는 차 신호로 때를 잡아 저주파 소리를 틀고 공조 · 창문을 실제로 조작하는 앱(조치), 버튼 기록 기반 기준(개인 학습),"], 12.5, fill=MUTED)
    s.rich(46 + tw("다름", 12.5, True) + 6, y + 66, ["Pleos 앱 마켓으로 어느 Pleos 차에나 배포 · 출처: 현대자동차그룹 스토리 · 뉴데일리 · EBN (2026.10)"], 12.5, fill=MUTED)
    s.rich(46, y + 88, [("¹", C_CAR, "bold"), " 미리 계산: 핸들 각도로 1–2초 앞당김 (화면 · 음성 회전 예고는 확장)   ",
                        ("²", C_CAR, "bold"), " 사람별 기준: 멀미 가능성 표시에 반영 (알림 시점은 같음)"], 12.5, fill=MUTED)
    s.rich(46, y + 110, [("DENSO · 나고야대", INK, "bold"),
                         "  100 Hz 소리 1분 연구(Gu 외, 2025) · 특허 미국 · 유럽 · 일본 · 중국 등록 · 차량 적용은 보도만, 공식 발표 확인 못 함"], 12.5, fill=MUTED)
    s.save("market_compare.svg", "다른 멀미 대응 서비스와 기능 비교")


SUBT = MUTED   # secondary text
ICONS.update({
    "hush": "M4 4.5h16v11H11l-4.5 4v-4H4z M9.5 7.5l5 5 M14.5 7.5l-5 5",
    "sprout": "M12 21v-9 M12 12c0-4 3-7 8-7 0 5-3 7-8 7z M12 14c0-3-2.5-5-7-5 0 4 2.5 5 7 5z",
    "info": "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 11v6 M12 7.5h.01",
})


def target():
    s = Svg(960, 332)
    s.icon("family", 62, 62, "#FFFFFF", s.grad(), 30)
    s.text(110, 72, "멀미하는 동승자를 태우는 운전자", 28, INK, "start", 800)
    why = [("person", ["승객 46%가", "차멀미를 겪음"], "5개국 4,479명 (Schmidt 외, 2020)"),
           ("hush", ["동승자는 다음", "움직임을 모름"], "운전자보다 동승자가 더 멀미함"),
           ("seat", ["뒷좌석일수록", "더 흔들림"], "버스 · 코치 조사 (Turner & Griffin, 1999)")]
    cw = (904 - 2 * 16) / 3
    for i, (ic, rows, note) in enumerate(why):
        x = 28 + i * (cw + 16)
        s.card(x, 116, cw, 112, CARD, LINE)
        s.icon(ic, x + 42, 156, C_BIZ, C_BIZ_T, 24)
        for j, r in enumerate(rows):
            s.text(x + 80, 150 + j * 24, r, 18, INK, "start", 700)
        s.text(x + 22, 208, note, 13.5, SUBT)
    s.glyph("turn", 46, 260, C_BIZ, 22)
    s.text(70, 266, "같은 엔진으로", 15, C_BIZ, "start", 700)
    s.text(70 + tw("같은 엔진으로", 15, True) + 14, 266, "택시 · 통학 차량 · 자율주행 승객", 18, INK, "start", 700)
    s.glyph("info", 46, 300, SUBT, 20)
    s.text(70, 305, "멀미 완화 버튼은 본인이나 동승자가 누름 → 누른 순간이 그 차의 멀미 기준으로 쌓임 (지금은 차 한 대에 기준 하나)", 14, SUBT)
    s.save("target.svg", "타깃: 멀미하는 동승자를 태우는 운전자")


def mau_funnel():
    cars = 71775 + 79335
    steps = [
        ("Pleos 탑재 차종 2025년 판매", "그랜저 + 아반떼 · 당시 Pleos 미탑재, 근사치", None, None, 1.0),
        ("× 멀미하는 동승자를 태우는 차", "승객 차멀미 경험 46%(Schmidt 외, 2020)로 대신함", "46%", "가정", 0.46),
        ("× 설치율", None, "10%", "가정", 0.10),
        ("× 월 사용", "멀미하는 사람을 태운 달", "60%", "가정", 0.60),
    ]
    vals, v = [], cars
    for *_, f in steps:
        v *= f
        vals.append(v)
    mau = vals[-1]
    assert 4100 < mau < 4250, mau
    s = Svg(960, 428)
    s.title("출시 1년 예상 MAU", 28, 50)
    y = 76
    for (lab, sub, rate, tag, _), n in zip(steps, vals):
        s.line(28, y, 620, y, LINE)
        s.text(28, y + 30, lab, 16, INK, "start", 700)
        if sub:
            s.text(28, y + 52, sub, 13.5, SUBT)
        if rate:
            s.text(410, y + 36, rate, 20, BLUE, "end", 800)
        if tag:
            s.tag_assume(420, y + 20)
        s.text(620, y + 36, f"{n:,.0f}" + ("대" if n == cars else ""), 20, INK, "end", 700)
        y += 66
    s.rect(28, y + 6, 592, 56, s.grad(), cs.RADIUS)
    s.text(48, y + 41, "출시 1년 예상 MAU", 17, "#FFFFFF", "start", 700)
    s.text(600, y + 43, "≈ 4,200명", 24, "#FFFFFF", "end", 800)
    rx_, rw = 652, 280
    s.card(rx_, 76, rw, 324)
    s.glyph("info", rx_ + 30, 108, SUBT, 22)
    s.text(rx_ + 50, 114, "읽는 법", 16, INK, "start", 700)
    notes = [("4,200명", "탑재 차종의 2025년 판매(당시 Pleos 미탑재)를 1년 탑재량으로 본 근사치"),
             ("1,400 ~ 9,700명", "설치 5% × 월 사용 40% (보수) ~ 20% × 70% (낙관)"),
             ("차량 2천만 대", "Pleos Connect 2030 적용 목표 (현대차그룹, 2026.4.30) · 차종이 늘수록 커짐")]
    yy = 150
    for big, why in notes:
        s.text(rx_ + 22, yy, big, 18, BLUE, "start", 800)
        for j, ln in enumerate(wrap(why, 13.5, rw - 44)):
            s.text(rx_ + 22, yy + 22 + j * 19, ln, 13.5, SUBT)
        yy += 22 + 19 * len(wrap(why, 13.5, rw - 44)) + 22
    s.save("mau_funnel.svg", "예상 MAU 계산")


def retention_revenue():
    s = Svg(960, 384)
    s.card(28, 28, 442, 328)
    s.text(48, 62, "다시 쓰게 하는 고리 (Retention)", 17, INK, "start", 700)
    pw = 196
    nodes = [(249, 108, "멀미 완화 버튼 누름"), (362, 214, "누른 순간 흔들림 기록"), (136, 214, "내 기준선 갱신")]
    for x, y, lab in nodes:
        s.rect(x - pw / 2, y - 22, pw, 44, BAND, 22)
        s.text(x, y + 5.5, lab, 15, C_BIZ, "middle", 700)
    s.path("M318 132 Q350 160 356 188", C_BIZ, 2)
    s.path("M349 183 L356 191 L361 182", C_BIZ, 2)
    s.path("M262 214 L240 214", C_BIZ, 2)
    s.path("M247 208 L239 214 L247 220", C_BIZ, 2)
    s.path("M124 190 Q132 150 168 132", C_BIZ, 2)
    s.path("M160 130 L169 131 L166 140", C_BIZ, 2)
    s.text(249, 172, "누를수록 기록이 쌓임", 14, SUBT, "middle", 700)
    s.glyph("power", 58, 280, C_BIZ, 22)
    s.text(80, 286, "주행마다 한 번 열면 화면을 닫아도 계속", 15, INK, "start", 700)
    s.text(80, 310, "시동 자동 시작은 Pleos 협의 후", 13.5, SUBT)
    s.text(80, 332, "→ MAU 식의 월 사용 60%(가정) 근거", 13.5, SUBT)
    x0 = 490
    s.card(x0, 28, 442, 328)
    s.text(x0 + 20, 62, "수익 모델 · 엔터테인먼트 연계", 17, INK, "start", 700)
    s.text(x0 + 20, 100, "판매", 14.5, C_BIZ, "start", 700)
    s.rect(x0 + 20, 110, 270, 60, BAND, cs.RADIUS_SM + 2)
    s.icon("palette", x0 + 48, 140, C_BIZ, "#FFFFFF", 17)
    s.text(x0 + 74, 146, "캐릭터 · 가상 아이돌 IP 테마", 15, C_BIZ, "start", 700)
    s.rect(x0 + 300, 110, 122, 60, BAND, cs.RADIUS_SM + 2)
    s.glyph("mic", x0 + 326, 140, C_BIZ, 20)
    s.text(x0 + 344, 146, "음성 팩", 15, C_BIZ, "start", 700)
    s.text(x0 + 20, 204, "무료", 14.5, MUTED, "start", 700)
    s.rect(x0 + 20, 214, 402, 54, "#FFFFFF", cs.RADIUS_SM + 2, LINE)
    s.icon("gift", x0 + 48, 241, MUTED, CARD, 17)
    s.text(x0 + 74, 246, "멀미 계산 · 공기 · 창문 · 음성 기본 기능", 15, INK, "start", 700)
    s.glyph("info", x0 + 32, 310, SUBT, 20)
    s.text(x0 + 52, 315, "앱 속 루미 테마는 오리지널 예시 · 실제 IP 계약 없음", 13.5, SUBT)
    s.save("retention_revenue.svg", "재사용 고리와 수익 모델")


def b2b_targets():
    tiles = [
        ("taxi", "택시 · 호출", "약 25만 대", "246,258대 (2026.8.31)", "승객은 늘 뒷좌석, 다음 회전을 모름"),
        ("bus", "통학 차량", "약 4만 대", "43,857대", "같은 길을 매일 달림"),
        ("shuttle", "자율주행 셔틀", "42곳", "시범운행지구", "모두가 동승자, 예고가 더 중요"),
        ("dashboard", "Pleos MiniApp · 계기판", None, "", "내비 옆에 표정 카드"),
    ]
    s = Svg(960, 292)
    tw_ = (904 - 3 * 16) / 4
    for i, (ic, name, big, small, why) in enumerate(tiles):
        x, d = 28 + i * (tw_ + 16), -38
        s.card(x, 66 + d, tw_, 236)
        s.icon(ic, x + 40, 104 + d, C_BIZ, C_BIZ_T, 20)
        s.text(x + 20, 152 + d, name, 15, INK, "start", 700)
        if big:
            s.text(x + 20, 194 + d, big, 28, BLUE, "start", 800)
            s.text(x + 20, 216 + d, small, 12, MUTED)
        else:
            s.text(x + 20, 194 + d, "Pleos 협의 후", 22, SLATE, "start", 700)
        s.line(x + 20, 236 + d, x + tw_ - 20, 236 + d, LINE)
        for j, ln in enumerate(wrap(why, 13, tw_ - 40)):
            s.text(x + 20, 260 + d + j * 19, ln, 13, INK)
    s.save("b2b_targets.svg", "확장 대상")


def ride_quality_uses():
    s = Svg(960, 290)
    s.rect(28, 28, 904, 56, CARD, 12, LINE)
    for k, (sym, col, tint) in enumerate([("I", V_I, V_I_T), ("D", V_D, V_D_T)]):
        s.circle(64 + k * 46, 56, 18, tint)
        s.text(64 + k * 46, 64, sym, 22, col, "middle", 700, True, SERIF)
    s.text(140, 61, "= 차 신호만으로 누구나 같은 값을 내는 승차감 숫자", 16, INK, "start", 700)
    tiles = [("carpair", "차종 비교", "같은 길에서 차종 · 서스펜션별 지수"), ("star", "택시 · 기사 평가", "같은 노선끼리 비교한 부드러움 점수"),
             ("badge", "업계 표준", "ISO 2631-1 형태 → 차량 데이터 표준 지표로 제안")]
    w = (904 - 2 * 16) / 3
    for i, (ic, ttl, d) in enumerate(tiles):
        x = 28 + i * (w + 16)
        s.card(x, 100, w, 162)
        s.icon(ic, x + 42, 140, BLUE, BAND, 22)
        s.text(x + 22, 192, ttl, 16, INK, "start", 700)
        for j, ln in enumerate(wrap(d, 13, w - 44)):
            s.text(x + 22, 216 + j * 19, ln, 13, MUTED)
    s.save("ride_quality_uses.svg", "승차감 정량 지표 쓰임")


def limits():
    cards = [
        ("db", "정답(실제 멀미) 데이터 없음", "기준선 · 환경 계수는 문헌값과 가정에서 시작", ("보정: 버튼 기록 + 동승 조사 (docs/ride-study.md)", "plain")),
        ("car", "실차 시험 불가", "공모전 기간 실차 없음 → 에뮬레이터 · 시뮬레이션까지", ("범위 제한", "plain")),
        ("road", "실제 주행 비교 범위", "고속도로 45초 한 구간 · 옆 흔들림 비교 못 함", ("범위 제한", "plain")),
        ("window", "창문: 명령 수락 확인", "명령은 받지만 움직임은 안 보임 (Virtual Console 창문 연결 꺼짐)", ("실차 동작 확인 필요", "check")),
        ("bell", "내비가 앞일 때 알림 카드", "Pleos 홈은 시스템 앱 알림만 지도 위에 → 소리가 주된 안내", ("Pleos 협의 (MiniApp)", "pleos")),
        ("map", "지도 탭", "Pleos 내비를 흉내 낸 화면 · 인터넷 지도 (OpenFreeMap) · 실차 모드는 차 위치만 · 지나온 길 지우기는 2D에서만 확인", ("시스템 앱 전용 API", "lock")),
        ("steering", "핸들 각도 상수", "조향비 · 축간거리 = 일반 승용차 값", ("정식 탑재 때 차종별", "plain")),
        ("power", "자동 실행", "한 번 열어야 시작 · 이후 백그라운드 계속", ("Pleos 협의 (시동 자동 시작)", "pleos")),
    ]
    cw, ch = 444, 96
    H = 28 + 4 * ch + 3 * 12 + 28
    s = Svg(960, H)
    for i, (ic, ttl, body, (tag, kind)) in enumerate(cards):
        x, y = 28 + (i % 2) * (cw + 16), 28 + (i // 2) * (ch + 12)
        s.card(x, y, cw, ch)
        s.icon(ic, x + 40, y + ch / 2, SLATE, SLATE_T, 20)
        s.text(x + 74, y + 40, ttl, 15, INK, "start", 700)
        tag = tag.replace(" (docs/ride-study.md)", "")
        ty = y + 54
        if kind == "pleos":
            s.tag_pleos(x + 74, ty, tag, h=24)
        elif kind == "check":
            s.chip(x + 74, ty, tag, MID_INK, MID_T, MID_T, 12, 24, bold=True)
        elif kind == "lock":
            s.chip(x + 74, ty, tag, SLATE, SLATE_T, SLATE_T, 12, 24, bold=True)
        else:
            s.chip(x + 74, ty, tag, INK, "#FFFFFF", LINE, 12, 24)
    s.save("limits.svg", "한계와 다음 단계")


def dev_env():
    items = [("Android Studio", "2026.2"), ("AGP · Gradle", "9.4.1 · 9.8.2"), ("Kotlin · compileSdk", "2.4 · 34"),
             ("Pleos Vehicle", "2.3.2"), ("NaviHelper", "3.0.5"), ("TextToSpeech / SpeechToText", "3.3.1")]
    s = Svg(960, 180)
    w = (904 - 2 * 16) / 3
    for i, (lab, ver) in enumerate(items):
        x, y = 28 + (i % 3) * (w + 16), 28 + (i // 3) * (54 + 14)
        s.rect(x, y, w, 54, CARD, cs.RADIUS_SM + 2, LINE)
        s.text(x + 18, y + 22, lab, 12, MUTED)
        s.text(x + 18, y + 44, ver, 18, INK, "start", 700)
    s.save("dev_env.svg", "개발 환경")


def remaining_work():
    rows = [("실제 동승 데이터 5–10회", "데이터"), ("창문이 실제로 열리는지 확인 (창문 연결이 되는 환경)", "확인"),
            ("시연 영상 v3 촬영 · README에 링크", "영상"), ("release APK 다시 빌드 · 접수 메일에 첨부", "빌드"),
            ("기획서를 접수 양식으로 옮기기", "문서")]
    s = Svg(960, 262)
    y = 28
    for txt, tag in rows:
        s.card(28, y, 904, 38, CARD)
        s.rect(46, y + 10, 18, 18, "#FFFFFF", 5, FOOT, 1.6)
        s.text(78, y + 24, txt, 14, INK)
        s.chip(914, y + 8, tag, SLATE, SLATE_T, SLATE_T, 12, 22, bold=True, anchor="end")
        y += 44
    s.save("remaining_work.svg", "남은 일")


FIGS = [low_tone, evidence, two_values, scenario_cards, emulator_checks, theme_names, map_elements, phone_vs_car,
        market_compare, target, mau_funnel, retention_revenue, b2b_targets, ride_quality_uses, limits, dev_env, remaining_work]

if __name__ == "__main__":
    for f in FIGS:
        f()

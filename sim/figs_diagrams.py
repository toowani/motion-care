"""README diagrams (plain SVG, python3 stdlib only).

    python3 sim/figs_diagrams.py      # writes docs/img/<diagram>.svg

Shared tokens: white rounded canvas, one colour per variable (I, D, tau, m, P, a, omega, d_cue),
one stage scale (green / amber / red) and the layer colours used by sim/readme_figures.py.
"""
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IMG = ROOT / "docs" / "img"

FONT = "Apple SD Gothic Neo, Noto Sans KR, Malgun Gothic, sans-serif"
MATH = "Times New Roman, Times, serif"
MONO = "SFMono-Regular, Menlo, Consolas, monospace"
INK, MUTED, LINE, CARD = "#1F2340", "#6B6E86", "#E3E4EE", "#F8FAFC"
WHITE = "#FFFFFF"

# variable colours: (ink, tint)
V = {
    "I": ("#2563EB", "#DBEAFE"), "D": ("#7C3AED", "#EDE9FE"), "tau": ("#EA580C", "#FFEDD5"),
    "m": ("#0D9488", "#CCFBF1"), "P": ("#DC2626", "#FEE2E2"), "a": ("#DB2777", "#FCE7F3"),
    "w": ("#8B5E3C", "#F3E8DD"), "cue": ("#65A30D", "#ECFCCB"), "car": ("#334155", "#E2E8F0"),
    "const": ("#6B7280", "#F3F4F6"),
}
# token -> (variable colour key, symbol, subscript)
TOK = {
    "I": ("I", "I", ""), "D": ("D", "D", ""), "Dbar": ("D", "D̄", ""), "tau": ("tau", "τ", ""),
    "tau0": ("tau", "τ", "0"), "m": ("m", "m", ""), "P": ("P", "P", ""), "ax": ("a", "a", "x"),
    "ay": ("a", "a", "y"), "w": ("w", "ω", ""), "dsw": ("car", "δ", "sw"), "v": ("car", "v", ""),
    "k": ("const", "k", ""),
}
LAYER = {"car": ("#3730A3", "#EEF2FF"), "sdk": ("#334155", "#F1F5F9"),
         "calc": ("#5B21B6", "#F5F3FF"), "out": ("#065F46", "#ECFDF5")}
GREEN, AMBER, RED = "#16A34A", "#CA8A04", "#DC2626"
GREEN_T, AMBER_T, RED_T = "#DCFCE7", "#FEF9C3", "#FEE2E2"
ORANGE = "#EA580C"
COND = ["#2563EB", "#7C3AED", "#0D9488", "#EA580C"]          # conditions 1-4 (also evidence.svg)
COND_T = ["#DBEAFE", "#EDE9FE", "#CCFBF1", "#FFEDD5"]

# line icons on a 24x24 grid (stroke 2, round caps)
ICONS = {
    "steering": '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="2.5"/><path d="M3.2 11h6.4M14.4 11h6.4M12 14.5V21"/>',
    "wave": '<path d="M2 12c1.7-5 3.3-5 5 0s3.3 5 5 0 3.3-5 5 0 2.5 3.5 5 1.5"/>',
    "child": '<circle cx="12" cy="5.5" r="2.8"/><path d="M12 8.5v7M7.5 11.5h9M12 15.5l-3.2 5.5M12 15.5l3.2 5.5"/>',
    "thermo": '<path d="M10 4.5a2 2 0 0 1 4 0V14a4 4 0 1 1-4 0z"/><path d="M12 9v8"/>',
    "person_slider": '<circle cx="8" cy="7" r="3"/><path d="M2.5 20c0-4 2.5-6.5 5.5-6.5s5.5 2.5 5.5 6.5"/><path d="M16 8h6M16 15h6"/><circle cx="18.5" cy="8" r="1.6"/><circle cx="20" cy="15" r="1.6"/>',
    "eye": '<path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
    "question": '<path d="M9 9a3 3 0 1 1 4.2 2.7c-.8.4-1.2 1-1.2 1.9V15"/><path d="M12 19h.01"/>',
    "map": '<path d="M3 6.5l6-2.5 6 2.5 6-2.5v13.5l-6 2.5-6-2.5-6 2.5z"/><path d="M9 4v13.5M15 6.5V20"/>',
    "database": '<ellipse cx="12" cy="5.5" rx="7.5" ry="2.8"/><path d="M4.5 5.5v13c0 1.5 3.4 2.8 7.5 2.8s7.5-1.3 7.5-2.8v-13"/><path d="M4.5 12c0 1.5 3.4 2.8 7.5 2.8s7.5-1.3 7.5-2.8"/>',
    "chip": '<rect x="6" y="6" width="12" height="12" rx="2"/><path d="M9.5 2.5V6M14.5 2.5V6M9.5 18v3.5M14.5 18v3.5M2.5 9.5H6M2.5 14.5H6M18 9.5h3.5M18 14.5h3.5"/>',
    "slider": '<path d="M4 6h16M4 12h16M4 18h16"/><circle cx="9" cy="6" r="2" fill="#fff"/><circle cx="15" cy="12" r="2" fill="#fff"/><circle cx="7" cy="18" r="2" fill="#fff"/>',
    "speaker": '<path d="M4 9.5h3.5L12 5.5v13l-4.5-4H4z"/><path d="M15.5 9.5a3.5 3.5 0 0 1 0 5M18.5 6.5a7.5 7.5 0 0 1 0 11"/>',
    "window": '<path d="M4 19V10l5-5h11v14z"/><path d="M4 13h16"/>',
    "wind": '<path d="M3 8.5h10.5a3 3 0 1 0-3-3"/><path d="M3 12.5h15a3 3 0 1 1-3 3"/><path d="M3 16.5h7"/>',
    "folder": '<path d="M3 7.5a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2V17a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
    "file": '<path d="M7 3h7l4 4v14H7z"/><path d="M14 3v4h4"/>',
    "check": '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
    "cpu": '<rect x="5" y="5" width="14" height="14" rx="3"/><path d="M9 12h6M12 9v6"/>',
    "replay": '<path d="M4 12a8 8 0 1 0 2.4-5.7"/><path d="M4 4v4.5h4.5"/><path d="M10.5 9.5l4 2.5-4 2.5z"/>',
    "coffee": '<path d="M4 9h12v5a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5z"/><path d="M16 10.5h1.5a2.5 2.5 0 0 1 0 5H16"/><path d="M8 3.5v2.5M12 3.5v2.5"/>',
    "family": '<circle cx="8" cy="6" r="2.6"/><circle cx="16.5" cy="9" r="2"/><path d="M3.5 20c0-4.5 2-7 4.5-7s4.5 2.5 4.5 7"/><path d="M13 20c0-3.5 1.5-5.5 3.5-5.5S20 16.5 20 20"/>',
    "gauge": '<path d="M3.5 17a8.5 8.5 0 1 1 17 0"/><path d="M12 17l4-5.5"/><path d="M3.5 17h17"/>',
    "route": '<circle cx="6" cy="18" r="2.4"/><circle cx="18" cy="6" r="2.4"/><path d="M8.4 18H15a3 3 0 0 0 0-6H9a3 3 0 0 1 0-6h6.6"/>',
    "bulb": '<path d="M9 18h6M10 21h4"/><path d="M12 3a6 6 0 0 0-3.8 10.6c.6.6.8 1.3.8 2.4h6c0-1.1.2-1.8.8-2.4A6 6 0 0 0 12 3z"/>',
    "touch": '<circle cx="12" cy="9" r="5.5"/><path d="M12 14.5V21M9 18l3 3 3-3"/>',
    "repeat": '<path d="M17 3l3 3-3 3"/><path d="M4 11V9a3 3 0 0 1 3-3h13"/><path d="M7 21l-3-3 3-3"/><path d="M20 13v2a3 3 0 0 1-3 3H4"/>',
    "gear": '<circle cx="12" cy="12" r="3"/><path d="M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1"/>',
    "power": '<path d="M12 3v8"/><path d="M6.3 6.8a8 8 0 1 0 11.4 0"/>',
}


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def var_tspan(tok, size):
    key, sym, sub = TOK[tok]
    c = V[key][0]
    out = f'<tspan fill="{c}" font-family="{MATH}" font-style="italic" font-weight="700" font-size="{size * 1.12:.1f}">{sym}</tspan>'
    if sub:
        out += f'<tspan fill="{c}" font-family="{MATH}" font-style="italic" font-size="{size * 0.72:.1f}" dy="{size * 0.28:.1f}">{sub}</tspan><tspan dy="{-size * 0.28:.1f}"> </tspan>'
    return out


TOKEN = re.compile(r"\{(\w+)\}")


def rich(s, size):
    """Escape text and turn {I}, {D}, {tau} ... into coloured math tspans."""
    parts, last = [], 0
    for mt in TOKEN.finditer(s):
        parts.append(esc(s[last:mt.start()]))
        parts.append(var_tspan(mt.group(1), size))
        last = mt.end()
    parts.append(esc(s[last:]))
    return "".join(parts)


def tw(s, size):
    """Rough text width (Apple SD Gothic Neo) for layout."""
    w = 0.0
    s = TOKEN.sub(lambda mt: "X" + ("x" if TOK[mt.group(1)][2] else ""), s)
    for ch in s:
        o = ord(ch)
        if 0xAC00 <= o <= 0xD7A3 or 0x3130 <= o <= 0x318F:
            w += 0.95
        elif ch == " ":
            w += 0.3
        elif ch in "il.,:;'|!()[]·/":
            w += 0.32
        elif ch.isupper() or ch.isdigit():
            w += 0.64
        elif o < 128:
            w += 0.55
        else:
            w += 0.8
    return w * size


class Fig:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.p = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}" font-family="{FONT}" font-size="14">',
                  f'<rect x="0.75" y="0.75" width="{w - 1.5}" height="{h - 1.5}" rx="16" fill="{WHITE}" stroke="{LINE}" stroke-width="1.5"/>']

    def add(self, s):
        self.p.append(s)

    def rect(self, x, y, w, h, fill=CARD, stroke=LINE, rx=12, sw=1.5, dash=None):
        st = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.add(f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{rx}" fill="{fill}"{st}{d}/>')

    def text(self, x, y, s, size=14, fill=INK, anchor="start", weight=400, family=None, italic=False):
        fam = f' font-family="{family}"' if family else ""
        it = ' font-style="italic"' if italic else ""
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{fill}" text-anchor="{anchor}" font-weight="{weight}"{fam}{it}>{rich(s, size)}</text>')

    def lines(self, x, y, rows, size=14, fill=INK, anchor="start", weight=400, lh=None):
        lh = lh or size * 1.4
        for i, r in enumerate(rows):
            self.text(x, y + i * lh, r, size, fill, anchor, weight)

    def line(self, x1, y1, x2, y2, stroke=LINE, width=1.5, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.add(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{stroke}" stroke-width="{width}" stroke-linecap="round"{d}/>')

    def path(self, d, stroke=INK, width=2, fill="none", dash=None):
        da = f' stroke-dasharray="{dash}"' if dash else ""
        self.add(f'<path d="{d}" fill="{fill}" stroke="{stroke}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round"{da}/>')

    def poly(self, pts, stroke, width=2, dash=None):
        self.path("M" + " L".join(f"{x:.1f} {y:.1f}" for x, y in pts), stroke, width, dash=dash)

    def arrow(self, pts, color=MUTED, width=2, dash=None, head=10):
        (x1, y1), (x2, y2) = pts[-2], pts[-1]
        a = math.atan2(y2 - y1, x2 - x1)
        bx, by = x2 - head * 0.8 * math.cos(a), y2 - head * 0.8 * math.sin(a)
        self.poly(pts[:-1] + [(bx, by)], color, width, dash)
        h = [(x2, y2), (x2 - head * math.cos(a - 0.45), y2 - head * math.sin(a - 0.45)),
             (x2 - head * math.cos(a + 0.45), y2 - head * math.sin(a + 0.45))]
        self.add('<path d="M' + " L".join(f"{x:.1f} {y:.1f}" for x, y in h) + f' Z" fill="{color}"/>')

    def icon(self, cx, cy, name, fg, bg, r=22, scale=1.0):
        if bg:
            self.add(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r}" fill="{bg}"/>')
        self.add(f'<g transform="translate({cx - 12 * scale:.1f} {cy - 12 * scale:.1f}) scale({scale})" fill="none" stroke="{fg}" '
                 f'stroke-width="{2 / scale:.2f}" stroke-linecap="round" stroke-linejoin="round">{ICONS[name]}</g>')

    def num(self, cx, cy, n, color, r=14, size=15):
        self.add(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r}" fill="{color}"/>')
        self.text(cx, cy + size * 0.36, str(n), size, WHITE, "middle", 700)

    def chip(self, x, y, label, fg=INK, bg=WHITE, size=13, h=None, pad=12, anchor="start", stroke=None,
             dash=None, weight=400, rx=None, family=None, w=None):
        """Pill with text; returns its width. x is left edge (or centre with anchor='middle')."""
        h = h or size + 16
        w = w or ((len(label) * size * 0.61) if family == MONO else tw(label, size)) + 2 * pad
        x0 = x - w / 2 if anchor == "middle" else x - w if anchor == "end" else x
        self.rect(x0, y, w, h, bg, stroke, rx if rx is not None else h / 2, 1.5, dash)
        fam = f' font-family="{family}"' if family else ""
        self.add(f'<text x="{x0 + w / 2:.1f}" y="{y + h / 2 + size * 0.36:.1f}" font-size="{size}" fill="{fg}" '
                 f'text-anchor="middle" font-weight="{weight}"{fam}>{rich(label, size)}</text>')
        return w

    def box(self, x, y, w, h, title, sub=None, fg=INK, bg=CARD, stroke=None, size=15, dash=None, rx=12, sub_fill=MUTED):
        self.rect(x, y, w, h, bg, stroke or bg, rx, 1.5, dash)
        if sub:
            subs = sub if isinstance(sub, list) else [sub]
            top = y + h / 2 - (len(subs) * 17) / 2 + size * 0.36 - 2
            self.text(x + w / 2, top, title, size, fg, "middle", 700)
            for i, sline in enumerate(subs):
                self.text(x + w / 2, top + 20 + i * 17, sline, 12.5, sub_fill, "middle")
        else:
            self.text(x + w / 2, y + h / 2 + size * 0.36, title, size, fg, "middle", 700)

    def save(self, name):
        IMG.mkdir(parents=True, exist_ok=True)
        (IMG / name).write_text("\n".join(self.p + ["</svg>"]) + "\n", encoding="utf-8")
        print(IMG / name)


def varbox(f, x, y, w, h, label, key, size=16):
    """Node tinted in a variable's colour."""
    ink, tint = V[key]
    f.rect(x, y, w, h, tint, ink, 12, 1.8)
    f.text(x + w / 2, y + h / 2 + size * 0.36, label, size, INK, "middle", 700)


# ---------------------------------------------------------------- figures

def conditions():
    f = Fig(960, 560)
    cards = [
        ("예고 없는 움직임", ["몸의 느낌과", "예상이 어긋남"], "steering", ["핸들 각도", "방향지시등", "다음 회전"],
         ["흔들림을", "1–2초 먼저 계산"]),
        ("느린 흔들림 누적", ["0.1–0.5 Hz", "흔들림이 쌓임"], "wave", ["속도", "진행 방향"],
         ["흔들림 지수 {I}", "누적량 {D}"]),
        ("덥고 답답한 실내", ["덥고 내기순환이", "길수록 쉬움"], "thermo", ["실내 온도", "내기순환"],
         ["외기 전환", "온도 낮춤"]),
        ("사람마다 다른 기준", ["같은 흔들림에도", "다름"], "person_slider", ["멀미 완화 버튼을", "누른 순간"],
         ["개인 기준선 {tau}", "학습"]),
    ]
    out_ink, out_bg = LAYER["out"]
    sdk_ink, sdk_bg = LAYER["sdk"]
    for i, (name, mean, ic, sigs, act) in enumerate(cards):
        x, y, w = 28 + i * (214 + 16), 40, 214
        c, t = COND[i], COND_T[i]
        f.rect(x, y, w, 480, CARD, LINE)
        f.add(f'<path d="M{x} {y + 12} a12 12 0 0 1 12 -12 h{w - 24} a12 12 0 0 1 12 12 v4 h-{w} z" fill="{c}"/>')
        f.num(x + 38, y + 50, i + 1, c, 16, 16)
        f.icon(x + w - 40, y + 50, ic, c, t)
        # band: condition
        f.text(x + 16, y + 102, "조건", 12, MUTED, weight=700)
        f.text(x + 16, y + 126, name, 16, INK, weight=700)
        f.lines(x + 16, y + 150, mean, 13, MUTED, lh=18)
        f.line(x + 16, y + 186, x + w - 16, y + 186)
        # band: signals
        f.text(x + 16, y + 210, "읽는 신호", 12, MUTED, weight=700)
        if len(sigs) == 2 and i == 3:
            f.rect(x + 16, y + 222, w - 32, 50, ORANGE, None, 25)
            f.lines(x + w / 2, y + 243, sigs, 13, WHITE, "middle", 700, 17)
        else:
            for j, sg in enumerate(sigs):
                f.chip(x + 16, y + 222 + j * 38, sg, sdk_ink, V["car"][1], 13, 30, w=w - 32, weight=700)
        f.line(x + 16, y + 346, x + w - 16, y + 346)
        # band: action
        f.text(x + 16, y + 370, "앱이 하는 일", 12, MUTED, weight=700)
        f.rect(x + 16, y + 382, w - 32, 78, out_bg, out_ink, 12, 1.8)
        f.add(f'<path d="M{x + 30} {y + 421} h16 m-6 -6 l6 6 -6 6" fill="none" stroke="{out_ink}" stroke-width="2.2" '
              f'stroke-linecap="round" stroke-linejoin="round"/>')
        f.lines(x + 56, y + 416, act, 14, out_ink, weight=700, lh=20)
    f.save("conditions.svg")


def who_gets_sick():
    f = Fig(960, 260)
    # left: top view, car heading right
    f.text(40, 44, "운전자 vs 동승자", 16, INK, weight=700)
    cx0, cy0 = 48, 64
    f.rect(cx0, cy0, 250, 150, "#F1F5F9", "#94A3B8", 46, 2)
    f.path(f"M{cx0 + 196} {cy0 + 22} q22 53 0 106", "#94A3B8", 2)          # windshield
    f.path(f"M{cx0 + 48} {cy0 + 26} q-14 49 0 98", "#94A3B8", 2)           # rear window
    seats = {"drv": (cx0 + 138, cy0 + 36), "pas": (cx0 + 138, cy0 + 114), "rl": (cx0 + 74, cy0 + 36),
             "rr": (cx0 + 74, cy0 + 114)}
    for k, (sx, sy) in seats.items():
        f.rect(sx - 22, sy - 22, 44, 44, WHITE, "#CBD5E1", 10, 1.5)
    f.icon(*seats["drv"], "eye", GREEN, GREEN_T, 20)
    f.icon(*seats["rr"], "child", RED, RED_T, 20)
    f.add(f'<circle cx="{seats["rr"][0] + 18}" cy="{seats["rr"][1] - 18}" r="10" fill="{RED}"/>')
    f.text(seats["rr"][0] + 18, seats["rr"][1] - 13.5, "?", 13, WHITE, "middle", 700)
    f.arrow([(cx0 + 120, 236), (cx0 + 200, 236)], MUTED, 2)
    f.text(cx0 + 110, 240, "진행 방향", 12, MUTED, "end")
    f.line(seats["drv"][0] + 24, seats["drv"][1], 330, seats["drv"][1], GREEN, 1.5, "3 3")
    f.text(338, seats["drv"][1] - 3, "운전자", 15, GREEN, weight=700)
    f.text(338, seats["drv"][1] + 17, "다음 움직임을 앎", 13, INK)
    f.line(seats["rr"][0] + 24, seats["rr"][1], 330, seats["rr"][1], RED, 1.5, "3 3")
    f.text(338, seats["rr"][1] - 3, "동승자", 15, RED, weight=700)
    f.text(338, seats["rr"][1] + 17, "모름", 13, INK)
    f.line(492, 30, 492, 230, LINE, 1.5)
    # right: age axis
    f.text(530, 44, "멀미가 가장 흔한 나이", 16, INK, weight=700)
    X = lambda a: 540 + 380 * a / 18
    f.rect(X(4), 72, X(13) - X(4), 118, RED_T, None, 10)
    f.icon((X(4) + X(13)) / 2, 104, "child", RED, WHITE)
    f.text((X(4) + X(13)) / 2, 152, "가장 약함 · 4–13세", 16, RED, "middle", 700)
    f.text((X(4) + X(13)) / 2, 174, "Huppert 외, 2019", 12.5, MUTED, "middle")
    f.arrow([(540, 196), (928, 196)], MUTED, 2)
    for a in (0, 4, 13, 18):
        f.line(X(a), 190, X(a), 202, INK if a in (4, 13) else MUTED, 2)
        f.text(X(a), 222, f"{a}세", 13, RED if a in (4, 13) else MUTED, "middle", 700 if a in (4, 13) else 400)
    f.save("who_gets_sick.svg")


def sig(x, x0, k):
    return 1 / (1 + math.exp(-(x - x0) / k))


def steering_lead():
    f = Fig(960, 240)
    car, brown, gps = V["car"][0], V["w"][0], "#9CA3AF"
    f.chip(932, 22, "개념도", MUTED, WHITE, 12, 26, anchor="end", stroke=LINE, dash="4 3")
    # legend
    lx = 40
    for label, c, dash in [("핸들 각도 {dsw}", car, None), ("{w} 핸들 각도로 예측", brown, None),
                           ("GPS 방향 (연속 위치에서)", gps, "6 5")]:
        f.line(lx, 35, lx + 28, 35, c, 3, dash)
        f.text(lx + 36, 40, label, 14, INK)
        lx += 60 + tw(label, 14)
    x0, x1, base = 70, 900, 206
    f.arrow([(x0, base), (x1 + 20, base)], MUTED, 1.8)
    f.text(x1 + 18, base + 22, "시간", 12.5, MUTED, "end")
    xs = [x0 + i * 4 for i in range(int((x1 - x0) / 4) + 1)]
    lo = base - 18
    f.poly([(x, lo - 104 * sig(x, 250, 7)) for x in xs], car, 3)
    f.poly([(x, lo - 76 * sig(x, 268, 12)) for x in xs], brown, 3)
    f.poly([(x, lo - 76 * sig(x, 400, 16)) for x in xs], gps, 3, "7 6")
    yb = lo - 14
    f.line(268, lo - 38, 268, yb + 6, MUTED, 1.2, "2 3")
    f.line(400, lo - 38, 400, yb + 6, MUTED, 1.2, "2 3")
    f.arrow([(334, yb), (272, yb)], INK, 2, head=8)
    f.arrow([(334, yb), (396, yb)], INK, 2, head=8)
    f.text(334, yb - 12, "1–2초", 17, INK, "middle", 700)
    f.chip(890, 142, "{k} = 0.2로 GPS 쪽으로 당김", INK, V["const"][1], 13, 30, anchor="end", stroke=LINE)
    f.save("steering_lead.svg")


def band_filter():
    f = Fig(960, 240)
    X = {0: 48, 0.1: 300, 0.5: 700, "end": 912}
    top, bot = 32, 176
    f.add('<defs><pattern id="hatch" width="10" height="10" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">'
          '<rect width="10" height="10" fill="#F3F4F6"/><line x1="0" y1="0" x2="0" y2="10" stroke="#D1D5DB" stroke-width="3"/></pattern></defs>')
    f.rect(X[0], top, X[0.1] - X[0], bot - top, "url(#hatch)", "#D1D5DB", 12)
    f.rect(X[0.1], top, X[0.5] - X[0.1], bot - top, V["a"][1], V["a"][0], 12, 1.8)
    f.rect(X[0.5], top, X["end"] - X[0.5], bot - top, "#F9FAFB", LINE, 12, 1.5, "5 4")
    # zone 1
    zx = (X[0] + X[0.1]) / 2
    f.chip(zx, top + 14, "< 0.1 Hz 제거", "#4B5563", WHITE, 15, 30, anchor="middle", stroke="#D1D5DB", weight=700)
    f.chip(zx - 56, top + 58, "꾸준한 가속", INK, WHITE, 13, 28, anchor="middle", stroke=LINE)
    f.chip(zx + 60, top + 58, "긴 커브", INK, WHITE, 13, 28, anchor="middle", stroke=LINE)
    f.chip(zx, top + 100, "최근 10초 평균 빼기", WHITE, "#4B5563", 13, 28, anchor="middle")
    # zone 2
    pink = V["a"][0]
    f.text(X[0.1] + 24, top + 34, "0.1–0.5 Hz 남겨 누적", 17, pink, weight=700)
    f.icon(X[0.1] + 46, top + 86, "wave", pink, WHITE)
    f.arrow([(X[0.1] + 76, top + 86), (X[0.1] + 128, top + 86)], pink, 2.2)
    f.chip(X[0.1] + 136, top + 52, "누적 흔들림 {D}", INK, V["D"][1], 14, 30, stroke=V["D"][0], weight=700)
    f.chip(X[0.1] + 136, top + 92, "흔들림 지수 {I}", INK, V["I"][1], 14, 30, stroke=V["I"][0], weight=700)
    f.text(X[0.1] + 292, top + 72, "주행 전체", 12.5, MUTED)
    f.text(X[0.1] + 292, top + 112, "최근 60초", 12.5, MUTED)
    # zone 3
    zx = (X[0.5] + X["end"]) / 2
    f.text(zx, top + 62, "> 0.5 Hz", 17, "#9CA3AF", "middle", 700)
    f.text(zx, top + 88, "1초 샘플이라", 13, MUTED, "middle")
    f.text(zx, top + 106, "담기지 않음", 13, MUTED, "middle")
    # axis
    f.arrow([(X[0], 200), (X["end"] + 20, 200)], MUTED, 1.8)
    for k, lab in [(0, "0"), (0.1, "0.1 Hz"), (0.5, "0.5 Hz")]:
        f.line(X[k], 194, X[k], 206, INK, 2)
        f.text(X[k], 224, lab, 13, INK, "middle", 700)
    f.text(X["end"] + 18, 224, "주파수", 12.5, MUTED, "end")
    f.save("band_filter.svg")


def engine_parity():
    f = Fig(960, 300)
    sdk_ink, sdk_bg = V["car"]
    f.rect(330, 26, 300, 60, sdk_bg, sdk_ink, 12, 1.8)
    f.icon(366, 56, "file", sdk_ink, WHITE)
    f.text(398, 52, "shared/params.json", 15, sdk_ink, weight=700, family=MONO)
    f.text(398, 72, "모든 상수", 13, MUTED)
    f.arrow([(430, 88), (240, 126)], sdk_ink, 2)
    f.arrow([(530, 88), (720, 126)], sdk_ink, 2)
    for x, path_, sub in [(50, "sim/engine.py", "파이썬 시뮬레이터"), (600, "android/core", "Kotlin 앱 엔진")]:
        f.rect(x, 130, 310, 64, LAYER["calc"][1], LAYER["calc"][0], 12, 1.8)
        f.icon(x + 36, 162, "cpu", LAYER["calc"][0], WHITE)
        f.text(x + 68, 158, path_, 15, LAYER["calc"][0], weight=700, family=MONO)
        f.text(x + 68, 178, sub, 13, MUTED)
    f.rect(384, 118, 192, 88, GREEN_T, GREEN, 14, 2)
    f.text(480, 146, "= 일치", 18, GREEN, "middle", 700)
    f.text(480, 168, "2,162개 샘플", 14, INK, "middle", 700)
    f.add(f'<text x="480" y="190" font-size="13" fill="{INK}" text-anchor="middle">오차 2×10<tspan font-size="9" dy="-6">−6</tspan>'
          f'<tspan dy="6"> 이하</tspan></text>')
    f.text(480, 224, "굽은 길 · 도심 · 앱 없는 나들이길", 12.5, MUTED, "middle")
    # change order strip
    f.rect(28, 240, 904, 44, CARD, LINE, 12)
    f.text(46, 267, "바꿀 때 순서", 13, MUTED, weight=700)
    steps = ["sim/engine.py 수정", "prototype.py 점검 · test_vectors.csv 갱신", "android/core를 같은 숫자로"]
    x = 150
    for i, st in enumerate(steps):
        f.num(x + 12, 262, i + 1, LAYER["calc"][0], 11, 13)
        f.text(x + 30, 267, st, 13.5, INK, weight=700)
        x += 30 + tw(st, 13.5) + 14
        if i < 2:
            f.arrow([(x, 262), (x + 26, 262)], MUTED, 2, head=8)
            x += 40
    f.save("engine_parity.svg")


def sdk_map():
    f = Fig(960, 560)
    lanes = [(28, 360, "읽기 · 1초마다", LAYER["car"]), (420, 200, "계산", LAYER["calc"]), (652, 280, "행동", LAYER["out"])]
    for x, w, title, (ink, bg) in lanes:
        f.rect(x, 26, w, 456, bg, None, 14)
        f.text(x + w / 2, 56, title, 17, ink, "middle", 700)
    sdk_ink, sdk_bg = LAYER["sdk"]

    def sdkcard(x, y, w, name, use, tag=None, dash=None, tag_fg=sdk_ink):
        h = 84 if tag else 64
        f.rect(x, y, w, h, WHITE, sdk_ink if not dash else "#94A3B8", 10, 1.5, dash)
        f.text(x + 14, y + 25, name, 14, sdk_ink, weight=700)
        f.text(x + 14, y + 47, use, 13, INK)
        if tag:
            f.chip(x + 14, y + 56, tag, tag_fg, sdk_bg, 11.5, 20, pad=8, family=MONO if tag.isupper() or "_" in tag else None)
        return h

    y = 74
    for name, use, tag in [("Vehicle · SteeringWheel", "핸들 각도 → 1–2초 먼저", "CAR_DRIVE"),
                           ("Vehicle · TurnSignal", "방향지시등", None),
                           ("Vehicle · HVAC", "실내 온도 · 내기순환", None),
                           ("위치 (GNSS) · CarPropertyManager", "속도 · 위치", None),
                           ("NaviHelper", "다음 회전 · 도착 (TBT, DrivingInfo)", None)]:
        y += sdkcard(44, y, 328, name, use, tag) + 10
    # engine
    ci, cb = LAYER["calc"]
    f.rect(436, 74, 168, 392, WHITE, ci, 12, 1.8)
    f.icon(520, 108, "cpu", ci, cb)
    f.text(520, 152, "순수 Kotlin 엔진", 14, ci, "middle", 700)
    f.text(520, 171, "SDK와 분리", 12.5, MUTED, "middle")
    for j, (tok, lab) in enumerate([("I", "흔들림 지수"), ("D", "누적 흔들림"), ("m", "환경 계수"),
                                    ("tau", "개인 기준선"), ("P", "멀미 가능성")]):
        key = TOK[tok][0]
        f.chip(452, 188 + j * 46, "{" + tok + "}  " + lab, INK, V[key][1], 13.5, 36, w=136, stroke=V[key][0], weight=700)
    f.text(520, 436, "식은 3절", 12.5, MUTED, "middle")
    y = 74
    for name, use, tag, dash in [("Vehicle · HVAC", "외기 · 22°C", None, None),
                                 ("Vehicle · Window", "뒷창문 20%", "CONTROL_CAR_WINDOWS", None),
                                 ("TextToSpeech", "안내 한 줄", "HYBRID", None),
                                 ("SpeechToText", "확장 기능 답", "확장", "5 4")]:
        y += sdkcard(668, y, 248, name, use, tag, dash) + 10
    for xa in (392, 624):
        f.arrow([(xa - 2, 270), (xa + 26, 270)], MUTED, 3, head=12)
    # closed loop: what the car did changes what it reports next second
    out_ink = LAYER["out"][0]
    f.arrow([(792, 484), (792, 522), (208, 522), (208, 488)], out_ink, 2.5, dash="8 6", head=12)
    f.chip(500, 507, "닫힌 고리 · 바뀐 실내 온도 · 환기가 다시 계산으로", out_ink, LAYER["out"][1], 14, 30, anchor="middle",
           stroke=out_ink, weight=700)
    f.save("sdk_map.svg")


def validation_ladder():
    f = Fig(960, 300)
    steps = [("map", "실제 도로 시나리오 시뮬레이션", "점검 10 / 10 통과", "도로 4종 · 주행 6개"),
             ("database", "실제 주행 데이터 비교 (comma2k19)", "가속도 1.41배", "기준 0.5–2배 · 고속도로 45초"),
             ("chip", "Pleos 에뮬레이터 (실제 SDK)", "11항목 점검 · 9 확인", "부분 1 · 거절 1 · v2.1 · API 34")]
    base, ch, rise = 276, 150, 36
    for i, (ic, title, big, sub) in enumerate(steps):
        x, w = 28 + i * 304, 296
        bottom = base - 16 - i * rise
        y = bottom - ch
        last = i == 2
        # riser block (the step)
        if i:
            f.rect(x, bottom, w, base - bottom, "#EEF0F5", None, 6)
        f.rect(x, y, w, ch, GREEN_T if last else CARD, GREEN if last else LINE, 14, 2 if last else 1.5)
        f.num(x + 30, y + 32, i + 1, GREEN if last else INK, 15, 15)
        f.icon(x + 80, y + 32, ic, GREEN if last else LAYER["calc"][0], WHITE if last else LAYER["calc"][1])
        f.text(x + 20, y + 80, title, 14, INK, weight=700)
        f.text(x + 20, y + 112, big, 23, GREEN if last else INK, weight=700)
        f.text(x + 20, y + 134, sub, 12.5, MUTED)
    f.line(28, base, 932, base, "#CBD5E1", 2)
    f.save("validation_ladder.svg")


def flow_chips(f, x, y, w, items, fg, bg, size=14, h=34, gap=10, stroke=None, dash=None):
    cx, cy = x, y
    for it in items:
        cw = tw(it, size) + 28
        if cx + cw > x + w:
            cx, cy = x, cy + h + gap
        f.chip(cx, cy, it, fg, bg, size, h, pad=14, stroke=stroke, dash=dash, weight=700)
        cx += cw + gap
    return cy + h


def real_or_sim():
    f = Fig(960, 300)
    f.chip(480, 22, "Pleos Connect Emulator v2.1 · Android Automotive API 34 · 실제 실행", INK, CARD, 13.5, 32,
           anchor="middle", stroke=LINE, weight=700)
    f.rect(28, 72, 436, 172, WHITE, GREEN, 14, 2)
    f.icon(62, 106, "check", GREEN, GREEN_T)
    f.text(94, 112, "진짜 · Pleos SDK", 16, GREEN, weight=700)
    f.text(46, 156, "차를 움직임", 12, MUTED, weight=700)
    flow_chips(f, 136, 134, 320, ["공조", "창문", "음성"], LAYER["out"][0], LAYER["out"][1])
    f.text(46, 208, "차 신호", 12, MUTED, weight=700)
    flow_chips(f, 136, 186, 320, ["방향지시등", "핸들 각도", "실내 온도"], V["car"][0], V["car"][1])
    f.rect(496, 72, 436, 172, WHITE, "#9CA3AF", 14, 2, "7 5")
    f.icon(530, 106, "replay", MUTED, "#F3F4F6")
    f.text(562, 112, "시뮬레이션 · 실제 도로 경로 재생", 16, "#4B5563", weight=700)
    flow_chips(f, 514, 140, 400, ["속도", "진행 방향", "경로", "회전 안내 카드", "도착 시간"], "#4B5563", "#F3F4F6",
               stroke="#D1D5DB", dash="4 3")
    f.icon(50, 272, "slider", V["car"][0], None, scale=0.9)
    f.text(72, 277, "차 신호 조작: Virtual Console", 13.5, INK, weight=700)
    f.text(72 + tw("차 신호 조작: Virtual Console", 13.5) + 10, 277, "Pleos 제공, 우리 앱 아님", 13, MUTED)
    f.save("real_or_sim.svg")


def relief_sequence():
    f = Fig(960, 480)
    # button
    f.rect(28, 186, 196, 72, ORANGE, None, 36)
    f.icon(66, 222, "touch", WHITE, None)
    f.text(140, 230, "멀미 완화", 22, WHITE, "middle", 700)
    f.text(126, 284, "지도 · 분석 · 한 화면 모드", 12.5, MUTED, "middle")
    f.text(126, 302, "어디서나", 12.5, MUTED, "middle")
    # steps
    sx, sw, sh, gap = 268, 368, 84, 12
    ys = [36 + i * (sh + gap) for i in range(4)]
    f.line(224, 222, 246, 222, MUTED, 2)
    f.line(246, ys[0] + sh / 2, 246, ys[3] + sh / 2, MUTED, 2)
    for y in ys:
        f.arrow([(246, y + sh / 2), (sx - 2, y + sh / 2)], MUTED, 2, head=8)
    out_ink, out_bg = LAYER["out"]
    sdk_ink, sdk_bg = LAYER["sdk"]
    data = [("window", "뒷창문 조금 (전체의 20%)", "Vehicle Window"),
            ("wind", "바깥 공기 + 22°C", "Vehicle HVAC"),
            ("speaker", None, "TextToSpeech"),
            ("database", "누른 순간 {D} → 내 기준선 {tau}", "앱 안 · 수식 ⑤")]
    for i, (ic, act, sdk) in enumerate(data):
        y = ys[i]
        inapp = i == 3
        f.rect(sx, y, sw, sh, LAYER["calc"][1] if inapp else out_bg, LAYER["calc"][0] if inapp else out_ink, 12, 1.8)
        f.num(sx + 24, y + sh / 2, i + 1, LAYER["calc"][0] if inapp else out_ink, 13, 14)
        f.icon(sx + 70, y + sh / 2, ic, LAYER["calc"][0] if inapp else out_ink, WHITE)
        if act:
            f.text(sx + 104, y + 36, act, 15, INK, weight=700)
        else:
            bx, by, bw, bh = sx + 102, y + 12, 250, 40
            f.add(f'<path d="M{bx + 10} {by} h{bw - 20} a10 10 0 0 1 10 10 v{bh - 20} a10 10 0 0 1 -10 10 h-{bw - 44} l-10 8 v-8 h-{14} '
                  f'a10 10 0 0 1 -10 -10 v-{bh - 20} a10 10 0 0 1 10 -10 z" fill="{WHITE}" stroke="{out_ink}" stroke-width="1.5"/>')
            f.text(bx + bw / 2, by + 17, "“창문 열고 바람 넣을게요.", 13, INK, "middle", 700)
            f.text(bx + bw / 2, by + 33, "먼 곳을 보세요.”", 13, INK, "middle", 700)
        f.chip(sx + 104 if act else sx + 250, y + (48 if act else 56), sdk, sdk_ink, WHITE, 12, 24, pad=10, stroke="#CBD5E1",
               family=None)
    f.chip(sx, 432, "pleos.car.permission.CONTROL_CAR_WINDOWS", sdk_ink, sdk_bg, 12.5, 30, pad=14, family=MONO)
    # fallbacks
    fx, fw = 676, 256
    f.text(fx, 32, "안 될 때", 14, "#92400E", weight=700)
    falls = [(44, 116, "창문 불가", "거절 · 뒷창문 없음 · 4초 무응답", ["화면 + 음성", "“창문을 조금 열어 주세요”"]),
             (172, 68, "공조 꺼짐", None, ["공조는 그대로"]),
             (256, 68, "20초 안 다시 누름", None, ["“바람 넣는 중이에요”만"])]
    for i, (top, h, cond, sub, res) in enumerate(falls):
        f.rect(fx, top, fw, h, "#FFFBEB", AMBER, 12, 1.8, "6 4")
        f.text(fx + 16, top + 26, cond, 14.5, "#92400E", weight=700)
        yy = top + 26
        if sub:
            f.text(fx + 16, top + 46, sub, 12, MUTED)
            yy = top + 46
        f.add(f'<path d="M{fx + 22} {yy + 10} v9 h10 m-5 -5 l5 5 -5 5" fill="none" stroke="#92400E" stroke-width="2" '
              f'stroke-linecap="round" stroke-linejoin="round"/>')
        f.lines(fx + 44, yy + 24, res, 13.5, INK, weight=700, lh=20)
        f.arrow([(sx + sw + 2, ys[i] + sh / 2), (fx - 2, top + (h / 2 if i else 42))], AMBER, 1.8, "5 4", head=8)
    f.save("relief_sequence.svg")


def status_chip(f, x, y, kind, label=None, size=12):
    styles = {"run": (GREEN, GREEN_T, None, "엔진에서 계산 중"), "off": ("#4B5563", "#E5E7EB", None, "앱 화면 · 소리에서 꺼 둠"),
              "idea": (MUTED, WHITE, "4 3", "제안")}
    fg, bg, dash, default = styles[kind]
    return f.chip(x, y, label or default, fg, bg, size, 24, pad=10, stroke=fg if dash else None, dash=dash, weight=700)


def extension_map():
    f = Fig(960, 500)
    x = 28
    f.text(x, 38, "상태", 12.5, MUTED, weight=700)
    x += 40
    for k in ("run", "off", "idea"):
        x += status_chip(f, x, 22, k, "제안 · 확장" if k == "idea" else None) + 10
    ci, cb = LAYER["calc"]
    h, y1, y2 = 190, 64, 266
    hx, hy, hw, hh = 392, (y1 + y2 + h) / 2 - 41, 176, 82
    for cx, cy in [(368, y1 + h / 2), (592, y1 + h / 2), (368, y2 + h / 2), (592, y2 + h / 2)]:
        f.line(hx + hw / 2, hy + hh / 2, cx, cy, "#C4B5FD", 2)
    f.rect(hx, hy, hw, hh, cb, ci, 14, 2)
    f.text(hx + hw / 2, hy + 32, "같은 엔진", 16, ci, "middle", 700)
    f.text(hx + hw / 2, hy + 60, "{I} · {D} · {m} · {tau} · {P}", 16, INK, "middle", 700)
    ours = "우리 몫: {D} 기반 멀미 가능성 · 개인 기준선 {tau} · 앱 배포"
    # overlap = 멀미 Zero (현대차·기아 2026 아이디어 페스티벌 최우수상), public reports only
    cards = [
        (28, y1, "speaker", "예측 음성 안내", "“곧 왼쪽으로 돌아요”", ["run", "off"],
         "근거: 1초 전 음성 예고 → 멀미 감소 (Kuiper 외, 2020)", "동승 실험 뒤 켬", True),
        (592, y1, "coffee", "휴게소 추천", "제안 시점 계산 (굽은 길 거칠게 3분 45초)", ["run", ("idea", "확장 · LLM + POI 검색 API")],
         "질문 코드는 보관", None, False),
        (28, y2, "family", "가족 모드", "아이마다 기준선, 가장 낮은 아이 먼저", ["run", "off"], None, None, False),
        (592, y2, "gauge", "승차감 정량 지표", "{I} · {D}를 승차감 숫자로", ["idea"], None, None, True),
    ]
    for i, (x, y, ic, title, line1, sts, note, tag, overlap) in enumerate(cards):
        w = 340
        f.rect(x, y, w, h, CARD, AMBER if overlap else LINE, 14)
        f.num(x + 26, y + 30, i + 1, ci, 13, 14)
        f.icon(x + w - 38, y + 36, ic, ci, cb)
        f.text(x + 48, y + 36, title, 16, INK, weight=700)
        if overlap:
            f.chip(x + 18 + tw(line1, 13.5) + 12, y + 53, "멀미 Zero와 겹침", AMBER, AMBER_T, 12, 24, pad=10,
                   stroke=AMBER, weight=700)
        f.text(x + 18, y + 70, line1, 13.5, INK)
        sx = x + 18
        for st in sts:
            k, lab = st if isinstance(st, tuple) else (st, None)
            sx += status_chip(f, sx, y + 84, k, lab) + 8
        if note:
            f.text(x + 18, y + 126, note, 12.5, MUTED)
        if tag:
            f.chip(x + 18, y + 134, tag, "#4B5563", WHITE, 12, 22, pad=10, stroke="#CBD5E1")
        if i == 3:
            cx = x + 18
            for u in ("차종 비교", "택시 · 기사 평가", "업계 표준"):
                cx += f.chip(cx, y + 120, u, INK, WHITE, 12.5, 26, pad=10, stroke=LINE) + 8
        if overlap:
            f.text(x + 18, y + h - 14, ours, 12, ci, weight=700)
    f.icon(42, 478, "folder", MUTED, None, scale=0.75)
    f.text(60, 482, "later/ 폴더에 코드 보관 (컴파일만)", 13, MUTED)
    f.text(932, 482, "겹침: 멀미 Zero (현대차·기아 2026 아이디어 페스티벌 최우수상) · 공개 자료 기준", 12, MUTED, "end")
    f.save("extension_map.svg")


def rest_stop_flow():
    f = Fig(960, 240)
    cy = 104
    f.box(28, cy - 34, 150, 68, "흔들림 많음이", "길게 이어짐", RED, RED_T, RED, 14.5, sub_fill=RED)
    f.arrow([(180, cy), (204, cy)], MUTED, 2)
    ci, cb = LAYER["calc"]
    f.box(206, cy - 34, 196, 68, "엔진: 제안 시점", "굽은 길 거칠게 3분 45초", ci, cb, ci, 14.5)
    f.arrow([(404, cy), (428, cy)], MUTED, 2)
    f.rect(430, 24, 200, 160, WHITE, "#94A3B8", 14, 2, "7 5")
    f.text(530, 50, "확장: LLM", 15, "#334155", "middle", 700)
    f.chip(530, 64, "남은 경로", INK, V["car"][1], 13, 30, anchor="middle", w=160, weight=700)
    f.chip(530, 102, "멀미 가능성 {P}", INK, V["P"][1], 13, 30, anchor="middle", w=160, stroke=V["P"][0], weight=700)
    f.chip(530, 140, "동승자", INK, V["car"][1], 13, 30, anchor="middle", w=160, weight=700)
    f.arrow([(632, cy), (656, cy)], MUTED, 2)
    f.box(658, cy - 34, 128, 68, "들를 곳", "한 줄 추천", LAYER["out"][0], LAYER["out"][1], LAYER["out"][0], 14.5)
    f.arrow([(788, cy), (812, cy)], MUTED, 2)
    f.box(814, cy - 34, 118, 68, "NaviHelper", "경유지", LAYER["sdk"][0], LAYER["sdk"][1], LAYER["sdk"][0], 14.5)
    f.icon(42, 214, "folder", MUTED, None, scale=0.75)
    f.text(60, 218, "질문 코드는 보관 · POI 검색 API 필요", 13, MUTED)
    f.save("rest_stop_flow.svg")


def repo_structure():
    f = Fig(960, 560)
    ENG, APPC, SIM, DOC, GREY = LAYER["calc"][0], LAYER["out"][0], "#334155", MUTED, "#9CA3AF"
    x = 28
    f.text(x, 38, "폴더 색", 12.5, MUTED, weight=700)
    x += 56
    for lab, c in [("엔진", ENG), ("앱", APPC), ("시뮬 · 공용", SIM), ("문서", DOC), ("로컬 전용", GREY)]:
        f.icon(x + 9, 33, "folder", c, None, scale=0.75)
        f.text(x + 24, 38, lab, 12.5, INK)
        x += 40 + tw(lab, 12.5)
    rows = [
        (0, "android/core/", "순수 Kotlin 판단 엔진 + SteeringYaw + 단위 테스트", ENG, "folder"),
        (0, "android/app/", "Android Automotive 앱", APPC, "folder"),
        (1, "TripService", "주행 판단과 행동 (내비가 앞이어도 계속)", APPC, "file"),
        (1, "Notifier", "앱이 안 보일 때 알림 카드", APPC, "file"),
        (1, "MainActivity", "화면만", APPC, "file"),
        (1, "ui/", "지도 · 분석 탭, 한 화면 모드, 설정", APPC, "folder"),
        (1, "theme/", "아이콘 테마 4종 (Canvas)", APPC, "folder"),
        (1, "later/", "숨긴 기능 (회전 예고 · 가족 모드 · 휴게소 질문)", APPC, "folder"),
        (0, "sim/", "시나리오 생성 · 기준 엔진 · 점검 · 실제 데이터 비교 · README 그래프", SIM, "folder"),
        (0, "shared/", "params.json · test_vectors.csv", SIM, "folder"),
        (0, "docs/", "기획서 · 검증 · 확인 기록 · 이미지", DOC, "folder"),
        (0, "research/", "문헌 근거 · 경쟁 조사", DOC, "folder"),
        (0, "data/rides/", "동승 기록 (로컬 전용)", GREY, "folder"),
        (0, "apk/", "release APK 빌드 위치 (git 제외 · GitHub Releases로 배포)", APPC, "folder"),
        (0, ".github/workflows/ci.yml", "CI: 시뮬 점검 + 테스트 벡터 + Kotlin 테스트 + 디버그 빌드", SIM, "file"),
    ]
    y0, rh = 62, 32
    f.rect(28, 52, 904, rh * len(rows) + 20, CARD, LINE, 14)
    for i, (d, name, role, c, ic) in enumerate(rows):
        y = y0 + i * rh + 16
        x = 48 + d * 34
        if d:
            f.line(48 + 9, y - rh / 2 - 6 if rows[i - 1][0] == 0 else y - rh + 2, 48 + 9, y, "#CBD5E1", 1.5)
            f.line(48 + 9, y, x - 2, y, "#CBD5E1", 1.5)
        f.icon(x + 10, y, ic, c, None, scale=0.8)
        f.text(x + 28, y + 5, name, 14, c, weight=700, family=MONO)
        f.text(310, y + 5, role, 13.5, GREY if c == GREY else INK)
        if i < len(rows) - 1:
            f.line(310, y + rh / 2, 916, y + rh / 2, "#EEF0F5", 1)
    f.save("repo_structure.svg")


def controls_layout():
    f = Fig(960, 400)
    sx, sy, sw, sh = 40, 28, 576, 324
    f.rect(sx, sy, sw, sh, "#F1F5F9", "#94A3B8", 18, 2)
    # top bar
    f.rect(sx + 12, sy + 12, sw - 24, 46, WHITE, LINE, 10)
    f.chip(sx + 26, sy + 19, "멀미 완화", WHITE, ORANGE, 14, 32, pad=18, weight=700)
    f.chip(sx + sw - 26, sy + 19, "설정", INK, "#E5E7EB", 13, 32, pad=16, anchor="end", weight=700)
    # tabs
    f.rect(sx + 12, sy + 68, 250, 36, WHITE, LINE, 10)
    f.rect(sx + 16, sy + 72, 130, 28, INK, None, 8)
    f.text(sx + 81, sy + 91, "지도 (기본)", 13, WHITE, "middle", 700)
    f.text(sx + 202, sy + 91, "분석", 13, INK, "middle", 700)
    f.chip(sx + 272, sy + 74, "고정", MUTED, WHITE, 11.5, 24, pad=9, stroke=LINE)
    # map placeholder
    f.path(f"M{sx + 120} {sy + 210} C{sx + 220} {sy + 190} {sx + 260} {sy + 140} {sx + 380} {sy + 130} S{sx + 500} {sy + 160} {sx + 540} {sy + 120}",
           "#93C5FD", 6)
    f.add(f'<circle cx="{sx + 120}" cy="{sy + 210}" r="8" fill="#2563EB" stroke="#fff" stroke-width="3"/>')
    # demo panel
    py = sy + 234
    f.rect(sx + 12, py, sw - 24, 78, WHITE, LINE, 10)
    x = sx + 24
    row = [["굽은 길 · 거칠게", "굽은 길 · 부드럽게", "도심 · 거칠게"], ["나들이길 (4배속)", "실차 신호", "정지"]]
    for r, items in enumerate(row):
        x = sx + 24
        for it in items:
            fg, bg = (WHITE, "#334155") if it == "실차 신호" else (INK, "#E5E7EB")
            if it == "정지":
                fg, bg = RED, RED_T
            x += f.chip(x, py + 8 + r * 34, it, fg, bg, 12.5, 28, pad=12, weight=700) + 8
    # callouts
    cx = 660
    for x_t, y_target, y_label, title, sub in [(sx + sw - 12, sy + 35, 70, "위쪽", "멀미 완화 · 설정"),
                                               (sx + 320, sy + 86, 150, "머리 줄 탭", "지도 (기본) · 분석, 고정"),
                                               (sx + sw - 12, py + 39, 280, "아래쪽 시연 패널", "시나리오 재생 · 실차 신호 · 정지")]:
        f.path(f"M{x_t} {y_target} L{cx - 14} {y_label - 5}", MUTED, 1.5, dash="3 3")
        f.add(f'<circle cx="{x_t}" cy="{y_target}" r="4" fill="{MUTED}"/>')
        f.text(cx, y_label, title, 15, INK, weight=700)
        f.text(cx, y_label + 20, sub, 12.5, MUTED)
    f.chip(sx, 362, "실차 신호: 앱을 켜면 자동 시작", "#334155", V["car"][1], 12.5, 26, pad=12, weight=700)
    f.save("controls_layout.svg")


def nav_front():
    """Navi in front: the service keeps working; the over-map card needs Pleos."""
    f = Fig(960, 176)
    sx, sy = 28, 28
    f.rect(sx, sy, 200, 120, "#F1F5F9", "#94A3B8", 14, 2)
    f.icon(sx + 100, sy + 46, "map", V["car"][0], WHITE)
    f.text(sx + 100, sy + 96, "Pleos 내비가 앞 화면", 14, INK, "middle", 700)
    f.arrow([(sx + 204, sy + 60), (sx + 240, sy + 60)], MUTED, 2.5)
    out_ink, out_bg = LAYER["out"]
    x = sx + 248
    f.rect(x, sy, 420, 120, out_bg, out_ink, 14, 1.8)
    f.text(x + 20, sy + 30, "주행 지킴이 · 뒤에서 계속", 15, out_ink, weight=700)
    cx = x + 20
    for ic, lab in [("cpu", "계산"), ("wind", "공조"), ("speaker", "음성 안내")]:
        w = 44 + tw(lab, 15) + 20
        f.rect(cx, sy + 52, w, 48, WHITE, out_ink, 24, 1.5)
        f.icon(cx + 24, sy + 76, ic, out_ink, None, scale=0.9)
        f.text(cx + 44, sy + 81, lab, 15, INK, weight=700)
        cx += w + 10
    x2 = x + 436
    f.rect(x2, sy, 932 - x2, 120, WHITE, "#94A3B8", 14, 1.8, "6 4")
    f.text(x2 + 18, sy + 30, "지도 위 알림 카드", 15, "#334155", weight=700)
    f.text(x2 + 18, sy + 54, "Pleos 홈은 시스템 앱 알림만", 13, MUTED)
    f.chip(x2 + 18, sy + 72, "Pleos 협의", "#334155", WHITE, 13, 28, pad=12, stroke="#334155", weight=700)
    f.save("nav_front.svg")


def quick_run():
    """Four steps for a judge: emulator, install, start, demo."""
    f = Fig(960, 190)
    steps = [("power", "에뮬레이터 켜기", ["Pleos Connect v2.1", "Wi-Fi › AndroidWifi"]),
             ("file", "APK 설치", ["GitHub Releases", "adb install"]),
             ("cpu", "앱 실행", ["Pleos 사용자 10", "--user 10"]),
             ("touch", "시연", None)]
    w, gap = 206, 26
    for i, (ic, title, sub) in enumerate(steps):
        x = 28 + i * (w + gap)
        f.rect(x, 28, w, 134, CARD, LINE, 14)
        f.num(x + 26, 56, i + 1, INK, 14, 15)
        f.icon(x + w - 34, 56, ic, LAYER["calc"][0], LAYER["calc"][1], 20)
        f.text(x + 18, 100, title, 16, INK, weight=700)
        if sub:
            f.text(x + 18, 124, sub[0], 13, MUTED)
            f.text(x + 18, 144, sub[1], 13, MUTED, family=MONO if sub[1].startswith(("-", "adb")) else None)
        else:
            f.chip(x + 18, 108, "굽은 길 · 거칠게", INK, "#E5E7EB", 12.5, 24, pad=10, weight=700)
            f.text(x + 18, 152, "→", 14, MUTED, weight=700)
            f.chip(x + 40, 136, "멀미 완화", WHITE, ORANGE, 12.5, 24, pad=12, weight=700)
        if i < 3:
            f.arrow([(x + w + 4, 95), (x + w + gap - 4, 95)], MUTED, 2.5, head=10)
    f.save("quick_run.svg")


def main():
    nav_front()
    quick_run()
    conditions()
    who_gets_sick()
    steering_lead()
    band_filter()
    engine_parity()
    sdk_map()
    validation_ladder()
    real_or_sim()
    relief_sequence()
    extension_map()
    rest_stop_flow()
    repo_structure()
    controls_layout()


if __name__ == "__main__":
    main()

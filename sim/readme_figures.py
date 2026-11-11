"""Figures for the README (plain SVG, no external libraries).

    python3 sim/readme_figures.py      # writes docs/img/*.svg
"""
import csv
import json
from pathlib import Path

import crave_style as cs
from engine import personal_threshold, sickness_probability
from prototype import P, run

ROOT = Path(__file__).resolve().parents[1]
IMG = ROOT / "docs" / "img"

FONT = cs.FONT
INK, MUTED, GRID = cs.INK, cs.MUTED, cs.LINE
FOOT, CARD, BAND, BLUE = cs.FOOT, cs.CARD, cs.BAND, cs.BLUE
INDEX = "#2563EB"          # flicker index I (same blue as the README formula images)
DOSE = "#7C3AED"           # accumulated dose D
VAR = {"I": INDEX, "D": DOSE, "m": "#0D9488", "τ": "#EA580C", "P": "#DC2626", "a": "#DB2777"}


def v(sym, sub=None, size=None):
    """A variable letter as a coloured math tspan (same colours as the formula images)."""
    fs = f' font-size="{size * 1.1:.1f}"' if size else ""
    out = (f'<tspan fill="{VAR[sym]}" font-family="Times New Roman, Times, serif" font-style="italic" '
           f'font-weight="700"{fs}>{sym}</tspan>')
    if sub:
        out += (f'<tspan fill="{VAR[sym]}" font-family="Times New Roman, Times, serif" font-style="italic" '
                f'font-size="{(size or 12) * 0.75:.1f}" dy="3">{sub}</tspan><tspan dy="-3">\u200a</tspan>')
    return out
# one risk scale for the app and the figures: green → yellow → red (yellow is darkened for lines/text on white)
LOW, CAUTION, HIGH = "#16A34A", "#CA8A04", "#DC2626"
APP, NO_APP = cs.INK, "#9AA2B8"   # what the app did: navy (blue is taken by the index I)
KIDS = {"민준": ("#EA580C", "6 4"), "서연": ("#9A3412", "2 4")}   # both tau: orange family, told apart by dash
TH_C, TH_H = "#9AA2B8", cs.INK   # alert thresholds: neutral grey / black dashes (amber and red are stage / P colours)
TAU = VAR["τ"]


class Svg:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" font-family="{FONT}" font-size="13">',
                      f'<defs>{cs.grad_def("cg")}</defs>',
                      f'<rect x="0.5" y="0.5" width="{w - 1}" height="{h - 1}" rx="{cs.RADIUS}" fill="{cs.BG}" stroke="{GRID}" stroke-width="1"/>']

    def add(self, s):
        self.parts.append(s)

    def text(self, x, y, s, size=13, fill=INK, anchor="start", weight=400):
        ls = f' letter-spacing="{cs.TITLE_TRACKING}"' if weight >= 700 and size >= 15 else ""
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{fill}" text-anchor="{anchor}" font-weight="{weight}"{ls}>{s}</text>')

    def title(self, x, y, s, size=17, anchor="start"):
        """Figure heading: navy, extra bold, tight tracking (deck style)."""
        self.text(x, y, s, size, INK, anchor, cs.TITLE_WEIGHT)

    def line(self, x1, y1, x2, y2, stroke=GRID, width=1, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.add(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{stroke}" stroke-width="{width}"{d}/>')

    def poly(self, pts, stroke, width=2, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        p = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
        self.add(f'<polyline points="{p}" fill="none" stroke="{stroke}" stroke-width="{width}" stroke-linejoin="round"{d}/>')

    def rect(self, x, y, w, h, fill, rx=0, stroke=None):
        s = f' stroke="{stroke}" stroke-width="1"' if stroke else ""
        self.add(f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{rx}" fill="{fill}"{s}/>')

    def save(self, name):
        IMG.mkdir(parents=True, exist_ok=True)
        (IMG / name).write_text("\n".join(self.parts + ["</svg>"]))
        print(IMG / name)


class Plot:
    """Time-series axes: minutes on x, one value on y."""

    def __init__(self, svg, x0, y0, w, h, tmax_s, ymin, ymax, yticks, ylabel):
        self.s, self.x0, self.y0, self.w, self.h = svg, x0, y0, w, h
        self.tmax, self.ymin, self.ymax = tmax_s, ymin, ymax
        for v in yticks:
            svg.line(x0, self.Y(v), x0 + w, self.Y(v))
            svg.text(x0 - 8, self.Y(v) + 4, f"{v:g}", 12, FOOT, "end")
        step = 1 if tmax_s <= 600 else 5
        for k in range(0, int(tmax_s // 60) + 1, step):
            svg.text(self.X(k * 60), y0 + h + 18, f"{k}분", 12, FOOT, "middle")
        svg.text(x0, y0 - 10, ylabel, 12, MUTED, weight=600)

    def X(self, t):
        return self.x0 + self.w * t / self.tmax

    def Y(self, v):
        v = min(max(v, self.ymin), self.ymax)
        return self.y0 + self.h * (1 - (v - self.ymin) / (self.ymax - self.ymin))

    def series(self, ts, vs, stroke, width=2, dash=None):
        self.s.poly([(self.X(t), self.Y(v)) for t, v in zip(ts, vs)], stroke, width, dash)


def mmss(seconds):
    """Korean minutes/seconds, e.g. 198 -> '3분 18초'."""
    m, sec = divmod(int(round(seconds)), 60)
    return f"{m}분 {sec}초" if m and sec else (f"{m}분" if m else f"{sec}초")


def legend(svg, x, y, items):
    for label, color, dash in items:
        svg.line(x, y - 4, x + 22, y - 4, color, 3 if not dash else 2, dash)
        svg.text(x + 28, y, label, 12, INK)
        x += 52 + 12 * len(label)


def arrow(s, pts, color=FOOT, width=2, dash=None):
    """Polyline with an arrowhead at the last point."""
    s.poly(pts, color, width, dash)
    (x1, y1), (x2, y2) = pts[-2], pts[-1]
    import math
    a = math.atan2(y2 - y1, x2 - x1)
    h = [(x2, y2), (x2 - 11 * math.cos(a - 0.45), y2 - 11 * math.sin(a - 0.45)),
         (x2 - 11 * math.cos(a + 0.45), y2 - 11 * math.sin(a + 0.45))]
    s.parts[-1] = s.parts[-1].replace('stroke-linejoin="round"', 'stroke-linejoin="round" stroke-linecap="round"')
    s.add('<path d="M' + " L".join(f"{x:.1f} {y:.1f}" for x, y in h) + f' Z" fill="{color}"/>')


# box styles (fill, title ink, border): car side = neutral card, calculation = light blue band, result = brand gradient
CAR = (CARD, INK, GRID)
CALC = (BAND, cs.BLUE_DEEP, None)
OUT = ("#FFFFFF", BLUE, BLUE)


def box(s, x, y, w, h, title, sub=None, fill=CARD, ink=INK, size=14, stroke=None):
    s.rect(x, y, w, h, fill, 12, stroke)
    if sub:
        s.text(x + w / 2, y + h / 2 - 3, title, size, ink, "middle", 700)
        s.text(x + w / 2, y + h / 2 + 16, sub, 12, MUTED, "middle")
    else:
        s.text(x + w / 2, y + h / 2 + 5, title, size, ink, "middle", 700)


def index_flow():
    """How the sickness index is computed, left to right (replaces a mermaid chart)."""
    s = Svg(960, 356)
    s.title(480, 28, "멀미 지수 계산 · 1초마다", 17, "middle")
    car, calc, out = CAR, CALC, OUT
    y, h = 120, 64
    row = [(20, 130, "속도 · 진행 방향", "차 신호", car), (180, 130, "가속도", f"앞뒤 {v('a', 'x', 12)} · 옆 {v('a', 'y', 12)}", calc),
           (340, 140, "느린 흔들림만", "10초 평균 제거", calc),
           (510, 140, f"지수 {v('I', size=13)} · 누적 {v('D', size=13)}", "최근 60초 · 주행 전체", calc),
           (680, 100, "판단", None, calc), (810, 130, "흔들림 단계", f"멀미 가능성 {v('P', size=12)}", out)]
    for x, w, t, sub, (f, ink, st) in row:
        box(s, x, y, w, h, t, sub, f, ink, 13 if len(t) > 7 else 14, st)
    for (x, w, *_), (nx, *_) in zip(row, row[1:]):
        arrow(s, [(x + w + 4, y + h / 2), (nx - 4, y + h / 2)])
    box(s, 20, 44, 130, 44, "핸들 각도", None, car[0], car[1], 13, car[2])
    arrow(s, [(85, 90), (85, y - 4)], BLUE, 2, "6 4")
    s.text(96, 108, "방향을 1–2초 앞당김", 11, BLUE, weight=600)
    box(s, 470, 236, 150, 52, "실내 온도 · 내기순환", None, car[0], car[1], 13, car[2])
    box(s, 650, 236, 120, 52, f"환경 계수 {v('m', size=13)}", "덥고 탁하면 ↓", "#E6F7F5", "#0F766E", size=13)
    box(s, 800, 236, 140, 52, f"개인 기준선 {v('τ', size=13)}", "완화 버튼으로 학습", "#FFF1E6", "#C2410C", size=13)
    arrow(s, [(624, 262), (646, 262)])
    # orthogonal routes straight up into the decision box
    arrow(s, [(710, 234), (710, 214), (718, 214), (718, y + h + 4)])
    arrow(s, [(870, 234), (870, 214), (762, 214), (762, y + h + 4)])
    for x, w, label, sub, ink, fill in [(150, 300, f"지수 {v('I', size=14)} &gt; 기준선 × {v('m', size=14)}", "외기 전환 · 22°C", cs.BLUE_DEEP, BAND),
                                        (510, 300, "멀미 완화 버튼", "창문 · 환기 · 음성", "#C2410C", "#FFF1E6")]:
        s.rect(x, 300, w, 38, fill, 19)
        s.text(x + 20, 324, label, 14, ink, "start", 700)
        s.text(x + w - 20, 324, "→ " + sub, 14, INK, "end", 700)
    s.save("index_flow.svg")


def architecture():
    """Where things run, in plain words: car → background calculation → car; the screen only shows it."""
    s = Svg(960, 400)
    s.title(480, 28, "앱 구조 · 계산은 뒤에서 계속, 화면은 볼 때만", 17, "middle")
    car, calc, ui = ("#FFFFFF", INK), (BAND, cs.BLUE_DEEP), ("#FFFFFF", BLUE)
    s.rect(20, 50, 230, 320, CARD, cs.RADIUS, GRID)
    s.text(135, 78, "Pleos 차량", 15, INK, "middle", cs.TITLE_WEIGHT)
    box(s, 40, 100, 190, 100, "차 신호 읽기", "속도 · 핸들 · 실내 온도 · 내비", *car, stroke=GRID)
    box(s, 40, 250, 190, 100, "차 움직이기", "공기 · 창문 · 음성", *car, stroke=GRID)
    s.rect(300, 50, 640, 320, CARD, cs.RADIUS, GRID)
    s.text(620, 78, "멀미 케어", 15, INK, "middle", cs.TITLE_WEIGHT)
    box(s, 330, 160, 220, 110, "주행 지킴이", "앱을 닫아도 뒤에서 계속 계산", *calc, size=16)
    box(s, 330, 300, 220, 52, "계산 엔진", "시뮬레이터와 같은 계산", *calc)
    box(s, 640, 110, 270, 80, "화면 (지도 · 분석)", "앱을 보고 있을 때만 그림", *ui, stroke=GRID)
    box(s, 640, 240, 270, 80, "알림 카드", "내비를 보는 동안 차가 한 일 안내", *ui, stroke=GRID)
    arrow(s, [(234, 150), (326, 200)], FOOT)
    arrow(s, [(326, 240), (234, 295)], FOOT)
    arrow(s, [(420, 274), (420, 296)], BLUE)
    arrow(s, [(460, 296), (460, 274)], BLUE)
    arrow(s, [(554, 195), (636, 150)], BLUE)
    arrow(s, [(554, 235), (636, 280)], BLUE)
    s.text(262, 146, "1초마다", 12, MUTED, "middle", 600)
    s.text(296, 298, "공기 · 창문", 12, MUTED, "middle", 600)
    s.save("architecture.svg")


def scenario_bars():
    rows = list(csv.DictReader(open(ROOT / "sim" / "out" / "summary.csv")))
    order = [("C_highway", "고속도로"), ("A_city_smooth", "도심 · 부드럽게"), ("A_city_rough", "도심 · 거칠게"),
             ("D_family", "나들이길 (34분)"), ("B_winding_smooth", "굽은 길 · 부드럽게"), ("B_winding_rough", "굽은 길 · 거칠게")]
    by = {r["scenario"]: r for r in rows}
    s = Svg(960, 470)
    s.title(28, 46, "도로 4곳 · 주행 6회 · 같은 엔진", 22)
    # legend chips
    lx = 28
    for kind, label in [("bar", "평균 지수 I"), ("tick", "최댓값"), ("c", "주의 0.9"), ("h", "높음 1.5")]:
        w = 40 + 13 * len(label) * 0.8
        s.rect(lx, 66, w, 28, CARD, 14, GRID)
        if kind == "bar":
            s.rect(lx + 10, 74, 18, 12, LOW, 3)
        elif kind == "tick":
            s.line(lx + 19, 72, lx + 19, 88, INK, 3)
        else:
            s.line(lx + 19, 71, lx + 19, 89, TH_C if kind == "c" else TH_H, 2, "4 3")
        s.text(lx + 34, 85, label, 13, INK)
        lx += w + 8
    s.text(932, 85, "알림 켜진 시간 / 주행 시간", 13, MUTED, "end", 600)
    x0, y0, w, vmax, rh = 220, 116, 470, 3.0, 50
    h = rh * len(order)
    for t in (0, 0.9, 1.5, 3.0):
        x = x0 + w * t / vmax
        if t in (0, 3.0):
            s.line(x, y0 - 4, x, y0 + h, GRID, 1)
        s.text(x, y0 + h + 20, f"{t:g}", 12, FOOT, "middle")
    s.text(x0 + w, y0 + h + 38, "흔들림 지수 (m/s²)", 12, FOOT, "end")
    for i, (key, label) in enumerate(order):
        r = by[key]
        y = y0 + i * rh
        if i % 2 == 0:
            s.rect(28, y, 904, rh, CARD, 10)
        mean, mx = float(r["mean_index"]), float(r["max_index"])
        color = HIGH if mean >= 1.5 else CAUTION if mean >= 0.9 else LOW
        s.text(x0 - 14, y + rh / 2 + 5, label, 14, INK, "end", 700)
        s.rect(x0, y + 14, w * mean / vmax, rh - 28, color, 4)
        s.line(x0 + w * mx / vmax, y + 8, x0 + w * mx / vmax, y + rh - 8, INK, 3)
        s.text(x0 + w * mean / vmax + 8, y + rh / 2 + 5, f"{mean:.2f}", 13, INK, weight=700)
        alert = int(r["caution_s"]) + int(r["high_s"])
        mins = float(r["minutes"])
        frac = min(1.0, alert / (mins * 60))
        bx, bw = 726, 120
        s.rect(bx, y + 18, bw, 14, GRID, 7)
        if alert:
            s.rect(bx, y + 18, max(14, bw * frac), 14, CAUTION if int(r["high_s"]) < int(r["caution_s"]) else HIGH, 7)
        s.text(932, y + rh / 2 + 5, f"{alert / 60:.1f}분 / {mins:g}분" if alert else f"0 / {mins:g}분", 13,
               INK if alert else FOOT, "end", 700 if alert else 400)
    # alert thresholds last, so the dashes stay visible on top of the bars
    for t, col in ((0.9, TH_C), (1.5, TH_H)):
        x = x0 + w * t / vmax
        s.line(x, y0 - 4, x, y0 + h, col, 1.8, "5 4")
    s.save("scenario_bars.svg")


def index_timeline(name, title, out, cues=False):
    rows = run(name)
    ts = [r[0]["t"] for r in rows]
    s = Svg(960, 380)
    # guidance points (turns, bends), not cue count: a red light at a turn earns one extra cue
    turns = next(m["turns"] for m in json.loads((ROOT / "sim" / "scenarios" / "scenarios.json").read_text())
                 if m["name"] == name)
    s.title(30, 30, title.format(min=round(len(rows) / 60), turns=turns), 16)
    pl = Plot(s, 70, 110, 860, 220, ts[-1], 0, 3.4, (0, 1, 2, 3), "흔들림 지수 I (m/s²)")
    bands = {"CAUTION": "#FEF9C3", "HIGH": "#FEE2E2"}
    for (smp, o) in rows:
        if o["state"] in bands:
            s.rect(pl.X(smp["t"]), pl.y0, pl.w / len(rows) + 0.6, pl.h, bands[o["state"]])
    pl.series(ts, [o["th_c"] for _, o in rows], TH_C, 1.8, "6 4")
    pl.series(ts, [o["th_h"] for _, o in rows], TH_H, 1.5, "6 4")
    # events: labelled vertical lines inside the plot
    events = {}
    for smp, o in rows:
        for a in o["actions"]:
            if a.startswith("CUE_") and cues:
                events.setdefault(int(smp["t"]), set()).add("cue")
            elif a == "TTS_DRIVER":
                events.setdefault(int(smp["t"]), set()).add("음성 안내")
            elif a == "HVAC_FRESH":
                events.setdefault(int(smp["t"]), set()).add("공조")
    last_end, row = -1e9, 0
    for t, kinds in sorted(events.items()):
        x = pl.X(t)
        if kinds == {"cue"}:
            s.line(x, pl.y0 + 22, x, pl.y0 + pl.h, MUTED, 1.2, "2 3")
            s.add(f'<circle cx="{x:.1f}" cy="{pl.y0 + 14}" r="7" fill="{INK}" stroke="#fff" stroke-width="2"/>')
            continue
        labels = [k for k in ("음성 안내", "공조") if k in kinds]
        s.line(x, pl.y0, x, pl.y0 + pl.h, APP, 2)
        lab = f"{t}초 " + " · ".join(labels)
        lw = 24 + 12.5 * len(lab) * 0.78
        row = row + 1 if x + 6 < last_end else 0
        ly = pl.y0 + 6 + row * 32
        s.rect(x + 6, ly, lw, 26, APP, 13)
        s.text(x + 6 + lw / 2, ly + 18, lab, 12.5, "#fff", "middle", 700)
        last_end = x + 6 + lw + 4
    pl.series(ts, [o["index"] for _, o in rows], INDEX, 2.4)
    items = [("지수 I", INDEX, None), ("주의 기준 × m", TH_C, "6 4"), ("높음 기준 × m", TH_H, "6 4")]
    legend(s, 70, 62, items)
    x = 930
    chips = [("배경: 주의 · 높음", None)]
    if cues:
        chips.insert(0, ("● 엔진 예고 시점 (앱에서 꺼 둠)", INK))
    for lab, _ in reversed(chips):
        w = 20 + 12 * len(lab) * 0.8
        s.rect(x - w, 46, w, 26, CARD, 13, GRID)
        s.text(x - w / 2, 63, lab, 12, INK, "middle", 600)
        x -= w + 8
    s.save(out)


def closed_loop():
    app, no_app = run("B_winding_rough"), run("B_winding_rough", react=False)
    ts = [r[0]["t"] for r in app]
    s = Svg(960, 350)
    s.title(30, 30, "닫힌 고리 · 굽은 길(26°C, 내기순환으로 출발)에서 공조를 바꿨을 때와 안 바꿨을 때", 16)
    left = Plot(s, 70, 100, 390, 200, ts[-1], 0.8, 1.02, (0.8, 0.9, 1.0), "환경 계수 m (1이면 쾌적)")
    # told apart by colour, not dash: dashes read as threshold lines
    ctl, no_ctl = cs.BLUE, NO_APP
    left.series(ts, [o["m"] for _, o in no_app], no_ctl, 2.4)
    left.series(ts, [o["m"] for _, o in app], ctl, 3.2)
    right = Plot(s, 540, 100, 390, 200, ts[-1], 21, 27, (22, 24, 26), "실내 온도 (°C)")
    right.series(ts, [r[0]["cabin_temp"] for r in no_app], no_ctl, 2.4)
    right.series(ts, [r[0]["cabin_temp"] for r in app], ctl, 3.2)
    t_fresh = next(r[0]["t"] for r in app if "HVAC_FRESH" in r[1]["actions"])
    for pl in (left, right):
        pl.s.line(pl.X(t_fresh), pl.y0, pl.X(t_fresh), pl.y0 + pl.h, MUTED, 1, "2 3")
    s.text(left.X(t_fresh) + 6, left.y0 + 14, f"{int(t_fresh)}초: 외기 전환 · 22°C", 12, MUTED, weight=600)
    lx = 70
    for label, col, wd in (("앱이 공조를 바꿈", ctl, 4), ("앱 없음 (공조 그대로)", no_ctl, 3)):
        s.line(lx, 52, lx + 28, 52, col, wd)
        s.text(lx + 36, 57, label, 13, col if col == ctl else MUTED, weight=700)
        lx += 60 + 13 * len(label)
    m0, m_end, m_no = min(o["m"] for _, o in app), app[-1][1]["m"], no_app[-1][1]["m"]
    s.text(left.x0 + left.w, left.Y(m_end) - 8, f"{m0:.2f} → {m_end:.2f}", 12, ctl, "end", 700)
    s.text(left.x0 + left.w, left.Y(m_no) + 18, f"앱 없으면 {m_no:.3f}에 머묾", 12, MUTED, "end")
    s.save("closed_loop.svg")


def family():
    rows = run("D_family")
    ts = [r[0]["t"] for r in rows]
    taus = {"민준": personal_threshold(P, [20, 22, 24, 21, 23, 22, 25, 20, 24, 23]),
            "서연": personal_threshold(P, [45, 48, 44])}
    s = Svg(960, 370)
    hits = {}
    s.title(30, 30, "확장 · 아이별 기준선 · 같은 나들이길(경부고속도로 → 남한산성로)", 16)
    pl = Plot(s, 70, 100, 800, 220, ts[-1], 0, 45, (0, 15, 30, 45), "누적 흔들림 D")
    for who, tau in taus.items():
        col, dash = KIDS[who]
        pl.series(ts, [tau * max(P["cabinFactorFloor"], o["m"]) for _, o in rows], col, 2, dash)
        t_hit = next((smp["t"] for smp, o in rows if sickness_probability(P, o["dose"], tau, o["m"]) >= 0.5), None)
        s.text(pl.x0 + 6, pl.Y(tau * rows[0][1]["m"]) - 8, f"{who} 기준선 τ = {tau:.1f}", 12, col, weight=700)
        hits[who] = t_hit
        if t_hit is not None:
            s.line(pl.X(t_hit), pl.y0, pl.X(t_hit), pl.y0 + pl.h, col, 1.2, dash)
            s.text(pl.X(t_hit) - 6, pl.y0 + (14 if who == "민준" else 32), f"{who} 도달 {t_hit / 60:.1f}분", 12, col, "end", 700)
    pl.series(ts, [o["dose"] for _, o in rows], DOSE, 2.4)
    legend(s, 70, 56, [("누적 흔들림 D", DOSE, None), ("민준 τ × m", KIDS["민준"][0], KIDS["민준"][1]), ("서연 τ × m", KIDS["서연"][0], KIDS["서연"][1])])
    if hits.get("민준") is not None and hits.get("서연") is not None:
        s.text(930, 56, f"민준(멀미 기록 10번, 약함)에게 {mmss(hits['서연'] - hits['민준'])} 먼저 대응", 12, MUTED, "end")
    s.save("family.svg")


def real_vs_sim():
    rows = {r["source"]: r for r in csv.DictReader(open(ROOT / "sim" / "out" / "public_check.csv"))}
    real = rows["comma2k19 실제 고속도로 (45초)"]
    sim = rows["시뮬 C 고속도로 (같은 45초)"]
    n = int(real["seconds"])
    s = Svg(960, 280)
    s.title(30, 30, f"현실성 점검 · 실제 고속도로(comma2k19, 공개 데이터)와 시뮬 고속도로, 같은 {n}초", 16)
    metrics = [("상위 5% 앞뒤 가속도 (m/s²)", "p95_ax", 1.5), ("평균 흔들림 지수 I", "mean_index", 1.0), ("평균 속도 (km/h)", "mean_speed_kmh", 100)]
    for i, (label, key, vmax) in enumerate(metrics):
        x = 40 + i * 310
        s.text(x, 70, label, 13, INK, weight=700)
        for j, (name, r, color) in enumerate([("실제", real, INK), ("시뮬", sim, INDEX)]):
            v = float(r[key])
            y = 92 + j * 52
            s.text(x, y + 22, name, 13, MUTED)
            s.rect(x + 44, y + 6, 220 * v / vmax, 26, color, 6)
            s.text(x + 50 + 220 * v / vmax, y + 25, f"{v:g}", 13, INK)
    ax = float(sim["p95_ax"]) / float(real["p95_ax"])
    s.text(40, 230, f"같은 {n}초: 앞뒤 가속도 시뮬 {ax:.2f}배, 평균 지수 {float(real['mean_index']):.2f} vs "
           f"{float(sim['mean_index']):.2f} · 둘 다 주의 기준({P['thetaCaution']}) 아래", 13, MUTED)
    s.text(40, 252, "옆 가속도: 비교 불가 (실제 구간이 직선)", 13, MUTED)
    s.save("real_vs_sim.svg")


def main():
    index_flow()
    architecture()
    scenario_bars()
    index_timeline("B_winding_rough", "굽은 길(북악스카이웨이) · 거칠게 {min}분 · 지수가 기준을 넘자 음성 안내 → 공조",
                   "timeline_winding.svg")
    index_timeline("A_city_rough", "확장 · 엔진 안의 회전 예고 · 도심(강남 테헤란로) 거칠게 {min}분, 회전 {turns}곳 + 신호 대기 뒤 한 번 더",
                   "timeline_city.svg", cues=True)
    closed_loop()
    family()
    real_vs_sim()


if __name__ == "__main__":
    main()

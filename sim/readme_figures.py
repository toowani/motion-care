"""Figures for the README (plain SVG, no external libraries).

    python3 sim/readme_figures.py      # writes docs/img/*.svg
"""
import csv
import json
from pathlib import Path

from engine import personal_threshold, sickness_probability
from prototype import P, run

ROOT = Path(__file__).resolve().parents[1]
IMG = ROOT / "docs" / "img"

FONT = "Apple SD Gothic Neo, Noto Sans KR, Malgun Gothic, sans-serif"
INK, MUTED, GRID = "#1F2340", "#6B6E86", "#E3E4EE"
INDEX, CAUTION, HIGH = "#453ECE", "#D97706", "#C2410C"
APP, NO_APP = "#0F766E", "#9CA3AF"
KIDS = {"민준": "#C2410C", "서연": "#0F766E"}


class Svg:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" font-family="{FONT}" font-size="13">',
                      f'<rect width="{w}" height="{h}" fill="#fff"/>']

    def add(self, s):
        self.parts.append(s)

    def text(self, x, y, s, size=13, fill=INK, anchor="start", weight=400):
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{fill}" text-anchor="{anchor}" font-weight="{weight}">{s}</text>')

    def line(self, x1, y1, x2, y2, stroke=GRID, width=1, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.add(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{stroke}" stroke-width="{width}"{d}/>')

    def poly(self, pts, stroke, width=2, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        p = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
        self.add(f'<polyline points="{p}" fill="none" stroke="{stroke}" stroke-width="{width}" stroke-linejoin="round"{d}/>')

    def rect(self, x, y, w, h, fill, rx=0, stroke=None):
        s = f' stroke="{stroke}"' if stroke else ""
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
            svg.text(x0 - 8, self.Y(v) + 4, f"{v:g}", 12, MUTED, "end")
        step = 1 if tmax_s <= 600 else 5
        for k in range(0, int(tmax_s // 60) + 1, step):
            svg.text(self.X(k * 60), y0 + h + 18, f"{k}분", 12, MUTED, "middle")
        svg.text(x0, y0 - 10, ylabel, 12, MUTED)

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


def how_it_works():
    s = Svg(960, 450)
    cols = [
        (30, "차가 아는 것", "#EEF2FF", "#3730A3",
         ["방향지시등 · 핸들 각도", "내비 다음 회전 거리", "속도 · 진행 방향", "실내 온도 · 내기순환", "뒷좌석 탑승"]),
        (350, "앱이 계산하는 것", "#F5F3FF", "#5B21B6",
         ["흔들림 지수 I (최근 1분)", "누적 흔들림 D", "환경 계수 m (덥고 탁하면 ↓)", "아이별 기준선 τ", "회전까지 남은 시간"]),
        (670, "차가 하는 일", "#ECFDF5", "#065F46",
         ["예고: 알림음 · 목소리 · 무드등", "외기 전환 · 22°C", "휴게소 경유지 추가", "가족 모드: 약한 아이 기준", "정차 후 한 질문으로 학습"]),
    ]
    for x, title, fill, ink, items in cols:
        s.rect(x, 40, 260, 300, fill, 14)
        s.text(x + 130, 72, title, 16, ink, "middle", 700)
        for i, it in enumerate(items):
            s.rect(x + 18, 92 + i * 47, 224, 36, "#fff", 8)
            s.text(x + 130, 115 + i * 47, it, 13, INK, "middle")
    for x in (290, 610):
        s.add(f'<path d="M{x + 6} 190 L{x + 52} 190" stroke="{MUTED}" stroke-width="3" fill="none"/>'
              f'<path d="M{x + 44} 182 L{x + 56} 190 L{x + 44} 198" fill="{MUTED}"/>')
    # closed loop: fresh air changes what the car reports
    s.add(f'<path d="M800 340 L800 385 L160 385 L160 340" stroke="{APP}" stroke-width="2.5" fill="none" stroke-dasharray="7 5"/>'
          f'<path d="M152 350 L160 338 L168 350" fill="{APP}"/>')
    s.text(480, 378, "닫힌 고리: 공조를 바꾸면 실내 온도·환기가 바뀌고, 그 값이 다시 계산에 들어갑니다", 13, APP, "middle")
    s.text(480, 24, "멀미 케어가 하는 일 · Pleos SDK로 차 신호를 읽고 차를 움직입니다", 15, INK, "middle", 700)
    s.text(480, 432, "판단은 백그라운드 서비스에서 계속 돕니다 · 내비가 앞 화면이어도 소리·무드등·공조로 안내", 13, MUTED, "middle")
    s.save("how_it_works.svg")


def arrow(s, pts, color=MUTED, width=2.5, dash=None):
    """Polyline with an arrowhead at the last point."""
    s.poly(pts, color, width, dash)
    (x1, y1), (x2, y2) = pts[-2], pts[-1]
    import math
    a = math.atan2(y2 - y1, x2 - x1)
    h = [(x2, y2), (x2 - 11 * math.cos(a - 0.45), y2 - 11 * math.sin(a - 0.45)),
         (x2 - 11 * math.cos(a + 0.45), y2 - 11 * math.sin(a + 0.45))]
    s.add('<path d="M' + " L".join(f"{x:.1f} {y:.1f}" for x, y in h) + f' Z" fill="{color}"/>')


def box(s, x, y, w, h, title, sub=None, fill="#F5F3FF", ink="#5B21B6", size=14):
    s.rect(x, y, w, h, fill, 12)
    if sub:
        s.text(x + w / 2, y + h / 2 - 3, title, size, ink, "middle", 700)
        s.text(x + w / 2, y + h / 2 + 16, sub, 12, MUTED, "middle")
    else:
        s.text(x + w / 2, y + h / 2 + 5, title, size, ink, "middle", 700)


def index_flow():
    """How the sickness index is computed, left to right (replaces a mermaid chart)."""
    s = Svg(960, 330)
    s.text(480, 26, "멀미 지수 계산 · 1초마다", 15, INK, "middle", 700)
    car, calc, out = ("#EEF2FF", "#3730A3"), ("#F5F3FF", "#5B21B6"), ("#ECFDF5", "#065F46")
    y, h = 120, 64
    row = [(20, 130, "속도 · 진행 방향", "차 신호", car), (180, 130, "가속도", "앞뒤 aₓ · 옆 aᵧ", calc),
           (340, 140, "느린 흔들림만", "10초 평균 제거", calc), (510, 140, "지수 I · 누적 D", "최근 60초 · 주행 전체", calc),
           (680, 100, "판단", None, calc), (810, 130, "흔들림 단계", "멀미 가능성 P (아이별)", out)]
    for x, w, t, sub, (f, ink) in row:
        box(s, x, y, w, h, t, sub, f, ink, 13 if len(t) > 7 else 14)
    for (x, w, *_), (nx, *_) in zip(row, row[1:]):
        arrow(s, [(x + w + 4, y + h / 2), (nx - 4, y + h / 2)])
    box(s, 20, 44, 130, 44, "핸들 각도", None, *car, size=13)
    arrow(s, [(85, 90), (85, y - 4)], "#3730A3", 2, "6 4")
    s.text(96, 108, "방향을 1–2초 앞당김", 11, "#3730A3")
    box(s, 470, 236, 150, 52, "실내 온도 · 내기순환", None, *car, size=13)
    box(s, 650, 236, 120, 52, "환경 계수 m", "덥고 탁하면 ↓", *calc, size=13)
    box(s, 800, 236, 140, 52, "아이별 기준선 τ", "정차 후 질문으로 학습", *calc, size=13)
    arrow(s, [(624, 262), (646, 262)])
    arrow(s, [(710, 234), (725, y + h + 4)])
    arrow(s, [(870, 234), (750, y + h + 4)])
    s.text(480, 318, "기준선 × m을 넘으면 주의·위험 · 가족 모드는 확률 P가 0.5를 넘는 아이가 생기면 대응", 13, MUTED, "middle")
    s.save("index_flow.svg")


def architecture():
    """Where things run: car signals → TripService → car actions; the screen only attaches (replaces a mermaid chart)."""
    s = Svg(960, 400)
    s.text(480, 26, "앱 구조 · 판단은 백그라운드 서비스, 화면은 보일 때만 붙음", 15, INK, "middle", 700)
    car, calc, ui = ("#EEF2FF", "#3730A3"), ("#F5F3FF", "#5B21B6"), ("#ECFDF5", "#065F46")
    s.rect(20, 50, 230, 320, "#F8FAFC", 16, GRID)
    s.text(135, 78, "Pleos 차량", 15, INK, "middle", 700)
    box(s, 40, 100, 190, 100, "읽기", "Vehicle · NaviHelper · 위치", *car)
    box(s, 40, 250, 190, 100, "행동", "공조 · 무드등 · TTS · 내비", *car)
    s.rect(300, 50, 640, 320, "#FBFAFF", 16, GRID)
    s.text(620, 78, "멀미 케어", 15, INK, "middle", 700)
    box(s, 330, 160, 220, 110, "TripService", "포그라운드 서비스 · 내비가 앞이어도 계속", *calc, size=16)
    box(s, 330, 300, 220, 52, "core 엔진", "순수 Kotlin · 파이썬과 숫자 일치", *calc)
    box(s, 640, 110, 270, 80, "화면 (주행 · 분석 · 지도)", "앱이 보일 때만 붙어서 그림", *ui)
    box(s, 640, 240, 270, 80, "알림 카드", "앱이 안 보일 때 질문 · 응/아니", *ui)
    arrow(s, [(234, 150), (326, 200)], "#3730A3")
    arrow(s, [(326, 240), (234, 295)], "#3730A3")
    arrow(s, [(420, 274), (420, 296)], "#5B21B6")
    arrow(s, [(460, 296), (460, 274)], "#5B21B6")
    arrow(s, [(554, 195), (636, 150)], APP)
    arrow(s, [(554, 235), (636, 280)], APP)
    s.text(262, 146, "1초마다", 12, MUTED, "middle")
    s.text(300, 298, "예고 · 공조", 12, MUTED, "middle")
    s.save("architecture.svg")


def scenario_bars():
    rows = list(csv.DictReader(open(ROOT / "sim" / "out" / "summary.csv")))
    order = [("C_highway", "고속도로"), ("A_city_smooth", "도심 · 부드럽게"), ("A_city_rough", "도심 · 거칠게"),
             ("B_winding_smooth", "굽은 길 · 부드럽게"), ("B_winding_rough", "굽은 길 · 거칠게")]
    by = {r["scenario"]: r for r in rows}
    s = Svg(960, 360)
    s.text(30, 30, "같은 엔진, 다섯 가지 도로 · 평균 흔들림 지수와 알림이 켜진 시간", 15, INK, weight=700)
    x0, y0, w, h, vmax = 200, 60, 520, 250, 3.0
    for v in (0, 0.9, 1.5, 3.0):
        x = x0 + w * v / vmax
        s.line(x, y0 - 6, x, y0 + h, GRID if v in (0, 3.0) else (CAUTION if v == 0.9 else HIGH), 1, None if v in (0, 3.0) else "5 4")
        s.text(x, y0 + h + 18, f"{v:g}", 12, MUTED, "middle")
    s.text(x0 + w * 0.9 / vmax + 4, y0 + h + 34, "주의 0.9", 12, CAUTION)
    s.text(x0 + w * 1.5 / vmax + 4, y0 + h + 34, "높음 1.5", 12, HIGH)
    bh = h / len(order)
    for i, (key, label) in enumerate(order):
        r = by[key]
        y = y0 + i * bh + 10
        mean, mx = float(r["mean_index"]), float(r["max_index"])
        color = HIGH if mean >= 1.5 else CAUTION if mean >= 0.9 else APP
        s.text(x0 - 12, y + bh / 2, label, 13, INK, "end")
        s.rect(x0, y + 6, w * mean / vmax, bh - 30, color, 4)
        s.line(x0 + w * mx / vmax, y + 2, x0 + w * mx / vmax, y + bh - 20, INK, 2)
        s.text(x0 + w * mean / vmax + 6, y + bh / 2 - 2, f"{mean:.2f}", 12, INK)
        alert = int(r["caution_s"]) + int(r["high_s"])
        mins = float(r["minutes"])
        s.text(760, y + bh / 2 - 2, f"알림 {alert}초 / {mins:g}분", 13, INK if alert else MUTED)
    s.text(760, y0 - 10, "알림이 켜진 시간", 12, MUTED)
    s.text(x0, y0 - 10, "막대: 평균 지수 I · 세로선: 최댓값 (m/s²)", 12, MUTED)
    s.save("scenario_bars.svg")


def index_timeline(name, title, out):
    rows = run(name)
    ts = [r[0]["t"] for r in rows]
    s = Svg(960, 370)
    # guidance points (turns, bends), not cue count: a red light at a turn earns one extra cue
    turns = next(m["turns"] for m in json.loads((ROOT / "sim" / "scenarios" / "scenarios.json").read_text())
                 if m["name"] == name)
    s.text(30, 28, title.format(min=round(len(rows) / 60), turns=turns), 15, INK, weight=700)
    pl = Plot(s, 70, 100, 860, 220, ts[-1], 0, 3.4, (0, 1, 2, 3), "흔들림 지수 I (m/s²)")
    bands = {"CAUTION": "#FEF3C7", "HIGH": "#FDE2D3"}
    for (smp, o) in rows:
        if o["state"] in bands:
            s.rect(pl.X(smp["t"]), pl.y0, pl.w / len(rows) + 0.6, pl.h, bands[o["state"]])
    pl.series(ts, [o["th_c"] for _, o in rows], CAUTION, 1.5, "6 4")
    pl.series(ts, [o["th_h"] for _, o in rows], HIGH, 1.5, "6 4")
    pl.series(ts, [o["index"] for _, o in rows], INDEX, 2.2)
    # one row per action type so markers at the same second don't overlap
    marks = {"CUE": ("●", INK, 0), "TTS_DRIVER": ("◆", INDEX, 1), "HVAC_FRESH": ("■", APP, 2), "SUGGEST_REST_STOP": ("▲", HIGH, 3)}
    for smp, o in rows:
        for a in o["actions"]:
            key = "CUE" if a.startswith("CUE_") else a
            if key in marks:
                g, c, row = marks[key]
                s.text(pl.X(smp["t"]), pl.y0 + 14 + row * 14, g, 12, c, "middle")
    legend(s, 70, 56, [("지수 I", INDEX, None), ("주의 기준 × m", CAUTION, "6 4"), ("높음 기준 × m", HIGH, "6 4")])
    s.text(930, 56, "● 회전·커브 예고  ◆ 운전자 안내  ■ 공조 외기 전환  ▲ 휴게소 제안 · 배경: 주의/높음", 12, MUTED, "end")
    s.save(out)


def closed_loop():
    app, no_app = run("B_winding_rough"), run("B_winding_rough", react=False)
    ts = [r[0]["t"] for r in app]
    s = Svg(960, 350)
    s.text(30, 28, "닫힌 고리 · 굽은 길(26°C, 내기순환으로 출발)에서 공조를 바꿨을 때와 안 바꿨을 때", 15, INK, weight=700)
    left = Plot(s, 70, 100, 390, 200, ts[-1], 0.8, 1.02, (0.8, 0.9, 1.0), "환경 계수 m (1이면 쾌적)")
    left.series(ts, [o["m"] for _, o in no_app], NO_APP, 2.2, "6 4")
    left.series(ts, [o["m"] for _, o in app], APP, 2.4)
    right = Plot(s, 540, 100, 390, 200, ts[-1], 21, 27, (22, 24, 26), "실내 온도 (°C)")
    right.series(ts, [r[0]["cabin_temp"] for r in no_app], NO_APP, 2.2, "6 4")
    right.series(ts, [r[0]["cabin_temp"] for r in app], APP, 2.4)
    t_fresh = next(r[0]["t"] for r in app if "HVAC_FRESH" in r[1]["actions"])
    for pl in (left, right):
        pl.s.line(pl.X(t_fresh), pl.y0, pl.X(t_fresh), pl.y0 + pl.h, APP, 1, "2 3")
    s.text(left.X(t_fresh) + 6, left.y0 + 14, f"{int(t_fresh)}초: 외기 전환 · 22°C", 12, APP)
    legend(s, 70, 56, [("앱이 공조를 바꿈", APP, None), ("앱 없음", NO_APP, "6 4")])
    m0, m_end, m_no = min(o["m"] for _, o in app), app[-1][1]["m"], no_app[-1][1]["m"]
    s.text(left.x0 + left.w, left.Y(m_end) - 8, f"{m0:.2f} → {m_end:.2f}", 12, APP, "end")
    s.text(left.x0 + left.w, left.Y(m_no) + 18, f"{m_no:.2f}에 머묾", 12, MUTED, "end")
    s.save("closed_loop.svg")


def family():
    rows = run("D_family")
    ts = [r[0]["t"] for r in rows]
    taus = {"민준": personal_threshold(P, [20, 22, 24, 21, 23, 22, 25, 20, 24, 23]),
            "서연": personal_threshold(P, [45, 48, 44])}
    s = Svg(960, 370)
    hits = {}
    s.text(30, 28, "가족 모드 · 같은 나들이길(경부고속도로 → 남한산성로), 아이마다 다른 기준선", 15, INK, weight=700)
    pl = Plot(s, 70, 100, 800, 220, ts[-1], 0, 45, (0, 15, 30, 45), "누적 흔들림 D")
    for who, tau in taus.items():
        pl.series(ts, [tau * max(P["cabinFactorFloor"], o["m"]) for _, o in rows], KIDS[who], 1.6, "6 4")
        t_hit = next((smp["t"] for smp, o in rows if sickness_probability(P, o["dose"], tau, o["m"]) >= 0.5), None)
        s.text(pl.x0 + 6, pl.Y(tau * rows[0][1]["m"]) - 8, f"{who} 기준선 τ = {tau:.1f}", 12, KIDS[who])
        hits[who] = t_hit
        if t_hit is not None:
            s.line(pl.X(t_hit), pl.y0, pl.X(t_hit), pl.y0 + pl.h, KIDS[who], 1, "2 3")
            s.text(pl.X(t_hit) - 6, pl.y0 + (14 if who == "민준" else 32), f"{who} 도달 {t_hit / 60:.1f}분", 12, KIDS[who], "end")
    pl.series(ts, [o["dose"] for _, o in rows], INDEX, 2.4)
    legend(s, 70, 56, [("누적 흔들림 D", INDEX, None), ("아이별 기준선 × 환경 계수", MUTED, "6 4")])
    if hits.get("민준") is not None and hits.get("서연") is not None:
        s.text(930, 56, f"민준(멀미 기록 10번, 약함)에게 {mmss(hits['서연'] - hits['민준'])} 먼저 대응", 12, MUTED, "end")
    s.save("family.svg")


def real_vs_sim():
    rows = {r["source"]: r for r in csv.DictReader(open(ROOT / "sim" / "out" / "public_check.csv"))}
    real = rows["comma2k19 실제 고속도로 (45초)"]
    sim = rows["시뮬 C 고속도로 (같은 45초)"]
    n = int(real["seconds"])
    s = Svg(960, 280)
    s.text(30, 28, f"현실성 점검 · 실제 고속도로(comma2k19, 공개 데이터)와 시뮬 고속도로, 같은 {n}초", 15, INK, weight=700)
    metrics = [("상위 5% 앞뒤 가속도 (m/s²)", "p95_ax", 1.5), ("평균 흔들림 지수 I", "mean_index", 1.0), ("평균 속도 (km/h)", "mean_speed_kmh", 100)]
    for i, (label, key, vmax) in enumerate(metrics):
        x = 40 + i * 310
        s.text(x, 70, label, 13, INK, weight=700)
        for j, (name, r, color) in enumerate([("실제", real, INK), ("시뮬", sim, INDEX)]):
            v = float(r[key])
            y = 92 + j * 52
            s.text(x, y + 22, name, 13, MUTED)
            s.rect(x + 44, y + 6, 220 * v / vmax, 26, color, 4)
            s.text(x + 50 + 220 * v / vmax, y + 25, f"{v:g}", 13, INK)
    ax = float(sim["p95_ax"]) / float(real["p95_ax"])
    s.text(40, 230, f"같은 {n}초: 앞뒤 가속도 시뮬 {ax:.2f}배, 평균 지수 {float(real['mean_index']):.2f} vs "
           f"{float(sim['mean_index']):.2f} · 둘 다 주의 기준({P['thetaCaution']}) 아래", 13, MUTED)
    s.text(40, 252, "옆 가속도: 비교 불가 (실제 구간이 직선)", 13, MUTED)
    s.save("real_vs_sim.svg")


def main():
    how_it_works()
    index_flow()
    architecture()
    scenario_bars()
    index_timeline("B_winding_rough", "굽은 길(북악스카이웨이) · 거칠게 {min}분 · 지수가 기준을 넘자 안내 → 공조 → 휴게소 제안",
                   "timeline_winding.svg")
    index_timeline("A_city_rough", "도심(강남 테헤란로) · 거칠게 {min}분 · 회전 {turns}곳마다 미리 예고, 신호 대기 뒤 한 번 더", "timeline_city.svg")
    closed_loop()
    family()
    real_vs_sim()


if __name__ == "__main__":
    main()

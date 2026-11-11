"""Frequency response of the engine's acceleration path, measured with sine inputs.

    python3 sim/freq_response.py            # table at the report frequencies
    python3 sim/freq_response.py --all      # all 32 log-spaced frequencies
    python3 sim/freq_response.py --svg      # docs/img/freq_response.svg (then sim/embed_font.py)

A pure lateral (or fore-aft) acceleration sine of 1 m/s^2 is turned into the 1 Hz car
signals the engine reads (bearing at constant 15 m/s, or speed), run through Engine.step,
and the RMS of the engine's a_y (a_x) over whole periods after settling is compared with
the RMS of the true acceleration at the same sample instants. Gain in dB.

Reference: lateral motion-sickness weighting of Donohew & Griffin (2004), flat
0.0315-0.25 Hz then -12 dB per octave (asymptote, 0 dB inside the band); below 0.0315 Hz
it is not defined (the study starts there), printed as "-".

"Before" is the engine up to 11.10, rebuilt here for the figure: EMA 0.5 on speed and yaw
rate, then a minus the mean of the last 10 s.
"""
import json
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from collections import deque  # noqa: E402

from engine import Engine, MotionEstimator  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
P = json.loads((ROOT / "shared" / "params.json").read_text())
REPORT = [0.01, 0.0315, 0.05, 0.1, 0.15, 0.2, 0.25, 0.5]
V0 = 15.0


def literature_db(f):
    if f < 0.0315 - 1e-9:
        return None
    return 0.0 if f <= 0.25 else -40 * math.log10(f / 0.25)


class OldChain:
    """The acceleration path before the band filter (until 11.10)."""

    def __init__(self, p):
        self.motion = MotionEstimator(dict(p, speedEmaAlpha=0.5))
        self.bx, self.by = deque(maxlen=10), deque(maxlen=10)

    def step(self, s):
        ax, ay = self.motion.step(s["speed"], s["bearing"], 1.0)
        self.bx.append(ax)
        self.by.append(ay)
        return {"ax": ax - sum(self.bx) / len(self.bx), "ay": ay - sum(self.by) / len(self.by)}


def gain_db(f, axis="y", p=P, chain=Engine):
    eng = chain(p)
    w = 2 * math.pi * f
    ph = math.pi / 4  # keeps the 0.5 Hz (Nyquist) samples non-zero
    period = 1 / f
    settle = max(300.0, 6 * period)
    n_meas = int(round(max(600.0, 10 * period) * f)) / f  # whole periods
    n = int(settle + n_meas)
    out_sq = in_sq = 0.0
    for t in range(n + 1):
        a = math.sin(w * t + ph)
        if axis == "y":
            # yaw rate = a / v; bearing (deg, clockwise = right turn = +a_y)
            heading = -math.cos(w * t + ph) / (V0 * w)
            s = {"speed": V0, "bearing": math.degrees(heading) % 360}
        else:
            s = {"speed": 30.0 - math.cos(w * t + ph) / w, "bearing": 90.0}
        s.update(t=float(t), turn_signal="OFF", next_turn_m=-1, next_turn_dir="",
                 cabin_temp=P["cabinT0"], recirc=0, window_open=0)
        o = eng.step(s)
        if t > settle:
            out_sq += o["a" + axis] ** 2
            in_sq += a * a
    return 10 * math.log10(out_sq / in_sq)


def sweep(freqs, axis="y"):
    return [(f, gain_db(f, axis), literature_db(f)) for f in freqs]


def figure(path):
    """Gain vs frequency: before (grey), now (blue), literature weighting (dashed), log x 0.01-2 Hz."""
    import crave_style as cs
    W, H = 960, 440
    x0, x1, y0, y1 = 92, 900, 92, 360          # plot box
    fmin, fmax, dbmin, dbmax = 0.01, 2.0, -25.0, 5.0
    X = lambda f: x0 + (x1 - x0) * math.log10(f / fmin) / math.log10(fmax / fmin)
    Y = lambda db: y0 + (y1 - y0) * (dbmax - max(dbmin, min(dbmax, db))) / (dbmax - dbmin)
    grey, blue = "#9AA2B8", cs.BLUE
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}" '
           f'font-family="{cs.FONT}" font-size="13">',
           f'<rect x="0.5" y="0.5" width="{W - 1}" height="{H - 1}" rx="{cs.RADIUS + 4}" fill="{cs.BG}" '
           f'stroke="{cs.LINE}" stroke-width="1"/>']

    def text(x, y, s, size=13, fill=cs.INK, anchor="start", weight=400):
        ls = f' letter-spacing="{cs.TITLE_TRACKING}"' if weight >= 800 else ""
        out.append(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{fill}" text-anchor="{anchor}" '
                   f'font-weight="{weight}"{ls}>{s}</text>')

    def line(xa, ya, xb, yb, stroke=cs.LINE, width=1, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        out.append(f'<line x1="{xa:.1f}" y1="{ya:.1f}" x2="{xb:.1f}" y2="{yb:.1f}" stroke="{stroke}" '
                   f'stroke-width="{width}"{d}/>')

    def poly(pts, stroke, width, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        p = " ".join(f"{X(f):.1f},{Y(g):.1f}" for f, g in pts)
        out.append(f'<polyline points="{p}" fill="none" stroke="{stroke}" stroke-width="{width}" '
                   f'stroke-linejoin="round" stroke-linecap="round"{d}/>')

    text(32, 44, "엔진 주파수 응답 · 옆 흔들림 사인파 입력", 20, cs.INK, weight=800)
    text(32, 68, "1 m/s² 사인파를 1 Hz 차 신호로 넣고 엔진이 낸 가속도의 크기 (dB, 0 = 그대로)", 13, cs.MUTED)
    # beyond Nyquist
    out.append(f'<rect x="{X(0.5):.1f}" y="{y0}" width="{x1 - X(0.5):.1f}" height="{y1 - y0}" fill="{cs.CARD_ALT}"/>')
    text((X(0.5) + x1) / 2, y0 + 22, "1초 샘플로 볼 수 없음", 12.5, cs.FOOT, "middle", 600)
    for db in range(-25, 6, 5):
        line(x0, Y(db), x1, Y(db), cs.LINE, 1)
        text(x0 - 10, Y(db) + 4, f"{db:+d}" if db else "0", 12, cs.FOOT, "end")
    text(x0 - 40, (y0 + y1) / 2, "dB", 12, cs.MUTED, "end", 600)
    for f in [0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0]:
        line(X(f), y0, X(f), y1, cs.LINE, 1)
        text(X(f), y1 + 20, f"{f:g}", 12, cs.FOOT, "middle")
    text(x1, y1 + 40, "주파수 (Hz, 로그 눈금)", 12, cs.MUTED, "end", 600)
    line(X(0.5), y0, X(0.5), y1, cs.INK, 1.4, "4 4")
    text(X(0.5) + 6, y1 - 10, "0.5 Hz 나이퀴스트", 12, cs.INK, "start", 700)
    # literature weighting (Donohew & Griffin 2004), defined 0.0315-0.8 Hz
    lit = [(f, literature_db(f)) for f in [0.0315 * (0.8 / 0.0315) ** (k / 80) for k in range(81)]]
    poly(lit, cs.INK, 2, "7 5")
    freqs = [fmin * (0.5 / fmin) ** (k / 60) for k in range(61)]
    poly([(f, gain_db(f, chain=OldChain)) for f in freqs], grey, 3)
    poly([(f, gain_db(f)) for f in freqs], blue, 3.5)
    # legend
    lx, ly = 352, y1 - 70
    out.append(f'<rect x="{lx - 14}" y="{ly - 20}" width="330" height="88" rx="{cs.RADIUS_SM}" fill="{cs.BG}" '
               f'stroke="{cs.LINE}"/>')
    for k, (lab, col, width, dash) in enumerate([("지금 엔진 (멀미 대역 필터)", blue, 3.5, None),
                                                 ("예전 엔진 (지수 이동 평균 + 10초 평균 빼기)", grey, 3, None),
                                                 ("문헌 옆 흔들림 가중 (Donohew &amp; Griffin 2004)", cs.INK, 2, "7 5")]):
        y = ly + k * 24
        d = f' stroke-dasharray="{dash}"' if dash else ""
        out.append(f'<line x1="{lx}" y1="{y:.1f}" x2="{lx + 28}" y2="{y:.1f}" stroke="{col}" stroke-width="{width}"{d}/>')
        text(lx + 38, y + 4.5, lab, 12.5, cs.INK if k == 0 else cs.MUTED, "start", 700 if k == 0 else 400)
    out.append("</svg>")
    Path(path).write_text("\n".join(out) + "\n", encoding="utf-8")
    print(path)


def main():
    if "--svg" in sys.argv:
        figure(ROOT / "docs" / "img" / "freq_response.svg")
        return
    freqs = REPORT
    if "--all" in sys.argv:
        freqs = [0.01 * (0.5 / 0.01) ** (k / 31) for k in range(32)]
    print(f"{'f (Hz)':>8} {'before':>7} {'lateral':>8} {'fore-aft':>9} {'literature':>11} {'lat - lit':>10}")
    for f in freqs:
        gb, gy, gx, lit = gain_db(f, chain=OldChain), gain_db(f, "y"), gain_db(f, "x"), literature_db(f)
        lit_s = "-" if lit is None else f"{lit:.1f}"
        d_s = "-" if lit is None else f"{gy - lit:+.1f}"
        print(f"{f:8.4f} {gb:7.1f} {gy:8.1f} {gx:9.1f} {lit_s:>11} {d_s:>10}")


if __name__ == "__main__":
    main()

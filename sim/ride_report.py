"""Real ride check, step 2: join every ride with its survey answers and see whether the
predicted dose D tracks how sick the rider actually felt. Survey format: docs/ride-study.md
(example: docs/ride-survey-example.csv).

    python3 sim/ride_report.py                       # data/rides/survey.csv + data/rides/<ride_id>.gpx|.csv
    python3 sim/ride_report.py --survey S.csv --rides DIR --out DIR
Outputs (in --out, default data/rides/, which stays out of git): report.csv, report.svg
"""
import csv
import math
import sys
from pathlib import Path

from import_ride import P, analyze, load_ride
from readme_figures import Svg, INK, MUTED, GRID, INDEX, CAUTION, HIGH, APP

ROOT = Path(__file__).resolve().parents[1]
RIDES = ROOT / "data" / "rides"
ROAD_COLOR = {"winding": HIGH, "city": INDEX, "highway": APP}
ROAD_LABEL = {"winding": "굽은 길", "city": "도심", "highway": "고속도로"}


def pearson(xs, ys):
    n = len(xs)
    if n < 3:
        return None
    mx, my = sum(xs) / n, sum(ys) / n
    sxy = sum((x - mx) * (y - my) for x, y in zip(xs, ys))
    sxx = sum((x - mx) ** 2 for x in xs)
    syy = sum((y - my) ** 2 for y in ys)
    return sxy / math.sqrt(sxx * syy) if sxx and syy else None


def ranks(xs):
    order = sorted(range(len(xs)), key=lambda i: xs[i])
    r = [0.0] * len(xs)
    i = 0
    while i < len(order):
        j = i
        while j + 1 < len(order) and xs[order[j + 1]] == xs[order[i]]:
            j += 1
        for k in range(i, j + 1):                 # ties share the average rank
            r[order[k]] = (i + j) / 2 + 1
        i = j + 1
    return r


def spearman(xs, ys):
    return pearson(ranks(xs), ranks(ys))


def fmt_r(r):
    return "-" if r is None else f"{r:+.2f}"


def ride_file(rides, ride_id):
    for ext in (".gpx", ".GPX", ".csv"):
        p = rides / f"{ride_id}{ext}"
        if p.exists():
            return p
    return None


def build(survey, rides):
    out = []
    for s in csv.DictReader(open(survey, encoding="utf-8-sig")):
        path = ride_file(rides, s["ride_id"])
        if path is None:
            print(f"  skip {s['ride_id']}: no {s['ride_id']}.gpx/.csv in {rides}")
            continue
        start = float(s["start_s"]) if s.get("start_s") else None
        end = float(s["end_s"]) if s.get("end_s") else None
        samples, info = load_ride(path, start=start, end=end)
        r = analyze(samples)
        onset = float(s["onset_min"]) * 60 if s.get("onset_min") else None
        d_onset = r["dose_curve"][min(len(samples) - 1, int(onset))] if onset is not None else None
        alerts = [t for t in (r["first_caution"], r["first_high"]) if t is not None]
        lead = round((onset - min(alerts)) / 60, 1) if onset is not None and alerts else ""
        out.append({"ride_id": s["ride_id"], "nickname": s["nickname"], "road": s["road"], "style": s["style"],
                    "score": float(s["score"]), "misc": int(s["misc"]),
                    "minutes": r["minutes"], "mean_index": r["mean_index"], "max_index": r["max_index"],
                    "dose": r["dose"], "alert_s": r["caution_s"] + r["high_s"],
                    "onset_min": s.get("onset_min", ""), "dose_at_onset": "" if d_onset is None else round(d_onset, 1),
                    "alert_lead_min": lead,
                    "dropped": info["dropped"], "speed_from": info["source"]})
    return out


def scatter(rows, r_p, r_s, path):
    s = Svg(720, 480)
    x0, y0, w, h = 80, 70, 580, 330
    dmax = max(10.0, max(r["dose"] for r in rows) * 1.15)
    step = 10 if dmax <= 80 else 20
    X = lambda d: x0 + w * d / dmax
    Y = lambda v: y0 + h * (1 - v / 10)
    s.text(30, 30, f"예측 누적량 D와 실제 멀미 점수 · 탑승 {len(rows)}회", 15, INK, weight=700)
    s.text(30, 50, f"Pearson r = {fmt_r(r_p)} · Spearman ρ = {fmt_r(r_s)}"
           + (" · 10회 미만이라 참고용" if len(rows) < 10 else ""), 12, MUTED)
    for v in range(0, 11, 2):
        s.line(x0, Y(v), x0 + w, Y(v))
        s.text(x0 - 8, Y(v) + 4, f"{v}", 12, MUTED, "end")
    for k in range(0, int(dmax) + 1, step):
        s.line(X(k), y0, X(k), y0 + h, GRID)
        s.text(X(k), y0 + h + 18, f"{k}", 12, MUTED, "middle")
    s.text(x0 + w / 2, y0 + h + 40, "누적 흔들림 D (앱 엔진, m/s^1.5)", 12, MUTED, "middle")
    s.add(f'<text transform="translate(28 {y0 + h / 2}) rotate(-90)" font-size="12" fill="{MUTED}" text-anchor="middle">'
          f'하차 직후 멀미 점수 (0–10)</text>')
    xs, ys = [r["dose"] for r in rows], [r["score"] for r in rows]
    n = len(rows)
    if n >= 2 and max(xs) > min(xs):                     # least-squares line, drawn only across the data
        mx, my = sum(xs) / n, sum(ys) / n
        b = sum((x - mx) * (y - my) for x, y in zip(xs, ys)) / sum((x - mx) ** 2 for x in xs)
        a = my - b * mx
        s.line(X(min(xs)), Y(a + b * min(xs)), X(max(xs)), Y(a + b * max(xs)), MUTED, 1.5, "6 4")
    for r in rows:
        color = ROAD_COLOR.get(r["road"], CAUTION)
        hollow = r["style"] == "smooth"
        fill = "#fff" if hollow else color
        s.add(f'<circle cx="{X(r["dose"]):.1f}" cy="{Y(r["score"]):.1f}" r="6" fill="{fill}" stroke="{color}" stroke-width="2"/>')
        s.text(X(r["dose"]) + 9, Y(r["score"]) - 7, f"{r['ride_id']} {r['nickname']}", 11, MUTED)
    x = x0
    for road, label in ROAD_LABEL.items():
        s.add(f'<circle cx="{x + 6}" cy="{y0 + h + 66}" r="6" fill="{ROAD_COLOR[road]}"/>')
        s.text(x + 18, y0 + h + 70, label, 12, INK)
        x += 90
    s.add(f'<circle cx="{x + 6}" cy="{y0 + h + 66}" r="6" fill="#fff" stroke="{MUTED}" stroke-width="2"/>')
    s.text(x + 18, y0 + h + 70, "빈 원: 부드럽게 · 채운 원: 거칠게", 12, INK)
    path.write_text("\n".join(s.parts + ["</svg>"]))


def main(argv):
    opts = {argv[i][2:]: argv[i + 1] for i in range(0, len(argv) - 1, 2) if argv[i].startswith("--")}
    survey = Path(opts.get("survey", RIDES / "survey.csv"))
    rides = Path(opts.get("rides", survey.parent))
    out = Path(opts.get("out", rides))
    if not survey.exists():
        sys.exit(f"no survey at {survey}; format: docs/ride-study.md")
    rows = build(survey, rides)
    if not rows:
        sys.exit("no rides matched the survey")
    out.mkdir(parents=True, exist_ok=True)
    with open(out / "report.csv", "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    for r in rows:
        print(f"{r['ride_id']:5} {r['nickname']:6} {r['road']:8} {r['style']:6} {r['minutes']:5} min  "
              f"I mean {r['mean_index']:.2f} max {r['max_index']:.2f}  D {r['dose']:6.1f}  alert {r['alert_s']:4d}s  "
              f"score {r['score']:4.1f}  MISC {r['misc']}  D@onset {r['dose_at_onset'] or '-'}  "
              f"alert lead {r['alert_lead_min'] if r['alert_lead_min'] != '' else '-'} min")
    score = [r["score"] for r in rows]
    print(f"\nn = {len(rows)}" + ("  (fewer than 10 rides: read r as a hint, not a result)" if len(rows) < 10 else ""))
    for key, label in [("dose", "D"), ("max_index", "max I"), ("mean_index", "mean I")]:
        xs = [r[key] for r in rows]
        print(f"  {label:6} vs score: Pearson {fmt_r(pearson(xs, score))}  Spearman {fmt_r(spearman(xs, score))}   "
              f"vs MISC: Spearman {fmt_r(spearman(xs, [r['misc'] for r in rows]))}")
    onsets = [r["dose_at_onset"] for r in rows if r["dose_at_onset"] != ""]
    if onsets:
        print(f"  D at symptom onset: {', '.join(f'{d:g}' for d in onsets)}  (mean {sum(onsets) / len(onsets):.1f}; "
              f"engine prior tau0 = {P['tau0']:g})")
    leads = [r["alert_lead_min"] for r in rows if r["alert_lead_min"] != ""]
    if leads:
        print(f"  first alert before onset in {sum(l > 0 for l in leads)}/{len(leads)} rides "
              f"(lead, min: {', '.join(f'{l:g}' for l in leads)})")
    xs = [r["dose"] for r in rows]
    scatter(rows, pearson(xs, score), spearman(xs, score), out / "report.svg")
    print(f"wrote {out / 'report.csv'}, {out / 'report.svg'}")


if __name__ == "__main__":
    main(sys.argv[1:])

"""STEP 4: run every scenario through the reference engine, check that the
results make sense, and write test vectors + charts.

    python3 sim/prototype.py
Outputs: sim/out/summary.csv, sim/out/*.svg, shared/test_vectors.csv
Exit code 1 if a sanity check fails (gate G2).
"""
import csv
import json
import sys
from pathlib import Path

from engine import Engine, personal_threshold, sickness_probability

ROOT = Path(__file__).resolve().parents[1]
SCN = ROOT / "sim" / "scenarios"
OUT = ROOT / "sim" / "out"
P = json.loads((ROOT / "shared" / "params.json").read_text())


def load(name):
    with open(SCN / f"{name}.csv") as f:
        for r in csv.DictReader(f):
            yield {"t": float(r["t"]), "speed": float(r["speed"]), "bearing": float(r["bearing"]),
                   "turn_signal": r["turn_signal"], "next_turn_m": float(r["next_turn_m"]),
                   "next_turn_dir": r["next_turn_dir"], "cabin_temp": float(r["cabin_temp"]),
                   "recirc": int(r["recirc"]), "window_open": int(r["window_open"]), "raw": r}


class CabinSim:
    """Stands in for the car's HVAC: after HVAC_FRESH the cabin switches to fresh air
    and the temperature relaxes toward the 22 °C set point (time constant 120 s)."""

    def __init__(self, react):
        self.react = react
        self.override = None

    def apply(self, s):
        if self.override:
            temp = self.override["temp"]
            temp += (P["cabinT0"] - temp) / 120.0
            self.override["temp"] = temp
            s = dict(s, cabin_temp=temp, recirc=0)
        return s

    def act(self, s, actions):
        if self.react and "HVAC_FRESH" in actions and self.override is None and (s["recirc"] or s["cabin_temp"] > P["cabinT0"] + 0.5):
            self.override = {"temp": s["cabin_temp"]}


def run(name, react=True):
    eng, cabin, rows = Engine(P), CabinSim(react), []
    for s in load(name):
        s = cabin.apply(s)
        out = eng.step(s)
        cabin.act(s, out["actions"])
        rows.append((s, out))
    return rows


def summarize(name, rows):
    idx = [o["index"] for _, o in rows]
    states = [o["state"] for _, o in rows]
    acts = [a for _, o in rows for a in o["actions"]]
    n = len(rows)
    return {
        "scenario": name, "minutes": round(n / 60, 1),
        "mean_index": round(sum(idx) / n, 3), "max_index": round(max(idx), 3),
        "dose": round(rows[-1][1]["dose"], 1),
        "dose_per_min": round(rows[-1][1]["dose"] / (n / 60) ** 0.5, 2),
        "caution_s": states.count("CAUTION"), "high_s": states.count("HIGH"),
        "cues": sum(a.startswith("CUE_") for a in acts), "hvac": acts.count("HVAC_FRESH"),
        "tts": acts.count("TTS_DRIVER"), "rest_stop": acts.count("SUGGEST_REST_STOP"),
        "min_m": round(min(o["m"] for _, o in rows), 3), "end_m": round(rows[-1][1]["m"], 3),
    }


def chart(name, rows, path):
    W, H, L, R, T, B = 900, 300, 60, 20, 30, 40
    n = len(rows)
    ymax = max(2.0, max(o["index"] for _, o in rows) * 1.1)
    X = lambda i: L + (W - L - R) * i / max(1, n - 1)
    Y = lambda v: H - B - (H - T - B) * min(v, ymax) / ymax
    band = {"CAUTION": "#FEF3C7", "HIGH": "#FDEBDD"}
    svg = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" font-family="Helvetica, Arial, sans-serif" font-size="11">',
           f'<rect width="{W}" height="{H}" fill="#fff"/>']
    for i, (_, o) in enumerate(rows):
        if o["state"] in band:
            svg.append(f'<rect x="{X(i):.1f}" y="{T}" width="{(W-L-R)/n+0.5:.2f}" height="{H-T-B}" fill="{band[o["state"]]}"/>')
    for v in [0, ymax / 2, ymax]:
        svg.append(f'<line x1="{L}" y1="{Y(v):.1f}" x2="{W-R}" y2="{Y(v):.1f}" stroke="#DADAEA"/><text x="{L-6}" y="{Y(v)+4:.1f}" text-anchor="end" fill="#6B6E86">{v:.1f}</text>')
    for key, color in [("th_c", "#B45309"), ("th_h", "#C2410C")]:
        pts = " ".join(f"{X(i):.1f},{Y(o[key]):.1f}" for i, (_, o) in enumerate(rows))
        svg.append(f'<polyline points="{pts}" fill="none" stroke="{color}" stroke-width="1.2" stroke-dasharray="5 4"/>')
    pts = " ".join(f"{X(i):.1f},{Y(o['index']):.1f}" for i, (_, o) in enumerate(rows))
    svg.append(f'<polyline points="{pts}" fill="none" stroke="#453ECE" stroke-width="2"/>')
    for i, (_, o) in enumerate(rows):
        for a in o["actions"]:
            if a.startswith("CUE_"):
                svg.append(f'<circle cx="{X(i):.1f}" cy="{T+8}" r="4" fill="#012348"/>')
            elif a == "HVAC_FRESH":
                svg.append(f'<rect x="{X(i)-4:.1f}" y="{T+16}" width="8" height="8" fill="#C2410C"/>')
    minutes = n / 60
    for k in range(0, int(minutes) + 1, max(1, int(minutes // 6) or 1)):
        svg.append(f'<text x="{X(k*60):.1f}" y="{H-B+16}" text-anchor="middle" fill="#6B6E86">{k}분</text>')
    svg.append(f'<text x="{L}" y="18" font-size="13" font-weight="700" fill="#012348">{name} · 멀미 지수 I (m/s²)</text>')
    svg.append(f'<text x="{W-R}" y="18" text-anchor="end" fill="#6B6E86">점선: 주의·높음 기준선(× 환경 계수) · ● 예고 ■ 공조 외기 전환 · 배경: 주의/높음</text>')
    svg.append("</svg>")
    path.write_text("\n".join(svg))


def write_test_vectors(name, rows, limit=300):
    with open(ROOT / "shared" / "test_vectors.csv", "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["scenario", "t", "speed", "bearing", "turn_signal", "next_turn_m", "next_turn_dir",
                    "cabin_temp", "recirc", "window_open", "ax", "ay", "index", "dose", "m", "state", "actions"])
        for s, o in rows[:limit]:
            w.writerow([name, int(s["t"]), s["speed"], s["bearing"], s["turn_signal"], int(s["next_turn_m"]),
                        s["next_turn_dir"], f"{s['cabin_temp']:.4f}", s["recirc"], s["window_open"],
                        f"{o['ax']:.6f}", f"{o['ay']:.6f}", f"{o['index']:.6f}", f"{o['dose']:.6f}",
                        f"{o['m']:.6f}", o["state"], "|".join(o["actions"])])


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    names = [m["name"] for m in json.loads((SCN / "scenarios.json").read_text())]
    results, summaries = {}, []
    for name in names:
        rows = run(name)
        results[name] = rows
        summaries.append(summarize(name, rows))
        chart(name, rows, OUT / f"{name}.svg")
    no_app = summarize("B_winding_rough (no app)", run("B_winding_rough", react=False))
    summaries.append(no_app)
    with open(OUT / "summary.csv", "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(summaries[0].keys()))
        w.writeheader()
        w.writerows(summaries)
    S = {s["scenario"]: s for s in summaries}
    for s in summaries:
        print(f"{s['scenario']:26} {s['minutes']:5} min  I mean {s['mean_index']:.2f} max {s['max_index']:.2f}  "
              f"D {s['dose']:6.1f}  caution {s['caution_s']:4d}s high {s['high_s']:4d}s  cues {s['cues']}  hvac {s['hvac']}  m {s['min_m']}→{s['end_m']}")

    # family mode on scenario D: same ride, two passengers
    tau_minjun = personal_threshold(P, [20, 22, 24, 21, 23, 22, 25, 20, 24, 23])
    tau_seoyeon = personal_threshold(P, [45, 48, 44])
    first = {}
    for who, tau in [("민준", tau_minjun), ("서연", tau_seoyeon)]:
        for s, o in results["D_family"]:
            if sickness_probability(P, o["dose"], tau, o["m"]) >= 0.5:
                first[who] = int(s["t"])
                break
    print(f"family D: tau 민준 {tau_minjun:.1f}, 서연 {tau_seoyeon:.1f}; P>=0.5 at {first or 'never'} (s)")

    write_test_vectors("B_winding_rough", results["B_winding_rough"])

    # gate G2: sanity checks
    checks = [
        ("order C < A < B (smooth)", S["C_highway"]["mean_index"] < S["A_city_smooth"]["mean_index"] < S["B_winding_smooth"]["mean_index"]),
        ("order C < A < B (rough)", S["C_highway"]["mean_index"] < S["A_city_rough"]["mean_index"] < S["B_winding_rough"]["mean_index"]),
        ("rough > smooth (city)", S["A_city_rough"]["dose_per_min"] > S["A_city_smooth"]["dose_per_min"]),
        ("rough > smooth (winding)", S["B_winding_rough"]["dose_per_min"] > S["B_winding_smooth"]["dose_per_min"]),
        ("highway never alerts", S["C_highway"]["caution_s"] + S["C_highway"]["high_s"] == 0),
        ("winding rough reaches HIGH", S["B_winding_rough"]["high_s"] > 0),
        ("closed loop: m recovers after fresh air", S["B_winding_rough"]["end_m"] >= 0.95 and S["B_winding_rough"]["hvac"] >= 1),
        ("no app: m stays low", no_app["end_m"] < 0.85),
        ("every city turn gets one cue", S["A_city_rough"]["cues"] == 4 and S["A_city_smooth"]["cues"] == 4),
        ("family: 민준 before 서연", "민준" in first and ("서연" not in first or first["민준"] < first["서연"])),
    ]
    ok = True
    print("\nG2 checks")
    for label, passed in checks:
        ok &= passed
        print(f"  {'PASS' if passed else 'FAIL'}  {label}")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()

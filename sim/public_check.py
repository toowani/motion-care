"""Realism check (team page formula ⑥): run one minute of real driving through the same
engine and compare it with the simulated highway scenario.

Data: comma2k19 example segment (MIT, github.com/commaai/comma2k19, Example_1 segment 40),
a California highway commute. global_pose gives ECEF position/velocity at 20 Hz; we turn it
into 1 Hz speed + compass bearing, the same signals the app gets.

    python3 sim/public_check.py      # writes sim/out/public_check.csv
"""
import ast
import csv
import json
import math
import struct
from pathlib import Path

from engine import Engine

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "sim" / "data" / "comma2k19_example"
P = json.loads((ROOT / "shared" / "params.json").read_text())


def load_npy(path):
    """Minimal .npy reader for little-endian float64 arrays (no numpy needed)."""
    raw = path.read_bytes()
    assert raw[:6] == b"\x93NUMPY"
    hlen = struct.unpack("<H", raw[8:10])[0]
    header = ast.literal_eval(raw[10:10 + hlen].decode())
    assert header["descr"] == "<f8" and not header["fortran_order"]
    shape = header["shape"]
    n = 1
    for s in shape:
        n *= s
    vals = struct.unpack(f"<{n}d", raw[10 + hlen:10 + hlen + 8 * n])
    if len(shape) == 1:
        return list(vals)
    cols = shape[1]
    return [vals[i * cols:(i + 1) * cols] for i in range(shape[0])]


def ecef_to_enu_rot(x, y, z):
    lon = math.atan2(y, x)
    lat = math.atan2(z, math.hypot(x, y))   # spherical approximation is enough for a rotation
    sl, cl, sp, cp = math.sin(lon), math.cos(lon), math.sin(lat), math.cos(lat)
    east = (-sl, cl, 0.0)
    north = (-sp * cl, -sp * sl, cp)
    return east, north


def real_samples():
    times = load_npy(DATA / "global_pose_frame_times.npy")
    pos = load_npy(DATA / "global_pose_frame_positions.npy")
    vel = load_npy(DATA / "global_pose_frame_velocities.npy")
    east, north = ecef_to_enu_rot(*pos[0])
    t0 = times[0]
    buckets = {}
    for t, v in zip(times, vel):
        ve = sum(a * b for a, b in zip(east, v))
        vn = sum(a * b for a, b in zip(north, v))
        buckets.setdefault(int(t - t0), []).append((ve, vn))
    out = []
    for sec in sorted(buckets):
        ve = sum(b[0] for b in buckets[sec]) / len(buckets[sec])
        vn = sum(b[1] for b in buckets[sec]) / len(buckets[sec])
        out.append({"t": float(sec), "speed": math.hypot(ve, vn), "bearing": math.degrees(math.atan2(ve, vn)) % 360,
                    "turn_signal": "OFF", "next_turn_m": -1.0, "next_turn_dir": "",
                    "cabin_temp": 22.0, "recirc": 0, "window_open": 0})
    return out


def sim_samples(name, seconds=None):
    with open(ROOT / "sim" / "scenarios" / f"{name}.csv") as f:
        rows = [{"t": float(r["t"]), "speed": float(r["speed"]), "bearing": float(r["bearing"]),
                 "turn_signal": r["turn_signal"], "next_turn_m": float(r["next_turn_m"]),
                 "next_turn_dir": r["next_turn_dir"], "cabin_temp": float(r["cabin_temp"]),
                 "recirc": int(r["recirc"]), "window_open": int(r["window_open"])} for r in csv.DictReader(f)]
    return rows[:seconds] if seconds else rows


def p95(xs):
    s = sorted(abs(x) for x in xs)
    return s[min(len(s) - 1, int(0.95 * len(s)))]


def stats(label, samples, skip=15):
    eng = Engine(P)
    outs = [eng.step(s) for s in samples]
    o = outs[skip:]                       # let the filters settle
    return {"source": label, "seconds": len(o),
            "mean_speed_kmh": round(sum(s["speed"] for s in samples[skip:]) / len(o) * 3.6, 1),
            "p95_ax": round(p95([x["ax"] for x in o]), 2), "p95_ay": round(p95([x["ay"] for x in o]), 2),
            "mean_index": round(sum(x["index"] for x in o) / len(o), 2),
            "max_index": round(max(x["index"] for x in o), 2),
            "alert_s": sum(x["state"] != "NORMAL" for x in o)}


REAL = "comma2k19 실제 고속도로 (45초)"
SIM_SAME = "시뮬 C 고속도로 (같은 45초)"


def main():
    real = real_samples()
    # the clip is 60 s; the first 15 s settle the filters, so 45 s are scored on both sides
    rows = [stats(REAL, real),
            stats(SIM_SAME, sim_samples("C_highway")[200:200 + len(real)]),
            stats("시뮬 C 고속도로 (전체)", sim_samples("C_highway")),
            stats("시뮬 A 도심 거칠게", sim_samples("A_city_rough")),
            stats("시뮬 B 굽은 길 거칠게", sim_samples("B_winding_rough"))]
    out = ROOT / "sim" / "out" / "public_check.csv"
    with open(out, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    for r in rows:
        print(f"{r['source']:28} {r['seconds']:4d}s  {r['mean_speed_kmh']:6.1f} km/h  P95 |ax| {r['p95_ax']:.2f}  "
              f"P95 |ay| {r['p95_ay']:.2f}  I mean {r['mean_index']:.2f} max {r['max_index']:.2f}  alert {r['alert_s']}s")
    real_r, sim_r = rows[0], rows[1]
    # same length on both sides: the index of a short clip is lower (zero-padded 60 s window),
    # so the real clip is compared with an equally long sim clip, not with the whole sim ride
    n = real_r["seconds"]
    ax = sim_r["p95_ax"] / real_r["p95_ax"]
    print(f"같은 길이 {n}초 비교 (시뮬/실제):")
    print(f"  P95 |ax|  {real_r['p95_ax']:.2f} vs {sim_r['p95_ax']:.2f}  = {ax:.2f}배  "
          f"({'PASS' if 0.5 <= ax <= 2 else 'FAIL'}: 0.5–2배)")
    print(f"  I 평균    {real_r['mean_index']:.2f} vs {sim_r['mean_index']:.2f}  = "
          f"{sim_r['mean_index'] / real_r['mean_index']:.2f}배  (둘 다 주의 기준 {P['thetaCaution']} 아래)")
    print(f"  P95 |ay|  {real_r['p95_ay']:.2f} vs {sim_r['p95_ay']:.2f}  비교 불가: 직선 구간")


if __name__ == "__main__":
    main()

"""STEP 3: build driving scenarios from road geometry (no real car needed).

Road = straights + arcs. Curve speed is capped by the style's lateral
acceleration (v = sqrt(a_lat_max / kappa)), then a forward/backward pass caps
longitudinal acceleration. The result is resampled to 1 Hz with GPS-like noise
and written as the same columns the app's ReplaySignalSource reads.

    python3 sim/generator.py            # writes sim/scenarios/*.csv
"""
import csv
import json
import math
import random
from pathlib import Path

DS = 1.0                      # geometry step (m)
EARTH_R = 6_371_000.0
ORIGIN = (37.50, 127.00)      # lat, lon of local origin (Seoul area)
OUT = Path(__file__).resolve().parent / "scenarios"

STYLES = {
    "smooth": {"a_lat": 2.0, "a_lon": 1.5, "a_brake": 1.8},
    "rough": {"a_lat": 3.5, "a_lon": 3.0, "a_brake": 3.5},
    "normal": {"a_lat": 2.6, "a_lon": 2.0, "a_brake": 2.5},
}

COLUMNS = ["t", "lat", "lon", "speed", "bearing", "turn_signal",
           "next_turn_m", "next_turn_dir", "cabin_temp", "recirc", "window_open"]


# ---------- road geometry ----------

def straight(length, vlim):
    return {"type": "S", "length": length, "vlim": vlim}


def arc(radius, angle_deg, direction, vlim, kind="curve"):
    """direction +1 = left, -1 = right. kind 'turn' = intersection turn (navigation announces it)."""
    return {"type": "A", "radius": radius, "angle": math.radians(angle_deg),
            "dir": direction, "vlim": vlim, "kind": kind}


def stop(dwell_s):
    return {"type": "STOP", "dwell": dwell_s}


def discretize(segments):
    """Sample the road every DS metres: position, heading, signed curvature, speed limit."""
    x = y = 0.0
    hdg = math.pi / 2          # start heading north (math angle)
    pts, stops, turns = [], {}, []
    for seg in segments:
        if seg["type"] == "STOP":
            stops[len(pts) - 1] = seg["dwell"]
            continue
        if seg["type"] == "S":
            n = max(1, int(round(seg["length"] / DS)))
            for _ in range(n):
                x += DS * math.cos(hdg)
                y += DS * math.sin(hdg)
                pts.append((x, y, hdg, 0.0, seg["vlim"]))
        else:
            length = seg["radius"] * seg["angle"]
            n = max(1, int(round(length / DS)))
            k = seg["dir"] / seg["radius"]
            if seg["kind"] == "turn":
                turns.append((len(pts), len(pts) + n, "L" if seg["dir"] > 0 else "R"))
            for _ in range(n):
                hdg += k * DS
                x += DS * math.cos(hdg)
                y += DS * math.sin(hdg)
                pts.append((x, y, hdg, k, seg["vlim"]))
    return pts, stops, turns


def speed_profile(pts, stops, style):
    vmax = []
    for i, (_, _, _, k, vlim) in enumerate(pts):
        v = vlim if abs(k) < 1e-9 else min(vlim, math.sqrt(style["a_lat"] / abs(k)))
        vmax.append(0.0 if i in stops else v)
    vmax[0] = vmax[-1] = 0.0
    v = vmax[:]
    for i in range(1, len(v)):                      # acceleration limit
        v[i] = min(v[i], math.sqrt(v[i - 1] ** 2 + 2 * style["a_lon"] * DS))
    for i in range(len(v) - 2, -1, -1):             # braking limit
        v[i] = min(v[i], math.sqrt(v[i + 1] ** 2 + 2 * style["a_brake"] * DS))
    return v


def timeline(v, stops):
    """Cumulative (time, distance) knots; dwell at stops keeps distance fixed."""
    knots = [(0.0, 0.0)]
    t = 0.0
    for i in range(1, len(v)):
        vavg = max((v[i - 1] + v[i]) / 2, 0.3)
        t += DS / vavg
        knots.append((t, i * DS))
        if i in stops:
            t += stops[i]
            knots.append((t, i * DS))
    return knots


def interp(knots, t):
    lo, hi = 0, len(knots) - 1
    while hi - lo > 1:
        mid = (lo + hi) // 2
        if knots[mid][0] <= t:
            lo = mid
        else:
            hi = mid
    (t0, s0), (t1, s1) = knots[lo], knots[hi]
    return s0 if t1 == t0 else s0 + (s1 - s0) * (t - t0) / (t1 - t0)


def to_latlon(x, y):
    lat0 = math.radians(ORIGIN[0])
    lat = ORIGIN[0] + math.degrees(y / EARTH_R)
    lon = ORIGIN[1] + math.degrees(x / (EARTH_R * math.cos(lat0)))
    return lat, lon


def compass_bearing(math_hdg):
    return (90.0 - math.degrees(math_hdg)) % 360.0


# ---------- scenario assembly ----------

def build(name, segments, style_name, cabin, seed):
    rng = random.Random(seed)
    style = STYLES[style_name]
    pts, stops, turns = discretize(segments)
    v = speed_profile(pts, stops, style)
    knots = timeline(v, stops)
    total = knots[-1][0]
    rows = []
    prev_s = None
    for sec in range(int(total) + 1):
        s = interp(knots, float(sec))
        i = min(int(s / DS), len(pts) - 1)
        x, y, hdg, _, _ = pts[i]
        speed = 0.0 if prev_s is not None and abs(s - prev_s) < 1e-6 else v[i]
        prev_s = s
        # navigation guidance and turn signal
        nxt = next(((a, b, d) for a, b, d in turns if b > i), None)
        next_m, next_dir, signal = -1, "", "OFF"
        if nxt:
            a, b, d = nxt
            dist = max(0.0, (a - i) * DS)
            if dist <= 1000:
                next_m, next_dir = round(dist), d
            if i >= a or dist <= max(30.0, speed * 4.0):
                signal = "LEFT" if d == "L" else "RIGHT"
        # GPS-like noise
        noisy_speed = max(0.0, speed + rng.gauss(0, 0.15)) if speed > 0 else 0.0
        # GNSS course accuracy improves with speed: about 0.6 deg at 18 km/h, 0.2 deg at 100 km/h
        bearing = compass_bearing(hdg) + rng.gauss(0, max(0.2, 3.0 / speed) if speed > 2 else 15.0)
        lat, lon = to_latlon(x + rng.gauss(0, 1.5), y + rng.gauss(0, 1.5))
        rows.append({
            "t": sec, "lat": f"{lat:.7f}", "lon": f"{lon:.7f}",
            "speed": f"{noisy_speed:.3f}", "bearing": f"{bearing % 360:.2f}",
            "turn_signal": signal, "next_turn_m": next_m, "next_turn_dir": next_dir,
            "cabin_temp": f"{cabin['temp']:.1f}", "recirc": int(cabin["recirc"]),
            "window_open": 0,
        })
    OUT.mkdir(parents=True, exist_ok=True)
    with open(OUT / f"{name}.csv", "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=COLUMNS)
        w.writeheader()
        w.writerows(rows)
    return {"name": name, "style": style_name, "duration_s": len(rows), "distance_m": round(len(pts) * DS),
            "turns": len(turns), "stops": len(stops), "cabin": cabin}


def city(seed):
    rng = random.Random(seed)
    segs, vlim = [], 50 / 3.6
    for blk in range(14):
        segs.append(straight(rng.uniform(150, 300), vlim))
        if rng.random() < 0.5:
            segs.append(stop(rng.uniform(20, 40)))
        if blk % 3 == 2:
            segs.append(straight(15, vlim))
            segs.append(arc(12, 90, rng.choice([1, -1]), vlim, kind="turn"))
    return segs


def winding(seed, n=40):
    rng = random.Random(seed)
    segs, vlim, d = [], 60 / 3.6, 1
    for _ in range(n):
        segs.append(straight(rng.uniform(20, 80), vlim))
        segs.append(arc(rng.uniform(30, 80), rng.uniform(50, 120), d, vlim))
        d = -d
    return segs


def highway(seed, km=15):
    """Long straights and gentle curves. Traffic makes the speed drift between about 60 and 105 km/h
    every few hundred metres (calibrated against the comma2k19 real highway sample, sim/public_check.py)."""
    rng = random.Random(seed)
    segs, dist = [], 0.0
    while dist < km * 1000:
        L = rng.uniform(2000, 3000)
        left = L
        while left > 0:
            chunk = min(left, rng.uniform(300, 700))
            segs.append(straight(chunk, rng.uniform(60, 105) / 3.6))
            left -= chunk
        segs.append(arc(rng.uniform(800, 1500), rng.uniform(8, 15), rng.choice([1, -1]), 100 / 3.6))
        dist += L
    return segs


def main():
    fresh = {"temp": 22.0, "recirc": False}
    stuffy = {"temp": 26.0, "recirc": True}
    warm = {"temp": 25.0, "recirc": True}
    meta = [
        build("A_city_smooth", city(1), "smooth", fresh, 11),
        build("A_city_rough", city(1), "rough", fresh, 12),
        build("B_winding_smooth", winding(2), "smooth", stuffy, 21),
        build("B_winding_rough", winding(2), "rough", stuffy, 22),
        build("C_highway", highway(3), "normal", fresh, 31),
        build("D_family", highway(4, km=30) + [stop(5)] + winding(5, n=25), "normal", warm, 41),
    ]
    with open(OUT / "scenarios.json", "w") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    for m in meta:
        print(f"{m['name']:18} {m['style']:7} {m['duration_s']:5d} s  {m['distance_m']/1000:5.1f} km  "
              f"turns {m['turns']:2d}  stops {m['stops']:2d}")


if __name__ == "__main__":
    main()

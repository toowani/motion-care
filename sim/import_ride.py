"""Real ride check: read a phone GPS log (GPX, or a CSV from Sensor Logger / GPSLogger),
resample it to 1 Hz, turn it into the same columns as sim/scenarios/*.csv and run the
reference engine. Protocol and survey: docs/ride-study.md.

    python3 sim/import_ride.py data/rides/R01.gpx               # summary only
    python3 sim/import_ride.py data/rides/R01.gpx --csv          # also writes data/rides/R01.csv
    python3 sim/import_ride.py sim/out/B_winding_rough.gpx --from-position
Options: --from-position  derive speed/bearing from positions even if the log has them
         --start S --end S  keep only seconds S..S of the ride (after resampling)
         --temp C --recirc  cabin state (unknown in a friend's car: 22 °C, fresh air)
"""
import csv
import json
import math
import re
import sys
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path

from engine import Engine

ROOT = Path(__file__).resolve().parents[1]
P = json.loads((ROOT / "shared" / "params.json").read_text())
MAX_SPEED = 250 / 3.6      # m/s; a jump faster than this is a GPS glitch
MAX_GAP_S = 10             # longer gaps are reported (tunnels, lost fix)
EARTH_R = 6371000.0
COLUMNS = ["t", "lat", "lon", "speed", "bearing", "turn_signal", "next_turn_m", "next_turn_dir",
           "cabin_temp", "recirc", "window_open"]


def parse_time(s):
    """ISO 8601 text or a numeric epoch in s / ms / ns -> epoch seconds."""
    s = s.strip()
    try:
        x = float(s)
    except ValueError:
        s = s.replace("Z", "+00:00").replace(" ", "T", 1)
        s = re.sub(r"(\.\d{6})\d+", r"\1", s)        # Python wants at most 6 fractional digits
        return datetime.fromisoformat(s).timestamp()
    for limit, scale in ((1e17, 1e9), (1e14, 1e6), (1e11, 1e3)):   # ns, us, ms since 1970
        if x > limit:
            return x / scale
    return x


def num(s):
    try:
        x = float(s)
    except (TypeError, ValueError):
        return None
    return x if math.isfinite(x) and x >= 0 else None


def read_gpx(path):
    pts = []
    for el in ET.parse(path).iter():
        if not el.tag.endswith("trkpt"):
            continue
        p = {"lat": float(el.get("lat")), "lon": float(el.get("lon")), "time": None, "speed": None, "bearing": None}
        for c in el.iter():
            tag = c.tag.split("}")[-1].lower()
            if tag == "time":
                p["time"] = parse_time(c.text)
            elif tag == "speed":                     # GPX 1.0 or an extension (m/s)
                p["speed"] = num(c.text)
            elif tag in ("course", "bearing"):
                p["bearing"] = num(c.text)
        if p["time"] is not None:
            pts.append(p)
    return pts


def find_col(header, *names):
    low = [h.strip().lower() for h in header]
    for n in names:
        for i, h in enumerate(low):
            if h == n or h.startswith(n + " ") or h.startswith(n + "("):
                return i
    return None


def read_csv(path):
    """Columns are found by name, so Sensor Logger (Location.csv: time in ns, latitude,
    longitude, speed, bearing) and GPSLogger (time, lat, lon, bearing, speed) both work."""
    with open(path, newline="", encoding="utf-8-sig") as f:
        rows = list(csv.reader(f))
    header, rows = rows[0], rows[1:]
    it = find_col(header, "time", "timestamp", "date time", "datetime", "time_utc", "utc")
    ilat = find_col(header, "latitude", "lat")
    ilon = find_col(header, "longitude", "lon", "lng")
    isp = find_col(header, "speed")
    ibr = find_col(header, "bearing", "course", "heading")
    if None in (it, ilat, ilon):
        sys.exit(f"{path}: need time, latitude and longitude columns, got {header}")
    kmh = isp is not None and "km" in header[isp].lower()
    pts = []
    for r in rows:
        try:
            p = {"time": parse_time(r[it]), "lat": float(r[ilat]), "lon": float(r[ilon])}
        except (ValueError, IndexError):
            continue
        sp = num(r[isp]) if isp is not None and isp < len(r) else None
        p["speed"] = sp / 3.6 if sp is not None and kmh else sp
        p["bearing"] = num(r[ibr]) if ibr is not None and ibr < len(r) else None
        pts.append(p)
    return pts


def dist_bearing(a, b):
    """Equirectangular distance (m) and compass bearing (deg) from a to b; fine at 1 s spacing."""
    lat0 = math.radians((a["lat"] + b["lat"]) / 2)
    dn = math.radians(b["lat"] - a["lat"]) * EARTH_R
    de = math.radians(b["lon"] - a["lon"]) * EARTH_R * math.cos(lat0)
    return math.hypot(de, dn), math.degrees(math.atan2(de, dn)) % 360


def drop_glitches(pts):
    """Sort by time, drop duplicate timestamps and points that imply > 250 km/h from the last kept one."""
    pts = sorted(pts, key=lambda p: p["time"])
    kept, dropped = [], 0
    for p in pts:
        if kept:
            dt = p["time"] - kept[-1]["time"]
            if dt <= 0:
                continue
            if dist_bearing(kept[-1], p)[0] / dt > MAX_SPEED or (p["speed"] or 0) > MAX_SPEED:
                dropped += 1
                continue
        kept.append(p)
    return kept, dropped


def lerp_angle(a, b, f):
    return (a + ((b - a + 180) % 360 - 180) * f) % 360


def resample(pts, from_position=False):
    """Linear interpolation onto whole seconds. Speed/bearing come from the log when every
    point has them (Doppler speed is far less noisy than position differences); otherwise
    from positions one second before and after (central difference)."""
    t0 = math.ceil(pts[0]["time"])
    n = int(math.floor(pts[-1]["time"]) - t0) + 1
    use_log = not from_position and all(p["speed"] is not None and p["bearing"] is not None for p in pts)
    out, j, gaps = [], 0, []
    for k in range(n):
        t = t0 + k
        while j < len(pts) - 2 and pts[j + 1]["time"] < t:
            j += 1
        a, b = pts[j], pts[j + 1] if j + 1 < len(pts) else pts[j]
        span = b["time"] - a["time"]
        f = 0.0 if span <= 0 else min(1.0, max(0.0, (t - a["time"]) / span))
        if span > MAX_GAP_S and (not gaps or gaps[-1][0] != a["time"]):
            gaps.append((a["time"], span))
        s = {"t": float(k), "lat": a["lat"] + (b["lat"] - a["lat"]) * f, "lon": a["lon"] + (b["lon"] - a["lon"]) * f}
        if use_log:
            s["speed"] = a["speed"] + (b["speed"] - a["speed"]) * f
            s["bearing"] = lerp_angle(a["bearing"], b["bearing"], f)
        out.append(s)
    if not use_log:
        last_brg = 0.0
        for k, s in enumerate(out):
            a, b = out[max(0, k - 1)], out[min(n - 1, k + 1)]
            d, brg = dist_bearing(a, b)
            span = b["t"] - a["t"]
            s["speed"] = d / span if span else 0.0
            # standing still: GPS jitter gives random bearings, keep the last good one
            if s["speed"] >= P["minSpeedForHeading"]:
                last_brg = brg
            s["bearing"] = last_brg
    return out, use_log, [(t - t0, g) for t, g in gaps]


def to_samples(rows, temp=22.0, recirc=0):
    return [dict(r, turn_signal="OFF", next_turn_m=-1.0, next_turn_dir="",
                 cabin_temp=temp, recirc=recirc, window_open=0) for r in rows]


def write_csv(samples, path):
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(COLUMNS)
        for s in samples:
            w.writerow([int(s["t"]), f"{s['lat']:.7f}", f"{s['lon']:.7f}", f"{s['speed']:.3f}", f"{s['bearing']:.2f}",
                        "OFF", -1, "", s["cabin_temp"], s["recirc"], 0])


def mmss(t):
    return f"{int(t) // 60}:{int(t) % 60:02d}"


def analyze(samples):
    eng = Engine(P)
    outs = [eng.step(s) for s in samples]
    n = len(outs)
    idx = [o["index"] for o in outs]
    first = {}
    for s, o in zip(samples, outs):
        if o["state"] != "NORMAL" and o["state"] not in first:
            first[o["state"]] = s["t"]
    return {"seconds": n, "minutes": round(n / 60, 1),
            "mean_speed_kmh": round(sum(s["speed"] for s in samples) / n * 3.6, 1),
            "mean_index": round(sum(idx) / n, 3), "max_index": round(max(idx), 3),
            "dose": round(outs[-1]["dose"], 1),
            "caution_s": sum(o["state"] == "CAUTION" for o in outs), "high_s": sum(o["state"] == "HIGH" for o in outs),
            "first_caution": first.get("CAUTION"), "first_high": first.get("HIGH"),
            "dose_curve": [o["dose"] for o in outs]}


def load_ride(path, from_position=False, start=None, end=None, temp=22.0, recirc=0):
    path = Path(path)
    raw = read_gpx(path) if path.suffix.lower() == ".gpx" else read_csv(path)
    if len(raw) < 2:
        sys.exit(f"{path}: fewer than 2 timed points")
    pts, dropped = drop_glitches(raw)
    rows, use_log, gaps = resample(pts, from_position)
    rows = rows[int(start or 0):int(end) if end is not None else None]
    for k, r in enumerate(rows):
        r["t"] = float(k)
    info = {"points": len(raw), "dropped": dropped, "source": "log speed/course" if use_log else "positions", "gaps": gaps}
    return to_samples(rows, temp, recirc), info


def main(argv):
    args, opts, i = [], {}, 0
    while i < len(argv):
        a = argv[i]
        if a in ("--start", "--end", "--temp"):
            opts[a[2:]] = float(argv[i + 1])
            i += 2
            continue
        if a.startswith("--"):
            opts[a[2:].replace("-", "_")] = True
        else:
            args.append(a)
        i += 1
    if not args:
        sys.exit(__doc__)
    for path in args:
        samples, info = load_ride(path, opts.get("from_position", False), opts.get("start"), opts.get("end"),
                                  opts.get("temp", 22.0), int(opts.get("recirc", 0)))
        r = analyze(samples)
        print(f"{Path(path).name}: {info['points']} points, dropped {info['dropped']} (>250 km/h), "
              f"speed/bearing from {info['source']}")
        for t, g in info["gaps"]:
            print(f"  gap {g:.0f}s at {mmss(t)} (interpolated)")
        print(f"  {r['minutes']} min  {r['mean_speed_kmh']} km/h  I mean {r['mean_index']:.2f} max {r['max_index']:.2f}  "
              f"D {r['dose']:.1f}  caution {r['caution_s']}s high {r['high_s']}s  "
              f"first caution {mmss(r['first_caution']) if r['first_caution'] is not None else '-'}  "
              f"first high {mmss(r['first_high']) if r['first_high'] is not None else '-'}")
        if opts.get("csv"):
            out = Path(path).with_suffix(".csv") if Path(path).suffix.lower() == ".gpx" else Path(path).with_name(Path(path).stem + "_1hz.csv")
            write_csv(samples, out)
            print(f"  wrote {out}")


if __name__ == "__main__":
    main(sys.argv[1:])

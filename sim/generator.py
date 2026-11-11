"""STEP 3: build driving scenarios on real roads (no real car needed).

Every scenario follows a real Korean road fetched once from OSRM (sim/fetch_routes.py,
cached in sim/routes/*.osrm.json; map data © OpenStreetMap contributors, ODbL).

Pipeline per road:
  1. project the OSRM polyline to local metres and resample it every 1 m;
  2. smooth the positions (Gaussian, width per road type) so the vertex kinks of OSM
     polylines do not become fake curvature spikes, then resample the smoothed path at 1 m;
  3. heading and signed curvature from the smoothed path (smoothing keeps corner radii realistic);
  4. speed limit per road type (city 50 km/h, arterial 60, winding 60, expressway drifts
     60-105 km/h every few hundred metres like real traffic, reached at 1 m/s^2 with rounded
     corners, calibrated against comma2k19 in sim/public_check.py), curve speed capped by the style's lateral acceleration
     v = sqrt(a_lat_max / kappa), then a forward/backward pass caps longitudinal acceleration;
  5. traffic-light stops at a seeded subset of real intersections; navigation turns
     (turn signal, next turn distance/direction) from the real OSRM maneuvers.
The result is resampled to 1 Hz with small GPS-like noise and written as the columns the
app's ReplaySignalSource reads, plus "s" (metres along the route geometry). Each scenario
also gets <NAME>.route.json (road polyline + maneuvers) for the map screen.

    python3 sim/generator.py            # writes sim/scenarios/*.csv, *.route.json, scenarios.json
"""
import csv
import json
import math
import random
from pathlib import Path

DS = 1.0                      # geometry step (m)
EARTH_R = 6_371_000.0
HERE = Path(__file__).resolve().parent
ROUTES = HERE / "routes"
OUT = HERE / "scenarios"
GEOMETRY_STEP_M = 5           # spacing of the polyline written to route.json

STYLES = {
    "smooth": {"a_lat": 2.0, "a_lon": 1.5, "a_brake": 1.8},
    "rough": {"a_lat": 3.5, "a_lon": 3.0, "a_brake": 3.5},
    "normal": {"a_lat": 2.6, "a_lon": 2.0, "a_brake": 2.5},
}

COLUMNS = ["t", "lat", "lon", "speed", "bearing", "turn_signal",
           "next_turn_m", "next_turn_dir", "cabin_temp", "recirc", "window_open", "s"]

# Gaussian smoothing width (m) and speed limit (km/h) per road type
SMOOTH_SIGMA = {"city": 8.0, "arterial": 8.0, "winding": 6.0, "ramp": 10.0, "expressway": 40.0}
VLIM_KMH = {"city": 50, "arterial": 60, "winding": 60, "ramp": 60}
# expressway traffic drift: the next target speed is reached gently, not at the style's full
# acceleration (traffic-flow speed changes, not a driver flooring it), and the ramp is rounded
# (jerk-limited). Calibrated against comma2k19 in sim/public_check.py
TRAFFIC_ACCEL = 1.0           # m/s^2, both ways (comma2k19 clip: about 0.5-1.5 m/s^2)
TRAFFIC_ROUND_M = 50          # moving average over the ramped target speed (m), about 2 s at 90 km/h
SPEED_NOISE = 0.15            # m/s, 1 sigma per 1 Hz sample

# Road description per cached route. Road type comes from the OSRM step (expressway = refs
# of 고속도로; unnamed steps on an expressway route are ramps), with per-name overrides.
ROADS = {
    "city": {"title": "강남 테헤란로", "dest": "선릉역", "default": "city", "seed": 1, "stops": 6},
    "winding": {"title": "북악스카이웨이", "dest": "성북동 대사관로", "default": "winding", "seed": 2, "stops": 0,
                "bends": True},
    "highway": {"title": "경부고속도로", "dest": "수원신갈IC", "default": "expressway", "seed": 3, "stops": 0},
    "family": {"title": "경부고속도로 → 남한산성로", "dest": "남한산성", "default": "expressway", "seed": 4, "stops": 3,
               "names": {"헌릉로": "arterial", "수정로": "city", "남한산성로": "winding"}},
}

TURN_TEXT = {
    "TURN_LEFT": "좌회전", "TURN_RIGHT": "우회전", "TURN_STRAIGHT": "직진", "TURN_KEEP_L": "왼쪽 방향",
    "TURN_KEEP_R": "오른쪽 방향", "TURN_UTURN": "유턴", "TURN_ENTER_IC_R": "고속도로 진입",
    "TURN_EXIT_IC_R": "오른쪽 출구", "TURN_ARRIVE": "도착",
    "TURN_CURVE_L": "왼쪽 커브", "TURN_CURVE_R": "오른쪽 커브",
}

# bend cues on winding roads: heading change above BEND_DEG within BEND_SPAN_M; a bend that
# starts within BEND_GAP_M of the previous one belongs to the same S-sequence and gets no cue
BEND_DEG = 45.0
BEND_SPAN_M = 150.0
BEND_GAP_M = 60.0


# ---------- geometry ----------

class Projection:
    """Local equirectangular projection around the route's mean latitude (metres)."""

    def __init__(self, lat0, lon0):
        self.lat0, self.lon0 = lat0, lon0
        self.kx = math.radians(1) * EARTH_R * math.cos(math.radians(lat0))
        self.ky = math.radians(1) * EARTH_R

    def xy(self, lat, lon):
        return (lon - self.lon0) * self.kx, (lat - self.lat0) * self.ky

    def latlon(self, x, y):
        return self.lat0 + y / self.ky, self.lon0 + x / self.kx


def resample(xy, step=DS):
    """Points every `step` metres along a polyline; also returns, per output point, the
    fractional index of the input segment it came from."""
    out, src = [xy[0]], [0.0]
    carry = 0.0
    for i in range(1, len(xy)):
        (x0, y0), (x1, y1) = xy[i - 1], xy[i]
        seg = math.hypot(x1 - x0, y1 - y0)
        if seg < 1e-9:
            continue
        d = step - carry
        while d <= seg:
            f = d / seg
            out.append((x0 + (x1 - x0) * f, y0 + (y1 - y0) * f))
            src.append(i - 1 + f)
            d += step
        carry = seg - (d - step)
    return out, src


def gaussian_smooth(xy, sigmas):
    """Smooth positions with a per-point Gaussian width (in samples = metres at DS = 1)."""
    kernels = {}
    n = len(xy)
    out = []
    for i in range(n):
        sg = sigmas[i]
        if sg not in kernels:
            r = int(3 * sg)
            kernels[sg] = (r, [math.exp(-0.5 * (k / sg) ** 2) for k in range(-r, r + 1)])
        r, ker = kernels[sg]
        lo, hi = max(0, i - r), min(n - 1, i + r)
        sx = sy = sw = 0.0
        for j in range(lo, hi + 1):
            w = ker[j - i + r]
            sx += w * xy[j][0]
            sy += w * xy[j][1]
            sw += w
        out.append((sx / sw, sy / sw))
    return out


def wrap_pi(a):
    return (a + math.pi) % (2 * math.pi) - math.pi


def heading_curvature(xy, h=2):
    n = len(xy)
    hdg = []
    for i in range(n):
        a, b = max(0, i - h), min(n - 1, i + h)
        hdg.append(math.atan2(xy[b][1] - xy[a][1], xy[b][0] - xy[a][0]))
    k = []
    for i in range(n):
        a, b = max(0, i - h), min(n - 1, i + h)
        kk = wrap_pi(hdg[b] - hdg[a]) / max(1, (b - a) * DS)
        # no clamp here: speed must follow the same heading the bearing column reports, or the
        # engine sees more lateral acceleration than the style allows; smoothing keeps radii realistic
        k.append(kk)
    return hdg, k


def nearest_index(xy, p, lo, hi):
    lo, hi = max(0, lo), min(len(xy) - 1, hi)
    best, bi = float("inf"), lo
    for i in range(lo, hi + 1):
        d = (xy[i][0] - p[0]) ** 2 + (xy[i][1] - p[1]) ** 2
        if d < best:
            best, bi = d, i
    return bi


def compass_bearing(math_hdg):
    return (90.0 - math.degrees(math_hdg)) % 360.0


# ---------- maneuvers ----------

def turn_type(m_type, modifier):
    mod = modifier or "straight"
    if m_type == "arrive":
        return "TURN_ARRIVE"
    if mod == "uturn":
        return "TURN_UTURN"
    if m_type == "on ramp" and "right" in mod:
        return "TURN_ENTER_IC_R"
    if m_type == "off ramp" and "right" in mod:
        return "TURN_EXIT_IC_R"
    if mod in ("left", "sharp left"):
        return "TURN_LEFT" if m_type in ("turn", "end of road", "continue") else "TURN_KEEP_L"
    if mod in ("right", "sharp right"):
        return "TURN_RIGHT" if m_type in ("turn", "end of road", "continue") else "TURN_KEEP_R"
    if mod == "slight left":
        return "TURN_KEEP_L"
    if mod == "slight right":
        return "TURN_KEEP_R"
    return "TURN_STRAIGHT"


def turn_dir(ttype):
    if ttype in ("TURN_LEFT", "TURN_KEEP_L", "TURN_UTURN", "TURN_CURVE_L"):
        return "L"
    if ttype in ("TURN_RIGHT", "TURN_KEEP_R", "TURN_ENTER_IC_R", "TURN_EXIT_IC_R", "TURN_CURVE_R"):
        return "R"
    return ""


def step_road(steps, i):
    """Road name after maneuver i: the step's name, else the next named step, else signposts."""
    st = steps[i]
    if st["name"]:
        return st["name"]
    dest = st.get("destinations") or ""
    if ":" in dest:
        dest = dest.split(":", 1)[1].strip()
    if dest:
        return f"{dest} 방면"
    for nxt in steps[i + 1:]:
        if nxt["name"]:
            return nxt["name"]
    return ""


def step_kind(road, st):
    ref = st.get("ref") or ""
    if st["name"].endswith("고속도로") or (road["default"] == "expressway" and ref in ("1", "100")):
        return "expressway"
    if st["name"] in road.get("names", {}):
        return road["names"][st["name"]]
    if road["default"] == "expressway":
        return "ramp" if not st["name"] else "city"
    return road["default"]


def find_bends(hdg, curv):
    """Bends (not junctions) worth a cue: the heading turns more than BEND_DEG within
    BEND_SPAN_M. Returns (entry index, 'L'/'R'); the entry is where the curvature first
    reaches 30 % of the bend's peak, so the cue comes before the car starts to turn."""
    n, half = len(hdg), int(BEND_SPAN_M / DS / 2)
    cum = [0.0]
    for i in range(1, n):
        cum.append(cum[-1] + wrap_pi(hdg[i] - hdg[i - 1]))
    delta = [cum[min(n - 1, i + half)] - cum[max(0, i - half)] for i in range(n)]
    th = math.radians(BEND_DEG)
    bends, i = [], 0
    while i < n:
        if abs(delta[i]) <= th:
            i += 1
            continue
        sign = 1 if delta[i] > 0 else -1
        j = i
        while j < n and delta[j] * sign > th:
            j += 1
        apex = max(range(i, j), key=lambda k: abs(delta[k]))
        lo, hi = max(0, apex - half), min(n - 1, apex + half)
        peak = max(curv[k] * sign for k in range(lo, hi + 1))
        entry = next(k for k in range(lo, hi + 1) if curv[k] * sign >= 0.3 * peak)
        # math heading grows counter-clockwise: a positive change is a left bend
        if not bends or entry - bends[-1][0] > BEND_GAP_M / DS:
            bends.append((entry, "L" if sign > 0 else "R"))
        i = j
    return bends


# ---------- one road: geometry, speed limits, maneuvers, stops ----------

def load_road(key):
    road = ROADS[key]
    data = json.loads((ROUTES / f"{key}.osrm.json").read_text())
    route = data["routes"][0]
    coords = [(lat, lon) for lon, lat in route["geometry"]["coordinates"]]
    proj = Projection(sum(c[0] for c in coords) / len(coords), sum(c[1] for c in coords) / len(coords))
    raw = [proj.xy(*c) for c in coords]
    dense, _ = resample(raw)

    # flatten steps; drop the arrive/depart pairs that via-waypoints insert
    steps = [st for leg in route["legs"] for st in leg["steps"]]
    acc = 0.0
    for st in steps:                     # where OSRM says each step starts (m along its polyline)
        st["_s"] = acc
        acc += st["distance"]
    scale = (len(dense) - 1) * DS / max(1.0, acc)
    last = len(steps) - 1
    steps = [st for i, st in enumerate(steps)
             if not (st["maneuver"]["type"] == "depart" and i > 0)
             and not (st["maneuver"]["type"] == "arrive" and i < last)]

    # locate every step and intersection on the dense polyline (monotonic search)
    pos, located = 0, []
    for i, st in enumerate(steps):
        lon, lat = st["maneuver"]["location"]
        guess = int(st["_s"] * scale / DS)
        j = nearest_index(dense, proj.xy(lat, lon), max(pos - 30, guess - 200), guess + 200)
        if i == 0:
            j = 0
        if st["maneuver"]["type"] == "arrive":
            j = len(dense) - 1
        inters = []
        for it in st.get("intersections", []):
            ilon, ilat = it["location"]
            inters.append((nearest_index(dense, proj.xy(ilat, ilon), j - 30, j + int(st["distance"]) + 60),
                           len(it["bearings"])))
        located.append((j, st, inters))
        pos = j

    # road type per dense point
    kinds = [road["default"]] * len(dense)
    for n, (j, st, _) in enumerate(located):
        end = located[n + 1][0] if n + 1 < len(located) else len(dense)
        kind = step_kind(road, st)
        for q in range(j, end):
            kinds[q] = kind

    # change the smoothing width gradually (0.2 m per m) so road-type borders do not kink
    sigmas = [SMOOTH_SIGMA[k] for k in kinds]
    for i in range(1, len(sigmas)):
        sigmas[i] = min(sigmas[i], sigmas[i - 1] + 0.2)
    for i in range(len(sigmas) - 2, -1, -1):
        sigmas[i] = min(sigmas[i], sigmas[i + 1] + 0.2)
    sm = gaussian_smooth(dense, [round(x, 1) for x in sigmas])
    pts, src = resample(sm)
    if math.hypot(pts[-1][0] - sm[-1][0], pts[-1][1] - sm[-1][1]) > 0.05:
        pts.append(sm[-1])
        src.append(len(sm) - 1.0)
    # dense index -> smoothed-path index
    to_s = [0] * len(dense)
    q = 0
    for i in range(len(dense)):
        while q + 1 < len(src) and src[q + 1] <= i:
            q += 1
        to_s[i] = q
    hdg, curv = heading_curvature(pts)
    kinds_s = [kinds[min(len(dense) - 1, int(round(s)))] for s in src]

    # speed limits; expressway traffic drifts between about 60 and 105 km/h every 300-700 m
    rng = random.Random(road["seed"])
    vlim, chunk_left, chunk_v = [], 0.0, 0.0
    for kind in kinds_s:
        if kind == "expressway":
            if chunk_left <= 0:
                chunk_left, chunk_v = rng.uniform(300, 700), rng.uniform(60, 105) / 3.6
            chunk_left -= DS
            vlim.append(chunk_v)
        else:
            chunk_left = 0.0
            vlim.append(VLIM_KMH[kind] / 3.6)
    vlim = traffic_drift(vlim, kinds_s)

    # navigation maneuvers (not curves)
    maneuvers = []
    for n, (j, st, _) in enumerate(located):
        m = st["maneuver"]
        if m["type"] in ("depart", "new name") or (m["type"] == "continue" and m.get("modifier") in (None, "straight")):
            continue
        ttype = turn_type(m["type"], m.get("modifier"))
        maneuvers.append({"i": to_s[j], "type": ttype, "dir": turn_dir(ttype), "osrm": m["type"],
                          "road": "" if ttype == "TURN_ARRIVE" else step_road([x[1] for x in located], n)})
    if road.get("bends"):
        for bi, d in find_bends(hdg, curv):
            if all(abs(bi - m["i"]) > BEND_GAP_M / DS for m in maneuvers):
                ttype = "TURN_CURVE_L" if d == "L" else "TURN_CURVE_R"
                maneuvers.append({"i": bi, "type": ttype, "dir": d, "osrm": "bend", "road": "", "bend": True})
        maneuvers.sort(key=lambda m: m["i"])

    # traffic-light stops: a seeded subset of real signalised-looking intersections
    # (4+ ways) on surface streets, about 12 m before the junction, at least 100 m apart
    stops = {}
    if road["stops"]:
        cands = []
        for j, st, inters in located:
            for ji, nb in inters:
                si = to_s[ji]
                if nb >= 4 and kinds_s[si] in ("city", "arterial") and 80 < si < len(pts) - 80:
                    if all(abs(si - c) > 40 for c in cands):
                        cands.append(si)
        srng = random.Random(road["seed"] * 101)
        chosen = []
        for c in srng.sample(cands, len(cands)):
            if len(chosen) < road["stops"] and all(abs(c - o) >= 100 for o in chosen):
                chosen.append(c)
        for c in sorted(chosen):
            stops[c - 12] = srng.uniform(20, 40)

    return {"key": key, "road": road, "proj": proj, "pts": pts, "hdg": hdg, "curv": curv, "vlim": vlim,
            "kinds": kinds_s, "maneuvers": maneuvers, "stops": stops}


def traffic_drift(vlim, kinds):
    """Expressway target speeds: ramp between drift chunks at TRAFFIC_ACCEL, then round the
    corners with a moving average over TRAFFIC_ROUND_M (other road types are left as they are)."""
    ex = [k == "expressway" for k in kinds]
    v = vlim[:]
    for i in range(1, len(v)):
        if ex[i] and ex[i - 1]:
            v[i] = min(v[i], math.sqrt(v[i - 1] ** 2 + 2 * TRAFFIC_ACCEL * DS))
    for i in range(len(v) - 2, -1, -1):
        if ex[i] and ex[i + 1]:
            v[i] = min(v[i], math.sqrt(v[i + 1] ** 2 + 2 * TRAFFIC_ACCEL * DS))
    h = int(TRAFFIC_ROUND_M / DS) // 2
    out = v[:]
    if h:
        pre = [0.0]
        for x in v:
            pre.append(pre[-1] + x)
        for i in range(len(v)):
            lo, hi = i - h, i + h
            if ex[i] and lo >= 0 and hi < len(v) and all(ex[lo:hi + 1:h]):
                out[i] = (pre[hi + 1] - pre[lo]) / (hi - lo + 1)
    return out


# ---------- motion ----------

def speed_profile(curv, vlim, stops, style):
    vmax = []
    for i, (k, vl) in enumerate(zip(curv, vlim)):
        v = vl if abs(k) < 1e-4 else min(vl, math.sqrt(style["a_lat"] / abs(k)))
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


# ---------- scenario assembly ----------

def write_route(name, R):
    pts, proj = R["pts"], R["proj"]
    geometry = []
    for i in list(range(0, len(pts), GEOMETRY_STEP_M)) + ([len(pts) - 1] if (len(pts) - 1) % GEOMETRY_STEP_M else []):
        lat, lon = proj.latlon(*pts[i])
        geometry.append([round(lat, 7), round(lon, 7)])
    steps = []
    for m in R["maneuvers"]:
        lat, lon = proj.latlon(*pts[m["i"]])
        steps.append({"s": round(m["i"] * DS, 1), "type": m["type"], "text": TURN_TEXT[m["type"]],
                      "road": m["road"], "lat": round(lat, 7), "lon": round(lon, 7)})
    doc = {"name": name, "title": R["road"]["title"], "dest": R["road"]["dest"],
           "total_m": round((len(pts) - 1) * DS, 1), "geometry": geometry, "steps": steps}
    (OUT / f"{name}.route.json").write_text(json.dumps(doc, ensure_ascii=False, separators=(",", ":")))


def build(name, R, style_name, cabin, seed):
    rng = random.Random(seed)
    style = STYLES[style_name]
    pts, hdg, stops, proj = R["pts"], R["hdg"], R["stops"], R["proj"]
    v = speed_profile(R["curv"], R["vlim"], stops, style)
    knots = timeline(v, stops)
    total = knots[-1][0]
    # guidance points: a short window around each real maneuver (the driver signals for it)
    # and each bend on a winding road (navigation announces it, but bends are not junctions:
    # nobody uses the indicator)
    turns = [(max(0, m["i"] - 8), m["i"] + 8, m["dir"], m["i"], not m.get("bend"))
             for m in R["maneuvers"] if m["dir"]]
    rows = []
    prev_s = None
    for sec in range(int(total) + 1):
        s = interp(knots, float(sec))
        i = min(int(round(s / DS)), len(pts) - 1)
        x, y = pts[i]
        speed = 0.0 if prev_s is not None and abs(s - prev_s) < 1e-6 else v[i]
        prev_s = s
        # navigation guidance and turn signal
        nxt = next((tn for tn in turns if tn[1] > i), None)
        next_m, next_dir, signal = -1, "", "OFF"
        if nxt:
            a, b, d, mi, signals = nxt
            dist = max(0.0, (mi - i) * DS)      # to the maneuver itself, as route.json steps measure it
            if dist <= 1000:
                next_m, next_dir = round(dist), d
            if signals and (i >= a or (a - i) * DS <= max(30.0, speed * 4.0)):
                signal = "LEFT" if d == "L" else "RIGHT"
        # GPS-like noise (small: the map shows the car on the road)
        noisy_speed = max(0.0, speed + rng.gauss(0, SPEED_NOISE)) if speed > 0 else 0.0
        # GNSS course accuracy improves with speed: about 0.6 deg at 18 km/h, 0.2 deg at 100 km/h
        bearing = compass_bearing(hdg[i]) + rng.gauss(0, max(0.2, 3.0 / speed) if speed > 2 else 15.0)
        nx, ny = (max(-1.5, min(1.5, rng.gauss(0, 0.5))) for _ in range(2))
        lat, lon = proj.latlon(x + nx, y + ny)
        rows.append({
            "t": sec, "lat": f"{lat:.7f}", "lon": f"{lon:.7f}",
            "speed": f"{noisy_speed:.3f}", "bearing": f"{bearing % 360:.2f}",
            "turn_signal": signal, "next_turn_m": next_m, "next_turn_dir": next_dir,
            "cabin_temp": f"{cabin['temp']:.1f}", "recirc": int(cabin["recirc"]),
            "window_open": 0, "s": f"{s:.1f}",
        })
    OUT.mkdir(parents=True, exist_ok=True)
    with open(OUT / f"{name}.csv", "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=COLUMNS)
        w.writeheader()
        w.writerows(rows)
    write_route(name, R)
    return {"name": name, "road": R["road"]["title"], "style": style_name, "duration_s": len(rows),
            "distance_m": round((len(pts) - 1) * DS), "turns": len(turns), "stops": len(stops), "cabin": cabin}


def main():
    fresh = {"temp": 22.0, "recirc": False}
    stuffy = {"temp": 26.0, "recirc": True}
    warm = {"temp": 25.0, "recirc": True}
    roads = {k: load_road(k) for k in ROADS}
    meta = [
        build("A_city_smooth", roads["city"], "smooth", fresh, 11),
        build("A_city_rough", roads["city"], "rough", fresh, 12),
        build("B_winding_smooth", roads["winding"], "smooth", stuffy, 21),
        build("B_winding_rough", roads["winding"], "rough", stuffy, 22),
        build("C_highway", roads["highway"], "normal", fresh, 31),
        build("D_family", roads["family"], "normal", warm, 41),
    ]
    with open(OUT / "scenarios.json", "w") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    for m in meta:
        print(f"{m['name']:18} {m['style']:7} {m['duration_s']:5d} s  {m['distance_m']/1000:5.1f} km  "
              f"turns {m['turns']:2d}  stops {m['stops']:2d}  {m['road']}")


if __name__ == "__main__":
    main()

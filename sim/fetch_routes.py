"""Fetch the real road routes the replay scenarios follow (run once; results are committed).

Each scenario drives a real Korean road. Waypoints (with a heading, so the router picks the
right carriageway) force the route onto the intended roads; the public OSRM demo server
returns the road polyline and the navigation maneuvers. Raw responses are cached in
sim/routes/<NAME>.osrm.json so sim/generator.py runs offline and deterministically.

    python3 sim/fetch_routes.py            # fetch only routes that are not cached yet
    python3 sim/fetch_routes.py --force    # fetch all again

Map data © OpenStreetMap contributors, available under the Open Database License (ODbL)
(https://www.openstreetmap.org/copyright). Routing by OSRM (project-osrm.org), demo server.
Be polite to the demo server: a handful of requests, one at a time, with a pause in between.
Standard library only.
"""
import json
import sys
import time
import urllib.request
from pathlib import Path

ROUTES = Path(__file__).resolve().parent / "routes"
SERVER = "https://router.project-osrm.org/route/v1/driving/"
USER_AGENT = "crave-motion-sickness-sim/1.0 (contest prototype; one-time route fetch)"

# name: (description, [(lat, lon, heading_deg or None), ...])
WAYPOINTS = {
    # 강남대로(강남역) → 우회전 테헤란로 → 좌회전 언주로 → 우회전 봉은사로 → 우회전 선릉로 → 선릉역
    "city": ("강남 테헤란로 일대 도심 격자", [
        (37.49656, 127.02838, 0), (37.49922, 127.03204, 70), (37.50472, 127.04146, 337),
        (37.50940, 127.04130, 67), (37.50710, 127.04660, 145), (37.50400, 127.04925, 145)]),
    # 북악산로(북악스카이웨이): 자하문고개 → 팔각정 → 성북동 대사관로 갈림길
    "winding": ("북악스카이웨이 (북악산로)", [
        (37.593159, 126.966727, None), (37.603669, 126.982553, None), (37.60197, 126.99778, None)]),
    # 경부고속도로 하행: 판교(달래내) → 수원신갈IC 나들목
    "highway": ("경부고속도로 판교 → 수원신갈IC", [
        (37.41268, 127.08751, 150), (37.26330, 127.103425, None)]),
    # 경부고속도로 상행(동탄) → 판교JC → 수도권제1순환 → 송파IC → 헌릉로 → 수정로 → 남한산성로
    "family": ("동탄 → 경부·수도권제1순환고속도로 → 남한산성로", [
        (37.22579, 127.10383, 0), (37.44509, 127.12469, 0), (37.47786, 127.18409, None)]),
}


def url_for(points):
    coords = ";".join(f"{lon:.6f},{lat:.6f}" for lat, lon, _ in points)
    bearings = ";".join("" if h is None else f"{h},45" for _, _, h in points)
    return (f"{SERVER}{coords}?overview=full&geometries=geojson&steps=true"
            f"&continue_straight=true&bearings={bearings}")


def fetch(name, points):
    req = urllib.request.Request(url_for(points), headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=60) as r:
        data = json.load(r)
    if data.get("code") != "Ok":
        raise RuntimeError(f"{name}: OSRM returned {data.get('code')}: {data.get('message')}")
    data["_request"] = {"waypoints": points, "attribution": "© OpenStreetMap contributors (ODbL), OSRM"}
    return data


def summary(name, data):
    r = data["routes"][0]
    print(f"{name}: {r['distance'] / 1000:.2f} km, {r['duration'] / 60:.1f} min (router estimate)")
    s = 0.0
    for leg in r["legs"]:
        for st in leg["steps"]:
            m = st["maneuver"]
            print(f"  {s:8.0f} m  {m['type']:12} {m.get('modifier', ''):12} {st['name']} {st.get('ref', '')}")
            s += st["distance"]


def main():
    force = "--force" in sys.argv
    ROUTES.mkdir(parents=True, exist_ok=True)
    first = True
    for name, (_, points) in WAYPOINTS.items():
        path = ROUTES / f"{name}.osrm.json"
        if path.exists() and not force:
            summary(name, json.loads(path.read_text()))
            continue
        if not first:
            time.sleep(3)
        first = False
        data = fetch(name, points)
        path.write_text(json.dumps(data, ensure_ascii=False))
        summary(name, data)


if __name__ == "__main__":
    main()

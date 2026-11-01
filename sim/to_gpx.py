"""Export a scenario as GPX so the emulator can "drive" it in live mode.

Emulator › Extended Controls (⋯) › Location › Routes › Load GPX/KML › Play route.
The app's live mode then gets heading/speed from the platform location provider.

    python3 sim/to_gpx.py B_winding_rough      # writes sim/out/B_winding_rough.gpx
"""
import csv
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main(name):
    start = datetime(2026, 11, 1, 9, 0, tzinfo=timezone.utc)
    with open(ROOT / "sim" / "scenarios" / f"{name}.csv") as f:
        rows = list(csv.DictReader(f))
    pts = "\n".join(
        f'      <trkpt lat="{r["lat"]}" lon="{r["lon"]}"><time>{(start + timedelta(seconds=float(r["t"]))).strftime("%Y-%m-%dT%H:%M:%SZ")}</time>'
        f'<speed>{r["speed"]}</speed><course>{r["bearing"]}</course></trkpt>'
        for r in rows)
    gpx = f'''<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="motion-care sim" xmlns="http://www.topografix.com/GPX/1/1">
  <trk><name>{name}</name>
    <trkseg>
{pts}
    </trkseg>
  </trk>
</gpx>
'''
    out = ROOT / "sim" / "out" / f"{name}.gpx"
    out.write_text(gpx)
    print(f"{out} ({len(rows)} points)")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "B_winding_rough")

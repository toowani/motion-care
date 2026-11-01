"""Drive the emulator's GPS from a scenario CSV (1 Hz) for the app's live mode.

`adb emu geo fix` sends speed and bearing as 0, so the app derives both from consecutive fixes.

    python3 -I sim/feed_geo.py ~/Library/Android/sdk/platform-tools/adb sim/scenarios/B_winding_rough.csv 90
"""
import csv
import subprocess
import sys
import time


def main(adb, path, seconds):
    with open(path) as f:
        rows = list(csv.DictReader(f))[:seconds]
    t0 = time.time()
    for i, r in enumerate(rows):
        subprocess.run([adb, "emu", "geo", "fix", r["lon"], r["lat"]], capture_output=True)
        print(f"t={r['t']} speed={float(r['speed']) * 3.6:.0f} km/h bearing={float(r['bearing']):.0f}", flush=True)
        time.sleep(max(0.0, t0 + i + 1 - time.time()))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 10**9)

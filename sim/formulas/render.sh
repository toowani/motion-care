#!/bin/sh
# Render the README formula cards (sim/formulas/f_*.html) to docs/img/f_*.png at 2x.
#   sh sim/formulas/render.sh            # all cards
#   sh sim/formulas/render.sh f_dose     # one card
#   DARK=1 sh sim/formulas/render.sh     # also write <name>_dark.png on #0d1117 into $OUT_DARK (check only)
# Needs Google Chrome and network access (MathJax 3.2.2 from cdn.jsdelivr.net).
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../.." && pwd)
CHROME=${CHROME:-"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"}
PROFILE=${PROFILE:-$(mktemp -d)}
OUT_DARK=${OUT_DARK:-$PROFILE}
W=960
names=${*:-$(cd "$HERE" && ls f_*.html | sed 's/\.html$//')}
chrome() { "$CHROME" --headless=new --disable-gpu --user-data-dir="$PROFILE" --hide-scrollbars \
  --allow-file-access-from-files --virtual-time-budget=15000 "$@" 2>/dev/null; }
for n in $names; do
  url="file://$HERE/$n.html"
  H=$(chrome --window-size=$W,2000 --dump-dom "$url" | sed -n 's/.*<title>H=\([0-9]*\)<\/title>.*/\1/p')
  [ -n "$H" ] || { echo "$n: MathJax did not finish" >&2; exit 1; }
  chrome --window-size=$W,$H --force-device-scale-factor=2 --default-background-color=00000000 \
    --screenshot="$ROOT/docs/img/$n.png" "$url"
  echo "docs/img/$n.png  ${W}x${H} CSS px"
  if [ -n "$DARK" ]; then
    chrome --window-size=$((W+80)),$((H+80)) --screenshot="$OUT_DARK/${n}_dark.png" "$url?bg=dark"
  fi
done

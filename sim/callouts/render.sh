#!/bin/sh
# Render numbered-callout screenshots: docs/img/ui_r6_<set>_callouts.jpg (2x, JPEG via sips).
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../.." && pwd)
CHROME=${CHROME:-"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"}
PROFILE=${PROFILE:-$(mktemp -d)}
chrome() { "$CHROME" --headless=new --disable-gpu --user-data-dir="$PROFILE" --hide-scrollbars \
  --allow-file-access-from-files --virtual-time-budget=5000 "$@" 2>/dev/null; }
for s in ${*:-analysis settings}; do
  url="file://$HERE/callouts.html?s=$s"
  H=$(chrome --window-size=960,2000 --dump-dom "$url" | sed -n 's/.*<title>H=\([0-9]*\)<\/title>.*/\1/p')
  png="$PROFILE/$s.png"
  chrome --window-size=960,$H --force-device-scale-factor=2 --default-background-color=ffffffff --screenshot="$png" "$url"
  sips -s format jpeg -s formatOptions 82 "$png" --out "$ROOT/docs/img/ui_r6_${s}_callouts.jpg" >/dev/null
  echo "docs/img/ui_r6_${s}_callouts.jpg  960x$H CSS px"
done

"""Embed a Pretendard subset into README SVGs so they render in the deck's typeface on GitHub.

GitHub shows README SVGs as <img>, which cannot load web fonts, so each SVG gets an inline
@font-face holding only the glyphs it uses, in only the weights it uses (WOFF, base64).
Runs after the figure generators; safe to run again (replaces its own <style>).

    pip install fonttools
    # Pretendard OTFs (SIL OFL 1.1), e.g. from
    # https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/packages/pretendard/dist/public/static/Pretendard-<Weight>.otf
    python3 sim/embed_font.py <dir with Pretendard-*.otf> [svg ...]   # default: every docs/img/*.svg using Pretendard
"""
import base64
import html
import io
import re
import sys
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parents[1]
WEIGHTS = {400: "Regular", 500: "Medium", 600: "SemiBold", 700: "Bold", 800: "ExtraBold"}
NAMED = {"normal": 400, "bold": 700}
TAG = "pretendard-embed"


def used_weights(svg):
    ws = {400}
    for w in re.findall(r'font-weight(?:="|\s*:\s*)([0-9a-z]+)', svg):
        w = NAMED.get(w, int(w) if w.isdigit() else 400)
        ws.add(min(WEIGHTS, key=lambda k: abs(k - w)))
    return sorted(ws)


def used_text(svg):
    body = re.sub(r'<style[^>]*>.*?</style>', '', svg, flags=re.S)
    chars = set()
    for t in re.findall(r'>([^<>]+)<', body):
        chars |= set(html.unescape(t))
    chars |= set("0123456789.,·-–—%()+×:/ ")
    return "".join(sorted(c for c in chars if not c.isspace() or c == " "))


def woff_b64(otf, text):
    font = TTFont(otf)
    opts = subset.Options()
    opts.flavor = "woff"
    opts.layout_features = ["*"]
    opts.name_IDs = []
    opts.notdef_outline = True
    s = subset.Subsetter(options=opts)
    s.populate(text=text)
    s.subset(font)
    buf = io.BytesIO()
    font.flavor = "woff"
    font.save(buf)
    return base64.b64encode(buf.getvalue()).decode()


def embed(path, fonts):
    svg = path.read_text(encoding="utf-8")
    svg = re.sub(r'<style id="%s">.*?</style>' % TAG, "", svg, flags=re.S)
    if "Pretendard" not in svg:
        return 0
    text = used_text(svg)
    faces = "".join(
        "@font-face{font-family:Pretendard;font-weight:%d;src:url(data:font/woff;base64,%s) format('woff')}"
        % (w, woff_b64(fonts / f"Pretendard-{WEIGHTS[w]}.otf", text))
        for w in used_weights(svg))
    m = re.search(r"<svg\b[^>]*>", svg)
    svg = svg[:m.end()] + f'<style id="{TAG}">{faces}</style>' + svg[m.end():]
    path.write_text(svg, encoding="utf-8")
    return len(svg)


if __name__ == "__main__":
    fonts = Path(sys.argv[1])
    targets = [Path(p) for p in sys.argv[2:]] or sorted((ROOT / "docs" / "img").glob("*.svg"))
    for p in targets:
        n = embed(p, fonts)
        if n:
            print(f"{p.name}: {n / 1024:.0f} KB")

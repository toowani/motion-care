"""Link buttons under the README demo (plain SVG, stdlib only).

    python3 sim/figs_criteria.py   # writes docs/img/link_*.svg
"""
from pathlib import Path

import crave_style as cs

ROOT = Path(__file__).resolve().parents[1]
IMG = ROOT / "docs" / "img"

FONT = cs.FONT
INK, MUTED, GRID = cs.INK, cs.MUTED, cs.LINE

# 24x24 line glyphs (stroke 2, round caps)
ICONS = {
    "play": ['<circle cx="12" cy="12" r="9"/>', '<path d="M10 8.5l5.5 3.5-5.5 3.5z"/>'],
    "doc": ['<path d="M6 3h9l4 4v14H6z"/>', '<path d="M14 3v5h5"/>', '<path d="M9 13h7M9 17h5"/>'],
    "down": ['<path d="M12 4v11"/>', '<path d="M7 10.5l5 5 5-5"/>', '<path d="M5 20h14"/>'],
    "box": ['<path d="M12 3l8 4.5v9L12 21l-8-4.5v-9z"/>', '<path d="M4 7.5l8 4.5 8-4.5"/>',
            '<path d="M12 12v9"/>', '<path d="M8 5.2l8 4.6"/>'],
}

def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def tw(s, size, bold=False):
    """Rough text width (Hangul ~1 em, Latin ~0.56 em) to lay out chips and wrap lines."""
    w = 0.0
    for ch in s:
        o = ord(ch)
        if 0xAC00 <= o <= 0xD7A3 or 0x3130 <= o <= 0x318F:
            w += 0.9
        elif ch == " ":
            w += 0.28
        elif ch in "il.,:;'|()·":
            w += 0.32
        elif ch.isupper() or ch in "mwMW%→–":
            w += 0.68
        else:
            w += 0.55
    return w * size * (1.06 if bold else 1.0)


class Svg:
    def __init__(self, w, h, rx=16, stroke=GRID):
        self.w, self.h = w, h
        self.parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}" '
                      f'font-family="{FONT}" font-size="14">',
                      f'<defs>{cs.grad_def("cg")}</defs>',
                      f'<rect x="0.75" y="0.75" width="{w - 1.5}" height="{h - 1.5}" rx="{rx}" fill="#FFFFFF" '
                      f'stroke="{stroke}" stroke-width="1.5"/>']

    def add(self, s):
        self.parts.append(s)

    def text(self, x, y, s, size=14, fill=INK, anchor="start", weight=400):
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{fill}" text-anchor="{anchor}" '
                 f'font-weight="{weight}">{esc(s)}</text>')

    def rect(self, x, y, w, h, fill, rx=0, stroke=None, sw=1):
        s = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        self.add(f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{rx}" fill="{fill}"{s}/>')

    def icon(self, name, cx, cy, d, circle, glyph="#FFFFFF", glyph_px=24):
        self.add(f'<circle cx="{cx}" cy="{cy}" r="{d / 2}" fill="{circle}"/>')
        k = glyph_px / 24
        self.add(f'<g transform="translate({cx - glyph_px / 2:.2f},{cy - glyph_px / 2:.2f}) scale({k:.4f})" '
                 f'fill="none" stroke="{glyph}" stroke-width="{2 / k if k < 1 else 2:.2f}" '
                 f'stroke-linecap="round" stroke-linejoin="round">' + "".join(ICONS[name]) + '</g>')

    def save(self, name):
        IMG.mkdir(parents=True, exist_ok=True)
        (IMG / name).write_text("\n".join(self.parts + ["</svg>"]) + "\n", encoding="utf-8")
        print(IMG / name)


# pill buttons in the deck's blue / navy system: (outline, icon disc, label colour)
BLUE_PILL = (cs.BLUE, "url(#cg)", cs.BLUE)
NAVY_PILL = (cs.INK, cs.INK, cs.INK)
SOFT_PILL = ("#C9D1E3", cs.MUTED, cs.MUTED)


def link_badge(name, label, sub, style, icon):
    stroke, disc, ink = style
    lw = tw(label, 19, True)
    w = round(62 + lw + (tw(sub, 15) + 16 if sub else 0) + 26)
    s = Svg(w, 52, rx=26, stroke=stroke)
    s.icon(icon, 26, 26, 40, disc, glyph_px=22)
    s.add(f'<text x="56" y="33" font-size="19" fill="{ink}" font-weight="{cs.TITLE_WEIGHT}" '
          f'letter-spacing="-0.02em">{esc(label)}</text>')
    if sub:
        s.text(56 + lw + 12, 32.5, sub, 15, cs.MUTED)
    s.save(name)


if __name__ == "__main__":
    link_badge("link_video.svg", "시연 콘티", "영상은 촬영 예정", BLUE_PILL, "doc")
    link_badge("link_apk.svg", "APK 받기", "GitHub Releases", NAVY_PILL, "down")
    link_badge("link_repo.svg", "실행 방법", "에뮬레이터 4단계", SOFT_PILL, "box")

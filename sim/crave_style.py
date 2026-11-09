"""Shared visual tokens for README figures, matching the D2SF deck (toowani.com/crave style).

Design only: these change how figures look, never what they say.
- canvas white, cards light blue-grey with a hairline border and round corners
- navy ink, one blue accent (+ blue→cyan gradient for hero elements), muted grey-blue secondary text
- Pretendard (embedded afterwards by sim/embed_font.py), bold tight headings
- semantic colours stay semantic: risk stages (green / yellow / red = the app's ring) and the
  per-variable colours of the formulas (I, D, m, τ, P) keep their meaning
"""

FONT = "Pretendard, 'Apple SD Gothic Neo', 'Noto Sans KR', 'Malgun Gothic', sans-serif"
MATH = "'Times New Roman', Times, serif"

# surfaces
BG = "#FFFFFF"
CARD = "#F6F8FC"          # card fill
CARD_ALT = "#EEF2F9"      # nested / alternate card
LINE = "#E6EAF2"          # card border, table rules
BAND = "#E7EFFF"          # light blue band / chip background

# text
INK = "#0F1838"           # headings, main text
MUTED = "#4A5470"         # secondary text
FOOT = "#8A93AB"          # captions, axis ticks, sources

# accent
BLUE = "#1F62DA"
BLUE_DEEP = "#1550D9"
CYAN = "#4CE1E9"
AQUA = "#6FE6EC"
RED = "#D03B52"           # warning text / negative accent (not the risk stage)

# risk stages (the app's ring colours: keep)
LOW, MID, HIGH = "#16A34A", "#EAB308", "#DC2626"
LOW_T, MID_T, HIGH_T = "#DCFCE7", "#FEF9C3", "#FEE2E2"
MID_INK = "#A16207"       # yellow text on white

# shape
RADIUS = 16               # card corner radius in a ~960 px wide figure (deck: 20 px at 1440)
RADIUS_SM = 10            # chips, small boxes
STROKE = 1                # card border width
PILL = 999                # chip radius (use min(h/2, PILL))

# typography scale (px, for ~960 px wide figures)
H1 = 26                   # figure title
H2 = 19                   # card title
BODY = 15
SMALL = 13
TITLE_WEIGHT = 800
TITLE_TRACKING = "-0.03em"


def grad_def(gid="cg"):
    """SVG <linearGradient> for the brand gradient (135°)."""
    return (f'<linearGradient id="{gid}" x1="0" y1="0" x2="1" y2="1">'
            f'<stop offset="0" stop-color="{BLUE_DEEP}"/><stop offset=".4" stop-color="{BLUE}"/>'
            f'<stop offset="1" stop-color="{CYAN}"/></linearGradient>')


def kicker(x, y, text, size=13):
    """Small uppercase label with a dot in front, as on every deck slide."""
    return (f'<circle cx="{x + 3.5}" cy="{y - size * 0.35:.1f}" r="3.5" fill="{BLUE}"/>'
            f'<text x="{x + 13}" y="{y}" font-size="{size}" font-weight="700" fill="{BLUE}" '
            f'letter-spacing=".06em">{text}</text>')

"""README hero: how the app is used, from the family's point of view (plain SVG, stdlib only).

    python3 sim/figs_hero.py      # writes docs/img/at_a_glance.svg

Title + one-line takeaway on top, the app icon in the centre and the two core functions as
top-to-bottom loops on either side: left = the car does it by itself, right = one button.
Big icons, 2-4 word labels, no formula letters.
"""
import crave_style as cs
from figs_diagrams import ICONS, INK, WHITE, Fig, tw

ICONS.update({
    "rise": '<path d="M3 18l5.5-5.5 4 3.5L20 8.5"/><path d="M14.5 8.5H20V14"/>',
    "bell": '<path d="M6 16.5v-5.5a6 6 0 0 1 12 0v5.5l1.8 1.8H4.2z"/><path d="M10 21a2.2 2.2 0 0 0 4 0"/>',
    "screen": '<rect x="3" y="4.5" width="18" height="12" rx="2"/><path d="M9 20.5h6M12 16.5v4"/>',
    "speech": '<path d="M4 4.5h16v11H11l-4.5 4v-4H4z"/><path d="M8 9h8M8 12h5"/>',
    "carwin": '<path d="M2.5 16.5v-4l2.8-5.5h10.2l3.8 5.5h2.2v4z"/><circle cx="7" cy="16.8" r="2"/><circle cx="17" cy="16.8" r="2"/>'
              '<path d="M7 11.5l1.6-3h3.4v3z"/><path d="M14 3.5h5M15.5 6h5"/>',
    "bookmark": '<path d="M6 3.5h12v17l-6-4-6 4z"/><path d="M9 9.5l2 2 4-4"/>',
})

# app launcher glyph (android/app/src/main/res/drawable/ic_launcher_foreground.xml, 108 x 108 viewport)
CAR = ("M33,60 L33,54 Q33,50 37,49.5 L41,49 Q44,40 52,40 L58,40 Q64,40 68,49 L71,49.5 Q75,50.5 75,55 L75,60 "
       "Q75,63 72,63 L70.5,63 A6,6 0 0 0 58.5,63 L49.5,63 A6,6 0 0 0 37.5,63 L36,63 Q33,63 33,60 Z "
       "M48.2,53.5 a1.5,1.5 0 1,0 3,0 a1.5,1.5 0 1,0 -3,0 Z M58.8,53.5 a1.5,1.5 0 1,0 3,0 a1.5,1.5 0 1,0 -3,0 Z "
       "M50.4,56.8 Q55,62.2 59.6,56.8 Q55,59.4 50.4,56.8 Z")
WHEELS = "M39.3,64 a4.2,4.2 0 1,0 8.4,0 a4.2,4.2 0 1,0 -8.4,0 Z M60.3,64 a4.2,4.2 0 1,0 8.4,0 a4.2,4.2 0 1,0 -8.4,0 Z"
BREEZE = "M57,36.5 Q62,31 68,33.5"
LEAF = "M68,33.5 Q70,26.5 77,26.5 Q76,34 68,33.5 Z"

SUB = cs.MUTED                                        # sub-labels: dark enough on white
# both sides share the deck's blue system (light card, blue icons, gradient header pill)
AUTO, AUTO_T, AUTO_BG = cs.BLUE, cs.BAND, cs.CARD        # the car does it
CALM, CALM_T, CALM_BG = cs.BLUE, cs.BAND, cs.CARD        # the button side (not an alarm)
BTN = "#EA580C"                                       # the in-app relief button is orange (as in the app)
RISK, RISK_T = "#B91C1C", cs.HIGH_T                   # risk stage colours keep their meaning
WARN, WARN_T = "#92400E", "#FEF3C7"

PW, TOP, PITCH, TILE_H = 425, 122, 116, 80


def app_icon(f, cx, cy, size):
    k = size / 108
    f.add(f'<g transform="translate({cx - size / 2:.1f} {cy - size / 2:.1f}) scale({k:.4f})">'
          f'<rect width="108" height="108" rx="24" fill="#363636"/>'
          f'<g transform="translate(0 4.24) translate(54 54) scale(1.06) translate(-54 -54)">'
          f'<path d="{CAR}" fill="#FFFFFF" fill-rule="evenodd"/><path d="{WHEELS}" fill="#FFFFFF"/>'
          f'<path d="{BREEZE}" fill="none" stroke="#FFFFFF" stroke-width="2.2" stroke-linecap="round"/>'
          f'<path d="{LEAF}" fill="#FFFFFF"/></g></g>')


def big_icon(f, cx, cy, name, fg, scale):
    f.add(f'<g transform="translate({cx - 12 * scale:.1f} {cy - 12 * scale:.1f}) scale({scale})" fill="none" stroke="{fg}" '
          f'stroke-width="{2.4 / scale:.2f}" stroke-linecap="round" stroke-linejoin="round">{ICONS[name]}</g>')


def tile(f, cx, cy, icons, fg, bg, button=False):
    """Rounded tile holding 1-3 big icons; returns its half width."""
    n = len(icons)
    step, sc = (48, 1.75) if n < 3 else (44, 1.6)
    w = 44 + step * n
    if button:
        f.rect(cx - w / 2, cy - TILE_H / 2, w, TILE_H, bg, None, TILE_H / 2)
    else:
        f.rect(cx - w / 2, cy - TILE_H / 2, w, TILE_H, bg, None, 22)
    for i, ic in enumerate(icons):
        big_icon(f, cx - step / 2 * (n - 1) + step * i, cy, ic, fg, sc)
    return w / 2


def panel(f, x0, head, sub, accent, tint, bg, steps, joins, back):
    """Panel: header, steps top to bottom, dashed arrow back to the first step."""
    H = 4 * PITCH + 106
    f.rect(x0, TOP, PW, H, bg, cs.LINE, 20, 1)
    w = f.chip(x0 + 22, TOP + 20, head, WHITE, "url(#cgt)", 20, 42, pad=18, weight=800)
    f.text(x0 + 22 + w + 14, TOP + 48, sub, 18, accent, weight=700)
    cx, lx = x0 + 116, x0 + 218
    ys = [TOP + 118 + i * PITCH for i in range(4)]
    halves = []
    for y, st in zip(ys, steps):
        halves.append(tile(f, cx, y, st["icons"], st["fg"], st["bg"], st.get("button", False)))
        f.text(lx, y - 4, st["title"], 20, st.get("tfg", INK), weight=800)
        f.text(lx, y + 22, st["sub"], 15.5, SUB)
    for i, j in enumerate(joins):
        a, b = ys[i] + TILE_H / 2 + 5, ys[i + 1] - TILE_H / 2 - 5
        if j == "+":
            f.text(cx, (a + b) / 2 + 9, "+", 26, accent, "middle", 700)
        else:
            f.arrow([(cx, a), (cx, b)], accent, 3, head=12)
    # loop back: down, left, up, into the first tile
    yb = ys[3] + TILE_H / 2 + 34
    xl = x0 + 16
    f.arrow([(cx, ys[3] + TILE_H / 2 + 5), (cx, yb), (xl, yb), (xl, ys[0]), (cx - halves[0] - 6, ys[0])],
            accent, 3, dash="8 6", head=12)
    f.text(cx + 18, yb + 7, back, 19, accent, weight=800)


def at_a_glance():
    W = 1000
    f = Fig(W, TOP + 4 * PITCH + 106 + 20)
    f.text(32, 54, "켜 두면 차가 알아서, 힘들면 버튼 하나", 30, INK, weight=800)
    f.text(32, 90, "뒷좌석 아이(4–13세)를 태운 가족 차 · 운전자가 한 번 켜 두면 주행 내내 뒤에서 계산", 17.5, SUB)
    panel(f, 20, "① 차가 알아서", "켜 두기만 하면", AUTO, AUTO_T, AUTO_BG, [
        {"icons": ["child", "rise"], "fg": RISK, "bg": RISK_T, "title": "아이 멀미 위험 오름", "tfg": RISK,
         "sub": "흔들림 · 더위로 멀미 지수 계산"},
        {"icons": ["screen", "bell"], "fg": WARN, "bg": WARN_T, "title": "화면 · 소리로 알림", "tfg": WARN,
         "sub": "주의 단계에 들어서면"},
        {"icons": ["wind", "thermo"], "fg": AUTO, "bg": AUTO_T, "title": "바깥 공기 · 온도 낮춤",
         "sub": "같은 순간 외기 순환 · 22°C"},
        {"icons": ["steering", "speech"], "fg": AUTO, "bg": AUTO_T, "title": "운전자에게 권고",
         "sub": "“브레이크 부드럽게”"},
    ], ["→", "+", "+"], "실내가 나아지면 다시 계산")
    # centre: the app
    cx = W / 2
    app_icon(f, cx, 400, 96)
    f.text(cx, 476, "멀미 케어", 20, INK, "middle", 800)
    f.text(cx, 500, "Pleos 차량 앱", 14.5, SUB, "middle")
    panel(f, W - 20 - PW, "② 버튼 하나", "아이가 힘들어하면", CALM, CALM_T, CALM_BG, [
        {"icons": ["hand"], "fg": WHITE, "bg": BTN, "button": True, "title": "“멀미 완화” 버튼", "tfg": BTN,
         "sub": "화면에서 한 번 누름"},
        {"icons": ["carwin", "wind", "speaker"], "fg": CALM, "bg": CALM_T, "title": "자동 조치",
         "sub": "창문 · 환기 · 음성 안내"},
        {"icons": ["bookmark"], "fg": CALM, "bg": CALM_T, "title": "그 순간을 기억",
         "sub": "누른 순간 흔들림 기록"},
        {"icons": ["person_slider"], "fg": CALM, "bg": CALM_T, "title": "개인 기준 맞춤",
         "sub": "버튼 기록으로 기준선 학습"},
    ], ["→", "→", "→"], "링이 우리 아이 기준으로")
    f.save("at_a_glance.svg")


if __name__ == "__main__":
    at_a_glance()

"""The one-sentence introduction as a figure (README top): docs/img/one_liner.svg

    python3 sim/figs_oneliner.py     # then: python3 sim/embed_font.py <Pretendard dir> docs/img/one_liner.svg
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import crave_style as cs
from figs_tables import Svg, INK, MUTED, LINE, BLUE, BAND

W, H = 960, 410
HI = BLUE          # keyword colour (brand blue)


def one_liner():
    s = Svg(W, H)
    size, lh, y = 27, 50, 84
    lines = [
        ["차량 주행 중 ", ("예고 없는 움직임", HI, "bold"), ", ", ("흔들림 누적", HI, "bold"), ", ", ("실내 환경", HI, "bold"), " 때문에"],
        ["사람마다 다르게 나타나는 멀미를"],
        [("저주파", HI, "bold"), ", ", ("환기", HI, "bold"), ", ", ("음성 안내", HI, "bold"), "를 통해 해결하며,"],
        ["그 순간을 기록해 ", ("멀미 기준을 학습", HI, "bold"), "하는"],
        [("개인화된 Pleos 차량용 앱", INK, "bold"), "입니다."],
    ]
    for segs in lines:
        s.rich(48, y, segs, size, fill=INK, weight=600)
        y += lh
    s.line(48, y - 22, W - 48, y - 22, LINE, 1)
    s.text(48, y + 26, "알려 주는 데서 멈추지 않습니다.", 30, HI, "start", 800, track="-0.03em")
    s.h = int(y + 56)
    s.parts[0] = (f'<rect x="0.75" y="0.75" width="{W - 1.5}" height="{s.h - 1.5}" rx="{cs.RADIUS}" '
                  f'fill="{cs.BG}" stroke="{LINE}" stroke-width="1.5"/>')
    s.save("one_liner.svg", "멀미 케어 한 줄 소개: 예고 없는 움직임, 흔들림 누적, 실내 환경 때문에 사람마다 다르게 나타나는 멀미를 저주파, 환기, 음성 안내로 해결하고 그 순간을 기록해 멀미 기준을 학습하는 개인화된 Pleos 차량용 앱. 알려 주는 데서 멈추지 않습니다.")


if __name__ == "__main__":
    one_liner()

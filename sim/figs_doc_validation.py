"""Infographic figures for docs/validation.md (minimal-text version).

    python3 sim/figs_doc_validation.py   # writes docs/img/doc_validation_<n>.svg

Text is copied from docs/validation.md (numbers, units, caveats kept); layout helpers come from figs_tables.py.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import crave_style as cs
import figs_tables as ft
from figs_tables import Svg, tw, wrap, INK, MUTED, LINE, CARD, BLUE, BAND, FOOT

X0, W = 28, 904
SZ, LH = 13.5, 20


class Doc(Svg):
    def __init__(self):
        super().__init__(960, 100)
        self.y = 28

    def save(self, name, title):
        self.h = self.y + 14
        self.parts[0] = (f'<rect x="0.75" y="0.75" width="{self.w - 1.5}" height="{self.h - 1.5}" rx="{cs.RADIUS}" '
                         f'fill="{cs.BG}" stroke="{LINE}" stroke-width="1.5"/>')
        super().save(name, title)

    # ---- blocks (each advances self.y)
    def head(self, icon, text, chips=()):
        self.icon(icon, X0 + 22, self.y + 22, BLUE, BAND, 20)
        self.text(X0 + 56, self.y + 29, text, 19, INK, "start", 800)
        x = X0 + 56 + tw(text, 19, True) + 16
        for lab, fg, bg in chips:
            x += self.chip(x, self.y + 10, lab, fg, bg, bg, 13, 26, bold=True, pad=11) + 8
        self.y += 56

    def note(self, text, size=SZ, fill=MUTED):
        for ln in wrap(text, size, W - 8):
            self.text(X0 + 4, self.y + 14, ln, size, fill)
            self.y += LH
        self.y += 8

    def bullets(self, items, title=None, icon=None):
        """items: (lead, text) pairs inside one card."""
        tx, tws = X0 + 40, W - 40 - 24
        laid = []
        for lead, txt in items:
            full = (lead + " " + txt).strip()
            laid.append((lead, wrap(full, SZ, tws - 14)))
        h = 20 + (50 if title else 0) + sum(len(w) * LH for _, w in laid) + 8 * len(laid)
        self.card(X0, self.y, W, h)
        y = self.y + 16
        if title:
            if icon:
                self.icon(icon, X0 + 34, y + 20, BLUE, BAND, 18)
            self.text(X0 + 64, y + 26, title, 16, INK, "start", 800)
            y += 50
        for lead, lines in laid:
            self.circle(tx + 3, y + 9.5, 3, BLUE)
            for k, ln in enumerate(lines):
                if k == 0 and lead and ln.startswith(lead):
                    self.rich(tx + 14, y + 14, [(lead, INK, "bold"), (ln[len(lead):], MUTED)], SZ)
                else:
                    self.text(tx + 14, y + 14, ln, SZ, MUTED)
                y += LH
            y += 8
        self.y += h + 14

    def table(self, cols, rows, size=13, hl=()):
        """cols: (header, width, anchor). cell text starting with '!' is bold blue."""
        pad = 10
        def cell_lines(t, w, bold=False):
            return wrap(t.lstrip("!"), size, w - 2 * pad, bold)
        hl_lines = [cell_lines(h, w, True) for h, w, _ in cols]
        hh = max(len(x) for x in hl_lines) * 18 + 16
        rws = []
        for r in rows:
            ls = [cell_lines(c, cols[i][1], c.startswith("!")) for i, c in enumerate(r)]
            rws.append((ls, max(len(x) for x in ls) * 18 + 16))
        total = hh + sum(h for _, h in rws)
        self.card(X0, self.y, W, total)
        self.rect(X0 + 1, self.y + 1, W - 2, hh, BAND, cs.RADIUS - 1)
        self.rect(X0 + 1, self.y + hh - 12, W - 2, 12, BAND)
        x = X0
        for (h, w, a), ls in zip(cols, hl_lines):
            tx = x + pad if a == "start" else x + w - pad if a == "end" else x + w / 2
            for k, ln in enumerate(ls):
                self.text(tx, self.y + 22 + k * 18, ln, size, INK, a, 700)
            x += w
        y = self.y + hh
        for i, (ls, h) in enumerate(rws):
            if i:
                self.line(X0 + 14, y, X0 + W - 14, y, LINE)
            x = X0
            for (c, w, a), cl, raw in zip(cols, ls, rows[i]):
                bold = raw.startswith("!")
                tx = x + pad if a == "start" else x + w - pad if a == "end" else x + w / 2
                for k, ln in enumerate(cl):
                    self.text(tx, y + 23 + k * 18, ln, size, BLUE if bold else (INK if x == X0 else MUTED), a, 700 if (bold or x == X0) else 400)
                x += w
            y += h
        self.y += total + 14

    def flow(self, steps):
        n = len(steps)
        gap = 22
        w = (W - gap * (n - 1)) / n
        ls = [wrap(t, SZ, w - 20) for t in steps]
        h = max(len(x) for x in ls) * LH + 24
        for i, (t, l) in enumerate(zip(steps, ls)):
            x = X0 + i * (w + gap)
            self.card(x, self.y, w, h, CARD)
            for k, ln in enumerate(l):
                self.text(x + w / 2, self.y + 26 + k * LH - (len(l) - 1) * 0, ln, SZ, INK, "middle", 600)
            if i < n - 1:
                self.arrow(x + w + 4, self.y + h / 2, x + w + gap - 4, self.y + h / 2, BLUE, 2)
        self.y += h + 14


OKC = (ft.LOW, ft.LOW_T)


def fig1():
    s = Doc()
    s.head("search", "실차 없이 두 가지로 검증", [("11.12 기준", BLUE, BAND)])
    s.bullets([
        ("1절 합리성 점검 G2:", "실제 도로를 따라가는 시나리오 6개를 돌려 지수 · 알림이 상식에 맞는지 확인"),
        ("2절 실제 주행 비교:", "실제 고속도로 45초와 같은 엔진으로 비교"),
        ("11.12 기준:", "멀미 대역 필터를 문헌 가중 곡선에 맞추고 (3절), 고속도로 속도 변화를 실제 주행에 다시 맞춘 뒤 (2절)"),
    ])
    s.head("route", "시나리오는 모두 실제 도로")
    s.table([("시나리오", 150, "start"), ("실제 도로", 754, "start")], [
        ["도심", "강남역 → 테헤란로 → 언주로 → 봉은사로 → 선릉로 → 선릉역"],
        ["굽은 길", "북악스카이웨이(북악산로): 자하문고개 → 팔각정 → 대사관로"],
        ["고속도로", "경부고속도로 판교(달래내) → 수원신갈IC"],
        ["가족 나들이", "동탄 → 경부고속도로 → 판교JC → 수도권제1순환고속도로 → 송파IC → 남한산성로"],
    ], size=14)
    s.text(X0 + 4, s.y + 12, "경로가 만들어지는 순서", 14, INK, "start", 700)
    s.y += 24
    s.flow(["sim/fetch_routes.py가 OSRM (OpenStreetMap 데이터)에서 경로를 받아 sim/routes/에 저장",
            "1 m 간격으로 다시 나눔",
            "도로 종류별로 매끄럽게",
            "곡률로 속도를 정함"])
    s.note("회전 예고와 방향지시등은 경로의 실제 교차로 회전에서 나옵니다.")
    s.save("doc_validation_1.svg", "검증 개요: 두 가지 검증 방법과 실제 도로 4개 시나리오, 경로 생성 순서")


def fig2():
    s = Doc()
    s.head("check", "1. 합리성 점검 G2", [("10개 모두 통과", ft.LOW, ft.LOW_T)])
    s.table([("시나리오", 160, "start"), ("길이", 70, "end"), ("평균 지수", 84, "end"), ("최대 지수", 84, "end"),
             ("누적량 D", 84, "end"), ("주의", 70, "end"), ("높음", 70, "end"), ("예고", 282, "start")], [
        ["도심 · 부드럽게", "8.8분", "0.68", "1.03", "16.6", "95초", "0초", "7회 (4곳 + 신호 대기 3)"],
        ["도심 · 거칠게", "8.2분", "1.07", "1.54", "25.2", "362초", "41초", "7회 (4곳 + 신호 대기 3)"],
        ["굽은 길 · 부드럽게", "7.9분", "1.65", "2.02", "37.9", "26초", "421초", "25회 (커브)"],
        ["굽은 길 · 거칠게", "6.2분", "2.58", "3.62", "53.3", "12초", "341초", "25회 (커브)"],
        ["고속도로", "13.0분", "0.36", "0.60", "10.9", "0초", "0초", "1회 (나들목)"],
        ["가족 나들이", "34.7분", "0.76", "2.47", "48.0", "280초", "299초", "9회 (8곳 + 신호 대기 1)"],
    ], size=13.5)
    s.save("doc_validation_2.svg", "합리성 점검 G2: 시나리오 6개의 길이, 평균 · 최대 지수, 누적량, 주의 · 높음 시간, 예고 횟수")


def fig3():
    s = Doc()
    s.head("graph", "1절 결과")
    s.bullets([
        ("순서:", "고속도로 < 도심 < 굽은 길 (부드럽게 · 거칠게 모두)"),
        ("운전 방식:", "같은 길에서 거칠게 몰면 시간 보정 누적량 (D/√t, t는 분)이 도심 1.57배, 굽은 길 1.58배"),
        ("첫 알림:", "굽은 길 거칠게 19초 (안내 · 공조), 도심 거칠게 25초, 굽은 길 부드럽게 26초, 도심 부드럽게 324초 (주의 95초), 고속도로 없음, 가족 나들이 951초"),
        ("고속도로:", "알림 0회 (회전 예고는 나들목 1번), 최대 지수 0.60"),
    ], "앱에서 보이는 결과", "gauge")
    s.bullets([
        ("휴게소 시점 (앱은 묻지 않음 · 확장 기능):", "굽은 길 거칠게 210초 (3분 30초)"),
        ("예고 (앱 화면 · 소리에서는 꺼 둠):", "회전 · 커브마다 마지막 예고가 지나기 2–15초 전 (실제 2–7초 전; 2초는 도심 거칠게의 신호 대기 후 출발 재예고). 회전 앞 신호 대기 (3초 넘게 정지)는 출발할 때 한 번 더"),
        ("공조 닫힌 고리:", "굽은 길 (26°C · 내기 시작)에서 외기 전환 후 환경 계수 0.84 → 0.99. 앱이 대응하지 않으면 0.825에 머묾"),
        ("가족 모드 점검 (화면에서는 숨김):", "가족 알림이 한 번 (1757초), 지수만 본 \"높음\" (1784초)보다 27초 먼저"),
        ("가족 모드 예시 (검증 아님):", "예시 프로필에서 민준 1757초, 서연 1887초에 기준선 도달. 기준선이 낮은 아이가 먼저인 것은 정의상 당연"),
    ], "엔진에만 있는 동작 (앱 화면에는 없음)", "gear")
    s.save("doc_validation_3.svg", "1절 결과: 시나리오 순서, 운전 방식 차이, 첫 알림 시각, 엔진에만 있는 동작")


def fig4():
    s = Doc()
    s.head("loop", "필터를 바꾸며 달라진 동작", [("11.11", BLUE, BAND)])
    s.note("멀미 대역 필터 (3절)가 0.1–0.25 Hz를 더 이상 깎지 않아 같은 주행의 지수가 커졌습니다. 알림 기준 (주의 0.9, 높음 1.5)과 다른 상수는 그대로여서 알림이 늘었습니다. 감추지 않고 적습니다.", 14, INK)
    s.table([("시나리오", 170, "start"), ("평균 지수 (전 → 후)", 170, "end"), ("최대 지수 (전 → 후)", 170, "end"),
             ("주의 · 높음 (전 → 후)", 220, "end"), ("첫 알림 (전 → 후)", 174, "end")], [
        ["도심 · 부드럽게", "0.62 → 0.68", "0.92 → 1.03", "!0 · 0초 → 95 · 0초", "!없음 → 324초"],
        ["도심 · 거칠게", "0.89 → 1.07", "1.29 → 1.54", "!309 · 0초 → 362 · 41초", "56초 → 25초"],
        ["굽은 길 · 부드럽게", "1.39 → 1.65", "1.82 → 2.02", "29 · 408초 → 26 · 421초", "36초 → 26초"],
        ["굽은 길 · 거칠게", "2.04 → 2.58", "2.77 → 3.62", "25 · 326초 → 12 · 341초", "21초 → 19초"],
    ], size=13.5)
    s.bullets([
        ("도심 · 부드럽게에서 이제 \"주의\"가 한 번 (324초, 95초 동안) 나옵니다.", "예전에는 알림이 없었습니다. 부드러운 도심 주행도 신호 출발 · 정지가 이어지면 0.1–0.25 Hz 흔들림이 꽤 있고, 문헌 가중대로 세면 최대 지수가 주의 기준을 잠깐 넘습니다. 이 기준이 실제 동승자의 멀미와 맞는지는 동승 조사로 확인해야 합니다 (4절)."),
        ("도심 · 거칠게는 \"높음\"까지 41초 올라갑니다.", "휴게소 제안 시점은 굽은 길 거칠게 225초 → 210초."),
        ("", "고속도로 · 가족 나들이 숫자는 2절의 생성기 보정도 함께 반영한 값입니다."),
    ])
    s.save("doc_validation_4.svg", "필터 변경 전후: 시나리오 4개의 평균 · 최대 지수, 주의 · 높음 시간, 첫 알림 변화")


def fig5():
    s = Doc()
    s.head("road", "2. 실제 주행 데이터와 비교", [("현실성 점검", BLUE, BAND)])
    s.note("comma2k19 (MIT) 예시 구간: 미국 캘리포니아 고속도로 출퇴근 45초. 20 Hz 위치 · 속도를 1 Hz 속도 · 방향으로 바꿔 앱과 같은 엔진에 넣었습니다.", 14, INK)
    s.table([("출처", 230, "start"), ("평균 속도", 120, "end"), ("상위 5% |앞뒤 가속도|", 200, "end"),
             ("상위 5% |옆 가속도|", 214, "end"), ("평균 지수", 140, "end")], [
        ["실제 고속도로 45초", "61 km/h", "0.75 m/s²", "0.08 m/s²", "0.42"],
        ["시뮬 고속도로 (같은 45초)", "78 km/h", "0.73 m/s²", "0.19 m/s²", "0.35"],
        ["시뮬 고속도로 (전체 13분)", "79 km/h", "0.71 m/s²", "0.24 m/s²", "0.36"],
    ], size=13.5)
    s.bullets([
        ("처음 시나리오:", "시속 100 km 정속이라 앞뒤 가속도가 실제의 0.22배였습니다. 차량 흐름에 따른 속도 변화 (60–105 km/h)를 넣어 1.4배로 맞췄습니다 (기준 0.5–2배, 실제 도로로 바꾼 뒤 1.41배)."),
        ("필터를 바꾸자 2.13배로 기준 (0.5–2배)을 넘었습니다.", "같은 필터를 양쪽에 쓰므로 필터 이득 탓이 아니라 스펙트럼 차이입니다. 시뮬 고속도로는 60–105 km/h 사이 목표 속도를 운전 방식의 최대 가속도 (2 m/s², 감속 2.5 m/s²)로 바로 바꿔, 몇 초짜리 사다리꼴 가속이 생겼습니다 (1초 차분 가속도 에너지의 약 40%가 0.1 Hz 위). 실제 45초 구간의 속도 변화는 약 0.5–1.5 m/s²로 천천히 시작하고 끝납니다. 예전 필터는 0.1–0.25 Hz를 3–9 dB 깎아 이 차이를 가렸습니다."),
    ], "보정 경과", "ruler")
    s.save("doc_validation_5.svg", "실제 주행 비교: comma2k19 고속도로 45초와 시뮬 고속도로의 속도, 가속도, 지수")


def fig6():
    s = Doc()
    s.head("gear", "생성기 보정", [("11.12", BLUE, BAND), ("결과 0.97배, 기준 안", ft.LOW, ft.LOW_T)])
    s.flow(["목표 속도 가감속을 1 m/s²로 (실제 구간의 가운데 값, 교통 흐름 속 가감속이지 급가속이 아님)",
            "바뀌는 구간을 50 m (시속 90 km에서 약 2초) 이동 평균으로 둥글게 (가가속도 제한)",
            "커브 · 신호 정지 · 다른 도로 종류와 GPS 잡음은 그대로"])
    s.bullets([
        ("기준은 그대로:", "기준 (0.5–2배)과 알림 기준은 바꾸지 않았습니다."),
        ("바뀐 시나리오:", "고속도로 구간이 있는 고속도로 · 가족 나들이 두 개뿐 (도심 · 굽은 길 CSV는 그대로). 고속도로 평균 지수 0.55 → 0.36, 최대 0.88 → 0.60, 가족 나들이 평균 0.88 → 0.76."),
        ("같은 45초 평균 지수:", "실제 0.42, 시뮬 0.35로 둘 다 알림 기준 (0.9) 아래. 예전 표 (0.46 vs 0.33)는 지수 창이 덜 찬 동안 찬 만큼으로 나눴습니다. 이제 늘 60초로 나누므로 45초 구간은 두 값 모두 낮게 나오고, 비교는 같은 길이끼리만 합니다."),
    ])
    s.card(X0, s.y, W, 70, ft.MID_T, ft.MID_T)
    s.icon("flag", X0 + 36, s.y + 35, ft.MID_INK, "#FFFFFF", 20)
    ls = wrap("한계: 실제 45초 구간은 직선이라 옆 가속도는 비교할 수 없습니다. 굽은 길 실측 구간이나 대중교통 탑승 기록으로 추가 확인이 필요합니다.", 14, W - 110)
    for k, ln in enumerate(ls):
        s.text(X0 + 70, s.y + 30 + k * 20, ln, 14, ft.MID_INK, "start", 700)
    s.y += 70 + 14
    s.save("doc_validation_6.svg", "생성기 보정 (11.12): 고속도로 목표 가속도 1 m/s², 50 m 이동 평균, 한계는 직선 구간")


def fig7():
    s = Doc()
    s.head("wave", "3. 엔진 주파수 응답", [("멀미 대역 필터", BLUE, BAND)])
    s.note("진폭 1 m/s²의 옆 (또는 앞뒤) 가속도 사인파를 시속 54 km 일정 속도의 1 Hz 진행 방향 (또는 속도) 신호로 바꿔 엔진에 넣고, 엔진이 낸 ay (ax)의 RMS를 같은 시각의 실제 가속도 RMS와 비교했습니다.", 14, INK)
    s.note("기준: 옆 흔들림 멀미 가중 곡선 (Donohew & Griffin 2004). 0.0315–0.25 Hz 평탄, 그 위로 옥타브당 12 dB 감소, 0.0315 Hz 아래는 실험 범위 밖.")
    s.table([("주파수", 200, "start"), ("예전 엔진", 230, "end"), ("지금 엔진", 230, "end"), ("문헌", 244, "end")], [
        ["0.01 Hz", "−11.1 dB", "!−20.0 dB", "범위 밖"],
        ["0.0315 Hz", "−2.2 dB", "!−3.0 dB", "대역 끝"],
        ["0.05 Hz", "0.0 dB", "!−0.5 dB", "0 dB"],
        ["0.1 Hz", "−2.6 dB", "!+0.3 dB", "0 dB"],
        ["0.15 Hz", "−5.3 dB", "!+0.5 dB", "0 dB"],
        ["0.2 Hz", "−6.3 dB", "!+0.2 dB", "0 dB"],
        ["0.25 Hz", "−8.8 dB", "!−1.0 dB", "0 dB"],
        ["0.5 Hz", "−13.5 dB", "!−11.8 dB", "−12 dB"],
    ], size=13.5)
    s.save("doc_validation_7.svg", "엔진 주파수 응답 표: 예전 엔진, 지금 엔진, 문헌 가중을 주파수별 dB로 비교")


def fig8():
    s = Doc()
    s.head("search", "필터: 예전 원인과 지금 구성")
    s.bullets([
        ("주원인:", "0.1–0.25 Hz가 3–9 dB 낮았던 주원인은 속도 · 요 각속도에 건 지수 이동 평균 (α = 0.5). 1 Hz에서 이는 약 0.1 Hz부터 깎이는 1차 저역 통과라 0.25 Hz에서 약 7 dB를 잃습니다."),
        ("추가 손실:", "1초 차분이 0.25 Hz에서 0.9 dB를 더 잃고, 최근 10초 평균 빼기는 0.1–0.25 Hz에서 ±1 dB 안팎의 물결을 더했습니다."),
    ], "예전 원인", "x")
    s.bullets([
        ("구성:", "이동 평균을 끄고 (α = 1), 축마다 2차 필터 두 개를 1초마다 한 번씩 돌립니다 (상태 변수 8개, 계산량 고정)."),
        ("① 고역 통과:", "0.0315 Hz 2차 버터워스 (꾸준한 가속 · 긴 커브 제거)"),
        ("② 저역 통과:", "0.27 Hz, Q = 0.95 2차 (아날로그 극을 z = e^(sT)로 옮기고 영점 하나를 z = −0.35에 둠, 0 Hz 이득 1)"),
        ("계수:", "shared/params.json의 band* 값에서 Python과 Kotlin이 같은 식으로 계산합니다."),
    ], "지금 필터", "check")
    s.bullets([
        ("0.05–0.25 Hz:", "문헌 평탄 구간과 +0.5 / −1.0 dB 안"),
        ("0.0315 Hz:", "−3 dB"),
        ("0.5 Hz:", "문헌 −12 dB와 0.2 dB 차이"),
        ("지수 변화:", "0.1–0.25 Hz를 덜 깎으므로 같은 주행의 지수가 커졌습니다 (1절 \"달라진 동작\" 표: 같은 시나리오에서 평균 지수 9–27% 증가). 알림 기준 (0.9 / 1.5)과 다른 값은 바꾸지 않았습니다."),
    ], "결과", "ruler")
    s.save("doc_validation_8.svg", "필터: 예전 지연의 원인(지수 이동 평균), 지금 구성(고역 + 저역 2차 필터), 결과")


def fig9():
    s = Doc()
    s.head("noentry", "4. 기획서에 쓸 정직한 한계")
    for ic, t in [("person", "사람이 실제로 멀미했는지 (정답 데이터)는 시뮬레이션으로 알 수 없습니다. 기준선과 환경 계수 크기는 문헌값과 가정에서 시작하고, 출시 후 정차 후 한 질문으로 사용자별로 보정합니다."),
                  ("road", "실제 주행 비교는 45초 고속도로 한 구간뿐입니다.")]:
        ls = wrap(t, 15, W - 100)
        h = len(ls) * 22 + 34
        s.card(X0, s.y, W, h, ft.MID_T, ft.MID_T)
        s.icon(ic, X0 + 36, s.y + h / 2, ft.MID_INK, "#FFFFFF", 20)
        for k, ln in enumerate(ls):
            s.text(X0 + 72, s.y + 28 + k * 22, ln, 15, INK, "start", 600)
        s.y += h + 14
    s.save("doc_validation_9.svg", "정직한 한계: 정답 데이터 없음, 실제 주행 비교는 45초 한 구간")


if __name__ == "__main__":
    for f in (fig1, fig2, fig3, fig4, fig5, fig6, fig7, fig8, fig9):
        f()

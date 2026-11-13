"""Infographics for docs/ride-study.md (weekend ride kit).

    python3 sim/figs_doc_ridestudy.py   # writes docs/img/doc_ridestudy_<n>.svg

Reads figs_tables / crave_style only; the Pretendard subset is copied from an existing figure.
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import crave_style as cs
from figs_tables import (Svg, tw, wrap, esc, INK, MUTED, LINE, CARD, FOOT, BLUE, BAND, IMG, SERIF)

W, M = 960, 28
CW = W - 2 * M
LOW, MID, HIGH = cs.LOW, cs.MID, cs.HIGH
FONT_STYLE = re.search(r'<style id="pretendard-embed">.*?</style>',
                       (IMG / "emulator_checks.svg").read_text(encoding="utf-8"), re.S).group(0)


def new():
    return Svg(W, 10)


def done(s, y, n, title):
    s.h = int(y + 24)
    s.parts[0] = (f'<rect x="0.75" y="0.75" width="{W - 1.5}" height="{s.h - 1.5}" rx="{cs.RADIUS}" '
                  f'fill="{cs.BG}" stroke="{LINE}" stroke-width="1.5"/>')
    s.parts.insert(0, FONT_STYLE)
    s.save(f"doc_ridestudy_{n}.svg", title)


def head(s, kick, title):
    s.add(cs.kicker(M, 42, kick))
    s.text(M, 78, title, 26, INK, "start", 800, track="-0.03em")
    return 104


def para(s, x, y, w, text, size=15, fill=MUTED, weight=400, lh=1.45):
    """Wrapped paragraph; s=None only measures. Returns y below the last line."""
    for i, ln in enumerate(wrap(text, size, w, weight >= 700)):
        if s:
            s.text(x, y + size * (1 + i * lh) - 2, ln, size, fill, "start", weight)
    n = len(wrap(text, size, w, weight >= 700))
    return y + size * lh * n + 2


def chips(s, x, y, w, items, size=13):
    """items: (label, fg, bg, stroke[, bold]); wraps rows; returns y below."""
    cx, h = x, 26
    for it in items:
        lab, fg, bg, st = it[:4]
        cw = tw(lab, size, True) + 20
        if cx + cw > x + w and cx > x:
            cx, y = x, y + h + 6
        if s:
            s.chip(cx, y, lab, fg, bg, st, size, h, bold=True)
        cx += cw + 8
    return y + h


def card(s, x, y, w, title, body=(), num=None, icon=None, ch=(), hmin=0, fill=CARD, stroke=LINE,
         accent=BLUE, tint=BAND, tsize=18, bsize=15, bfill=MUTED, h=None):
    pad, lead = 18, (46 if (num is not None or icon) else 0)
    tx, tw_ = x + pad + lead, w - 2 * pad - lead
    if h is None:
        h = max(hmin, card(None, x, y, w, title, body, num, icon, ch, 0, fill, stroke, accent, tint, tsize, bsize, bfill, h=-1))
    if h == -1:
        h = 10 ** 4
    if s:
        s.card(x, y, w, h, fill, stroke)
        if icon:
            s.icon(icon, x + pad + 20, y + pad + 20, accent, tint, 20)
        elif num is not None:
            s.circle(x + pad + 20, y + pad + 20, 20, s.grad("cg"))
            s.text(x + pad + 20, y + pad + 27, str(num), 19, "#FFFFFF", "middle", 800)
    yy = y + pad
    if title:
        for ln in wrap(title, tsize, tw_, True):
            if s:
                s.text(tx, yy + tsize, ln, tsize, INK, "start", 800, track="-0.02em")
            yy += tsize * 1.35
        yy += 4
    for b in ([body] if isinstance(body, str) else body):
        yy = para(s, tx, yy, tw_, b, bsize, bfill) + 3
    if ch:
        yy = chips(s, tx, yy + 2, tw_, ch) + 3
    return (yy + pad - 3 - y) if s is None else h


def row_cards(s, y, specs, cols=2, gap=16):
    """Equal-height grid of cards; specs: dicts of card() kwargs (title first)."""
    w = (CW - gap * (cols - 1)) / cols
    for i in range(0, len(specs), cols):
        grp = specs[i:i + cols]
        hh = max(card(None, M, y, w, **sp) for sp in grp)
        for j, sp in enumerate(grp):
            card(s, M + j * (w + gap), y, w, h=hh, **sp)
        y += hh + gap
    return y - gap


def strip(s, y, text, fill, stroke, icon, col, size=16, bold=True):
    ln = wrap(text, size, CW - 80, bold)
    h = 28 + len(ln) * size * 1.4
    s.card(M, y, CW, h, fill, stroke)
    s.glyph(icon, M + 34, y + h / 2, col, 26)
    for i, l in enumerate(ln):
        s.text(M + 62, y + 20 + size + i * size * 1.4 - 4, l, size, INK, "start", 700 if bold else 400)
    return y + h


def formula(s, x, y, size=26):
    toks = [("D", "i"), (" = √( Σ", "n"), ("t", "s"), (" ( w", "n"), ("x", "s"), ("a", "n"), ("x", "s"), ("(t)", "n"),
            ("2", "p"), (" + w", "n"), ("y", "s"), ("a", "n"), ("y", "s"), ("(t)", "n"), ("2", "p"), (" ) Δt )", "n")]
    out, shift = [], 0
    for t, k in toks:
        want = {"n": 0, "i": 0, "s": size * 0.2, "p": -size * 0.3}[k]
        d = want - shift
        shift = want
        dy = f' dy="{d:.1f}"' if d else ""
        fs = f' font-size="{size * 0.68:.0f}"' if k in "sp" else ""
        it = ' font-style="italic"' if k in "ip" or t in ("w", "a") else ""
        out.append(f'<tspan{dy}{fs}{it}>{esc(t)}</tspan>')
    s.add(f'<text x="{x}" y="{y}" font-family="{SERIF}" font-size="{size}" fill="{INK}">{"".join(out)}</text>')


def code(s, x, y, text, size=14, fill=BLUE):
    s.text(x, y, text, size, fill, "start", 700, family="ui-monospace, Menlo, Consolas, monospace")


# ------------------------------------------------------------------ 1 intro
def fig1():
    s = new()
    y = head(s, "WEEKEND RIDE KIT", "실제 탑승 멀미 데이터 모으기")
    s.rect(M, y, CW, 64, BAND, cs.RADIUS)
    s.text(W / 2, y + 40, "시뮬레이션과 공개 고속도로 1분으로는 사람이 실제로 멀미했는지를 알 수 없습니다", 18, INK, "middle", 800, track="-0.02em")
    y += 88
    bw, gap = 268, 50
    steps = [("car", "가족·친구 차에 동승", "주말 동승 키트"),
             ("phone", "폰 GPS 기록 + 하차 직후 설문", "위치 1초 간격 · 설문 1분"),
             ("graph", "앱 엔진이 예측한 흔들림 vs 실제 멀미", "서로 맞는지 확인")]
    for i, (ic, t, sub) in enumerate(steps):
        x = M + i * (bw + gap)
        s.card(x, y, bw, 150)
        s.icon(ic, x + bw / 2, y + 36, BLUE, BAND, 22)
        for j, l in enumerate(wrap(t, 17, bw - 32, True)):
            s.text(x + bw / 2, y + 82 + j * 24, l, 17, INK, "middle", 800)
        s.text(x + bw / 2, y + 136, sub, 14, MUTED, "middle")
        if i < 2:
            s.arrow(x + bw + 8, y + 75, x + bw + gap - 8, y + 75, BLUE, 2.2, 8)
    y += 174
    s.card(M, y, CW, 96)
    s.text(M + 22, y + 32, "엔진이 1초마다 앞뒤·옆 가속도의 느린 성분으로 계산", 16, INK, "start", 800)
    for k, (sym, lab, x0) in enumerate([("I", "흔들림 지수 · 최근 60초 RMS", M + 22), ("D", "누적량", M + 22 + 400)]):
        s.rect(x0, y + 46, 360 if k == 0 else 200, 36, BAND, 18)
        s.text(x0 + 22, y + 72, sym, 24, BLUE, "middle", 700, italic=True, family=SERIF)
        s.text(x0 + 44, y + 70, lab, 16, INK, "start", 700)
    y += 112
    y = strip(s, y, "공모전용 비공식 예비 조사 · 연구윤리위원회(IRB) 승인 연구가 아님 → 결과는 “가능성 확인” 수준으로만 사용",
              cs.MID_T, MID, "info", cs.MID_INK, 16)
    done(s, y, 1, "실제 탑승 멀미 데이터 모으기: 동승 키트 개요")


# ------------------------------------------------------------------ 2 questions
def fig2():
    s = new()
    y = head(s, "1 · WHAT TO CHECK", "무엇을 확인하나: 질문 4개")
    s.card(M, y, CW, 84, BAND, BAND)
    s.text(M + 24, y + 28, "누적량(엔진이 1초마다 계산)", 14, MUTED, "start", 700)
    formula(s, M + 24, y + 62)
    s.text(W - M - 24, y + 62, "I: 최근 60초 RMS 흔들림 지수", 14, MUTED, "end")
    y += 102
    G, R, B = (LOW, cs.LOW_T, LOW), (HIGH, cs.HIGH_T, HIGH), (BLUE, BAND, BLUE)
    sp = [
        dict(title="순위가 맞는가", num=1,
             body="D가 큰 탑승일수록 하차 직후 멀미 점수도 높은가. 판단 기준은 미리 정해 둡니다.",
             ch=[("탑승 8회 이상", BLUE, BAND, BLUE), ("Spearman ρ ≥ 0.6 → 맞는 방향", "#166534", cs.LOW_T, LOW),
                 ("ρ < 0.3 → 다시 설계", "#991B1B", cs.HIGH_T, HIGH)]),
        dict(title="알림이 먼저 오는가", num=2,
             body="증상이 시작됐다고 답한 탑승에서, 앱의 첫 주의 알림이 증상 시작보다 먼저인가."),
        dict(title="기준선 크기", num=3,
             body="증상이 시작된 순간의 누적량 D가 엔진의 초기 기준선 τ0 = 30 근처인가.",
             ch=[("초기 기준선 τ0 = 30", BLUE, BAND, BLUE)]),
        dict(title="저주파 소리의 효과 (탐색)", num=4,
             body=["출발 직전 100 Hz 소리를 1분 들은 탑승과 듣지 않은 탑승의 멀미 점수가 다른가 (Gu 외, 2025).",
                   "같은 사람·같은 길끼리만 비교하고, 횟수가 적어 방향만 봅니다."]),
    ]
    y = row_cards(s, y, sp)
    done(s, y, 2, "확인할 질문 4가지")


# ------------------------------------------------------------------ 3 apps
def fig3():
    s = new()
    y = head(s, "2 · GEAR", "GPS 기록 앱 (무료 · 1초 간격 · 파일 내보내기)")
    sp = [
        dict(title="Sensor Logger", icon="phone", body=[
            "기기: iPhone · Android", "내보내기: 압축 CSV(무료 기본), 안에 Location.csv",
            "설정: Location만 켜고 샘플링 1 Hz. 가속도계도 같이 켜 두면 나중에 비교용으로 쓸 수 있음"]),
        dict(title="GPSLogger (mendhak, 오픈소스)", icon="signal", body=[
            "기기: Android", "내보내기: GPX 1.1 또는 CSV",
            "설정: 기록 간격 1초, 거리 필터 0 m. 0초 간격은 앱이 불안정해질 수 있어 1초 권장"]),
    ]
    y = row_cards(s, y, sp) + 16
    y += card(s, M, y, CW, "Sensor Logger 무료판", icon="info",
              body="압축 CSV·JSON 내보내기가 기본이고 Excel·KML은 유료입니다. GPX는 목록에서 확인되지 않아 CSV를 씁니다.") + 16
    # recognised columns
    cols = [("시간", "time / timestamp"), ("위도", "latitude / lat"), ("경도", "longitude / lon"),
            ("속도", "speed"), ("방향", "bearing / course")]
    h0 = y
    s.card(M, y, CW, 168)
    s.icon("search", M + 38, y + 38, BLUE, BAND, 20)
    s.text(M + 70, y + 46, "import_ride.py가 열 이름으로 자동 인식", 18, INK, "start", 800, track="-0.02em")
    cx = M + 20
    cw_ = (CW - 40 - 4 * 10) / 5
    for i, (k, v) in enumerate(cols):
        x = cx + i * (cw_ + 10)
        s.rect(x, y + 66, cw_, 48, "#FFFFFF", cs.RADIUS_SM, LINE)
        s.text(x + cw_ / 2, y + 86, k, 13, MUTED, "middle", 700)
        s.text(x + cw_ / 2, y + 105, v, 13, BLUE, "middle", 700)
    s.text(M + 22, y + 140, "시간: ISO 8601 문자열 또는 1970년 기준 초·밀리초·나노초 숫자 모두 가능", 14, MUTED)
    s.text(M + 22, y + 160, "속도 열 이름에 km가 있으면 km/h, 아니면 m/s로 읽음", 14, MUTED)
    y += 168 + 16
    # speed/bearing warning
    s.card(M, y, CW, 190, cs.HIGH_T, HIGH)
    s.icon("noentry", M + 38, y + 38, HIGH, "#FFFFFF", 20)
    s.text(M + 70, y + 46, "속도·방향이 기록되는 앱을 고르세요", 18, INK, "start", 800, track="-0.02em")
    para(s, M + 22, y + 64, CW - 44, "위치 차이로만 속도를 구하면 위치 오차(수 m)가 그대로 가속도가 됩니다. 시뮬레이션 GPX로 시험했을 때 D가 더 크게 나온 비율:", 15, MUTED)
    for k, (lab, v, x0) in enumerate([("굽은 길", 4, M + 22), ("저속 도심", 35, M + 22 + 440)]):
        bw = 360
        s.text(x0, y + 134, lab, 14, INK, "start", 700)
        s.rect(x0 + 84, y + 119, bw - 84, 20, "#FFFFFF", 10)
        s.rect(x0 + 84, y + 119, max(8, (bw - 84) * v / 40), 20, HIGH, 10)
        s.text(x0 + bw + 10, y + 135, f"+{v}%", 16, HIGH, "start", 800)
    s.text(M + 22, y + 170, "D가 실제보다 크게 계산됨 (위치 오차만으로)", 13, MUTED)
    y += 190
    done(s, y, 3, "GPS 기록 앱 두 가지와 열 이름 자동 인식, 속도·방향 기록 앱을 고르는 이유")


# ------------------------------------------------------------------ 4 test + kit
def fig4():
    s = new()
    y = head(s, "2 · BEFORE THE WEEKEND", "주말 전 시험 기록 · 준비물")
    s.card(M, y, CW, 196)
    s.icon("check", M + 38, y + 38, BLUE, BAND, 20)
    s.text(M + 70, y + 46, "주말 전에 꼭 시험 기록", 18, INK, "start", 800, track="-0.02em")
    steps = [("route", "집 근처 5분 걷기\n또는 버스 기록"), ("doc", "파일 내보내기"), ("gear", "import_ride.py\n파일 실행 확인")]
    bw, gap = 250, 60
    for i, (ic, t) in enumerate(steps):
        x = M + 22 + i * (bw + gap)
        s.rect(x, y + 68, bw, 66, "#FFFFFF", cs.RADIUS_SM, LINE)
        s.icon(ic, x + 32, y + 101, BLUE, BAND, 18)
        for j, l in enumerate(t.split("\n")):
            s.text(x + 62, y + 96 + j * 20 - (0 if "\n" in t else -8), l, 15, INK, "start", 700)
        if i < 2:
            s.arrow(x + bw + 8, y + 101, x + bw + gap - 8, y + 101, BLUE, 2, 7)
    s.text(M + 22, y + 156, "출력에 아래 줄이 나오면 앱 속도를 쓰는 것입니다", 14, MUTED)
    code(s, M + 22, y + 178, "speed/bearing from log speed/course", 14)
    y += 196 + 24
    s.text(M, y + 16, "그 밖에 준비물", 19, INK, "start", 800, track="-0.02em")
    y += 34
    sp = [dict(title="충전·고정", icon="power", body="충전된 폰과 보조배터리, 폰을 고정할 거치대나 컵홀더"),
          dict(title="설문지·시계", icon="clock", body="설문지(이 문서 4절을 인쇄하거나 메모 앱에 복사), 시계"),
          dict(title="안전", icon="hush", body="멀미 봉투, 물. 운전자에게 “언제든 세워 달라고 하면 세워 주기”를 미리 부탁")]
    w3 = (CW - 32) / 3
    hh = max(card(None, M, y, w3, **a) for a in sp)
    for i, a in enumerate(sp):
        card(s, M + i * (w3 + 16), y, w3, h=hh, **a)
    y += hh
    done(s, y, 4, "주말 전 시험 기록 3단계와 준비물")


# ------------------------------------------------------------------ 5 procedure
def fig5():
    s = new()
    y = head(s, "3 · RIDE PROTOCOL", "탑승 절차")
    s.card(M, y, CW, 210)
    s.text(M + 22, y + 38, "출발 전 기준 상태 (모두 해당해야 기록)", 19, INK, "start", 800, track="-0.02em")
    items = [("지금 멀미 증상 없음", "MISC 0 또는 1 (4절 표)"),
             ("최근 2시간 안에 멀미약 안 먹음 · 과식·공복 아님", "식사 시각은 메모"),
             ("같은 날 이전 탑승과 30분 이상 쉼", "이전 탑승 MISC가 3 이상이었다면 그날은 더 타지 않음")]
    for i, (a, b) in enumerate(items):
        yy = y + 78 + i * 44
        s.status("ok", M + 38, yy, 12)
        s.text(M + 62, yy - 2, a, 16, INK, "start", 700)
        s.text(M + 62, yy + 17, b, 14, MUTED)
    y += 210 + 16
    sp = [
        dict(title="좌석", icon="seat",
             body="같은 사람은 모든 탑승에서 같은 자리. 앱이 겨냥하는 뒷좌석(오른쪽 또는 왼쪽) 권장. seat에 적습니다."),
        dict(title="폰 거치", icon="phone",
             body="GPS는 방향과 상관없지만 가속도계를 같이 기록하면 폰이 굴러다니지 않게 컵홀더·좌석 주머니·거치대에 고정. 화면을 보지 않는 자세로."),
        dict(title="탑승 중 할 일", icon="window",
             body="한 사람 안에서는 같은 행동 유지(창밖 보기 권장). 폰을 보거나 책을 읽었다면 activity에 적기. 멀미는 행동에 따라 크게 달라집니다."),
        dict(title="기록 시작·끝", icon="clock",
             body="차에 타서 출발 직전에 켜고, 도착해 정차한 뒤 끕니다. 걸어가는 구간이 들어갔다면 설문의 start_s, end_s로 잘라냅니다."),
    ]
    y = row_cards(s, y, sp) + 16
    y = strip(s, y, "탑승 중 증상이 시작되면 그 시각을 바로 메모 (출발 후 몇 분인지)", cs.BAND, BLUE, "bell", BLUE, 16) + 12
    y = strip(s, y, "MISC 6(메스꺼움) 이상이 되면 그 탑승은 끝내고 차를 세워 쉽니다. 데이터보다 안전이 먼저입니다.",
              cs.HIGH_T, HIGH, "noentry", HIGH, 16)
    done(s, y, 5, "탑승 절차: 출발 전 기준 상태, 좌석·폰 거치, 기록 시작과 끝, 안전 기준")


# ------------------------------------------------------------------ 6 survey
def fig6():
    s = new()
    y = head(s, "4 · SURVEY (1 MIN)", "하차 직후 설문")
    s.text(M, y + 4, "하차하고 바로, 다른 사람과 이야기하기 전에 답합니다.", 15, MUTED)
    y += 26
    # Q1
    s.card(M, y, CW, 118)
    s.circle(M + 38, y + 38, 20, s.grad("cg"))
    s.text(M + 38, y + 45, "1", 19, "#FFFFFF", "middle", 800)
    s.text(M + 70, y + 36, "전체 멀미 점수", 18, INK, "start", 800, track="-0.02em")
    s.text(M + 70, y + 58, "지금 멀미가 얼마나 나나요? (0–10 정수 또는 0.5 단위)", 14, MUTED)
    bx, bw = M + 70, CW - 100
    s.add(f'<clipPath id="q1"><rect x="{bx}" y="{y + 72}" width="{bw}" height="14" rx="7"/></clipPath><g clip-path="url(#q1)">')
    for i, c in enumerate([LOW, MID, HIGH]):
        s.rect(bx + bw * [0, .3, .6][i], y + 72, bw * [.3, .3, .4][i], 14, c)
    s.add("</g>")
    s.text(bx, y + 108, "0 = 전혀 없음", 14, INK, "start", 700)
    s.text(bx + bw, y + 108, "10 = 토할 것 같거나 토함", 14, INK, "end", 700)
    y += 134
    # Q2 MISC
    hd = 78
    rows = [(0, ["아무 문제 없음"]), (1, ["약간 불편하지만 뚜렷한 증상은 없음"]),
            (2, ["어지러움·더움·두통·속 거북함·식은땀 등이 ", ("아주 약하게", INK, "bold"), " 있음"]),
            (3, ["위 증상이 ", ("조금", INK, "bold"), " 있음"]), (4, ["위 증상이 ", ("중간 정도", INK, "bold"), " 있음"]),
            (5, ["위 증상이 ", ("심함", INK, "bold")]),
            (6, ["메스꺼움이 ", ("조금", INK, "bold"), " 있음"]), (7, ["메스꺼움이 ", ("중간 정도", INK, "bold"), " 있음"]),
            (8, ["메스꺼움이 ", ("심함", INK, "bold")]), (9, ["헛구역질을 함"]), (10, ["토함"])]
    rh = 34
    H = hd + rows.__len__() * rh + 44
    s.card(M, y, CW, H)
    s.circle(M + 38, y + 38, 20, s.grad("cg"))
    s.text(M + 38, y + 45, "2", 19, "#FFFFFF", "middle", 800)
    s.text(M + 70, y + 36, "MISC (Misery Scale) 한국어판", 18, INK, "start", 800, track="-0.02em")
    s.text(M + 70, y + 58, "탑승 중 가장 심했던 순간에 해당하는 번호 하나", 14, MUTED)
    for i, (n, segs) in enumerate(rows):
        yy = y + hd + i * rh
        tint, col = (cs.LOW_T, LOW) if n <= 1 else (cs.MID_T, MID) if n <= 5 else (cs.HIGH_T, HIGH)
        if n == 10:
            col = "#991B1B"
        s.rect(M + 20, yy, CW - 40, rh - 4, tint, 8)
        s.circle(M + 46, yy + (rh - 4) / 2, 12, col)
        s.text(M + 46, yy + (rh - 4) / 2 + 5, str(n), 14, "#FFFFFF" if n != 2 and n not in (3, 4, 5) else INK, "middle", 800)
        s.rich(M + 72, yy + (rh - 4) / 2 + 5, segs, 15, fill=MUTED)
    s.text(M + 22, y + H - 14, "Bos 등(2005)의 0–10 단계 멀미 척도 · 이 표는 이 키트를 위한 비공식 번역", 13, MUTED)
    y += H + 16
    # Q3 + Q4
    w2 = (CW - 16) / 2
    hh = max(card(None, M, y, w2, **a) for a in [
        dict(title="증상 시작 시각", num=3, body="처음 불편함(MISC 2 이상)을 느낀 때가 출발 후 몇 분인가요? 없었다면 빈칸."),
        dict(title="탑승 중 주로 한 일", num=4, body="", ch=[("창밖 보기", BLUE, BAND, BLUE), ("폰", BLUE, BAND, BLUE), ("책", BLUE, BAND, BLUE), ("잠", BLUE, BAND, BLUE)])])
    card(s, M, y, w2, "증상 시작 시각", "처음 불편함(MISC 2 이상)을 느낀 때가 출발 후 몇 분인가요? 없었다면 빈칸.", num=3, h=hh)
    card(s, M + w2 + 16, y, w2, "탑승 중 주로 한 일", "", num=4, h=hh,
         ch=[("창밖 보기", BLUE, BAND, BLUE), ("폰", BLUE, BAND, BLUE), ("책", BLUE, BAND, BLUE), ("잠", BLUE, BAND, BLUE)])
    y += hh
    done(s, y, 6, "하차 직후 설문 4문항과 MISC 0–10 척도")


# ------------------------------------------------------------------ 7 consent
def fig7():
    s = new()
    y = head(s, "4 · CONSENT", "참여 동의 문구")
    s.card(M, y, CW, 56, BAND, BAND)
    s.glyph("person", M + 34, y + 28, BLUE, 26)
    s.text(M + 62, y + 34, "탑승 전에 읽어 주고 말로 동의를 받습니다. 미성년자는 보호자 동의도 받습니다.", 16, INK, "start", 700)
    y += 76
    txt = ("이 기록은 차 안 멀미를 미리 알려 주는 앱을 만드는 공모전 준비용입니다. 탑승하는 동안 폰으로 위치와 속도를 기록하고, "
           "내린 뒤 멀미 정도를 1분 동안 묻습니다. 이름 대신 별명으로 적고, 위치 기록은 분석이 끝나면 지웁니다. "
           "결과는 숫자와 그래프로만 공모전 자료에 쓰이며 누구인지 알 수 없게 합니다. 언제든 그만둘 수 있고, 불편하면 바로 말해 주세요. 차를 세웁니다.")
    h = para(None, 0, 0, CW - 70, txt, 16, INK) + 36
    s.card(M, y, CW, h)
    s.rect(M, y + 14, 5, h - 28, BLUE, 2.5)
    para(s, M + 30, y + 22, CW - 70, txt, 16, INK)
    y += h + 16
    t2 = "저는 위 내용을 듣고, 제 자녀(별명: ______)가 이 탑승 기록에 참여하는 것에 동의합니다. 자녀가 원하지 않거나 불편해하면 즉시 중단합니다."
    h2 = para(None, 0, 0, CW - 70, t2, 16, INK) + 104
    s.card(M, y, CW, h2, "#FFFFFF", LINE)
    s.rect(M, y + 14, 5, h2 - 28, MID, 2.5)
    s.text(M + 30, y + 36, "보호자 동의", 17, INK, "start", 800)
    yy = para(s, M + 30, y + 48, CW - 70, t2, 16, INK)
    s.text(M + 30, yy + 28, "보호자 서명: ______", 16, INK, "start", 700)
    s.text(M + 330, yy + 28, "날짜: ______", 16, INK, "start", 700)
    y += h2 + 16
    y = strip(s, y, "자녀 본인에게도 쉬운 말로 설명하고, 싫다고 하면 기록하지 않습니다.", cs.MID_T, MID, "child", cs.MID_INK, 16)
    done(s, y, 7, "참여 동의 문구와 보호자 동의 서식")


# ------------------------------------------------------------------ 8 ride plan
def fig8():
    s = new()
    y = head(s, "5 · RIDE PLAN", "탑승 구성 (총 5–10회)")
    cols = [("순서", 56), ("길 (road)", 300), ("운전 (style)", 112), ("길이", 96), ("비고", CW - 40 - 56 - 300 - 112 - 96)]
    rows = [("1", ("highway", "고속도로·자동차전용도로"), "smooth", "15–30분", "기준 탑승. 멀미가 거의 없어야 정상"),
            ("2", ("city", "도심 (신호·회전 많음)"), "smooth", "15–20분", ""),
            ("3", ("city", "도심"), "normal", "15–20분", "운전자 평소대로"),
            ("4", ("winding", "굽은 길 (산길·램프·지하주차장 나선로)"), "smooth", "10–20분", ""),
            ("5", ("winding", "굽은 길"), "normal", "10–20분", "멀미가 가장 많이 날 것으로 예상"),
            ("6–10", None, "", "", "")]
    rh = 52
    H = 52 + len(rows) * rh + 12
    s.card(M, y, CW, H)
    x = M + 20
    for lab, cw_ in cols:
        s.text(x, y + 34, lab, 14, MUTED, "start", 700)
        x += cw_
    s.line(M + 20, y + 46, M + CW - 20, y + 46, LINE, 1.5)
    for i, r in enumerate(rows):
        yy = y + 52 + i * rh
        if i % 2 == 0:
            s.rect(M + 12, yy, CW - 24, rh - 4, "#FFFFFF", 8)
        x = M + 20
        for j, (lab, cw_) in enumerate(cols):
            ty = yy + 29
            if j == 0:
                s.text(x, ty, r[0], 17, BLUE, "start", 800)
            elif j == 1:
                if r[1]:
                    wch = s.chip(x, yy + 8, r[1][0], BLUE, BAND, BLUE, 13, 26, bold=True)
                    s.text(x + wch + 8, ty, r[1][1], 14, INK, "start", 700)
                else:
                    s.text(x, ty, "위 조합 반복 또는 다른 탑승자", 14, INK, "start", 700)
            elif j == 2 and r[2]:
                s.chip(x, yy + 8, r[2], MUTED, "#FFFFFF", FOOT, 13, 26, bold=True)
            elif j == 3:
                s.text(x, ty, r[3], 15, INK, "start", 700)
            elif j == 4:
                txt = r[4] or ("사람·길을 섞어 8회 이상이면 상관계수 판단 가능" if i == 5 else "")
                for k, ln in enumerate(wrap(txt, 14, cw_ - 8)):
                    s.text(x, ty - (8 if len(wrap(txt, 14, cw_ - 8)) > 1 else 0) + k * 18, ln, 14, MUTED)
            x += cw_
    y += H + 18
    sp = [
        dict(title="“거칠게”를 운전자에게 요구하지 마세요", icon="noentry", accent=HIGH, tint=cs.HIGH_T, body=[
            "과속·급제동을 부탁하는 것은 위험하고 불법일 수 있습니다.",
            "smooth는 “평소보다 천천히, 부드럽게 가감속”을 부탁하고, 그 밖에는 운전자 평소 운전(normal)으로 둡니다. 원래 빠르게 운전하는 편이면 rough로 적습니다.",
            "실제 흔들림 차이는 엔진 출력 I, D에 그대로 나옵니다."]),
        dict(title="순서 효과 줄이기", icon="clock", body=[
            "앞 탑승의 멀미가 남는 것을 줄이려면 하루에 2–3회만 타고, 멀미가 클 것 같은 굽은 길은 그날 마지막에 둡니다.",
            "운전자 휴대폰 조작은 절대 부탁하지 않습니다. 기록은 동승자가 합니다."]),
    ]
    y = row_cards(s, y, sp)
    y += 16
    # tone protocol
    th = 232
    s.card(M, y, CW, 206)
    s.icon("tone", M + 38, y + 38, BLUE, BAND, 20)
    s.text(M + 70, y + 40, "저주파 소리 (4번 질문)", 18, INK, "start", 800, track="-0.02em")
    s.text(M + 70, y + 60, "같은 사람·같은 길을 두 번 탈 때", 14, MUTED)
    w2 = (CW - 44 - 40) / 2
    for k, (tag, t1, t2, col, tint) in enumerate([("tone=on", "출발 직전 100 Hz 순음 1분", "들려줌", BLUE, BAND),
                                                  ("tone=off", "들려주지 않음", "비교용", MUTED, "#FFFFFF")]):
        x = M + 22 + k * (w2 + 40)
        s.rect(x, y + 76, w2, 52, tint, cs.RADIUS_SM, LINE)
        s.chip(x + 12, y + 89, tag, col, "#FFFFFF", col, 13, 26, bold=True)
        s.text(x + 24 + tw(tag, 13, True) + 20, y + 107, t1, 15, INK, "start", 700)
    s.text(M + 22 + w2 + 20, y + 107, "↔", 20, MUTED, "middle", 700)
    yy = para(s, M + 22, y + 142, CW - 44, "차 스피커(블루투스)나 동승자 폰 스피커로 틀고, 탑승자 귀 근처에서 대화 소리 정도(약 60–66 dBA, 소음계 앱으로 확인)로 맞춥니다.", 15, MUTED)
    yy = para(s, M + 22, yy, CW - 44, "어느 쪽인지는 설문지 notes에 tone=on / tone=off로 적고, 순서는 탑승마다 바꿉니다. 소리가 불편하면 바로 끕니다.", 15, MUTED)
    y += 206
    done(s, y, 8, "탑승 구성표(5–10회), 운전자 요청 원칙, 저주파 소리 비교 방법")


# ------------------------------------------------------------------ 9 csv columns
def fig9():
    s = new()
    y = head(s, "6 · SURVEY CSV", "설문 파일 열 (survey.csv)")
    cols = [("ride_id", "파일 이름과 같은 탑승 번호", "R01"), ("date", "날짜", "2026-11-07"),
            ("nickname", "별명(이름 금지)", "별"), ("age_group", "adult / minor", ""),
            ("seat", "rear_right / rear_left / rear_center / front", ""),
            ("road", "highway / city / winding", ""), ("style", "smooth / normal / rough", ""),
            ("activity", "look_out / phone / read / sleep", ""),
            ("baseline_misc", "출발 전 MISC (0–1이어야 함)", "0"), ("score", "하차 직후 0–10 점수", "7"),
            ("misc", "탑승 중 최고 MISC", "6"), ("onset_min", "증상 시작, 출발 후 분. 없으면 빈칸", "12"),
            ("start_s, end_s", "기록에서 쓸 구간(초). 비우면 전체. onset_min은 start_s 이후부터 셉니다", "40"),
            ("notes", "창문, 식사, 에어컨 등 메모", "")]
    c1, c2 = 190, 540
    rh = 38
    H = 52 + len(cols) * rh + 10
    s.card(M, y, CW, H)
    for lab, x in [("열", M + 22), ("뜻", M + 22 + c1), ("예", M + 22 + c1 + c2)]:
        s.text(x, y + 34, lab, 14, MUTED, "start", 700)
    s.line(M + 20, y + 46, M + CW - 20, y + 46, LINE, 1.5)
    for i, (k, d, e) in enumerate(cols):
        yy = y + 52 + i * rh
        if i % 2 == 0:
            s.rect(M + 12, yy, CW - 24, rh - 2, "#FFFFFF", 8)
        code(s, M + 22, yy + 24, k, 14)
        lines = wrap(d, 14, c2 - 12)
        if len(lines) == 1:
            s.text(M + 22 + c1, yy + 24, d, 14, INK)
        else:
            s.text(M + 22 + c1, yy + 17, d[:0] + lines[0], 14, INK)
        if e:
            code(s, M + 22 + c1 + c2, yy + 24, e, 14, INK)
    y += H
    done(s, y, 9, "survey.csv 열 14개의 의미와 예시 값")


# ------------------------------------------------------------------ 10 analysis
def fig10():
    s = new()
    y = head(s, "6 · ANALYSIS", "파일 정리와 분석 4단계")
    G = 16
    w = (CW - G) / 2
    sp = [
        dict(title="탑승 파일 이름 정리", num=1, body="내보낸 파일 이름을 탑승 번호로 바꿔 data/rides/에 둡니다.",
             ch=[("R01.gpx", BLUE, BAND, BLUE), ("R02.csv", BLUE, BAND, BLUE), ("…", MUTED, "#FFFFFF", FOOT)]),
        dict(title="설문을 survey.csv로", num=2, body="data/rides/survey.csv에 설문을 적습니다. 형식 예는 ride-survey-example.csv (값은 가짜).",
             ch=[("data/rides/survey.csv", BLUE, BAND, BLUE)]),
    ]
    y = row_cards(s, y, sp) + G
    # step 3
    items = [("점 개수", None), ("버린 점", "250 km/h보다 빠르게 튄 위치"),
             ("신호가 끊긴 구간", "10초 이상 · 직선으로 채움 → 그 구간의 D는 작게 나옴"),
             ("길이 · 평균 속도", None), ("평균·최대 지수 I", None), ("누적량 D", None),
             ("주의·높음 알림 시간", None), ("첫 알림 시각", None)]
    s.card(M, y, CW, 320)
    s.circle(M + 38, y + 38, 20, s.grad("cg"))
    s.text(M + 38, y + 45, "3", 19, "#FFFFFF", "middle", 800)
    s.text(M + 70, y + 36, "탑승 하나씩 확인: import_ride.py 출력", 18, INK, "start", 800, track="-0.02em")
    cw_ = (CW - 44 - 3 * 10) / 4
    yy0 = y + 64
    rows_y = [yy0, yy0 + 66]
    for i, (a, b) in enumerate(items[:4] + items[4:]):
        r_, c_ = divmod(i, 4)
        x = M + 22 + c_ * (cw_ + 10)
        yy = yy0 + r_ * 88
        s.rect(x, yy, cw_, 78, "#FFFFFF", cs.RADIUS_SM, LINE)
        for k, ln in enumerate(wrap(a, 14, cw_ - 20, True)):
            s.text(x + 12, yy + 24 + k * 18, ln, 14, INK, "start", 700)
        if b:
            for k, ln in enumerate(wrap(b, 13, cw_ - 20)):
                s.text(x + 12, yy + 46 + k * 16, ln, 13, MUTED)
    yb = yy0 + 2 * 88 + 4
    s.rect(M + 22, yb, CW - 44, 54, BAND, cs.RADIUS_SM)
    code(s, M + 38, yb + 22, "--csv", 14)
    s.text(M + 90, yb + 22, "를 붙이면 sim/scenarios와 같은 열 형식의 1 Hz CSV를 같은 폴더에 씁니다", 14, INK, "start", 700)
    s.text(M + 38, yb + 42, "예: R01.csv, Sensor Logger CSV면 Location_1hz.csv", 14, MUTED)
    y += 320 + G
    # step 4
    s.card(M, y, CW, 222)
    s.circle(M + 38, y + 38, 20, s.grad("cg"))
    s.text(M + 38, y + 45, "4", 19, "#FFFFFF", "middle", 800)
    s.text(M + 70, y + 36, "전체 보고서: ride_report.py", 18, INK, "start", 800, track="-0.02em")
    for k, (fn, lines) in enumerate([("data/rides/report.csv", "탑승별 I, D, 증상 시작 시점의 D, 알림이 증상보다 몇 분 먼저였는지"),
                                     ("data/rides/report.svg", "누적량 D와 멀미 점수의 산점도, Pearson·Spearman 상관계수")]):
        x = M + 22 + k * ((CW - 44 - 12) / 2 + 12)
        ww = (CW - 44 - 12) / 2
        s.rect(x, y + 62, ww, 90, "#FFFFFF", cs.RADIUS_SM, LINE)
        code(s, x + 14, y + 86, fn, 14)
        para(s, x + 14, y + 96, ww - 28, lines, 14, MUTED)
    s.rect(M + 22, y + 164, CW - 44, 44, cs.MID_T, 10, MID)
    s.text(M + 40, y + 192, "탑승이 10회 미만이면 상관계수는 참고용으로만 표시됩니다", 15, INK, "start", 700)
    y += 222 + G
    y = strip(s, y, "친구 차의 실내 온도·내기순환은 모르므로 기본값(22 °C, 외기)으로 계산합니다. 메모가 있으면 import_ride.py --temp 26 --recirc 로 다시 볼 수 있습니다.",
              cs.BAND, BLUE, "thermo", BLUE, 15, False)
    done(s, y, 10, "분석 4단계: 파일 정리, 설문 입력, import_ride.py 출력, ride_report.py 보고서")


# ------------------------------------------------------------------ 11 privacy
def fig11():
    s = new()
    y = head(s, "7 · PRIVACY", "개인정보")
    sp = [
        dict(title="이름 대신 별명만", icon="person", body="별명과 실제 사람의 연결은 적어 두지 않습니다."),
        dict(title="분석 뒤 위치 파일 삭제", icon="lock", accent=HIGH, tint=cs.HIGH_T, body=[
            "GPS 기록(*.gpx, 원본 CSV, --csv로 만든 1 Hz CSV)에는 집·학교 위치가 들어 있습니다.",
            "report.csv를 만든 뒤 위치 파일은 지웁니다. report.csv·report.svg에는 좌표가 없습니다."]),
        dict(title="git에 올리지 않기", icon="doc", body=[
            "저장소 .gitignore에 두 줄을 추가합니다: 폴더 안 모든 파일을 무시하고 빈 폴더 표시 파일만 남김."]),
        dict(title="공모전 자료에는 숫자만", icon="graph", body="산점도와 요약 숫자만 쓰고, 지도나 경로 그림은 넣지 않습니다."),
    ]
    y = row_cards(s, y, sp)
    done(s, y, 11, "개인정보 지침 4가지: 별명, 위치 파일 삭제, git 무시, 공개 자료")


if __name__ == "__main__":
    for f in (fig1, fig2, fig3, fig4, fig5, fig6, fig7, fig8, fig9, fig10, fig11):
        f()

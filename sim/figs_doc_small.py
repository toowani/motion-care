"""Infographics for docs/feasibility.md and android/.../later/README.md.

    python3 sim/figs_doc_small.py   # writes docs/img/doc_small_<n>.svg (then sim/embed_font.py <fonts dir>)
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import crave_style as cs
from figs_tables import Svg, wrap, INK, MUTED, LINE, CARD, FOOT, BLUE, BAND

W, M = 960, 28
SZ, LH = 14, 21
MONO = "ui-monospace, Menlo, Consolas, monospace"


def finish(s, y, n, title):
    s.h = int(y + 14)
    s.parts[0] = (f'<rect x="0.75" y="0.75" width="{W - 1.5}" height="{s.h - 1.5}" rx="{cs.RADIUS}" '
                  f'fill="{cs.BG}" stroke="{LINE}" stroke-width="1.5"/>')
    s.save(f"doc_small_{n}.svg", title)


def header(s, kick, title):
    s.add(cs.kicker(M, 42, kick))
    s.text(M, 78, title, 26, INK, "start", 800, track="-0.03em")
    return 100


def lines_h(paras, width):
    return sum(len(wrap(t, SZ, width)) for _, t in paras) * LH + 4 * (len(paras) - 1)


def draw_paras(s, x, y, width, paras):
    """paras: (label|None, text). Label is a bold blue prefix chip-like word."""
    for lab, t in paras:
        ls = wrap((lab + " " if lab else "") + t, SZ, width)
        for i, ln in enumerate(ls):
            if i == 0 and lab:
                s.rich(x, y + 15, [(lab, BLUE, "bold"), " " + ln[len(lab) + 1:]], SZ, fill=MUTED)
            else:
                s.text(x, y + 15, ln, SZ, MUTED)
            y += LH
        y += 4
    return y - 4


def cards(s, y, items, tx=96, status=False):
    """items: dict(icon|status, head, mono, chip, paras). Returns y below."""
    tw_ = W - M - 20 - tx
    for it in items:
        h = 58 + lines_h(it["paras"], tw_) + 6
        s.card(M, y, W - 2 * M, h)
        cy = y + 33
        if it.get("status"):
            if it["status"] == "ok":
                s.status("ok", 64, cy, 17)
            else:
                s.circle(64, cy, 16, "#FFFFFF", FOOT, 2)
                s.text(64, cy + 5, "?", 16, FOOT, "middle", 800)
        else:
            s.icon(it["icon"], 64, cy, BLUE, BAND, 20)
        if it.get("mono"):
            s.text(tx, y + 38, it["head"], 17, INK, "start", 800, family=MONO)
        else:
            s.text(tx, y + 38, it["head"], 17, INK, "start", 800)
        if it.get("chip"):
            s.chip(W - M - 16, y + 18, it["chip"], BLUE, "#FFFFFF", BLUE, 12, 24, bold=True, anchor="end")
        draw_paras(s, tx, y + 54, tw_, it["paras"])
        y += h + 12
    return y


# ---------------------------------------------------------------- feasibility
def fig1():
    s = Svg(W, 10)
    y = header(s, "STEP 1 · G1", "신호 확인: 6개 중 4개 통과")
    s.text(M, y + 14, "Pleos 에뮬레이터에서 앱의 \"실차 신호 (Pleos)\" 버튼을 누르고 Logcat과 화면을 보며 채움 (11.1)", SZ, MUTED)
    y += 34
    items = [
        dict(status="ok", head="방향지시등", chip="pleos.car.permission.CAR_LIGHTS", paras=[
            ("API", "Vehicle.getTurnSignal().registerTurnSignalState"),
            ("확인", "Virtual Console에서 좌/우 켜기 → 예고 음성이 나오는지"),
            ("결과", "11.1 · Turn Signal State \"Blink left\" → \"곧 왼쪽으로 돌아요\"")]),
        dict(status="ok", head="실내 온도", chip="CAR_CLIMATE", paras=[
            ("API", "HVAC.registerHvacTemperatureCurrent(ROW_1_LEFT)"),
            ("확인", "화면의 \"실내 °C\"가 바뀌는지"),
            ("결과", "11.1 · 19.1°C 수신")]),
        dict(status="ok", head="내기/외기 읽기", chip="CAR_CLIMATE", paras=[
            ("API", "HVAC.registerHvacRecirculationState"),
            ("확인", "화면의 \"내기/외기\" 표시"),
            ("결과", "11.1 · 지원 영역은 checkHvacRecirculationCapabilities로 받아야 함 (운전석 ROW_1_LEFT는 미지원)")]),
        dict(status="ok", head="내기/외기 · 온도 바꾸기", chip="CONTROL_CAR_CLIMATE", paras=[
            ("API", "HVAC.setHvacRecirculation, setHvacTemperature"),
            ("확인", "이벤트 기록에 \"공조: …\"가 뜨는지, 실패 메시지가 뜨는지"),
            ("결과", "11.1 · 외기 전환 · 22°C 설정 성공, 차량 공조 바에 22.0° 표시")]),
        dict(status="todo", head="속도", chip="CAR_SPEED", paras=[
            ("API", "CarPropertyManager PERF_VEHICLE_SPEED  (android.car.permission.CAR_SPEED)"),
            ("확인", "Virtual Console에서 속도 변경"),
            ("결과", "☐ 미확인 · 콘솔에 속도 직접 설정 없음 (ACCEL 페달 · 크루즈 목표 속도만)")]),
        dict(status="todo", head="진행 방향", chip="ACCESS_FINE_LOCATION", paras=[
            ("API", "LocationManager GPS (에뮬레이터 GPX 재생)"),
            ("확인", "Extended Controls › Location에서 경로 재생"),
            ("결과", "☐")]),
    ]
    y = cards(s, y, items)
    finish(s, y, 1, "G1 신호 확인표: 방향지시등, 실내 온도, 내기/외기 읽기와 쓰기는 통과, 속도와 진행 방향은 미확인")


def flow_row(s, y, cond, then, icon):
    lw, rw, h = 360, 460, 0
    cl = wrap(cond, SZ, lw - 28)
    rl = wrap(then, SZ, rw - 28)
    h = max(len(cl), len(rl)) * LH + 28
    s.card(M, y, lw, h, cs.CARD_ALT)
    s.card(W - M - rw, y, rw, h, "#FFFFFF", BLUE)
    s.arrow(M + lw + 14, y + h / 2, W - M - rw - 14, y + h / 2, BLUE, 2.2, 8)
    s.text(M + lw + 14 + (W - M - rw - 14 - M - lw - 14) / 2, y + h / 2 - 8, "대체", 12, BLUE, "middle", 700)
    for i, ln in enumerate(cl):
        s.text(M + 14, y + 26 + i * LH - (len(cl) - 1) * 0 , ln, SZ, INK, "start", 700)
    for i, ln in enumerate(rl):
        s.text(W - M - rw + 14, y + 26 + i * LH, ln, SZ, MUTED)
    return y + h + 12


def fig2():
    s = Svg(W, 10)
    y = header(s, "G1 판단", "안 되면 이렇게 바꿉니다")
    y += 10
    y = flow_row(s, y, "방향지시등이 안 되면", "예고는 경로 안내(NaviHelper)의 다음 회전만으로", "turn")
    y = flow_row(s, y, "공조 쓰기가 막히면", "음성 제안(\"창문을 조금 열어 볼까요?\")으로 대체, 기획서에 권한 신청 명시", "wind")
    y = flow_row(s, y, "GPX 재생이 위치로 안 들어오면", "앱 안의 재생 모드로만 시연", "route")
    finish(s, y, 2, "G1 대체안: 방향지시등 실패 시 내비 다음 회전으로, 공조 쓰기 차단 시 음성 제안과 권한 신청 명시, GPX 실패 시 앱 재생 모드")


def fig3():
    s = Svg(W, 10)
    y = header(s, "결과 · 11.1", "G1 통과: 예고와 공조가 실차 신호로 돈다")
    y += 4
    items = [
        dict(status="ok", head="G1 통과", paras=[
            (None, "방향지시등 이벤트 수신, 공조 읽기 · 쓰기 모두 동작 → 예고와 공조 닫힌 고리를 실차 신호로 시연 가능")]),
        dict(status="todo", head="속도 · 위치는 추가 확인", paras=[
            (None, "시연은 재생 모드로 충분")]),
        dict(icon="dashboard", head="Virtual Console 접속", paras=[
            (None, "adb forward tcp:8088 tcp:8088 후 http://127.0.0.1:8088/ (Emulation Controls 검색창에 항목 이름 입력)")]),
        dict(icon="seat", head="콘솔에 있는 다른 신호", paras=[
            (None, "Seat Occupancy(뒷좌석 탑승), Window Position, Steering Angle도 있음 → 환기 파라미터에 활용 가능")]),
    ]
    y = cards(s, y, items)
    finish(s, y, 3, "11.1 결과: G1 통과, 속도와 위치는 추가 확인, Virtual Console 접속 방법, 활용 가능한 추가 신호")


# ---------------------------------------------------------------- later/README
def fig4():
    s = Svg(W, 10)
    y = header(s, "later/ · parked UI", "Hidden from the app, still compiled")
    s.text(M, y + 14, "Round 6 and 7 decisions. Nothing on screen uses this code; the engine side is untouched.", SZ, MUTED)
    y += 34
    items = [
        dict(icon="bell", head="TurnCue.kt", mono=True, paras=[
            ("Parked", "TurnCueSound (chime / theme voice / silent, never over the navigation's own turn speech), LightCue (ambient-light turn pulse, restores the driver's setting)"),
            ("Used by", "TripService.act for CUE_LEFT / CUE_RIGHT")]),
        dict(icon="turn", head="CueBubble.kt", mono=True, paras=[
            ("Parked", "map turn bubble (arrow + \"70m\" → \"곧\") under the character"),
            ("Used by", "MapPanel.showCue / followCue")]),
        dict(icon="route", head="RestStopOffer.kt", mono=True, paras=[
            ("Parked", "rest-stop question \"가까운 휴게소에 들를까요?\" (voice + buttons, 응 → nearest demo rest stop as a waypoint), asked on the engine's SUGGEST_REST_STOP and 6 s after 멀미 완화"),
            ("Used by", "TripService.act, TripService.relief")]),
    ]
    y = cards(s, y, items)
    y += 2
    s.card(M, y, W - 2 * M, 66, cs.CARD_ALT)
    s.icon("check", 62, y + 33, BLUE, BAND, 18)
    ls = wrap("Engine side unchanged: android/core and sim/engine.py still plan turn cues and rest stops (SUGGEST_REST_STOP); the sim checks still cover them.", SZ, 800)
    for i, ln in enumerate(ls):
        s.text(96, y + 29 + i * LH, ln, SZ, MUTED)
    y += 66
    finish(s, y, 4, "Parked UI files: TurnCue, CueBubble, RestStopOffer, what each parks and who used it")


def fig5():
    s = Svg(W, 10)
    y = header(s, "Still in place", "Logic stays outside this folder")
    y += 4
    items = [
        dict(icon="bell", head="TripService", mono=True, paras=[
            (None, "reportSick / recordSick (old \"멀미 났어\")")]),
        dict(icon="route", head="NaviGuide.addRestStop", mono=True, paras=[
            (None, "and its demo rest-stop list (Navi.kt); Voice.phrase(SUGGEST_REST_STOP)")]),
        dict(icon="gear", head="Settings · ProfileStore", mono=True, paras=[
            (None, "Settings.cueStyle, Settings.lightCueOn; ProfileStore.load/save")]),
        dict(icon="palette", head="MoodTheme.cueLine", mono=True, paras=[
            (None, "stays in the theme API (each theme's turn sentence); moving it would break the theme interface, so it is simply not called")]),
    ]
    y = cards(s, y, items)
    finish(s, y, 5, "Logic still in place outside later/: TripService reportSick, NaviGuide rest stop, Settings and ProfileStore, MoodTheme.cueLine")


def fig6():
    s = Svg(W, 10)
    y = header(s, "Re-enabling", "Two switches to turn the UI back on")
    y += 4
    items = [
        dict(icon="bell", head="Turn cues", chip="TripService.act", paras=[
            ("1", "replace the early return for cues with TurnCueSound(settings, navi, ::say).play(line) and LightCue(vehicle, ::note); call start() in onCreate, release() in onDestroy"),
            ("2", "add a CueBubble under MapPanel's character; call show(left) on a cue, follow(s.nextTurnM) per sample"),
            ("3", "add cueStylePicker back to settings and a cue tile/marker if wanted")]),
        dict(icon="route", head="Rest stop", chip="RestStopOffer", paras=[
            ("1", "in TripService create RestStopOffer(…) (constructor call below; for replays, keep the last sample's lat/lon and return that when live is null)"),
            ("2", "call reset() in startTripState"),
            ("3", "offer(Voice.phrase(a)) for SUGGEST_REST_STOP in act (instead of the early return)"),
            ("4", "main.postDelayed({ if (source === trip) offer() }, RestStopOffer.DELAY_AFTER_RELIEF_MS) in both relief window callbacks")]),
    ]
    y = cards(s, y, items)
    finish(s, y, 6, "Re-enabling steps for turn cues and the rest-stop offer")


for f in (fig1, fig2, fig3, fig4, fig5, fig6):
    f()

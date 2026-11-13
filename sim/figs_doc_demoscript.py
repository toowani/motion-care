"""Figures for docs/demo-script.md (python3 sim/figs_doc_demoscript.py from repo root)."""
import sys
sys.path.insert(0, str(__import__("pathlib").Path(__file__).parent))
import crave_style as cs
from figs_tables import Svg, wrap, tw, INK, MUTED, LINE, CARD, BLUE, BAND

SZ, LH = 13.5, 20


def cards_fig(name, title, heading, cards, tag=None):
    """cards: (icon, head, [paragraphs], chip-or-None). Returns nothing, saves svg."""
    tx, tw_ = 96, 804
    laid, H = [], 28
    if heading:
        H += 40
    for ic, head, paras, chip in cards:
        wr = [wrap(p, SZ, tw_ - 14) for p in paras]
        h = 62 + sum(len(w) for w in wr) * LH + (len(wr) - 1) * 6 + 10
        laid.append((ic, head, wr, H, h, chip))
        H += h + 14
    H += 14
    s = Svg(960, H)
    if heading:
        s.text(28, 56, heading, 19, INK, "start", 800)
    for ic, head, wr, y, h, chip in laid:
        s.card(28, y, 904, h)
        s.icon(ic, 64, y + 34, BLUE, BAND, 20)
        s.text(tx, y + 40, head, 17, INK, "start", 800)
        if chip:
            s.chip(914, y + 22, chip, BLUE, "#FFFFFF", BLUE, 12, 24, bold=True, anchor="end")
        yy = y + 72
        for k, w in enumerate(wr):
            if k:
                yy += 6
            for ln in w:
                s.text(tx, yy, ln, SZ, MUTED)
                yy += LH
    s.save(name, title)


def table_fig(name, title, heading, cols, rows, notes=()):
    """cols: [(header, width)], rows: list of cells. notes: (icon, text)."""
    pad = 12
    xs, x = [], 28
    for _, w in cols:
        xs.append(x)
        x += w
    s_rows, H = [], 28 + (40 if heading else 0) + 40
    for r in rows:
        wr = [wrap(c, SZ, w - 2 * pad, bold=(i == 0)) for i, (c, (_, w)) in enumerate(zip(r, cols))]
        h = max(len(w) for w in wr) * LH + 18
        s_rows.append((wr, H, h))
        H += h + 8
    note_l = []
    H += 6
    for ic, t in notes:
        w = wrap(t, SZ, 904 - 96)
        h = max(52, len(w) * LH + 24)
        note_l.append((ic, w, H, h))
        H += h + 10
    H += 18
    s = Svg(960, H)
    y0 = 28
    if heading:
        s.text(28, 56, heading, 19, INK, "start", 800)
        y0 = 68
    s.rect(28, y0, 904, 34, BAND, 10)
    for (hd, w), xx in zip(cols, xs):
        s.text(xx + pad, y0 + 22, hd, 14, BLUE, "start", 800)
    for wr, y, h in s_rows:
        s.card(28, y, 904, h)
        for i, (w, xx) in enumerate(zip(wr, xs)):
            for k, ln in enumerate(w):
                s.text(xx + pad, y + 26 + k * LH - (0 if h > 40 else 3), ln, SZ, INK if i == 0 else MUTED, "start", 800 if i == 0 else 400)
    for ic, w, y, h in note_l:
        s.card(28, y, 904, h, cs.CARD_ALT)
        s.icon(ic, 62, y + h / 2, BLUE, BAND, 18)
        for k, ln in enumerate(w):
            s.text(96, y + h / 2 - (len(w) - 1) * LH / 2 + 5 + k * LH, ln, SZ, MUTED)
    s.save(name, title)


def fig1():
    cards_fig("doc_demoscript_1.svg", "시연 영상 콘티 v3: 변경 이력과 제출 필름", None, [
        ("tone", "11.13 저주파 소리 추가", [
            "앱이 출발할 때 · 주의 단계 · 멀미 완화 버튼에서 100 Hz 소리를 1분 틉니다.",
            "화면은 설정에 \"저주파 소리\" 켜기 · 끄기가 하나 늘고 지도 위에 \"저주파 소리 1분\"이 잠깐 뜨는 것 말고는 같습니다. 바뀐 것은 소리입니다: 음성 문구가 \"바깥 공기로 바꾸고, 멀미를 덜어 주는 낮은 소리를 1분 틀게요.\", \"낮은 소리 틀고 환기할게요. 먼 곳을 보세요.\"로 바뀌었습니다.",
            "11.8 필름은 그 전 문구라 다시 만들지 않으면 README와 접수 양식에 \"저주파를 넣기 전 영상\"이라고 적어 둡니다.",
            "노트북 스피커는 100 Hz가 잘 안 나므로 녹음은 외장 스피커나 소리 파일 합성으로 합니다."], None),
        ("phone", "v3 (11.3, 선택 촬영) · 11.4 · 11.8 화면 변경", [
            "v3 (11.3): 팀장 회의 반영 화면 기준 (탭 두 개 · 멀미 완화 버튼 · 색 통일). 회전 예고와 가족 모드는 화면에서 숨겨 장면에서 뺌.",
            "11.4 팀 회의 2: 캐릭터 둘레 링, 휴게소 질문 없음(확장 기능), 지나온 길 안 그림. 아래 \"준비\"와 \"장면\".",
            "11.8 화면 변경: 링 색은 캐릭터 표정(Mood)과 같은 색, 링 길이는 멀미 가능성. 루미(가상 아이돌) 표정 그림 3장 새로 그림. 아래 장면은 이 화면 기준."], None),
        ("x", "옛 v1 · v2 (11.2): 쓰지 않음", [
            "../멀미케어_시연_v1.mp4 · v2.mp4: 회전 예고 · 무드등 · 가족 모드 · 정차 후 질문이 나오는 옛 화면이라 쓰지 않음. 이 기능들은 지금 앱에 없음 (android/app/.../later/, 확장)."], None),
        ("car", "필름 (11.8, 제출용)", [
            "25초, 소리 없음(음성 문구는 자막). 저장소 밖 ../ai_video/explainer/멀미케어_필름_v3.mp4 (음성판 멀미케어_필름_v3_음성.mp4: 앱 음성 문구 3개를 macOS Yuna 음성으로, 도로 소리), README 맨 위 docs/img/explainer.webp.",
            "가족이 차 안에서 겪는 순서: 굽은 산길 → 뒷좌석 아이 \"속이 울렁거려…\" → 센터 화면에 몽글이(말랑 3D 친구)와 \"브레이크 부드럽게\" 알림, 음성 \"뒷좌석이 흔들려요. 브레이크를 부드럽게 밟아 주세요.\" → 운전자가 브레이크를 부드럽게 → 송풍구 \"바깥 공기로 바꾸고 온도를 조금 낮출게요.\" → \"그래도 어지러워…\" → 멀미 완화 버튼 → \"창문 열고 바람 넣을게요. 먼 곳을 보세요.\" → 창문 바람, \"이제 좀 살 것 같아\" → 멀미 케어 끝 화면.",
            "센터 화면 속 앱은 Pleos 에뮬레이터 실제 녹화를 합성, 차 안 · 도로 장면은 AI 생성(Higgsfield · Kling 2.5 Turbo)."], "25초"),
        ("map", "짧은 실제 화면 (docs/img/demo.webp)", [
            "에뮬레이터에서 B_winding_rough를 2배속으로 재생, 몽글이 테마, 링 노랑 → 빨강 → 멀미 완화. 아래 \"멀미 완화\" 장면과 같은 재생 옵션에 --es theme clay."], None),
    ])


def fig2():
    cards_fig("doc_demoscript_2.svg", "72초 콘티의 목표 3개와 촬영 준비", "72초 에뮬레이터 전체 녹화 (선택, 필름 다음)", [
        ("star", "목표: \"폰으로는 못 하는 장면\" 3개", [
            "① 흔들림이 쌓이면 차가 저주파 소리를 틀고 실내 공기를 바꿈",
            "② 멀미 완화 버튼 하나로 저주파 · 창문 · 환기 · 음성 · 내 기준 기록",
            "③ 내비 옆 한 화면으로 함께 쓰기"], None),
        ("gear", "준비 (명령은 아래 코드 블록)", [
            "에뮬레이터는 셸이 끝나도 꺼지지 않게 nohup으로 켭니다. 설정 › 연결 › Wi-Fi › AndroidWifi 연결 (지도 · 음성에 필요).",
            "Virtual Console: http://127.0.0.1:8088/ · 시작 값: Target Temperature 26°C, Air Recirculation = Recirculate (덥고 탁한 실내). 공조 전원 켬 (꺼져 있으면 공기 장면에서 공조가 안 바뀜).",
            "앱 설치: cd android && ./gradlew :app:installDebug (디버그 빌드라야 아래 재생 옵션이 동작).",
            "테마: 필름 · demo.webp와 맞추려면 몽글이(말랑 3D 친구). 재생 명령에 --es theme clay를 붙이거나 설정에서 고름.",
            "앱 소리: 에뮬레이터 화면 녹화에는 소리가 안 담김 → 호스트(Mac)에서 시스템 소리를 함께 녹음.",
            "장면마다 원하는 순간으로 바로 가기 (디버그 재생 옵션, 모두 --user 10)."], None),
    ])


def fig3():
    table_fig("doc_demoscript_3.svg", "장면별 재생 옵션과 촬영 주의", "장면별 재생 옵션",
        [("장면", 150), ("replay", 190), ("replay_from", 130), ("replay_speed", 130), ("화면에 나오는 때", 304)],
        [["지도 · 링", "B_winding_rough", "0", "3", "처음 초록으로 조금, 시나리오 약 1분 30초에 노랑"],
         ["공기 바꿈", "B_winding_rough", "0", "2", "약 10초 뒤 안내 · 공조 (시나리오 19초)"],
         ["멀미 완화", "B_winding_rough", "60", "2", "재생 중 위쪽 \"멀미 완화\" (누적량 5 이상이어야 학습 기록)"]],
        [("hand", "멀미 완화 버튼 좌표 (2560x1440): adb shell input tap 2248 150"),
         ("clock", "20초 안에 다시 누르면 \"환기 중이에요\"만 나옴 → 장면 사이 20초 띄우기"),
         ("tone", "저주파 소리는 한 번에 1분이고, 나오는 중에는 다시 틀지 않음 → 출발 장면과 주의 장면 사이가 1분보다 짧으면 주의 단계 음성은 원래 문구(\"바깥 공기로 바꾸고 온도를 조금 낮출게요.\")"),
         ("phone", "화면 녹화: adb shell screenrecord --size 1920x1080 --bit-rate 8000000 /sdcard/demo.mp4 (최대 3분, Ctrl+C로 끝내고 adb pull /sdcard/demo.mp4)")])


def fig4():
    table_fig("doc_demoscript_4.svg", "72초 장면표", "장면 (72초)",
        [("시간", 90), ("화면", 400), ("내레이션/자막", 414)],
        [["0–6초", "검은 화면에 질문", "\"뒷좌석은 왜 더 멀미가 날까?\" → \"다음 움직임을 모르고, 덥고 답답하기 때문입니다.\""],
         ["6–18초", "지도 탭(첫 화면): 굽은 길 · 거칠게, 캐릭터 둘레 링이 차오르며 초록 → 노랑", "자막: 링 길이 = 멀미 가능성, 링 색 = 표정 (초록 · 노랑 · 빨강)"],
         ["18–30초", "\"브레이크 부드럽게\" 알림 → 공조: 아래 공조 바 26°C · 내기 → 22°C · 외기", "\"뒷좌석이 흔들려요. 브레이크를 부드럽게 밟아 주세요.\" → \"바깥 공기로 바꾸고, 멀미를 덜어 주는 낮은 소리를 1분 틀게요.\" 자막: 차가 저주파 소리를 틀고 공기를 바꿈 (100 Hz, 나고야대 연구)"],
         ["30–44초", "\"멀미 완화\" 한 번: \"창문 열고 환기했어요\" → 음성 \"낮은 소리 틀고 환기할게요. 먼 곳을 보세요.\"", "자막: 버튼 하나로 저주파 · 창문 · 환기 · 음성"],
         ["44–54초", "분석 탭: 게이지, 완화 버튼 1 · 공기 바꿈 1 타일, 흔들림 지수 ↔ 멀미 가능성 토글", "자막: 누른 순간이 내 기준이 됩니다"],
         ["54–64초", "Pleos 화면 나누기 손잡이: 왼쪽 표정 카드 + \"멀미 가능성 N%\" 알약 + 멀미 완화 버튼, 오른쪽 Pleos 내비", "자막: 내비 옆에는 표정과 %만"],
         ["64–72초", "구조도 한 장 (docs/img/sdk_map.svg)", "자막: Pleos Vehicle (HVAC · Window · SteeringWheel) · NaviHelper · TTS · AudioTrack 저주파 (STT는 확장)"]],
        [("window", "창문: 에뮬레이터에서는 명령은 수락되지만 Virtual Console에서 창문이 움직이지 않음 → 화면의 안내 알약과 로그의 창문 위치 응답(2)으로 보여 줌"),
         ("hand", "화면 나누기: adb shell input swipe 2545 717 1700 717 1500, 되돌리기 adb shell input swipe 1705 717 2545 717 1500"),
         ("info", "손잡이를 끄는 동안 Pleos가 앱을 흐리게 그림 → 빠르게 끌거나 그 부분을 짧게 자름")])


def fig5():
    cards_fig("doc_demoscript_5.svg", "숏폼 30초 구성과 해시태그", "숏폼 (선택, 30초)", [
        ("car", "구성", [
            "필름(../ai_video/explainer/멀미케어_필름_v3.mp4, 25초)을 그대로 쓰거나 줄여서: \"뒷좌석 동승자가 멀미하는 순간\" → \"멀미 완화\"를 누르자 낮은 소리가 나고 창문이 조금 열리며 바람이 바뀌는 장면 → \"#HMG차량용앱개발공모전\".",
            "차량 내외부 이미지 노출은 최소화."], "30초"),
        ("badge", "해시태그", [
            "공모전 공식 이름은 \"현대자동차그룹 차량용 앱 개발 공모전\", 숏폼 안내의 필수 해시태그는 #HMG차량용앱개발공모전 (10.28 설명회 정리 기준, 접수 전 공지 다시 확인)."], None),
    ])


for f in (fig1, fig2, fig3, fig4, fig5):
    f()

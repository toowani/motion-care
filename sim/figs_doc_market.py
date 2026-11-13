"""Infographics for research/market.md (writes docs/img/doc_market_<n>.svg).

    python3 sim/figs_doc_market.py

Reuses the Svg class / helpers of sim/figs_tables.py (read-only). Every number and caveat comes
from research/market.md; wording is tightened, nothing is dropped.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import crave_style as cs
from figs_tables import Svg, tw, wrap, INK, MUTED, LINE, CARD, FOOT, BLUE, BAND, LOW, HIGH, MID_INK, MID_T

W_TXT = 800  # wrapped text width inside a card
CAV = MID_INK


# ---------------------------------------------------------------- blocks (s, y) -> height
def heading(text, sub=None, kick=None):
    def b(s, y):
        h = 0
        if kick:
            s.add(cs.kicker(32, y + 14, kick))
            h += 26
        s.text(32, y + h + 24, text, 24, INK, "start", 800, track="-0.03em")
        h += 36
        if sub:
            s.text(32, y + h + 12, sub, 14, MUTED)
            h += 24
        return h + 6
    return b


def group(label, ic="doc"):
    def b(s, y):
        s.icon(ic, 52, y + 18, BLUE, BAND, 15)
        s.text(76, y + 24, label, 17, BLUE, "start", 800)
        return 44
    return b


def item(ic, head, chip, lines, accent=False):
    """lines: str (bullet) or '※ ...' caveat."""
    def b(s, y):
        wr = [wrap(t, 14, W_TXT - 14) for t in lines]
        chip_inline = chip and tw(head, 17, True) + tw(chip, 13) + 20 + 40 < W_TXT
        h = 16 + 26 + (0 if chip_inline or not chip else 30) + sum(len(w) for w in wr) * 21 + 4 * len(wr) + 10
        s.card(28, y, 904, h, cs.BAND if accent else CARD, BLUE if accent else LINE)
        s.icon(ic, 64, y + 34, BLUE, "#FFFFFF" if accent else BAND, 19)
        s.text(100, y + 36, head, 17, INK, "start", 800)
        yy = y + 38
        if chip:
            if chip_inline:
                s.chip(912, y + 18, chip, BLUE, "#FFFFFF", BAND, 13, 26, anchor="end", bold=True)
            else:
                s.chip(100, yy + 6, chip, BLUE, "#FFFFFF", BAND, 13, 26, bold=True)
                yy += 30
        yy += 22
        for t, w in zip(lines, wr):
            cav = t.startswith("※")
            s.circle(106, yy - 5, 2.5, CAV if cav else FOOT)
            for ln in w:
                s.text(118, yy, ln, 14, CAV if cav else MUTED, "start", 600 if cav else 400)
                yy += 21
            yy += 4
        return h + 12
    return b


def table(cols, rows, fs=14, hdr=True):
    """cols: [(label, width)]; cell: str or (str, 'assume'|'bold')."""
    def b(s, y):
        x0, tot = 28, 904
        laid, h = [], 0
        for r in rows:
            cells = []
            for (lab, cw), c in zip(cols, r):
                txt, kind = (c, "") if isinstance(c, str) else c
                cells.append((wrap(txt, fs, cw - 22, kind == "bold"), kind))
            rh = max(len(c[0]) for c in cells) * 21 + 18
            laid.append((cells, rh))
            h += rh
        hh = 38 if hdr else 0
        s.card(x0, y, tot, hh + h)
        if hdr:
            s.rect(x0, y, tot, hh, BAND, cs.RADIUS)
            s.rect(x0, y + hh - 12, tot, 12, BAND)
            cx = x0
            for lab, cw in cols:
                s.text(cx + 14, y + 25, lab, 14, BLUE, "start", 800)
                cx += cw
        yy = y + hh
        for k, (cells, rh) in enumerate(laid):
            if k:
                s.line(x0 + 10, yy, x0 + tot - 10, yy, LINE, 1)
            cx = x0
            for (lab, cw), (ls, kind) in zip(cols, cells):
                ty = yy + 26
                for ln in ls:
                    s.text(cx + 14, ty, ln, fs, INK if (kind == "bold" or cx == x0) else MUTED, "start",
                           800 if kind == "bold" else (700 if cx == x0 else 400))
                    ty += 21
                if kind == "assume":
                    s.chip(cx + 14 + tw(ls[-1], fs) + 8, ty - 21 - 16, "가정", MUTED, "#FFFFFF", FOOT, 13, 22, bold=False, pad=8)
                cx += cw
            yy += rh
        return hh + h + 14
    return b


def stats(tiles):
    """tiles: [(big, label, sub)] equal columns."""
    def b(s, y):
        n = len(tiles)
        gap = 14
        w = (904 - gap * (n - 1)) / n
        subs = [wrap(t[2], 13.5, w - 28) if t[2] else [] for t in tiles]
        labs = [wrap(t[1], 14.5, w - 28, True) for t in tiles]
        h = 24 + 38 + max(len(l) for l in labs) * 21 + max(len(x) for x in subs) * 20 + 18
        for i, (big, lab, sub) in enumerate(tiles):
            x = 28 + i * (w + gap)
            s.card(x, y, w, h)
            s.text(x + 16, y + 52, big, 30, BLUE, "start", 800, track="-0.03em")
            yy = y + 78
            for ln in labs[i]:
                s.text(x + 16, yy, ln, 14.5, INK, "start", 700)
                yy += 21
            for ln in subs[i]:
                s.text(x + 16, yy, ln, 13.5, MUTED)
                yy += 20
        return h + 14
    return b


def calc(lines):
    """Formula strip: lines [(label, expr)]"""
    def b(s, y):
        h = 24 + len(lines) * 40
        s.card(28, y, 904, h, cs.BAND, BLUE)
        for i, (lab, ex) in enumerate(lines):
            yy = y + 40 + i * 40
            s.text(52, yy, lab, 14.5, BLUE, "start", 800)
            s.text(190, yy, ex, 19, INK, "start", 800, track="-0.01em")
        return h + 14
    return b


def note(text):
    def b(s, y):
        ls = wrap(text, 14, 840, True)
        h = len(ls) * 21 + 20
        s.rect(28, y, 904, h, MID_T, 12)
        yy = y + 27
        for ln in ls:
            s.text(48, yy, ln, 14, CAV, "start", 700)
            yy += 21
        return h + 12
    return b


def render(name, title, blocks):
    def run(s):
        y = 22
        for bl in blocks:
            y += bl(s, y)
        return y
    H = int(run(Svg(960, 10)) + 14)
    s = Svg(960, H)
    run(s)
    s.save(name, title)


# ---------------------------------------------------------------- figures
def fig1():
    render("doc_market_1.svg", "경쟁 서비스: 폰 기능 · 앱과 착용 도구", [
        heading("경쟁 서비스 (1)", "폰 화면 · 착용 도구는 ‘보는 것과 느끼는 것의 어긋남’을 줄임", "2026-11-02 웹 조사"),
        group("폰 기능 · 앱", "phone"),
        item("phone", "Apple Vehicle Motion Cues", "2024년 5월 · iOS 18 · iPadOS 18", [
            "화면 가장자리 점이 차의 움직임을 따라 움직여 보는 것과 느끼는 것의 어긋남을 줄임",
            "폰 센서로 차 안인지 감지 · 2025년 Mac, 2026년 visionOS로 확대"]),
        item("phone", "Google Motion Assist", "2026년 8월 · Android 17 Pixel", [
            "2024년 12월 ‘Motion Cues’라는 이름으로 코드에서 발견 → 2026년 8월부터 단계적 배포 · Apple과 같은 화면 점 방식",
            "※ Google 공식 발표는 확인하지 못함 (기사 기준)"]),
        item("phone", "KineStop (Urbandroid)", "2018년 · Android 앱", [
            "다른 앱 위에 가상 수평선을 띄움",
            "※ 설치 수는 집계 사이트 기준이라 기획서에는 쓰지 않음"]),
        note("Honor MagicOS 9에도 비슷한 기능이 있다는 기사가 있으나 원문 확인 못함."),
        group("착용 도구", "face"),
        item("face", "Citroën Seetroën", "2018년 · Boarding Ring 기술", [
            "안경테 속 액체가 인공 수평선 역할 · 증상이 시작되면 10–12분 착용 · 10세 이상",
            "※ 효과 95%는 개발사 주장"]),
    ])


def fig2():
    render("doc_market_2.svg", "경쟁 서비스: 완성차 · 부품사", [
        heading("경쟁 서비스 (2)", "차가 직접 움직임을 다듬거나 센서로 감각을 자극하는 시도", "완성차 · 부품사"),
        item("car", "현대차 스무스 모드", "2025년 7월 · 더 뉴 아이오닉 6", [
            "현대차 최초 적용 · 가감속 반응을 부드럽게 해 전기차 멀미를 줄임",
            "※ 기사 일부의 ‘세계 최초’는 공식 표현에서 확인 못함"]),
        item("car", "기아 i-페달 3.0", "EV3 · 2024년", [
            "회생제동 감속을 더 선형적으로 만들어 울컥거림 완화",
            "※ 멀미 전용 기능이라기보다 승차감 개선"]),
        item("gauge", "현대모비스 멀미 저감 기술", "회사 발표 · 2023.12", [
            "자세 · 심박 · 뇌파 센서 → 화면 안내, 시원한 바람 등 감각 자극",
            "회사 발표 ‘예방 최대 70%, 증상 40% 감소’ (측정 방법 미공개)",
            "2023.12 대한민국 해병대와 상륙돌격장갑차 적용 협약",
            "※ 승용차 양산은 확인 못함"]),
        item("graph", "JLR 웰니스 점수", "2018년 연구 · 2020년 후속", [
            "생체 신호와 차량 움직임으로 사람별 멀미 취약도 계산 · 2020년 후속 단계에서 자율주행 소프트웨어의 주행 방식에 적용",
            "※ 시트 조정은 확인되지 않음 (11.2 재확인) · 양산 소식 없음",
            "※ ‘증상 최대 60% 감소’는 보도마다 표현이 달라 기획서에는 쓰지 않음"]),
        item("video", "holoride", "2018 분사 · 2022 출시 · 2024 파산", [
            "Audi에서 2018년 분사 · 차 움직임 · 위치에 맞춘 VR 콘텐츠",
            "2022년 €699 키트 출시, 2024년 파산 신청"]),
        item("doc", "Mercedes-Benz 특허 · BMW 특허", "특허 단계", [
            "Mercedes-Benz: 바람 · LED 조명으로 움직임 감각",
            "BMW: 천장 화면에 차 움직임과 맞는 영상",
            "※ 특허 단계, 기사 기준"]),
    ])


def fig3():
    render("doc_market_3.svg", "DENSO · 나고야대 100 Hz 소리와 특허 확인", [
        heading("DENSO · 나고야대 100 Hz 소리", "우리 핵심 기술과 가장 가까운 선행 사례 · sound spice®", "경쟁 서비스 (3)"),
        item("tone", "DENSO 후원 · 2차 보도", "Gu 외, 2025 (나고야대)", [
            "나고야대 연구를 DENSO가 후원 (저자 2명이 DENSO 소속) · 2차 보도에서는 차량 인포테인먼트에 넣는 작업을 함께 한다고 함",
            "※ DENSO 공식 발표 · 제품은 확인 못 함"]),
        item("lock", "나고야대 특허", "확인 2026-11-13 · Google Patents · 논문 이해관계 공개", [
            "‘50–140 Hz · 70–85 dB 소리로 이석을 자극하는 장치’ 등록: 미국 US12023222B2 · 유럽 EP3804672B1 · 일본 JP7191400B2 · 중국 CN112243368B",
            "DENSO가 이 특허의 라이선스 옵션 비용을 냄 (논문 이해관계 공개)",
            "Google Patents 패밀리 기준 한국 출원 없음 · PCT 국내 진입 기한(2020년 말)도 지나, 국내 사용은 이 특허에 걸리지 않는 것으로 봄",
            "해외 판매 차량에 넣으려면 라이선스가 필요함",
            "※ KIPRIS 직접 조회와 변리사 확인은 전"]),
        item("doc", "US12023222B2 청구항 1", "만료 2041-10-28 (미국)", [
            "제어 장치가 70–85 dB, 50–140 Hz 소리를 정해 이석으로 전정 기능을 활성화하고 소리 발생 장치가 내는 장치"]),
        item("doc", "US20260007850A1 · EP4555983A1 (심사 중)", None, [
            "청구항 1: 주파수가 시간에 따라 바뀌는 소리 자극 → 고정 100 Hz인 멀미 케어와 청구항 1이 다름",
            "청구항 7: 멀미 자극을 받는 중임을 감지해 그동안 소리를 냄 (청구항 1에 종속)"]),
        note("sound spice®는 상표로 표기되며, 앱은 이 이름을 쓰지 않습니다."),
        item("star", "우리 차이", None, [
            "차 신호로 재생 때를 잡고 환기 · 음성 안내 · 개인 기준 학습과 한 앱으로 묶음"], accent=True),
    ])


def fig4():
    render("doc_market_4.svg", "연구 프로토타입과 조사 결론", [
        heading("연구 프로토타입 · 결론", "뒷좌석 시험장 실험과, 조사한 범위에서의 판단", "경쟁 서비스 (4)"),
        item("seat", "Diels, Dugenetet, Brietzke & Pham Xuan (2023)", "IEEE ITSC 2023 · Bentley · Volkswagen 후원", [
            "Design strategies to alleviate motion sickness in rear seat passengers – a test track study",
            "뒷좌석 16명, e-리더를 읽으며 반복 가감속",
            "ACC 설정 · 서스펜션 조정은 효과 없음",
            "속도를 보여 주는 측면 시각 흐름과 예측 조명 신호는 줄이는 경향이었지만 유의하지 않음",
            "모두 합친 ‘웰니스 팩’은 멀미 28% 감소 (유의)"]),
        note("예고 음성 실험(Kuiper 외, 2020) · 얼굴 바람 실험(D'Amour 외, 2017)은 research/literature.md 참고"),
        group("결론", "flag"),
        item("x", "‘원인을 차가 직접 줄이는 첫 시도’는 틀림", None, [
            "스무스 모드(양산), 현대모비스 · JLR(시범 · 연구)이 이미 있음"]),
        item("search", "찾지 못한 것", None, [
            "차에 이미 있는 신호(핸들 각도 · 속도 · 공조)로 저주파 소리를 틀 때를 잡고, 환기 · 음성 안내와 한 고리로 묶고, 버튼 기록으로 사람마다 기준을 학습하는 양산 서비스",
            "※ 모든 서비스를 다 본 것은 아니므로 ‘조사한 범위에서’라고 씀"], accent=True),
    ])


def fig5():
    render("doc_market_5.svg", "Pleos Connect 공개 · 탑재 · 확대 일정", [
        heading("Pleos · Pleos Connect", "정확한 이름은 브랜드 Pleos가 아니라 인포테인먼트 Pleos Connect", "2"),
        table([("항목", 150), ("내용", 560), ("출처", 194)], [
            ["공개", "2026.4.29 미디어 행사, 보도자료 2026.4.30", "현대차그룹 보도자료"],
            ["첫 탑재", "‘더 뉴 그랜저’ (5월 출시 예정 → 2026.5.14 출시)", "보도자료, 파이낸셜뉴스 영문"],
            ["확대", "국내부터 현대차 · 기아 · 제네시스 글로벌 차량에 순차 적용", "보도자료"],
            ["목표", ("2030년까지 약 2천만 대", "bold"), "보도자료"],
            ["앱 마켓", "외부 앱 마켓, 개발사 플랫폼 Pleos Playground에서 차량 API · 개발 도구 제공", "보도자료"],
            ["두 번째 차종", "‘디 올 뉴 아반떼’ 2026년 6월 부산모빌리티쇼 공개, 2026.8.5 계약 개시, 기본 트림부터 Pleos Connect", "이투데이, 현대차그룹 뉴스룸"],
            ["참고", "현대차 2025년 국내 판매 71만 2,954대", "현대차 2025 실적"],
        ]),
        stats([("2026", "‘2026년 그랜저부터’ 숫자 맞음", "초안의 문장 확인"),
               ("2,000만", "‘2030년 2,000만 대’ 숫자 맞음", "약 2천만 대 · 국내 비중은 발표 없음")]),
        note("Pleos Connect 실제 누적 탑재 대수는 공식 발표가 없어 판매량으로 대신함. MAU 가정값은 3절 통계로 바꿈 (11.2)."),
    ])


def fig6():
    render("doc_market_6.svg", "MAU 근거: 바뀐 식", [
        heading("MAU 근거: 바뀐 식", "가정이던 두 값(탑재 차량 · 자녀 가구 비율)을 통계로 바꿔 MAU를 낮춤", "3 · 2026-11-02 조사"),
        table([("항목", 170), ("초안", 190), ("바꾼 값", 280), ("근거", 264)], [
            ["탑재 차량", ("30만 대", "assume"), ("15만 1,110대 (1년)", "bold"), "그랜저 · 아반떼 2025년 국내 판매 합"],
            ["자녀 가구 비율", ("40%", "assume"), ("20.3%", "bold"), "통계청 18세 이하 자녀 가구 비율"],
            ["설치율", ("10%", "assume"), ("10%", "assume"), "통계 없음, 아래 이유"],
            ["월 사용", ("60%", "assume"), ("60%", "assume"), "통계 없음, 아래 이유"],
            ["MAU", "약 7,200", ("약 1,800 (1년) / 약 560 (2026.9 말 현재)", "bold"), "—"],
        ]),
        calc([("1년", "151,110 × 0.203 × 0.10 × 0.60 = 1,840"),
              ("현재", "45,911 × 0.203 × 0.10 × 0.60 = 559")]),
        note("현재 45,911대 = 그랜저 5–9월 합 38,606 + 아반떼 9월 7,305 (신형으로 본 값)"),
    ])


def fig7():
    render("doc_market_7.svg", "MAU 근거: 탑재 차량과 월별 판매", [
        heading("MAU 근거: 탑재 차량", "Pleos Connect가 들어간 국내 차종은 두 개", "3"),
        stats([("71,775", "그랜저 2025 국내 판매", "‘더 뉴 그랜저’ · 2026.5.14 출시"),
               ("79,335", "아반떼 2025 국내 판매", "‘디 올 뉴 아반떼’ · 2026.8.5 계약 개시"),
               ("151,110", "합 = 1년 탑재량 근사값", "그랜저 + 아반떼")]),
        item("car", "디 올 뉴 아반떼", "전 트림 기본", [
            "가솔린 2.0은 3분기 출고 · 하이브리드는 친환경차 고시 뒤 출고"]),
        table([("2026 월별 국내 판매 (현대차 월간 실적)", 464), ("그랜저", 220), ("아반떼", 220)], [
            ["5월", "5,183", "4,526"],
            ["6월", "10,062", "4,316"],
            ["7월", "8,532", "2,624"],
            ["8월", "5,931", "1,462"],
            ["9월", "8,898", "7,305"],
            [("5–9월 합", "bold"), ("38,606", "bold"), "—"],
        ]),
        note("※ 5월: ‘출고가 이번 달부터 본격화’라고만 해 그랜저 5월 숫자에 구형이 섞였을 수 있음 [확인 필요]"),
        note("※ 아반떼 5–8월은 구형으로 보이고(8월 급감) 9월 7,305대를 신형으로 봄 · 신형 · 구형 나눈 숫자 없음 [확인 필요]"),
        note("※ 2026년 10월 실적은 발표 전 (11.2 기준) · 기아 · 제네시스 적용 차종과 시기는 블로그 · 나무위키에만 있고 공식 발표 없음 [확인 필요] · 2030년 2천만 대 중 국내 비중도 발표 없음"),
    ])


def fig8():
    render("doc_market_8.svg", "MAU 근거: 자녀 가구 비율과 설치율 · 월 사용률", [
        heading("MAU 근거: 비율 가정", "자녀 가구 비율은 통계로, 설치율 · 월 사용률은 가정으로", "3"),
        group("자녀 가구 비율", "family"),
        stats([("20.3%", "18세 이하 자녀 가구", "일반가구 22,294천 중 4,517천 가구 · 전년보다 1.6% 감소"),
               ("5.8%", "5세 이하 자녀 가구", "1,284천 가구"),
               ("2024", "통계청 인구주택총조사", "등록센서스 보도자료 2025.7.29 · PDF 91쪽 표 70")]),
        item("ruler", "한계", None, [
            "가구 기준이라 그랜저 · 아반떼 구매자 중 비율과 다름",
            "아이 있는 가구가 차를 더 많이 가질 수 있어 위로, 그랜저 구매층이 나이가 많아 아래로 치우칠 수 있음",
            "※ 4–13세 자녀 가구 비율은 공표 표에 없음 [확인 필요] · KOSIS 자녀 연령별 표나 마이크로데이터로 확인 가능할 것",
            "가구당 승용차 보유 대수는 이번 계산에 필요하지 않아 찾지 않음"]),
        group("설치율 · 월 사용률", "gauge"),
        item("search", "공개 통계 없음", "참고: 컨슈머인사이트 제25차 연례 자동차 기획조사 (2025.7)", [
            "차량 내장 앱 마켓의 앱별 설치율 · 월 사용률 공개 통계는 찾지 못함",
            "3년 내 새 차 구입자 중 커넥티드 카 이용자 1,489명: AI 서비스 이용 경험률 9%, 주이용률 1% · 다른 기능의 이용 경험률은 공개 본문에 없음"]),
        stats([("10%", "설치율 [가정]", "기본 기능이 아닌 부가 서비스를 써 본 비율이 한 자릿수(9%)라는 점에 맞춤"),
               ("60%", "월 사용 [가정]", "시동 때 자동 실행되지만 아이가 탄 날만 의미가 있음 · 설치한 자녀 가구 중 한 달에 한 번 이상 아이를 태우는 비율을 보수적으로 잡음")]),
        note("출시 후 앱 마켓 통계로 바로 바꿀 값"),
    ])


def fig9():
    render("doc_market_9.svg", "B2B 시장 규모: 택시 · 통학버스 · 자율주행 셔틀", [
        heading("B2B 시장 규모", "예상 MAU와 함께 docs/proposal.md 8절의 근거", "3"),
        item("taxi", "택시", "2026.8.31 기준", [
            "전국 면허 246,258대 = 개인택시 164,806 + 일반(법인)택시 81,452",
            "법인 등록 대수는 64,520대 (면허보다 적음)",
            "참고: 서울 면허 71,615대 (2026.1 말, 서울시)",
            "출처: 전국개인택시운송사업조합연합회 면허 · 등록대수 현황 (EUC-KR 페이지, ‘합계’ 행)"]),
        item("bus", "어린이 통학버스", "헤럴드경제 2026.3.31", [
            "전국 약 43,857대 · 경찰 · 교육부 합동 점검(그중 10%, 약 4,300대) 기사의 숫자",
            "※ 기사에 집계 기준(경찰 신고 대수인지)이 없음 [확인 필요]",
            "2014년 교육부 전수조사(67,363대 중 신고 37,409대, 학원 신고율 7.1%)는 오래되고 원문 확인 못해 기획서에는 쓰지 않음"]),
        item("shuttle", "자율주행 셔틀", "서울경제 2025.4.1", [
            "2020년부터 9차례, 고속도로 포함 17개 시 · 도 42곳 시범운행지구 지정",
            "지정 1년이 지난 34곳 중 14곳은 서비스 미운영",
            "※ 2025년 하반기 이후 추가 지정(양주, 성남 등)이 있어 현재 총수는 더 많을 수 있음",
            "※ 전국 운행 차량 대수는 찾지 못함 [확인 필요] · 예: 성남 2개 노선에 1대씩 (디지털타임스, 검색 요약 기준, 원문 미확인)"]),
    ])


if __name__ == "__main__":
    for f in (fig1, fig2, fig3, fig4, fig5, fig6, fig7, fig8, fig9):
        f()

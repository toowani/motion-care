"""Infographic figures for research/literature.md.

    python3 sim/figs_doc_literature.py      # writes docs/img/doc_literature_<n>.svg

Helpers come from sim/figs_tables.py (read-only). Text is the literature.md content, tightened.
A leading "!" marks an emphasised line, "~" a limit/caveat line.
"""
from figs_tables import Svg, wrap, tw, INK, MUTED, FOOT, BLUE, BAND, LINE, CARD, LOW, MID, HIGH, MID_INK, LOW_T, MID_T, HIGH_T
import crave_style as cs

RED = cs.RED

# (badge, short cite, venue/title line, used-in, icon, bullets)
P = {
    "1": ("1", "Turner & Griffin (1999)", "Ergonomics 42(12), 1646–1664 · Motion sickness in public road transport: the effect of driver, route and vehicle",
          "문제 정의 · 파라미터 1·2 · 운전 방식 시나리오", "bus", [
              "영국 본토 버스·코치 56회 운행, 승객 3,256명 조사 (운전자 17명, 차종 5종)",
              "앞뒤·옆 흔들림을 크게 만드는 운전자일수록 멀미가 많음",
              "교외(cross-country) 위주 노선은 옆 흔들림이 커서 메스꺼움이 많음",
              "!0.5 Hz 미만의 옆 흔들림 노출이 멀미와 가장 잘 맞음",
              "뒤쪽 좌석일수록 옆 흔들림과 멀미 증가 · 차종 간 차이는 없음"]),
    "2": ("2", "Donohew & Griffin (2004)", "Aviat Space Environ Med 75(8), 649–656 · Motion sickness: effect of the frequency of lateral oscillation · DOI 없음, PubMed 15328780",
          "느린 흔들림 대역(수식 ①②) · 옆 방향 가중", "wave", [
              "120명(20명씩 6개 주파수, 0.0315–0.20 Hz) 실험을 이전 실험과 합쳐 0.0315–0.8 Hz 가중을 제안",
              "!옆 방향 가중: 0.0315–0.25 Hz에서 평평, 0.25–0.8 Hz에서 옥타브당 12 dB 감소 (수직 방향 가중과 다름)",
              "~옆 흔들림과 함께 롤이 있는 환경에는 맞지 않을 수 있음"]),
    "3": ("3", "ISO 2631-1:1997", "Mechanical vibration and shock — Evaluation of human exposure to whole-body vibration — Part 1 · 표준이라 DOI 없음",
          "누적량 공식(수식 ③) · 기준선 tau0", "gauge", [
              "멀미 노출량 MSDVz (단위 m/s^1.5)",
              "구토 비율 ≈ Km × MSDVz, 성인 남녀 혼합 Km = 1/3",
              "~수직 흔들림 기준이라 차량 수평 흔들림에는 그대로 맞지 않는다는 비판이 있음",
              "!우리는 공식 형태만 빌리고 방향 가중은 1·2번으로 보완",
              "~표준 원문은 확인 못 함(유료) · Km = 1/3과 수직 한정은 Diels 외(2022, SAE 예고본)·Bos(2022, Vibration) 인용으로 확인"]),
    "4": ("4", "Kuiper, Bos, Diels & Schmidt (2020)", "Applied Ergonomics 85, 103068 · Knowing what's coming: Anticipatory audio cues can mitigate motion sickness",
          "예고 음성(수식 ⑦) · \"운전자는 왜 멀미를 안 할까\"", "speaker", [
              "20명, 레일 위 썰매에서 앞뒤 9 m 이동, 15분 × 2조건",
              "!움직임 1초 전에 \"앞으로/뒤로\" 음성을 준 조건이, 같은 음성을 움직임 시작 4–6초 뒤에 준 조건보다 멀미 점수가 유의하게 낮음",
              "~실험실 모션 장치 결과라 실제 차량 효과는 추가 확인 필요"]),
    "4a": ("4a", "Bos, Kuiper & Schmidt (2020)", "DSC 2020 Europe VR 발표 논문, Antibes, 9–11 Sep 2020, 179–182 · Motion predictability and sickness · DOI 없음, TNO 원문 PDF",
           "4번의 수치 인용", "doc", [
               "4번(실험 B)과 Kuiper 외(2020) Human Factors 62(8), 1339–1348(실험 A, 17명)을 묶어 정리한 학회 논문",
               "실험 A: 예측 불가 움직임(방향 또는 시점 무작위) 두 조건이 예측 가능 조건보다 멀미 점수 52% 높음",
               "실험 B: 움직임 1초 전 음성 예고가 움직임 중 음성보다 멀미 점수 17% 낮춤",
               "!17%는 '예고 없음'이 아니라 '움직임 중 음성' 대비 · 저널 초록에는 이 백분율이 없음"]),
    "5": ("5", "D'Amour, Bos & Keshavarz (2017)", "Experimental Brain Research 235(9), 2811–2820 · The efficacy of airflow and seat vibration on reducing visually induced motion sickness",
          "환경 계수(수식 ④) · 공조 닫힌 고리", "wind", [
              "82명을 4개 집단(바람, 진동, 바람+진동, 대조)에 무작위 배정, 1인칭 자전거 영상 15분",
              "!바람은 멀미를 유의하게 줄였고, 좌석 진동은 효과 없음",
              "~시각 유발 멀미 연구라 차량 멀미에는 근거 수준을 낮춰 표현"]),
    "6": ("6", "Huppert, Grill & Brandt (2019)", "Journal of Neurology 266(Suppl 1), 65–73 · Survey of motion sickness susceptibility in children and adolescents aged 3 months to 18 years",
          "가족 모드 · 타깃(아이 있는 가족)", "child", [
              "독일 인구 기반 설문 2건, 부모 3,285명이 아이 5,041명 응답",
              "!6개월–18세 조사에서 9.2%가 멀미 취약",
              "4–13세에 가장 흔하고 사춘기 이후 감소하는 역U자형",
              "1세 이후 발생하면 어릴수록 심하고 6–9세에서 가장 심함 · 1세 미만 첫 발생은 극히 드묾(2명)"]),
    "8": ("8", "Gu 외 (2025, 나고야대)", "Environ Health Prev Med 30, 22 · Just 1-min exposure to a pure tone at 100 Hz with daily exposable sound pressure levels may improve motion sickness",
          "핵심 기술: 저주파 소리 (LowTone.kt)", "tone", [
              "생쥐 속귀 조직 실험으로 고른 100 Hz 순음을 80–85 dBZ(60.9–65.9 dBA)로 1분, 양쪽 귀에, 흔들리기 직전에 재생",
              "그네 · 주행 시뮬레이터 · 실제 차(머리받침 스피커, 10–25 km/h 정지·출발 3분, 책 읽기) 모두에서 자세 흔들림 증가가 줄어듦",
              "!시뮬레이터에서 심박 변이(HF 증가, LF/HF 감소), 실제 차에서 MSAQ 점수가 낮아짐 (P < 0.05)",
              "그네 실험 29명(멀미 잘 하는 쪽에서만 유의), 전체 등록 82명",
              "~같은 사람이 소리 있음·없음을 모두 받는 설계이고 가짜 소리 대조는 없음",
              "~사람에서 효과 지속 시간은 재지 않음 (생쥐에서 120분 이상) · 4 kHz 청력 영향 없음",
              "~흔들리는 중 재생 효과는 저자도 추가 연구가 필요하다고 씀",
              "DENSO 후원, 저자 2명(DENSO 소속)과 특허 발명자 2명 · 특허 내용과 한국 출원 여부는 research/market.md · J-STAGE 원문으로 2026-11-13 확인"]),
    "7": ("7", "Schmidt, Kuiper, Wolter, Diels & Bos (2020)", "Transportation Research Part F 71, 76–87 · An international survey on the incidence and modulating factors of carsickness",
          "요약 슬라이드의 \"승객 차멀미 경험 46%\"", "person", [
              "온라인 설문, 5개국(브라질·중국·독일·영국·미국) 4,479명",
              "!46%가 지난 5년 동안 승용차 승객으로 어느 정도의 차멀미를 겪음, 어릴 때 경험까지 넣으면 59%",
              "여성·젊은 층에서 더 많고, 주요 요인은 주행 방식·시각 활동·나쁜 공기",
              "~자기 보고 설문이라 실제 발생률과는 다를 수 있음 · 서지·초록은 Crossref·OpenAlex로 2026-11-12 확인"]),
}

W, X0, CW = 960, 28, 904
TX, TW = 96, 804          # text column
LH, SZ = 20, 14


def paper_card(s, y, key):
    badge, cite, venue, used, ic, bullets = P[key]
    vw = wrap(venue, 12.5, TW)
    rows = []
    for b in bullets:
        kind = b[0] if b[0] in "!~" else ""
        t = b[1:] if kind else b
        rows.append((kind, wrap(t, SZ, TW - 16, bold=(kind == "!"))))
    h = 30 + 24 + len(vw) * 17 + 34 + sum(len(r[1]) for r in rows) * LH + len(rows) * 5 + 14
    s.card(X0, y, CW, h)
    s.circle(64, y + 38, 20, s.grad())
    s.text(64, y + 38 + (5 if len(badge) == 1 else 4.5), badge, 16 if len(badge) == 1 else 14, "#FFFFFF", "middle", 800)
    s.text(TX, y + 36, cite, 18, INK, "start", 800)
    yy = y + 58
    for ln in vw:
        s.text(TX, yy, ln, 12.5, FOOT)
        yy += 17
    yy += 4
    w = s.chip(TX, yy, "쓰는 곳", BLUE, BAND, BAND, 12, 24, bold=True)
    s.text(TX + w + 10, yy + 17, used, 14, BLUE, "start", 700)
    yy += 24 + 26
    for kind, ls in rows:
        col = {"!": INK, "~": MUTED, "": INK}[kind]
        dot = {"!": BLUE, "~": MID, "": FOOT}[kind]
        s.circle(TX + 3, yy - 5, 3, dot)
        for ln in ls:
            s.text(TX + 16, yy, ln, SZ, col, "start", 700 if kind == "!" else 400)
            yy += LH
        yy += 5
    return h


def papers_fig(name, title, keys, banner=None):
    # dry run for height
    probe = Svg(W, 10)
    H = 28
    if banner:
        H += 84 + 14
    for k in keys:
        H += paper_card(probe, 0, k) + 14
    H += 14
    s = Svg(W, H)
    y = 28
    if banner:
        y += banner(s, y) + 14
    for k in keys:
        y += paper_card(s, y, k) + 14
    s.save(name, title)


def banner(s, y):
    h = 84
    s.card(X0, y, CW, h, cs.BAND, cs.BAND)
    s.icon("search", 64, y + 42, "#FFFFFF", s.grad(), 22)
    s.text(TX, y + 34, "shared/params.json 값의 출처 · 서지 재확인 2026-11-02", 18, INK, "start", 800)
    x = TX
    for lab, bg, fg in [("PubMed · Crossref 서지·초록", "#FFFFFF", BLUE), ("공개 원문 PDF: 4번 두 편(4, 4a)", "#FFFFFF", BLUE),
                        ("ISO 2631-1 원문은 유료 → 2차 문헌만", MID_T, MID_INK)]:
        x += s.chip(x, y + 48, lab, fg, bg, LINE, 13, 26, bold=True) + 10
    return h


def params_fig():
    cols = [(48, 200, "항목"), (258, 150, "현재 값"), (418, 270, "근거"), (712, 200, "상태")]
    rows = [
        ("누적량 공식 D = (Σ w a² Δt)^1/2", "—", "3", ("ok", "근거 있음 (형태만 차용)")),
        ("느린 흔들림 대역", "10 s 평균 제거, 1 Hz 샘플", "1, 2 (옆 방향 0.25 Hz 이하가 핵심)", ("ok", "근거 있음")),
        ("누적 기준선 tau0", "30", "3: Km 1/3 → MSDV 30에서 약 10% 구토 예측 (수직 기준)", ("as", "수평 흔들림 적용은 가정")),
        ("방향 가중 wX, wY", "1.0, 1.0", "1: 옆 흔들림이 멀미와 가장 강하게 연관", ("as", "wY를 1보다 크게 둘 근거 있음 · 시뮬레이션 점검 후 조정 후보")),
        ("지수 기준선 thetaCaution, thetaHigh", "0.9, 1.5 m/s²", "시뮬레이션 합리성 점검(G2)에 맞춘 출발값", ("no", "문헌 직접 근거 없음 → 기획서에 \"초기값, 출시 후 보정\" 명시")),
        ("환경 계수 gammaT, gammaV", "0.04 /°C, 0.15", "5 (바람 효과의 방향), 크기는 가정", ("as", "방향만 근거, 크기는 가정")),
        ("예고 시점", "말 2 s + 준비 3 s", "4는 1초 전 예고로도 효과", ("ok", "근거 있음 (우리는 더 여유 있게)")),
        ("개인 기준선 수축 tauPriorN", "5", "적은 표본에서의 수축 추정 (방법론)", ("na", "—")),
        ("뒷좌석 우선", "뒷좌석 탑승 시 가족 모드 제안", "1: 뒤쪽일수록 흔들림·멀미 증가", ("ok", "근거 있음")),
    ]
    sz, lh = 13.5, 19
    laid, y = [], 28 + 52 + 36
    for r in rows:
        cells = [wrap(r[0], sz, 195, True), wrap(r[1], sz, 145), wrap(r[2], sz, 270), wrap(r[3][1], sz, 190)]
        h = max(len(c) for c in cells) * lh + 20
        laid.append((r, cells, y, h))
        y += h + 8
    s = Svg(W, y + 20)
    s.title("파라미터별 근거 상태", 28, 58, 22)
    hy = 28 + 52 + 20
    for x, w, lab in cols:
        s.text(x, hy, lab, 13, FOOT, "start", 700)
    kc = {"ok": (LOW, "근거 있음"), "as": (MID, "일부 가정"), "no": (HIGH, "근거 없음"), "na": (FOOT, "")}
    # legend
    lx = 912
    for k in ("no", "as", "ok"):
        col, lab = kc[k]
        lx -= tw(lab, 12.5) + 22
        s.circle(lx, 50, 5, col)
        s.text(lx + 10, 54.5, lab, 12.5, MUTED)
        lx -= 8
    for r, cells, yy, h in laid:
        s.card(X0, yy, CW, h)
        for (x, w, _), c, i in zip(cols, cells, range(4)):
            ty = yy + 24
            for ln in c:
                if i == 3:
                    s.text(x + 18, ty, ln, sz, INK)
                else:
                    s.text(x, ty, ln, sz, INK, "start", 700 if i == 0 else 400)
                ty += lh
        s.circle(712 + 4, yy + 19, 5, kc[r[3][0]][0])
    s.save("doc_literature_5.svg", "파라미터별 근거 상태")


def sentences_fig():
    items = [
        ("steering", "운전자는 다음 움직임을 예측하기 때문에 멀미를 덜 한다. 실험에서도 움직임을 미리 알려 주는 음성만으로 멀미가 줄었다", "Kuiper 외, 2020"),
        ("bus", "버스 승객 3천여 명 조사에서 멀미는 운전 방식과 0.5 Hz 미만의 옆 흔들림에 좌우됐다. 우리 앱이 차량 데이터로 이 느린 흔들림을 계산하는 이유다", "Turner & Griffin, 1999"),
        ("wind", "얼굴로 오는 바람은 멀미를 줄인다. 차가 직접 공조를 바꿀 수 있는 이유가 여기에 있다", "D'Amour 외, 2017"),
        ("child", "아이의 멀미는 4–13세에 가장 흔하다. 멀미 케어의 효과가 아이에게 가장 클 것으로 보는 이유다", "Huppert 외, 2019"),
        ("tone", "100 Hz 소리를 1분 들려주면 뒤이은 흔들림에서 멀미가 줄었다. 차는 언제 흔들릴지 알기 때문에 그 때를 잡아 소리를 틀 수 있다", "Gu 외, 2025"),
    ]
    y, laid = 28 + 50, []
    for ic, t, c in items:
        ls = wrap(t, 15, 640)
        h = max(64, len(ls) * 22 + 24)
        laid.append((ic, ls, c, y, h))
        y += h + 10
    s = Svg(W, y + 20)
    s.title("기획서에 쓸 문장 (초안)", 28, 58, 22)
    for ic, ls, c, yy, h in laid:
        s.card(X0, yy, CW, h)
        s.icon(ic, 66, yy + h / 2, BLUE, BAND, 22)
        ty = yy + h / 2 - (len(ls) - 1) * 11 + 5
        for ln in ls:
            s.text(104, ty, ln, 15, INK)
            ty += 22
        s.chip(914, yy + h / 2 - 13, c, BLUE, BAND, BAND, 12.5, 26, bold=True, anchor="end")
    s.save("doc_literature_6.svg", "기획서에 쓸 문장 초안 다섯 개")


if __name__ == "__main__":
    papers_fig("doc_literature_1.svg", "핵심 근거 1–3: 버스 조사, 옆 흔들림 가중, ISO 누적량", ["1", "2", "3"], banner)
    papers_fig("doc_literature_2.svg", "핵심 근거 4, 4a: 예고 음성 실험", ["4", "4a"])
    papers_fig("doc_literature_3.svg", "핵심 근거 5–6: 바람, 아이 멀미", ["5", "6"])
    papers_fig("doc_literature_4.svg", "핵심 근거 8, 7: 100 Hz 저주파 소리, 차멀미 국제 설문", ["8", "7"])
    params_fig()
    sentences_fig()

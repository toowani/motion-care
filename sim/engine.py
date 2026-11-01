"""Reference engine (STEP 4). The Kotlin app/core must reproduce these numbers.

Formulas (team page numbering):
  1  a_x = dv/dt, a_y = v * dtheta/dt (EMA on v and yaw rate), low-frequency = a - mean(last 10 s)
  2  dose D = sqrt(sum (wX ax^2 + wY ay^2) dt), index I = RMS over last 60 s
  3  cabin factor m = 1 - gT*max(0, T - T0) - gV*min(1, t_recirc/3600), floor 0.6
  4  cue distance = v * (say + prepare)
  5  personal threshold tau = (n*meanD + n0*tau0) / (n + n0), P = sigmoid((D - tau)/s)
"""
import math
from collections import deque


def wrap_pi(a):
    """Wrap an angle difference to (-pi, pi]."""
    a = (a + math.pi) % (2 * math.pi) - math.pi
    return math.pi if a == -math.pi else a


class MotionEstimator:
    def __init__(self, p):
        self.alpha = p["speedEmaAlpha"]
        self.vmin = p["minSpeedForHeading"]
        self.v = None
        self.theta = None
        self.omega = 0.0

    def step(self, speed, bearing_deg, dt):
        theta = math.radians(bearing_deg)
        if self.v is None:
            self.v, self.theta = speed, theta
            return 0.0, 0.0
        v_prev = self.v
        self.v = self.alpha * speed + (1 - self.alpha) * self.v
        ax = (self.v - v_prev) / dt
        # compass bearing grows clockwise, so a right turn is a positive change
        # yaw rate gets the same EMA as speed: GPS bearing noise is multiplied by v in a_y
        omega = wrap_pi(theta - self.theta) / dt
        self.theta = theta
        self.omega = self.alpha * omega + (1 - self.alpha) * self.omega
        ay = self.v * self.omega if self.v >= self.vmin else 0.0
        return ax, ay


class LowFreqFilter:
    def __init__(self, window):
        self.buf = deque(maxlen=window)

    def step(self, a):
        self.buf.append(a)
        return a - sum(self.buf) / len(self.buf)


class SicknessIndex:
    def __init__(self, p):
        self.wx, self.wy = p["wX"], p["wY"]
        self.win = deque(maxlen=int(p["indexWindowS"] * p["sampleHz"]))
        self.dose_sq = 0.0

    def step(self, ax, ay, dt):
        e = self.wx * ax * ax + self.wy * ay * ay
        self.win.append(e)
        self.dose_sq += e * dt
        return math.sqrt(sum(self.win) / len(self.win)), math.sqrt(self.dose_sq)


class CabinFactor:
    def __init__(self, p):
        self.p = p
        self.recirc_s = 0.0

    def step(self, temp, recirc, window_open, dt):
        if recirc and not window_open:
            self.recirc_s += dt
        else:
            self.recirc_s = 0.0
        p = self.p
        m = 1 - p["gammaT"] * max(0.0, temp - p["cabinT0"]) \
              - p["gammaV"] * min(1.0, self.recirc_s / p["recircSaturationS"])
        return max(p["cabinFactorFloor"], m)


NORMAL, CAUTION, HIGH = "NORMAL", "CAUTION", "HIGH"


class AlertMachine:
    """Hysteresis on the index: enter after enterHoldS above theta*m, leave below exitRatio*theta*m."""

    def __init__(self, p):
        self.p = p
        self.state = NORMAL
        self.above_c = self.above_h = 0.0
        self.last_tts = -1e9
        self.high_s = 0.0

    def step(self, t, index, m, dt):
        p = self.p
        th_c, th_h = p["thetaCaution"] * m, p["thetaHigh"] * m
        self.above_c = self.above_c + dt if index > th_c else 0.0
        self.above_h = self.above_h + dt if index > th_h else 0.0
        prev = self.state
        if self.state == NORMAL and self.above_c >= p["enterHoldS"]:
            self.state = CAUTION
        if self.state in (NORMAL, CAUTION) and self.above_h >= p["enterHoldS"]:
            self.state = HIGH
        if self.state == HIGH and index < p["exitRatio"] * th_h:
            self.state = CAUTION
        if self.state == CAUTION and index < p["exitRatio"] * th_c:
            self.state = NORMAL
        self.high_s = self.high_s + dt if self.state == HIGH else 0.0
        actions = []
        if self.state != prev and self.state in (CAUTION, HIGH):
            if t - self.last_tts >= p["ttsCooldownS"]:
                actions.append("TTS_DRIVER")
                self.last_tts = t
            actions.append("HVAC_FRESH")
        if self.state == HIGH and abs(self.high_s - p["highHoldForRestStopS"]) < dt / 2:
            actions.append("SUGGEST_REST_STOP")
        return self.state, actions, th_c, th_h


class CueScheduler:
    """Speak once per upcoming turn: on turn-signal rising edge or when the turn is within v*(say+prepare)."""

    def __init__(self, p):
        self.lead = p["cueSayS"] + p["cuePrepareS"]
        self.prev_signal = "OFF"
        self.prev_next_m = -1
        self.armed = True

    def step(self, speed, signal, next_m, next_dir):
        # a new turn ahead (distance jumped up or guidance ended) re-arms the cue
        if signal == "OFF" and (next_m < 0 or next_m > self.prev_next_m + 20):
            self.armed = True
        cue = None
        rising = signal != "OFF" and self.prev_signal == "OFF"
        near = 0 <= next_m <= max(15.0, speed * self.lead)
        if self.armed and (rising or near):
            if signal != "OFF":
                cue = signal
            else:
                cue = "LEFT" if next_dir == "L" else "RIGHT"
            self.armed = False
        self.prev_signal = signal
        self.prev_next_m = next_m
        return cue


def personal_threshold(p, onset_doses):
    n, n0 = len(onset_doses), p["tauPriorN"]
    mean = sum(onset_doses) / n if n else 0.0
    return (n * mean + n0 * p["tau0"]) / (n + n0)


def sickness_probability(p, dose, tau, m=1.0):
    return 1 / (1 + math.exp(-(dose - tau * max(p["cabinFactorFloor"], m)) / p["tauSlope"]))


class Engine:
    def __init__(self, p):
        self.p = p
        self.motion = MotionEstimator(p)
        self.lfx = LowFreqFilter(int(p["lowFreqWindowS"] * p["sampleHz"]))
        self.lfy = LowFreqFilter(int(p["lowFreqWindowS"] * p["sampleHz"]))
        self.index = SicknessIndex(p)
        self.cabin = CabinFactor(p)
        self.alerts = AlertMachine(p)
        self.cues = CueScheduler(p)
        self.dt = 1.0 / p["sampleHz"]

    def step(self, s):
        dt = self.dt
        ax, ay = self.motion.step(s["speed"], s["bearing"], dt)
        ax, ay = self.lfx.step(ax), self.lfy.step(ay)
        idx, dose = self.index.step(ax, ay, dt)
        m = self.cabin.step(s["cabin_temp"], s["recirc"], s["window_open"], dt)
        state, actions, th_c, th_h = self.alerts.step(s["t"], idx, m, dt)
        cue = self.cues.step(s["speed"], s["turn_signal"], s["next_turn_m"], s["next_turn_dir"])
        stale_air = s["recirc"] or s["cabin_temp"] > self.p["cabinT0"] + 0.5
        actions = [a for a in actions if a != "HVAC_FRESH" or stale_air]
        if cue:
            actions = actions + ["CUE_" + cue]
        return {"ax": ax, "ay": ay, "index": idx, "dose": dose, "m": m,
                "state": state, "th_c": th_c, "th_h": th_h, "actions": actions}

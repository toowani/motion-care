"""Reference engine (STEP 4). The Kotlin app/core must reproduce these numbers.

Formulas (team page numbering):
  1  a_x = dv/dt, a_y = v * dtheta/dt, then a band filter shaped like the lateral motion-sickness
     weighting (Donohew & Griffin 2004: flat 0.0315-0.25 Hz, falling above): 2nd-order Butterworth
     high-pass at 0.0315 Hz (removes steady acceleration and long curves) + 2nd-order low-pass
     (0.27 Hz, Q 0.95, one zero at -0.35). 1 Hz samples hold up to 0.5 Hz (Nyquist), where the
     whole path is about -12 dB; sim/freq_response.py measures it
  2  dose D = sqrt(sum (wX ax^2 + wY ay^2) dt), index I = RMS over last 60 s (zero-padded window)
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
        # optional EMA on speed and yaw rate (speedEmaAlpha 1 = off). It used to be 0.5, a 1st-order
        # low-pass at ~0.1 Hz that cut 0.1-0.25 Hz by 3-9 dB; BandFilter's low-pass now does the
        # smoothing of GPS bearing noise (which v multiplies in a_y)
        omega = wrap_pi(theta - self.theta) / dt
        self.theta = theta
        self.omega = self.alpha * omega + (1 - self.alpha) * self.omega
        ay = self.v * self.omega if self.v >= self.vmin else 0.0
        return ax, ay


class Biquad:
    """y = (b0 x + b1 x1 + b2 x2 - a1 y1 - a2 y2), direct form I, zero initial state."""

    def __init__(self, b, a):
        self.b0, self.b1, self.b2 = b
        self.a1, self.a2 = a
        self.x1 = self.x2 = self.y1 = self.y2 = 0.0

    def step(self, x):
        y = self.b0 * x + self.b1 * self.x1 + self.b2 * self.x2 - self.a1 * self.y1 - self.a2 * self.y2
        self.x2, self.x1 = self.x1, x
        self.y2, self.y1 = self.y1, y
        return y


def highpass_coeffs(f0, q, fs):
    """2nd-order high-pass (bilinear transform, pre-warped: -3 dB at f0 for q = 1/sqrt(2))."""
    w0 = 2 * math.pi * f0 / fs
    c, al = math.cos(w0), math.sin(w0) / (2 * q)
    a0 = 1 + al
    return ((1 + c) / 2 / a0, -(1 + c) / a0, (1 + c) / 2 / a0), (-2 * c / a0, (1 - al) / a0)


def lowpass_coeffs(fn, q, zero, fs):
    """2nd-order low-pass: analog poles (natural frequency fn, quality q) mapped by z = exp(sT),
    one zero at z = -zero, unity gain at 0 Hz. No forced zero at Nyquist (unlike the bilinear
    transform), so 0.5 Hz keeps about -8 dB here and the whole path about -12 dB."""
    zeta, wn = 1 / (2 * q), 2 * math.pi * fn / fs
    r, th = math.exp(-zeta * wn), wn * math.sqrt(1 - zeta * zeta)
    a1, a2 = -2 * r * math.cos(th), r * r
    g = (1 + a1 + a2) / (1 + zero)
    return (g, g * zero, 0.0), (a1, a2)


class BandFilter:
    """Motion-sickness band for one axis: high-pass (steady part out) then low-pass. O(1) state."""

    def __init__(self, p):
        fs = p["sampleHz"]
        self.hp = Biquad(*highpass_coeffs(p["bandHighPassHz"], p["bandHighPassQ"], fs))
        self.lp = Biquad(*lowpass_coeffs(p["bandLowPassHz"], p["bandLowPassQ"], p["bandLowPassZero"], fs))

    def step(self, a):
        return self.lp.step(self.hp.step(a))


class SicknessIndex:
    def __init__(self, p):
        self.wx, self.wy = p["wX"], p["wY"]
        self.win = deque(maxlen=int(p["indexWindowS"] * p["sampleHz"]))
        self.dose_sq = 0.0

    def step(self, ax, ay, dt):
        e = self.wx * ax * ax + self.wy * ay * ay
        self.win.append(e)
        self.dose_sq += e * dt
        # fixed denominator (the window is zero-padded at start-up): dividing by len(win)
        # let the first few samples of a ride inflate I into a false early alert
        return math.sqrt(sum(self.win) / self.win.maxlen), math.sqrt(self.dose_sq)


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
    """Speak once per upcoming turn: on turn-signal rising edge or when the turn is within v*(say+prepare).
    A red light right before the turn (stopped >= 3 s with the turn < 40 m ahead) allows one more
    cue on departure, so the passenger hears it a few seconds before the turn, not 40 s early."""

    STOP_SPEED = 0.5      # m/s
    STOP_HOLD_S = 3.0
    STOP_NEAR_M = 40.0

    def __init__(self, p):
        self.lead = p["cueSayS"] + p["cuePrepareS"]
        self.prev_signal = "OFF"
        self.prev_next_m = -1
        self.armed = True
        self.stopped_s = 0.0
        self.recued = False

    def step(self, speed, signal, next_m, next_dir, dt=1.0):
        # a new turn ahead (distance jumped up or guidance ended) re-arms the cue,
        # whatever the indicator is doing (a lingering signal must not swallow the next cue)
        if next_m < 0 or next_m > self.prev_next_m + 20:
            self.armed = True
            self.recued = False
        if speed < self.STOP_SPEED:
            self.stopped_s += dt
        else:
            if self.stopped_s >= self.STOP_HOLD_S and 0 <= next_m < self.STOP_NEAR_M and not self.recued:
                self.armed = True
                self.recued = True
            self.stopped_s = 0.0
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
        self.lfx = BandFilter(p)
        self.lfy = BandFilter(p)
        self.index = SicknessIndex(p)
        self.cabin = CabinFactor(p)
        self.alerts = AlertMachine(p)
        self.cues = CueScheduler(p)
        self.dt = 1.0 / p["sampleHz"]
        self.last = {"speed": 0.0, "bearing": 0.0, "cabin_temp": p["cabinT0"]}

    def _finite(self, s, key):
        v = s[key]
        if isinstance(v, float) and not math.isfinite(v):
            return self.last[key]
        self.last[key] = v
        return v

    def step(self, s):
        dt = self.dt
        # sensor dropouts arrive as NaN: hold the last finite value (cabin falls back to T0)
        nm = s["next_turn_m"]
        s = dict(s, speed=self._finite(s, "speed"), bearing=self._finite(s, "bearing"),
                 cabin_temp=self._finite(s, "cabin_temp"),
                 next_turn_m=-1 if isinstance(nm, float) and not math.isfinite(nm) else nm)
        ax, ay = self.motion.step(s["speed"], s["bearing"], dt)
        ax, ay = self.lfx.step(ax), self.lfy.step(ay)
        idx, dose = self.index.step(ax, ay, dt)
        m = self.cabin.step(s["cabin_temp"], s["recirc"], s["window_open"], dt)
        state, actions, th_c, th_h = self.alerts.step(s["t"], idx, m, dt)
        cue = self.cues.step(s["speed"], s["turn_signal"], s["next_turn_m"], s["next_turn_dir"], dt)
        stale_air = s["recirc"] or s["cabin_temp"] > self.p["cabinT0"] + 0.5
        actions = [a for a in actions if a != "HVAC_FRESH" or stale_air]
        if cue:
            actions = actions + ["CUE_" + cue]
        return {"ax": ax, "ay": ay, "index": idx, "dose": dose, "m": m,
                "state": state, "th_c": th_c, "th_h": th_h, "actions": actions}

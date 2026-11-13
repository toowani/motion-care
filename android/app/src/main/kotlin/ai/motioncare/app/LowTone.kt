package ai.motioncare.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin

private const val TAG = "MotionCare"

/**
 * One minute of a 100 Hz pure tone through the car speakers.
 *
 * Source: Gu et al. (Nagoya Univ.), Environ Health Prev Med 2025;30:22, doi:10.1265/ehpm.24-00247.
 * Heard with both ears for 1 minute right before shaking, the tone lowered postural sway (swing,
 * driving simulator, real car), improved heart-rate variability (simulator) and lowered MSAQ
 * scores (real car). All four conditions mattered: both ears, 100 Hz, 80-85 dBZ, 1 minute.
 *
 * The paper's level is measured at the ear; car speakers, cabins and the media volume differ, so
 * [LEVEL] is a calibration knob to set per car model with a sound meter at the rear headrest.
 * Other audio is ducked, not stopped, while the tone plays.
 */
class LowTone(context: Context, private val log: (String) -> Unit) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs).build()
    private var track: AudioTrack? = null

    val playing get() = track != null

    /** Starts the minute unless one is already playing; returns whether it started. */
    fun play(): Boolean {
        if (track != null) return false
        val t = runCatching { build() }.onFailure { Log.w(TAG, "low tone", it) }.getOrNull() ?: return false
        track = t
        audio?.requestAudioFocus(focus)
        t.setVolume(0f)
        t.play()
        ramp(t, 0f, LEVEL)
        main.postDelayed({ if (track === t) ramp(t, LEVEL, 0f) { stop() } }, DURATION_MS - FADE_MS)
        log("저주파 소리: 100 Hz 1분 재생")
        return true
    }

    fun stop() {
        val t = track ?: return
        track = null
        main.removeCallbacksAndMessages(null)
        runCatching { t.stop() }
        t.release()
        audio?.abandonAudioFocusRequest(focus)
    }

    /**
     * One second holds exactly 100 cycles and starts and ends at zero, so looping it is seamless;
     * 59 extra loops make the minute.
     */
    private fun build(): AudioTrack {
        val pcm = ShortArray(RATE) { i -> (sin(2 * PI * FREQ_HZ * i / RATE) * Short.MAX_VALUE).toInt().toShort() }
        val t = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        t.write(pcm, 0, pcm.size)
        t.setLoopPoints(0, pcm.size, (DURATION_MS / 1000 - 1).toInt())
        return t
    }

    /** Fades over [FADE_MS] so the speakers do not thump. */
    private fun ramp(t: AudioTrack, from: Float, to: Float, then: () -> Unit = {}) {
        val steps = 20
        for (k in 1..steps) main.postDelayed({
            if (track === t) t.setVolume(from + (to - from) * k / steps)
            if (k == steps) then()
        }, FADE_MS * k / steps)
    }

    companion object {
        const val FREQ_HZ = 100.0
        const val DURATION_MS = 60_000L
        private const val FADE_MS = 2_000L
        private const val RATE = 8_000
        /** Calibration knob (0-1 of the media volume): set per car so the rear headrest reads 61-66 dBA. */
        const val LEVEL = 0.6f
    }
}

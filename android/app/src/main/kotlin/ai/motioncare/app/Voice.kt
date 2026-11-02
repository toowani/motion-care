package ai.motioncare.app

import ai.motioncare.core.Action
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.tts.listener.OnServerConnectionListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInit
import ai.pleos.playground.stt.listener.OnServerConnectionListener as SttConnection
import android.speech.tts.TextToSpeech as PlatformTts

private const val TAG = "MotionCare"

/**
 * Voice in and out through the Pleos SDKs (HYBRID: server voice when registered, on-device otherwise).
 * Client ID/secret come from local.properties via BuildConfig. If the Pleos TTS service is not
 * available the platform TextToSpeech is used, so cues are never silent.
 */
class Voice(private val context: Context, private val log: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val clientId = BuildConfig.PLEOS_CLIENT_ID
    private val clientSecret = BuildConfig.PLEOS_CLIENT_SECRET

    private var pleosTts: TextToSpeech? = null
    private var pleosReady = false
    private var fallback: PlatformTts? = null
    private var stt: SpeechToText? = null
    private var onHeard: ((String) -> Unit)? = null
    private var onNoVoice: (() -> Unit)? = null
    private var askSeq = 0

    fun start() {
        runCatching {
            val tts = TextToSpeech(context, Mode.HYBRID)
            pleosTts = tts
            tts.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    tts.setLocale(Locale.KOREAN)
                    pleosReady = true
                    if (clientId.isNotBlank()) {
                        tts.registerApp(clientId, clientSecret, object : OnServerConnectionListener {
                            override fun onConnected() { main.post { log("Pleos TTS: 서버 음성 연결됨") } }
                            override fun onFailed(errorType: ErrorType) { main.post { log("Pleos TTS: 서버 연결 실패($errorType), 차량 내장 음성 사용") } }
                        })
                    }
                    main.post { log("Pleos TTS 준비됨") }
                }
                override fun onFailure(errorType: ErrorType) {
                    main.post { log("Pleos TTS 초기화 실패($errorType), 기본 음성 사용"); usePlatformTts() }
                }
            })
        }.onFailure { Log.w(TAG, "pleos tts", it); usePlatformTts() }

        runCatching {
            val s = SpeechToText(context, SttMode.HYBRID)
            stt = s
            s.initialize(object : SttInit {
                override fun onSuccess() {
                    s.setLocale(Locale.KOREAN)
                    s.addListener(resultListener)
                    if (clientId.isNotBlank()) {
                        s.registerApp(clientId, clientSecret, object : SttConnection {
                            override fun onConnected() { main.post { log("Pleos STT: 서버 연결됨") } }
                            override fun onFailed(errorType: SttError) { main.post { log("Pleos STT: 서버 연결 실패($errorType)") } }
                        })
                    }
                }
                override fun onFailure(errorType: SttError) { main.post { log("Pleos STT 초기화 실패($errorType)") } }
            })
        }.onFailure { Log.w(TAG, "pleos stt", it) }
    }

    private val resultListener = object : ResultListener {
        override fun onUpdated(text: String, isFinal: Boolean) {
            if (isFinal && text.isNotBlank()) main.post {
                log("들은 말: \"$text\"")
                onHeard?.invoke(text)
                onHeard = null
                onNoVoice = null
            }
        }
        override fun onUpdatedEpdData(start: Long, end: Long) = Unit
        override fun onStartedRecognition() = Unit
        override fun onEndedRecognition() = Unit
        override fun onError(errorType: SttError) { main.post { log("음성 인식이 안 돼요 · 버튼으로 답해 주세요"); Log.w("MotionCare", "stt error $errorType"); giveUpListening() } }
        override fun onReady() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
    }

    private fun usePlatformTts() {
        if (fallback != null) return
        fallback = PlatformTts(context) { status ->
            if (status == PlatformTts.SUCCESS) fallback?.language = Locale.KOREAN
        }
    }

    fun say(text: String) {
        val tts = pleosTts
        if (pleosReady && tts != null) {
            runCatching { tts.speak(text, false) }.onFailure { usePlatformTts(); platformSay(text) }
        } else {
            platformSay(text)
        }
    }

    private fun platformSay(text: String) {
        fallback?.speak(text, PlatformTts.QUEUE_ADD, null, text.hashCode().toString())
    }

    /**
     * Asks a question and hands the recognized answer to [onAnswer] (once). If recognition fails
     * or nothing is heard within a few seconds, [noVoice] runs instead (e.g. on-screen buttons).
     */
    fun ask(question: String, noVoice: (() -> Unit)? = null, onAnswer: (String) -> Unit) {
        // a newer question replaces an unanswered one; its timers check the sequence number
        val id = ++askSeq
        say(question)
        onHeard = onAnswer
        onNoVoice = noVoice
        main.postDelayed({ if (id == askSeq) runCatching { stt!!.request() }.onFailure { giveUpListening() } }, 2500)
        main.postDelayed({ if (id == askSeq) giveUpListening() }, 10_000)
    }

    private fun giveUpListening() {
        val f = onNoVoice ?: return
        onHeard = null
        onNoVoice = null
        f()
    }

    fun release() {
        main.removeCallbacksAndMessages(null)
        onHeard = null
        onNoVoice = null
        runCatching { pleosTts?.release() }
        runCatching { stt?.release() }
        runCatching { fallback?.shutdown() }
    }

    companion object {
        // whole answers only: a bare syllable like 어 inside "싫어" must not count as yes
        private val yes = Regex("^(응|어|네|예|좋아|그래|들러|가자|오케이|그러자|켜 ?줘)")
        private val no = Regex("^(아니|아뇨|싫어|안 ?돼|괜찮아|됐어|그냥 가)")

        fun isYes(answer: String) = answer.trim().let { yes.containsMatchIn(it) && !no.containsMatchIn(it) }
        fun isNo(answer: String) = no.containsMatchIn(answer.trim())

        fun phrase(a: Action): String = when (a) {
            Action.CUE_LEFT -> "곧 왼쪽으로 돌아요."
            Action.CUE_RIGHT -> "곧 오른쪽으로 돌아요."
            Action.TTS_DRIVER -> "뒷좌석이 흔들리고 있어요. 조금만 부드럽게 가 볼까요?"
            Action.HVAC_FRESH -> "바깥 공기로 바꾸고 온도를 조금 낮출게요."
            Action.SUGGEST_REST_STOP -> "흔들림이 계속돼요. 가까운 휴게소에 들를까요?"
        }
    }
}

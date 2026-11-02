package ai.motioncare.app

import ai.motioncare.app.theme.Mood
import ai.motioncare.app.theme.MoodTheme
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas

/**
 * How the app speaks while another app (usually the navigation) is in front: a quiet ongoing
 * notification for the foreground service, and short heads-up cards for events and questions.
 * Cards carry the theme character as their picture and disappear on their own; questions have
 * answer buttons that go back to [TripService].
 */
class Notifier(private val context: Context) {
    private val nm = context.getSystemService(NotificationManager::class.java)
    private var nextId = 100

    init {
        nm.createNotificationChannel(NotificationChannel(CH_TRIP, "주행 지켜보기", NotificationManager.IMPORTANCE_LOW).apply {
            description = "멀미 케어가 주행 신호를 받고 있을 때 표시돼요"
        })
        // heads-up cards; silent because the car already speaks the same line
        nm.createNotificationChannel(NotificationChannel(CH_ALERT, "예고와 질문", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "회전 예고, 공기 바꾸기, 휴게소·가족 모드 질문"
            setSound(null, null)
            enableVibration(false)
        })
    }

    private fun openApp() = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
        PendingIntent.FLAG_IMMUTABLE)

    fun ongoing(text: String): Notification = Notification.Builder(context, CH_TRIP)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("멀미 케어")
        .setContentText(text)
        .setOngoing(true)
        .setContentIntent(openApp())
        .build()

    fun updateOngoing(text: String) = nm.notify(ID_ONGOING, ongoing(text))

    /** A short card over the navigation; gone after a few seconds. */
    fun event(theme: MoodTheme, mood: Mood, title: String, text: String) {
        nm.notify(nextId++, Notification.Builder(context, CH_ALERT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(themeIcon(theme, mood))
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setTimeoutAfter(EVENT_TIMEOUT_MS)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build())
    }

    /** A question card with up to three answer buttons; returns its id so it can be withdrawn. */
    fun prompt(theme: MoodTheme, mood: Mood, question: String, answers: List<String>, promptId: Int): Int {
        val id = ID_PROMPT
        val b = Notification.Builder(context, CH_ALERT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(themeIcon(theme, mood))
            .setContentTitle("멀미 케어")
            .setContentText(question)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setTimeoutAfter(PROMPT_TIMEOUT_MS)
            .setContentIntent(openApp())
        answers.take(3).forEachIndexed { i, label ->
            val intent = Intent(context, TripService::class.java).setAction(TripService.ACTION_ANSWER)
                .putExtra(TripService.EXTRA_PROMPT, promptId).putExtra(TripService.EXTRA_ANSWER, i)
            val pi = PendingIntent.getService(context, promptId * 10 + i, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            b.addAction(Notification.Action.Builder(null, label, pi).build())
        }
        nm.notify(id, b.build())
        return id
    }

    fun cancelPrompt() = nm.cancel(ID_PROMPT)

    /** The theme character as the card picture, so a child sees 루미 or 부릉이 over the map. */
    private fun themeIcon(theme: MoodTheme, mood: Mood): Bitmap {
        val px = (96 * context.resources.displayMetrics.density).toInt().coerceAtLeast(96)
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        theme.draw(Canvas(bmp), px / 2f, px / 2f, px.toFloat(), mood, when (mood) { Mood.CALM -> 0.2f; Mood.UNEASY -> 0.75f; Mood.SICK -> 1.2f }, 0f)
        return bmp
    }

    companion object {
        const val CH_TRIP = "trip"
        const val CH_ALERT = "alerts"
        const val ID_ONGOING = 1
        const val ID_PROMPT = 2
        const val EVENT_TIMEOUT_MS = 7_000L
        const val PROMPT_TIMEOUT_MS = 20_000L
    }
}

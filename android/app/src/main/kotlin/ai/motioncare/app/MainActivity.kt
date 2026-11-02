package ai.motioncare.app

import ai.motioncare.app.theme.MoodThemes
import ai.motioncare.app.ui.HomeView
import ai.motioncare.app.ui.SettingsPanel
import ai.motioncare.core.Output
import ai.motioncare.core.Params
import ai.motioncare.core.Sample
import android.app.Activity
import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout

private const val TAG = "MotionCare"
private const val PERMISSIONS_REQUEST = 1

/**
 * The screen. The trip itself runs in [TripService] so the car keeps helping while the navigation
 * is in front; this activity shows it (home + settings) while visible and sends the buttons back.
 */
class MainActivity : Activity(), HomeView.Callbacks {
    private lateinit var settings: Settings
    private lateinit var profiles: ProfileStore
    private lateinit var home: HomeView
    private lateinit var panel: SettingsPanel
    private var car: Car? = null
    private var service: TripService? = null
    private var resumed = false
    private var bound = false
    private var pendingRoute: Triple<Double, Double, String>? = null
    private var askedPermissions = false
    /** Last region reported to the Pleos navigation; empty = none. */
    private var region = Rect()

    private val screen = object : TripService.TripUi {
        override fun onReset() { home.dashboard.reset(); home.resetTrip() }
        override fun onSample(s: Sample, o: Output) {
            home.dashboard.add(s, o)
            home.setNaviGuidance(service?.naviGuidance)
            home.update(s, o, s.nextTurnM)
        }
        override fun onRoute(name: String?, track: DoubleArray?) = home.setRoute(name, track)
        override fun onCue(left: Boolean, line: String) = home.showCue(line)
        override fun onFamily(on: Boolean, names: List<String>) = home.setFamily(on, names)
        override fun onFamilyProbabilities(values: List<Pair<String, Double>>) = home.setFamilyProbabilities(values)
        override fun onAction(msg: String) = home.showAction(msg)
        override fun onNote(msg: String) = home.note(msg)
        override fun onPrompt(question: String, answers: List<String>, answer: (Int) -> Unit) =
            home.prompt(question, answers.mapIndexed { i, label -> label to { answer(i) } })
        override fun onHidePrompt() = home.hidePrompt()
        override fun onSummary(minutes: Int, cues: Int, airs: Int, peak: Float) = home.updateSummary(minutes, cues, airs, peak)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val s = (binder as TripService.LocalBinder).service
            if (!s.ready) {
                // could not become a foreground service (location not allowed): let it go
                unbindService(this); bound = false
                home.showAction("위치 권한이 있어야 주행을 지켜볼 수 있어요")
                return
            }
            service = s
            if (resumed && panel.visibility != View.VISIBLE) s.attach(screen)
            pendingRoute?.let { (lat, lon, n) -> home.postDelayed({ s.routeTo(lat, lon, n) }, 3000) }
            pendingRoute = null
            reportRegion()
        }
        override fun onServiceDisconnected(name: ComponentName?) { service = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val params = Params.fromJson(assets.open("params.json").bufferedReader().readText())
        settings = Settings(this)
        profiles = ProfileStore(this)
        home = HomeView(this, this)
        panel = SettingsPanel(this, settings, profiles, params,
            onThemeChanged = { t -> home.setTheme(t) },
            onChanged = { service?.onSettingsChanged(); applySettings() },
            onClose = { closeSettings() })
        setContentView(FrameLayout(this).apply {
            addView(home, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(panel, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        })
        home.setTheme(MoodThemes.byId(settings.themeId))
        applySettings()
        if (settings.onboarded) panel.visibility = View.GONE else onSettingsShown(first = true)
        watchDrivingRestrictions()

        // debug/QA: --es route_to "lat,lon,name" requests a route; --es theme toon; --ez skip_welcome true
        if (BuildConfig.DEBUG) {
            intent.getStringExtra("route_to")?.split(",")?.takeIf { it.size == 3 }?.let { (lat, lon, n) -> pendingRoute = Triple(lat.toDouble(), lon.toDouble(), n) }
            intent.getStringExtra("theme")?.let { id -> settings.themeId = id; home.setTheme(MoodThemes.byId(id)) }
            if (intent.getBooleanExtra("skip_welcome", false)) closeSettings()
        }
        startTrip()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        if (panel.visibility != View.VISIBLE) service?.attach(screen)
        home.post { reportRegion() }
    }

    override fun onPause() {
        resumed = false
        reportRegion()
        service?.detach(screen)   // from now on the service talks through heads-up cards
        super.onPause()
    }

    override fun onDestroy() {
        resumed = false
        reportRegion()
        service?.detach(screen)
        home.destroy()
        if (bound) unbindService(connection)
        runCatching { car?.disconnect() }
        super.onDestroy()
    }

    /** Starts (or reconnects to) the trip service once speed/location may be read. */
    private fun startTrip() {
        // location and vehicle speed feed the engine; the microphone and notifications are optional
        val wanted = arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION,
            "android.car.permission.CAR_SPEED", android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.POST_NOTIFICATIONS)
        val needed = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        // ask once; only speed and location are required, the microphone and notifications are optional
        if (needed.isNotEmpty() && !askedPermissions) {
            askedPermissions = true
            requestPermissions(needed.toTypedArray(), PERMISSIONS_REQUEST)
            return
        }
        val required = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, "android.car.permission.CAR_SPEED")
        if (required.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            home.showAction("속도·위치 권한이 있어야 차 신호를 읽을 수 있어요")
            return
        }
        val intent = Intent(this, TripService::class.java)
        startForegroundService(intent)
        if (!bound) bound = bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSIONS_REQUEST) return
        startTrip()
    }

    /** Locks theme/profile editing while the car says setup or typing is not allowed (moving). */
    private fun watchDrivingRestrictions() {
        runCatching {
            val c = Car.createCar(this)
            car = c
            val mgr = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            val apply = { r: CarUxRestrictions? ->
                val blocked = r != null && (r.activeRestrictions and
                    (CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP or CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD)) != 0
                runOnUiThread { panel.setRestricted(blocked) }
            }
            mgr.registerListener { r -> apply(r) }
            apply(mgr.currentCarUxRestrictions)
        }.onFailure { Log.w(TAG, "ux restrictions", it) }
    }

    private fun applySettings() {
        home.setDemoPanel(settings.demoPanelOn)
        home.setFamily(service?.familyMode ?: false, profiles.load().map { it.name })
    }

    override fun onSettings() = onSettingsShown(first = false)

    private fun onSettingsShown(first: Boolean) {
        panel.showWelcome(first)
        panel.visibility = View.VISIBLE
        home.visibility = View.INVISIBLE   // stops the animated icon and graph from drawing underneath
        service?.detach(screen)            // questions arriving meanwhile go to cards, not a hidden screen
    }

    private fun closeSettings() {
        settings.onboarded = true
        panel.visibility = View.GONE
        home.visibility = View.VISIBLE
        applySettings()
        if (resumed) service?.attach(screen)
    }

    @Deprecated("Activity back handling")
    override fun onBackPressed() {
        if (panel.visibility == View.VISIBLE) closeSettings() else @Suppress("DEPRECATION") super.onBackPressed()
    }

    override fun onFamilyToggle() { service?.toggleFamily() }
    override fun onReportSick() { service?.reportSick() }
    override fun onScenario(file: String) { service?.startScenario(file) }
    override fun onLive() { service?.startLive() }
    override fun onStopTrip() { service?.stopTrip() }
    override fun onCompactChanged(compact: Boolean) = reportRegion()

    /**
     * In the one-pane layout beside the navigation, tell it which screen region we cover so its
     * map keeps the car in the visible part; clear it when we leave that layout or the screen.
     */
    private fun reportRegion() {
        val s = service ?: return
        val r = Rect()
        if (resumed && home.isCompact && home.isAttachedToWindow) {
            val loc = IntArray(2)
            home.getLocationOnScreen(loc)
            r.set(loc[0], loc[1], loc[0] + home.width, loc[1] + home.height)
        }
        if (r == region) return
        region = r
        s.reportOccupiedRegion(r)
    }
}

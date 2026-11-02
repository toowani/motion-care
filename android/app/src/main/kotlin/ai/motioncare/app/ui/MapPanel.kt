package ai.motioncare.app.ui

import ai.motioncare.app.theme.Mood
import ai.motioncare.app.theme.MoodIconView
import ai.motioncare.app.theme.MoodTheme
import ai.motioncare.app.ui.Ui.dp
import ai.motioncare.app.ui.Ui.dpf
import ai.motioncare.app.ui.Ui.gone
import ai.motioncare.app.ui.Ui.text
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import java.util.Locale

/**
 * Our own map screen (prototype). Apps cannot draw over the Pleos navigation, so we show a
 * Leaflet/OSM map ourselves and float the motion-sickness character on it the way navigation
 * apps float a speed-camera badge. The map is a WebView; the badge and turn pill are native
 * views so they keep animating even while tiles load.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class MapPanel(context: Context) : FrameLayout(context) {
    private val web = WebView(context)
    private val icon = MoodIconView(context)
    private val moodLabel = context.text("편안해요", 16f, Ui.INK, bold = true).apply { gravity = Gravity.CENTER }
    private val turnPill = context.text("", 17f, Color.WHITE, bold = true)
    private val fallback = context.text("지도를 불러오지 못했어요.\n네트워크 연결을 확인해 주세요.", 18f, Ui.MUTED)

    private val main = Handler(Looper.getMainLooper())
    private var destroyed = false
    private var ready = false
    /** Only the newest fix matters; older ones queued before load would just replay the trail. */
    private var pending: Triple<Double, Double, Double>? = null

    init {
        setBackgroundColor(Ui.BG)
        web.setBackgroundColor(Ui.BG)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                // the inline page itself always "loads"; Leaflet missing means unpkg was unreachable
                if (destroyed) return
                view.evaluateJavascript("typeof L") { type ->
                    if (destroyed) return@evaluateJavascript
                    if (type == null || type.contains("undefined")) { showFallback(); return@evaluateJavascript }
                    ready = true
                    pending?.let { (lat, lon, b) -> pending = null; setPosition(lat, lon, b) }
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showFallback()
            }
        }
        addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        fallback.gravity = Gravity.CENTER
        fallback.gone(true)
        addView(fallback, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // speed-camera style badge: white rounded card, top-right, character + one-line label
        val badge = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = Ui.rounded(Ui.CARD, context.dpf(24f), context.dp(2), Ui.LINE)
            elevation = context.dpf(6f)
            setPadding(context.dp(12), context.dp(12), context.dp(12), context.dp(10))
            addView(icon, LinearLayout.LayoutParams(context.dp(140), context.dp(140)))
            moodLabel.maxLines = 1
            addView(moodLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(4) })
        }
        addView(badge, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            setMargins(0, context.dp(16), context.dp(16), 0)
        })

        turnPill.apply {
            maxLines = 1
            background = Ui.rounded(Ui.INK, context.dpf(24f))
            elevation = context.dpf(4f)
            setPadding(context.dp(18), context.dp(10), context.dp(18), context.dp(10))
            gone(true)
        }
        // bottom margin keeps the pill clear of the Leaflet attribution line (bottom-right)
        addView(turnPill, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.START).apply {
            setMargins(context.dp(16), 0, 0, context.dp(24))
        })

        web.loadDataWithBaseURL("https://motioncare.local/", HTML, "text/html", "utf-8", null)
    }

    /** Moves the car marker, appends to the trail and pans the map to follow. */
    fun setPosition(lat: Double, lon: Double, bearing: Double) = onMain {
        if (!lat.isFinite() || !lon.isFinite()) return@onMain
        val b = if (bearing.isFinite()) bearing else 0.0
        if (!ready) { pending = Triple(lat, lon, b); return@onMain }
        web.evaluateJavascript(String.format(Locale.US, "setPos(%.7f,%.7f,%.1f)", lat, lon, b), null)
    }

    fun setMood(theme: MoodTheme, mood: Mood, level: Float, label: String) = onMain {
        icon.theme = theme
        icon.mood = mood
        icon.level = level
        moodLabel.text = label
    }

    /** Next turn hint ("300m 앞 좌회전"); null hides the pill. */
    fun setNextTurn(text: String?) = onMain {
        turnPill.text = text ?: ""
        turnPill.gone(text.isNullOrBlank())
    }

    /** Clears the trail, e.g. when a new trip starts. */
    fun reset() = onMain {
        pending = null
        if (ready) web.evaluateJavascript("resetTrail()", null)
    }

    /** Releases the WebView; call from the host's onDestroy. The panel is unusable afterwards. */
    fun destroy() = onMain {
        destroyed = true
        ready = false
        pending = null
        main.removeCallbacksAndMessages(null)
        web.stopLoading()
        removeView(web)
        web.destroy()
    }

    /** Sensor/location SDK callbacks may arrive off the main thread; WebView and views are main-only. */
    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) { if (!destroyed) block() }
        else main.post { if (!destroyed) block() }
    }

    private fun showFallback() {
        ready = false
        web.gone(true)
        fallback.gone(false)
    }

    private companion object {
        val HTML = """
<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
html,body,#map{margin:0;height:100%;background:#F4F6FA}
.car{width:28px;height:28px;border-radius:50%;background:#0F766E;border:4px solid #fff;
 box-shadow:0 2px 8px rgba(0,0,0,.35);box-sizing:border-box;position:relative}
.car:after{content:'';position:absolute;left:50%;top:-14px;margin-left:-7px;
 border-left:7px solid transparent;border-right:7px solid transparent;border-bottom:11px solid #0F766E}
</style></head><body><div id="map"></div>
<script>
var map=L.map('map',{zoomControl:false,attributionControl:true}).setView([37.5665,126.978],16);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,
 attribution:'&copy; OpenStreetMap contributors'}).addTo(map);
var car=L.marker([37.5665,126.978],{icon:L.divIcon({className:'',html:'<div class="car"></div>',
 iconSize:[28,28],iconAnchor:[14,14]}),interactive:false});
var trail=L.layerGroup().addTo(map),pts=[],MAX=120,CHUNKS=6,placed=false;
function drawTrail(){
 trail.clearLayers();
 var n=Math.ceil(MAX/CHUNKS);
 for(var i=0;i*n<pts.length-1;i++){
  var seg=pts.slice(i*n,Math.min(pts.length,(i+1)*n+1));
  var age=(pts.length-1-i*n)/MAX; // older chunks fade out
  L.polyline(seg,{color:'#0F766E',weight:6,opacity:Math.max(0.1,0.85*(1-age)),interactive:false}).addTo(trail);
 }
}
function setPos(lat,lon,b){
 var p=[lat,lon];
 pts.push(p); if(pts.length>MAX) pts.shift();
 car.setLatLng(p);
 if(!placed){car.addTo(map);placed=true;map.setView(p,16,{animate:false});}
 else map.panTo(p,{animate:true,duration:0.8,easeLinearity:0.5});
 var el=car.getElement(); if(el) el.firstChild.style.transform='rotate('+b+'deg)';
 drawTrail();
}
function resetTrail(){pts=[];trail.clearLayers();}
</script></body></html>
""".trimIndent()
    }
}

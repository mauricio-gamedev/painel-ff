package com.ffaimlab.a06

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * User-enabled quick overlay for reading and copying sensitivity profiles.
 * This service never injects code, reads game memory, or synthesizes touch input.
 */
class FloatingAimService : Service() {
    companion object {
        const val ACTION_START = "com.ffaimlab.a06.action.START_FLOATING"
        const val ACTION_STOP = "com.ffaimlab.a06.action.STOP_FLOATING"
        const val EXTRA_PROFILE_JSON = "profile_json"
        const val PREFS = "yoru_overlay_state"
        private const val CHANNEL_ID = "yoru_floating_panel"
        private const val NOTIFICATION_ID = 7153
    }

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelStatus: TextView? = null
    private var panelValues: TextView? = null
    private var active = "balanced"
    private var values = defaultValues("balanced")

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Painel flutuante Yoru", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Notificação enquanto o usuário mantém ativo o painel flutuante."
                    setShowBadge(false)
                }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPanel()
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startNotification()
        intent?.getStringExtra(EXTRA_PROFILE_JSON)?.let { readIncomingProfile(it) }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", true).apply()
        if (bubble == null && panel == null) showBubble() else refreshPanel()
        return START_NOT_STICKY
    }

    private fun defaultValues(profile: String): MutableMap<String, Int> = when (profile) {
        "capa" -> mutableMapOf("general" to 172, "red" to 164, "x2" to 151, "x4" to 133, "awm" to 102, "look" to 40, "fire" to 65)
        "precision" -> mutableMapOf("general" to 158, "red" to 151, "x2" to 142, "x4" to 126, "awm" to 108, "look" to 34, "fire" to 62)
        else -> mutableMapOf("general" to 166, "red" to 158, "x2" to 148, "x4" to 132, "awm" to 105, "look" to 38, "fire" to 66)
    }

    private fun readIncomingProfile(json: String) {
        try {
            val root = JSONObject(json)
            active = root.optString("active", "custom")
            val v = root.optJSONObject("values")
            if (v != null) {
                values = defaultValues(active)
                listOf("general", "red", "x2", "x4", "awm", "look", "fire").forEach { key ->
                    if (v.has(key)) values[key] = v.optInt(key, values[key] ?: 0).coerceIn(0, if (key == "fire") 100 else 200)
                }
            } else {
                active = "balanced"
                values = defaultValues(active)
            }
            saveValues()
        } catch (_: Exception) {
            loadValues()
        }
    }

    private fun saveValues() {
        val e = getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("active", active)
        values.forEach { (key, value) -> e.putInt("v_" + key, value) }
        e.putBoolean("running", bubble != null || panel != null).apply()
    }

    private fun loadValues() {
        val p = getSharedPreferences(PREFS, MODE_PRIVATE)
        active = p.getString("active", active) ?: active
        values.keys.toList().forEach { key -> values[key] = p.getInt("v_" + key, values[key] ?: 0) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, stroke: Int = 0, radius: Int = 14): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    @Suppress("DEPRECATION")
    private fun params(width: Int, height: Int, x: Int, y: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        return WindowManager.LayoutParams(width, height, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    private fun safeAdd(view: View, lp: WindowManager.LayoutParams) {
        try { wm.addView(view, lp) } catch (_: Exception) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", false).apply()
        }
    }

    private fun showBubble() {
        if (bubble != null) return
        loadValues()
        val v = label("夜", 23f, Color.rgb(255, 79, 145), true).apply {
            gravity = Gravity.CENTER
            contentDescription = "Abrir mini painel Yoru Aim Lab"
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(46, 23, 55), Color.rgb(20, 15, 32))).apply {
                cornerRadius = dp(19).toFloat()
                setStroke(dp(1), Color.rgb(255, 79, 145))
            }
            elevation = dp(8).toFloat()
        }
        val lp = params(dp(54), dp(54), dp(14), dp(210))
        bubble = v
        bubbleParams = lp
        v.setOnTouchListener(BubbleDragListener())
        safeAdd(v, lp)
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", true).apply()
    }

    private inner class BubbleDragListener : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var rawX = 0f
        private var rawY = 0f
        private var moved = false
        private val slop = ViewConfiguration.get(this@FloatingAimService).scaledTouchSlop

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val lp = bubbleParams ?: return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y
                    rawX = event.rawX; rawY = event.rawY
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - rawX).toInt()
                    val dy = (event.rawY - rawY).toInt()
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) {
                        lp.x = (startX + dx).coerceIn(0, max(0, resources.displayMetrics.widthPixels - view.width))
                        lp.y = (startY + dy).coerceIn(0, max(0, resources.displayMetrics.heightPixels - view.height))
                        try { wm.updateViewLayout(view, lp) } catch (_: Exception) {}
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) openPanel()
                    return true
                }
            }
            return false
        }
    }

    private fun openPanel() {
        if (panel != null) return
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble = null; bubbleParams = null
        loadValues()
        val width = min(dp(326), resources.displayMetrics.widthPixels - dp(16)).coerceAtLeast(dp(250))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(13))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(28, 17, 40), Color.rgb(13, 11, 23))).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), Color.rgb(113, 65, 124))
            }
            elevation = dp(12).toFloat()
        }
        panel = root
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(label("夜  YORU QUICK PANEL", 12f, Color.rgb(255, 79, 145), true))
        titles.addView(label("CALIBRATION · A06", 9f, Color.rgb(173, 161, 189), true))
        header.addView(titles, LinearLayout.LayoutParams(0, dp(42), 1f))
        header.addView(button("⌄", false) { minimizePanel() }, LinearLayout.LayoutParams(dp(39), dp(36)))
        root.addView(header)
        root.addView(label("Perfis rápidos", 11f, Color.rgb(173, 161, 189), true).apply { setPadding(0, dp(6), 0, dp(5)) })
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(button("🌙 Equilibrado", false) { choose("balanced") }, LinearLayout.LayoutParams(0, dp(39), 1f).apply { rightMargin = dp(4) })
        row1.addView(button("⚡ Capa", false) { choose("capa") }, LinearLayout.LayoutParams(0, dp(39), 1f).apply { leftMargin = dp(4) })
        root.addView(row1)
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(7), 0, 0) }
        row2.addView(button("✧ Precisão", false) { choose("precision") }, LinearLayout.LayoutParams(0, dp(39), 1f).apply { rightMargin = dp(4) })
        row2.addView(button("▣ Copiar ajustes", true) { copyProfile() }, LinearLayout.LayoutParams(0, dp(39), 1f).apply { leftMargin = dp(4) })
        root.addView(row2)
        panelStatus = label("Perfil: " + profileName(), 11f, Color.rgb(255, 181, 217), true).apply { setPadding(0, dp(11), 0, dp(4)) }
        panelValues = label(summary(), 12f, Color.rgb(232, 223, 245), true).apply { setPadding(0, 0, 0, dp(6)) }
        root.addView(panelStatus); root.addView(panelValues)
        root.addView(label("Mostra/copia valores. Não altera a mira do jogo.", 9f, Color.rgb(173, 161, 189)))
        val footer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, 0) }
        footer.addView(button("Recolher", false) { minimizePanel() }, LinearLayout.LayoutParams(0, dp(36), 1f).apply { rightMargin = dp(4) })
        footer.addView(button("Parar painel", false) { stopPanel() }, LinearLayout.LayoutParams(0, dp(36), 1f).apply { leftMargin = dp(4) })
        root.addView(footer)
        val params = params(width, WindowManager.LayoutParams.WRAP_CONTENT, dp(8), dp(135))
        safeAdd(root, params)
        saveValues()
    }

    private fun button(text: String, primary: Boolean, action: () -> Unit): TextView =
        label(text, 10f, if (primary) Color.rgb(25, 9, 24) else Color.rgb(246, 238, 252), true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            background = if (primary) rounded(Color.rgb(255, 79, 145), 0, 10)
                else rounded(Color.rgb(36, 27, 49), Color.rgb(76, 53, 93), 10)
            setOnClickListener { action() }
        }

    private fun choose(name: String) {
        active = name
        values = defaultValues(name)
        saveValues()
        refreshPanel()
    }

    private fun profileName(): String = when (active) {
        "capa" -> "Capa suave"
        "precision" -> "Precisão"
        "custom" -> "Personalizado"
        else -> "Equilibrado"
    }

    private fun summary(): String = "Geral " + values["general"] + "  ·  Vermelho " + values["red"] +
        "\n2× " + values["x2"] + "  ·  4× " + values["x4"] + "  ·  Tiro " + values["fire"] + "%"

    private fun refreshPanel() {
        panelStatus?.text = "Perfil: " + profileName()
        panelValues?.text = summary()
    }

    private fun copyProfile() {
        val data = "YORU AIM LAB · Galaxy A06\nPerfil: " + profileName() +
            "\nGeral: " + values["general"] + "; Ponto vermelho: " + values["red"] +
            "; Mira 2×: " + values["x2"] + "; Mira 4×: " + values["x4"] +
            "; AWM: " + values["awm"] + "; Olhadinha: " + values["look"] +
            "; Botão de tiro: " + values["fire"] + "%\nAplicar manualmente nas configurações do jogo."
        try {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Yoru Aim Lab", data))
            Toast.makeText(this, "Perfil copiado para aplicação manual.", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Falha ao copiar perfil.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun minimizePanel() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null; panelStatus = null; panelValues = null
        showBubble()
    }

    @Suppress("DEPRECATION")
    private fun startNotification() {
        val openApp = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        val notification = builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Yoru Aim Lab está ativo")
            .setContentText("Painel flutuante de perfis. Toque para voltar ao app e parar.")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopPanel() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null; bubble = null; bubbleParams = null; panelStatus = null; panelValues = null
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", false).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE) else stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null; bubble = null
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", false).apply()
        super.onDestroy()
    }
}
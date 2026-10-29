package com.ffaimlab.a06

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    @Volatile
    private var pendingExportJson: String? = null

    companion object {
        private const val REQUEST_IMPORT_PROFILE = 5101
        private const val REQUEST_EXPORT_PROFILE = 5102
        private const val FREE_FIRE_PACKAGE = "com.dts.freefireth"
        private const val FREE_FIRE_MAX_PACKAGE = "com.dts.freefiremax"
    }

    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 12, 15)
        window.navigationBarColor = android.graphics.Color.rgb(11, 12, 15)

        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(11, 12, 15))
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return true
                    // Keep navigation local; never expose the bridge to arbitrary web pages.
                    return !url.startsWith("file:///android_asset/")
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = filePathCallback
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json", "text/plain", "application/octet-stream"))
                    }
                    return try {
                        startActivityForResult(intent, REQUEST_IMPORT_PROFILE)
                        true
                    } catch (_: Exception) {
                        this@MainActivity.filePathCallback?.onReceiveValue(null)
                        this@MainActivity.filePathCallback = null
                        false
                    }
                }
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
            }
            addJavascriptInterface(AndroidBridge(), "AndroidBridge")
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.postDelayed({
                if (!isFinishing && ::webView.isInitialized) {
                    webView.evaluateJavascript("window.refreshOverlayStatus && window.refreshOverlayStatus()", null)
                }
            }, 350)
        }
    }

    private fun readBatteryPercent(): Int {
        return try {
            val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
            val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) level * 100 / scale else -1
        } catch (_: Exception) { -1 }
    }

    private fun thermalLabel(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "API indisponível"
        val power = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return "Indisponível"
        return when (power.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "Normal"
            PowerManager.THERMAL_STATUS_LIGHT -> "Leve"
            PowerManager.THERMAL_STATUS_MODERATE -> "Moderado"
            PowerManager.THERMAL_STATUS_SEVERE -> "Alto"
            PowerManager.THERMAL_STATUS_CRITICAL -> "Crítico"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergência"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "Desligamento"
            else -> "Indisponível"
        }
    }

    private fun openSetting(key: String): String {
        val intent = when (key.lowercase(java.util.Locale.ROOT)) {
            "battery" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "notifications" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            }
            "freefire" -> {
                val target = when {
                    isLaunchable("com.dts.freefireth") -> "com.dts.freefireth"
                    isLaunchable("com.dts.freefiremax") -> "com.dts.freefiremax"
                    else -> null
                } ?: return JSONObject().put("ok", false).put("message", "Nenhuma edição do Free Fire foi encontrada.").toString()
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$target"))
            }
            else -> return JSONObject().put("ok", false).put("message", "Atalho não reconhecido.").toString()
        }
        return try {
            runOnUiThread {
                try { startActivity(intent) }
                catch (_: Exception) { Toast.makeText(this, "Configuração não disponível neste aparelho.", Toast.LENGTH_SHORT).show() }
            }
            JSONObject().put("ok", true).put("message", "Abrindo configurações do Android.").toString()
        } catch (_: Exception) {
            JSONObject().put("ok", false).put("message", "Não foi possível abrir esta configuração.").toString()
        }
    }

    private fun isLaunchable(packageName: String): Boolean {
        return try {
            packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (_: Exception) {
            false
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun getIntegrationStatus(): String {
            val memoryInfo = ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
            val totalRam = String.format(Locale.US, "%.1f GB", memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0))
            return JSONObject()
                .put("model", (Build.MANUFACTURER + " " + Build.MODEL).trim())
                .put("manufacturer", Build.MANUFACTURER)
                .put("androidRelease", Build.VERSION.RELEASE ?: "Android")
                .put("sdkInt", Build.VERSION.SDK_INT)
                 .put("totalRam", totalRam)
                .put("totalRamBytes", memoryInfo.totalMem)
                .put("freeFireNormal", isLaunchable(FREE_FIRE_PACKAGE))
                .put("freeFireMax", isLaunchable(FREE_FIRE_MAX_PACKAGE))
                .toString()
        }

        @JavascriptInterface
        fun getSystemStatus(): String {
            val memoryInfo = ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
            val storageFree = try { StatFs(Environment.getDataDirectory().absolutePath).availableBytes } catch (_: Exception) { -1L }
            return JSONObject()
                .put("totalRamBytes", memoryInfo.totalMem)
                .put("availableRam", memoryInfo.availMem)
                .put("lowMemory", memoryInfo.lowMemory)
                .put("batteryLevel", readBatteryPercent())
                .put("thermalLabel", thermalLabel())
                .put("storageFreeBytes", storageFree)
                .toString()
        }

        @JavascriptInterface
        fun copyText(text: String): Boolean {
            if (text.length > 20_000) return false
            return try {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Yoru Aim Lab", text))
                runOnUiThread { Toast.makeText(this@MainActivity, "Perfil copiado.", Toast.LENGTH_SHORT).show() }
                true
            } catch (_: Exception) { false }
        }

        @JavascriptInterface
        fun openSystemSetting(key: String): String = openSetting(key)

        @JavascriptInterface
        fun getOverlayStatus(): String {
            val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this@MainActivity)
            val running = getSharedPreferences(FloatingAimService.PREFS, MODE_PRIVATE).getBoolean("running", false)
            return JSONObject().put("permission", allowed).put("running", running).toString()
        }

        @JavascriptInterface
        fun startFloatingPanel(profileJson: String): String {
            if (profileJson.isBlank() || profileJson.length > 200_000) {
                return JSONObject().put("ok", false).put("message", "Os dados do perfil são inválidos.").toString()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                runOnUiThread {
                    try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
                    catch (_: Exception) { Toast.makeText(this@MainActivity, "Abra as permissões de sobreposição nas configurações.", Toast.LENGTH_LONG).show() }
                }
                return JSONObject().put("ok", false).put("needsPermission", true).put("message", "Conceda Exibir sobre outros apps e toque em Ativar novamente.").toString()
            }
            runOnUiThread {
                try {
                    val serviceIntent = Intent(this@MainActivity, FloatingAimService::class.java)
                        .setAction(FloatingAimService.ACTION_START)
                        .putExtra(FloatingAimService.EXTRA_PROFILE_JSON, profileJson)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "Não foi possível iniciar o painel flutuante.", Toast.LENGTH_LONG).show()
                }
            }
            return JSONObject().put("ok", true).put("message", "Solicitação de painel flutuante enviada.").toString()
        }

        @JavascriptInterface
        fun stopFloatingPanel(): Boolean {
            runOnUiThread {
                try { startService(Intent(this@MainActivity, FloatingAimService::class.java).setAction(FloatingAimService.ACTION_STOP)) }
                catch (_: Exception) { stopService(Intent(this@MainActivity, FloatingAimService::class.java)) }
            }
            return true
        }

        @JavascriptInterface
        fun launchGame(edition: String): String {
            val packageName = when (edition.lowercase(Locale.ROOT)) {
                "normal" -> FREE_FIRE_PACKAGE
                "max" -> FREE_FIRE_MAX_PACKAGE
                else -> return JSONObject().put("ok", false).put("message", "Edição não reconhecida.").toString()
            }
            val intent = packageManager.getLaunchIntentForPackage(packageName)
                ?: return JSONObject().put("ok", false).put("message", "Esta edição não foi encontrada no aparelho.").toString()

            runOnUiThread {
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "Não foi possível abrir o jogo.", Toast.LENGTH_SHORT).show()
                }
            }
            return JSONObject().put("ok", true).put("message", "Solicitação enviada para abrir o jogo.").toString()
        }

        @JavascriptInterface
        fun exportProfile(json: String): Boolean {
            if (json.isBlank() || json.length > 200_000) return false
            pendingExportJson = json
            runOnUiThread {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "ff-aim-lab-a06-perfil.json")
                }
                try {
                    startActivityForResult(intent, REQUEST_EXPORT_PROFILE)
                } catch (_: Exception) {
                    pendingExportJson = null
                    Toast.makeText(this@MainActivity, "Não foi possível abrir o salvamento de arquivo.", Toast.LENGTH_LONG).show()
                }
            }
            return true
        }
    }

    @Deprecated("Retained to support the Android document picker on the current minimum SDK")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMPORT_PROFILE -> {
                val result = if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null
                filePathCallback?.onReceiveValue(result)
                filePathCallback = null
            }
            REQUEST_EXPORT_PROFILE -> {
                val json = pendingExportJson
                pendingExportJson = null
                val uri = if (resultCode == RESULT_OK) data?.data else null
                if (uri != null && json != null) {
                    try {
                        val output = contentResolver.openOutputStream(uri, "wt")
                            ?: throw IllegalStateException("Não foi possível abrir o arquivo de destino")
                        output.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        Toast.makeText(this, "Perfil exportado com sucesso.", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(this, "Falha ao salvar o perfil.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Android API; back navigation is retained for compatibility")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
            webView.removeJavascriptInterface("AndroidBridge")
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
package com.jarvis.mobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jarvis.mobile.R
import com.jarvis.mobile.config.ConfigStore
import com.jarvis.mobile.live.LiveSession
import com.jarvis.mobile.service.JarvisService

class MainActivity : AppCompatActivity(), LiveSession.Callbacks {

    private lateinit var hud: HudView
    private lateinit var logView: TextView
    private lateinit var input: EditText
    private lateinit var sendBtn: Button
    private lateinit var muteBtn: Button
    private lateinit var bottomPanel: LinearLayout
    private val logBuffer = StringBuilder()
    private var session: LiveSession? = null

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val mic = granted[Manifest.permission.RECORD_AUDIO] == true
        if (mic) startSession()
        else Toast.makeText(this, "Mikrofon izni gerekli", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ConfigStore.isConfigured(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        hud         = findViewById(R.id.hud)
        logView     = findViewById(R.id.logView)
        input       = findViewById(R.id.input)
        sendBtn     = findViewById(R.id.sendBtn)
        muteBtn     = findViewById(R.id.muteBtn)
        bottomPanel = findViewById(R.id.bottomPanel)
        logView.movementMethod = ScrollingMovementMethod()
        playEntryAnimation()

        sendBtn.setOnClickListener {
            val t = input.text.toString().trim()
            if (t.isNotEmpty()) {
                session?.sendText(t)
                input.text.clear()
            }
        }
        muteBtn.setOnClickListener {
            session?.toggleMute()
            muteBtn.text = if (session?.isMuted() == true) "🔇 SESI AC" else "🎙 SESI KAPAT"
        }
        findViewById<Button>(R.id.settingsBtn).setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        ensurePermissionsAndStart()
    }

    private fun ensurePermissionsAndStart() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startSession()
        else permLauncher.launch(missing.toTypedArray())
    }

    private fun startSession() {
        val svc = Intent(this, JarvisService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc)
        else startService(svc)

        session = LiveSession(applicationContext, this).also { it.start() }
    }

    override fun onState(state: LiveSession.State) {
        runOnUiThread { hud.state = state }
    }

    override fun onLog(line: String) {
        runOnUiThread {
            logBuffer.append(line).append('\n')
            // keep last ~120 lines
            val text = logBuffer.toString()
            val lines = text.split('\n')
            if (lines.size > 120) {
                logBuffer.clear()
                logBuffer.append(lines.takeLast(120).joinToString("\n"))
            }
            logView.text = logBuffer.toString()
            logView.scrollTo(0, Int.MAX_VALUE)
        }
    }

    private fun playEntryAnimation() {
        // Bottom panel: hidden, slides up after the HUD boot intro finishes (~3s).
        bottomPanel.alpha = 0f
        bottomPanel.translationY = 800f
        bottomPanel.animate()
            .translationY(0f).alpha(1f)
            .setStartDelay(2700)
            .setDuration(620)
            .setInterpolator(DecelerateInterpolator(1.6f))
            .start()

        // Send button: pop in with overshoot
        sendBtn.scaleX = 0f; sendBtn.scaleY = 0f
        sendBtn.animate()
            .scaleX(1f).scaleY(1f)
            .setStartDelay(3100)
            .setDuration(380)
            .setInterpolator(OvershootInterpolator(2.4f))
            .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.shutdown()
        stopService(Intent(this, JarvisService::class.java))
    }
}

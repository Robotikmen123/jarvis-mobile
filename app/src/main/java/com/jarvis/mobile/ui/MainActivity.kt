package com.jarvis.mobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private lateinit var chatScroll: ScrollView
    private lateinit var chatList: LinearLayout
    private lateinit var statusView: TextView
    private lateinit var input: EditText
    private lateinit var sendBtn: Button
    private lateinit var muteBtn: Button
    private lateinit var bottomPanel: LinearLayout
    private var session: LiveSession? = null

    // Live (still streaming) bubbles per speaker
    private var liveUserBubble: TextView? = null
    private var liveJarvisBubble: TextView? = null

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
        chatScroll  = findViewById(R.id.chatScroll)
        chatList    = findViewById(R.id.chatList)
        statusView  = findViewById(R.id.statusView)
        input       = findViewById(R.id.input)
        sendBtn     = findViewById(R.id.sendBtn)
        muteBtn     = findViewById(R.id.muteBtn)
        bottomPanel = findViewById(R.id.bottomPanel)
        playEntryAnimation()

        sendBtn.setOnClickListener {
            val t = input.text.toString().trim()
            if (t.isNotEmpty()) {
                session?.sendText(t)
                input.text.clear()
            }
        }
        input.setOnEditorActionListener { _, _, _ -> sendBtn.performClick(); true }
        muteBtn.setOnClickListener {
            session?.toggleMute()
            muteBtn.text = if (session?.isMuted() == true) "🔇  SES KAPALI" else "🎙  MIC"
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
        // Status strip: show only the latest one-liner. Strip any prefix tags.
        runOnUiThread { statusView.text = line }
    }

    override fun onTranscript(speaker: LiveSession.Speaker, text: String, partial: Boolean) {
        runOnUiThread {
            when (speaker) {
                LiveSession.Speaker.USER -> {
                    val v = liveUserBubble ?: addBubble(speaker).also { liveUserBubble = it }
                    v.text = text
                    if (!partial) liveUserBubble = null
                }
                LiveSession.Speaker.JARVIS -> {
                    val v = liveJarvisBubble ?: addBubble(speaker).also { liveJarvisBubble = it }
                    v.text = text
                    if (!partial) liveJarvisBubble = null
                }
            }
            chatScroll.post { chatScroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun addBubble(speaker: LiveSession.Speaker): TextView {
        val tv = TextView(this)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(6)
            when (speaker) {
                LiveSession.Speaker.USER -> {
                    gravity = Gravity.END
                    leftMargin = dp(56)
                }
                LiveSession.Speaker.JARVIS -> {
                    gravity = Gravity.START
                    rightMargin = dp(56)
                }
            }
        }
        tv.layoutParams = lp
        tv.setPadding(dp(12), dp(8), dp(12), dp(8))
        tv.maxWidth = (resources.displayMetrics.widthPixels * 0.78f).toInt()
        tv.textSize = 13.5f
        tv.typeface = Typeface.MONOSPACE
        tv.setLineSpacing(0f, 1.15f)
        when (speaker) {
            LiveSession.Speaker.USER -> {
                tv.setBackgroundResource(R.drawable.bg_msg_user)
                tv.setTextColor(Color.parseColor("#E6F8FF"))
            }
            LiveSession.Speaker.JARVIS -> {
                tv.setBackgroundResource(R.drawable.bg_msg_jarvis)
                tv.setTextColor(Color.parseColor("#A8EBFF"))
            }
        }
        chatList.addView(tv)
        // Subtle pop-in
        tv.alpha = 0f
        tv.translationY = dp(8).toFloat()
        tv.animate().alpha(1f).translationY(0f).setDuration(220).start()
        return tv
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun playEntryAnimation() {
        bottomPanel.alpha = 0f
        bottomPanel.translationY = 800f
        bottomPanel.animate()
            .translationY(0f).alpha(1f)
            .setStartDelay(2700)
            .setDuration(620)
            .setInterpolator(DecelerateInterpolator(1.6f))
            .start()

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

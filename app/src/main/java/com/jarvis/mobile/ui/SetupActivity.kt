package com.jarvis.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.mobile.R
import com.jarvis.mobile.config.ConfigStore

class SetupActivity : AppCompatActivity() {

    private val voices = listOf(
        "Puck", "Charon", "Kore", "Fenrir", "Aoede", "Leda", "Orus", "Zephyr"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val etGemini    = findViewById<EditText>(R.id.etGemini)
        val etAnthropic = findViewById<EditText>(R.id.etAnthropic)
        val spVoice     = findViewById<Spinner>(R.id.spVoice)
        val btnSave     = findViewById<Button>(R.id.btnSave)

        etGemini.setText(ConfigStore.geminiKey(this).orEmpty())
        etAnthropic.setText(ConfigStore.anthropicKey(this).orEmpty())

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, voices).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spVoice.adapter = adapter
        spVoice.setSelection(voices.indexOf(ConfigStore.voiceName(this)).coerceAtLeast(0))

        btnSave.setOnClickListener {
            val key = etGemini.text.toString().trim()
            if (key.isEmpty()) { etGemini.error = "Gerekli"; return@setOnClickListener }
            ConfigStore.setGeminiKey(this, key)
            ConfigStore.setAnthropicKey(this, etAnthropic.text.toString())
            ConfigStore.setVoiceName(this, spVoice.selectedItem as String)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

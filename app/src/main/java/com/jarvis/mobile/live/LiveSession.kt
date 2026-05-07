package com.jarvis.mobile.live

import android.content.Context
import android.util.Log
import com.jarvis.mobile.config.ConfigStore
import com.jarvis.mobile.memory.MemoryStore
import com.jarvis.mobile.tools.ShutdownTool
import com.jarvis.mobile.tools.ToolDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LiveSession(
    private val ctx: Context,
    private val callbacks: Callbacks,
) : GeminiLiveClient.Listener {

    interface Callbacks {
        fun onState(state: State)
        fun onLog(line: String)
        /** Streamed conversation entries. partial=true means "still being filled in". */
        fun onTranscript(speaker: Speaker, text: String, partial: Boolean)
    }

    enum class State { IDLE, CONNECTING, LISTENING, THINKING, SPEAKING, MUTED, ERROR }
    enum class Speaker { USER, JARVIS }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var client: GeminiLiveClient? = null
    private var audio:  AudioIO? = null
    private var welcomeSent = false
    private var reconnectJob: Job? = null
    private val userBuf = StringBuilder()
    private val jarvisBuf = StringBuilder()

    fun start() {
        callbacks.onState(State.CONNECTING)
        callbacks.onLog("SYS: Baglaniliyor...")
        connect()
    }

    fun stop() {
        reconnectJob?.cancel()
        client?.close()
        audio?.stop()
        client = null
        audio  = null
        callbacks.onState(State.IDLE)
    }

    fun toggleMute() {
        val a = audio ?: return
        a.muted = !a.muted
        callbacks.onState(if (a.muted) State.MUTED else State.LISTENING)
        callbacks.onLog(if (a.muted) "SYS: Mikrofon kapali." else "SYS: Mikrofon acik.")
    }

    fun isMuted(): Boolean = audio?.muted == true

    fun sendText(text: String) {
        callbacks.onTranscript(Speaker.USER, text, partial = false)
        client?.sendUserText(text)
        callbacks.onState(State.THINKING)
    }

    /** Internal-only: send a system instruction to the model that should NOT show in chat. */
    private fun sendInternalInstruction(text: String) {
        client?.sendUserText(text)
    }

    private fun connect() {
        val key = ConfigStore.geminiKey(ctx) ?: run {
            callbacks.onState(State.ERROR)
            callbacks.onLog("ERR: Gemini API key yok.")
            return
        }
        val voice = ConfigStore.voiceName(ctx)

        val sysPrompt = buildSystemInstruction()
        val tools = ToolDispatcher.declarationsJson()

        val c = GeminiLiveClient(apiKey = key, voiceName = voice)
        client = c

        val a = AudioIO(onMicChunk = { chunk -> c.sendMicChunk(chunk) })
        audio = a

        c.connect(sysPrompt, tools, this)
    }

    private fun buildSystemInstruction(): String {
        val now = SimpleDateFormat("EEEE, MMMM d, yyyy — h:mm a", Locale.US).format(Date())
        val timeCtx = "[CURRENT DATE & TIME]\nRight now it is: $now\n" +
                      "Use this to calculate exact times for reminders.\n\n"
        val mem = MemoryStore.formatForPrompt(ctx)
        val core = runCatching {
            ctx.assets.open("prompt.txt").bufferedReader().use { it.readText() }
        }.getOrElse { "You are JARVIS, an efficient assistant. Use tools, no fluff." }
        return timeCtx + (if (mem.isNotEmpty()) mem + "\n" else "") + core
    }

    private suspend fun speakWelcomeIfNeeded() {
        if (welcomeSent) return
        welcomeSent = true
        delay(1500)
        runCatching {
            val msg = Welcome.build(ctx)
            callbacks.onLog("SYS: Welcome → $msg")
            val instr = "Asagidaki cumleyi aynen, hicbir ekleme yapmadan, sicak " +
                        "bir tonla seslendir:\n\"$msg\""
            sendInternalInstruction(instr)
        }
    }

    // ── GeminiLiveClient.Listener callbacks ──

    override fun onSetupComplete() {
        scope.launch {
            audio?.start()
            callbacks.onState(State.LISTENING)
            callbacks.onLog("SYS: JARVIS online.")
            speakWelcomeIfNeeded()
        }
    }

    override fun onAudioChunk(pcm: ByteArray) {
        audio?.enqueuePlayback(pcm)
        scope.launch {
            if (audio?.isJarvisSpeaking() == true) callbacks.onState(State.SPEAKING)
        }
    }

    override fun onInputTranscript(text: String) {
        scope.launch {
            userBuf.append(text)
            callbacks.onTranscript(Speaker.USER, userBuf.toString(), partial = true)
        }
    }

    override fun onOutputTranscript(text: String) {
        scope.launch {
            jarvisBuf.append(text)
            callbacks.onTranscript(Speaker.JARVIS, jarvisBuf.toString(), partial = true)
        }
    }

    override fun onTurnComplete() {
        scope.launch {
            if (userBuf.isNotEmpty()) {
                callbacks.onTranscript(Speaker.USER, userBuf.toString(), partial = false)
                userBuf.clear()
            }
            if (jarvisBuf.isNotEmpty()) {
                callbacks.onTranscript(Speaker.JARVIS, jarvisBuf.toString(), partial = false)
                jarvisBuf.clear()
            }
            audio?.endOfTurn()
            if (audio?.muted != true) callbacks.onState(State.LISTENING)
            if (ShutdownTool.requested) {
                ShutdownTool.requested = false
                stop()
            }
        }
    }

    override fun onToolCall(calls: List<GeminiLiveClient.ToolCall>) {
        scope.launch {
            callbacks.onState(State.THINKING)
            val results = mutableListOf<Pair<GeminiLiveClient.ToolCall, String>>()
            for (call in calls) {
                callbacks.onLog("TOOL: ${call.name} ${call.args}")
                val res = ToolDispatcher.dispatch(ctx, call.name, call.args)
                results += call to res
            }
            client?.sendToolResponses(results)
        }
    }

    override fun onError(reason: String) {
        scope.launch {
            callbacks.onState(State.ERROR)
            callbacks.onLog("ERR: $reason")
            scheduleReconnect()
        }
    }

    override fun onClosed() {
        scope.launch {
            callbacks.onLog("SYS: Baglanti kapandi.")
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            audio?.stop()
            audio = null
            client = null
            delay(3000)
            callbacks.onLog("SYS: Yeniden baglaniliyor...")
            connect()
        }
    }

    fun shutdown() {
        scope.cancel()
        stop()
    }

    companion object { private const val TAG = "LiveSession" }
}

package com.jarvis.mobile.live

import android.util.Base64
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
    private val apiKey: String,
    private val model: String  = "models/gemini-2.5-flash-native-audio-preview-12-2025",
    private val voiceName: String = "Puck",
) {
    interface Listener {
        fun onSetupComplete()
        fun onAudioChunk(pcm: ByteArray)
        fun onInputTranscript(text: String)
        fun onOutputTranscript(text: String)
        fun onTurnComplete()
        fun onToolCall(calls: List<ToolCall>)
        fun onError(reason: String)
        fun onClosed()
    }

    data class ToolCall(val id: String, val name: String, val args: Map<String, Any?>)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null
    private var listener: Listener? = null

    fun connect(systemInstruction: String, toolsBlock: JsonObject, listener: Listener) {
        this.listener = listener
        val url = "wss://generativelanguage.googleapis.com/ws/" +
                  "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val req = Request.Builder().url(url).build()
        ws = http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket open")
                sendSetup(webSocket, systemInstruction, toolsBlock)
            }
            override fun onMessage(webSocket: WebSocket, text: String) = handleJson(text)
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = handleJson(bytes.utf8())
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                listener.onError(t.message ?: "ws failure")
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $code $reason")
                listener.onClosed()
            }
        })
    }

    fun close() {
        ws?.close(1000, "client close")
        ws = null
    }

    fun sendMicChunk(pcm: ByteArray) {
        val b64 = Base64.encodeToString(pcm, Base64.NO_WRAP)
        val msg = JsonObject().apply {
            add("realtime_input", JsonObject().apply {
                add("media_chunks", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("mime_type", "audio/pcm;rate=16000")
                        addProperty("data", b64)
                    })
                })
            })
        }
        ws?.send(msg.toString())
    }

    fun sendUserText(text: String, turnComplete: Boolean = true) {
        val msg = JsonObject().apply {
            add("client_content", JsonObject().apply {
                add("turns", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        add("parts", JsonArray().apply {
                            add(JsonObject().apply { addProperty("text", text) })
                        })
                    })
                })
                addProperty("turn_complete", turnComplete)
            })
        }
        ws?.send(msg.toString())
    }

    fun sendToolResponses(responses: List<Pair<ToolCall, String>>) {
        val arr = JsonArray()
        for ((call, result) in responses) {
            arr.add(JsonObject().apply {
                addProperty("id", call.id)
                addProperty("name", call.name)
                add("response", JsonObject().apply {
                    addProperty("result", result)
                })
            })
        }
        val msg = JsonObject().apply {
            add("tool_response", JsonObject().apply {
                add("function_responses", arr)
            })
        }
        ws?.send(msg.toString())
    }

    private fun sendSetup(webSocket: WebSocket, systemInstruction: String, toolsBlock: JsonObject) {
        val setup = JsonObject().apply {
            add("setup", JsonObject().apply {
                addProperty("model", model)
                add("generation_config", JsonObject().apply {
                    add("response_modalities", JsonArray().apply { add("AUDIO") })
                    add("speech_config", JsonObject().apply {
                        add("voice_config", JsonObject().apply {
                            add("prebuilt_voice_config", JsonObject().apply {
                                addProperty("voice_name", voiceName)
                            })
                        })
                    })
                })
                add("system_instruction", JsonObject().apply {
                    addProperty("role", "system")
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", systemInstruction) })
                    })
                })
                add("tools", JsonArray().apply { add(toolsBlock) })
                add("output_audio_transcription", JsonObject())
                add("input_audio_transcription", JsonObject())
            })
        }
        webSocket.send(setup.toString())
    }

    private fun handleJson(raw: String) {
        val l = listener ?: return
        val root = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull() ?: return

        if (root.has("setupComplete")) {
            l.onSetupComplete()
            return
        }

        root.getAsJsonObject("serverContent")?.let { sc ->
            sc.getAsJsonObject("modelTurn")?.getAsJsonArray("parts")?.forEach { p ->
                val part = p.asJsonObject
                part.getAsJsonObject("inlineData")?.let { inline ->
                    val data = inline.get("data")?.asString ?: return@let
                    val pcm = Base64.decode(data, Base64.DEFAULT)
                    l.onAudioChunk(pcm)
                }
            }
            sc.getAsJsonObject("inputTranscription")?.get("text")?.asString?.let {
                if (it.isNotBlank()) l.onInputTranscript(it)
            }
            sc.getAsJsonObject("outputTranscription")?.get("text")?.asString?.let {
                if (it.isNotBlank()) l.onOutputTranscript(it)
            }
            if (sc.get("turnComplete")?.asBoolean == true) {
                l.onTurnComplete()
            }
        }

        root.getAsJsonObject("toolCall")?.getAsJsonArray("functionCalls")?.let { arr ->
            val calls = mutableListOf<ToolCall>()
            for (e in arr) {
                val o = e.asJsonObject
                val argsObj = o.getAsJsonObject("args") ?: JsonObject()
                val argsMap = mutableMapOf<String, Any?>()
                for ((k, v) in argsObj.entrySet()) {
                    argsMap[k] = when {
                        v.isJsonPrimitive && v.asJsonPrimitive.isString  -> v.asString
                        v.isJsonPrimitive && v.asJsonPrimitive.isBoolean -> v.asBoolean
                        v.isJsonPrimitive && v.asJsonPrimitive.isNumber  -> v.asDouble
                        v.isJsonNull -> null
                        else -> v.toString()
                    }
                }
                calls += ToolCall(
                    id   = o.get("id")?.asString ?: "",
                    name = o.get("name")?.asString ?: "",
                    args = argsMap
                )
            }
            if (calls.isNotEmpty()) l.onToolCall(calls)
        }
    }

    companion object { private const val TAG = "GeminiLive" }
}

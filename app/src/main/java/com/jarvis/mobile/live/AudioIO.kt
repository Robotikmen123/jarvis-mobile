package com.jarvis.mobile.live

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class AudioIO(
    private val onMicChunk: (ByteArray) -> Unit,
) {
    private val micScope    = CoroutineScope(Dispatchers.IO)
    private val playScope   = CoroutineScope(Dispatchers.IO)
    private var micJob: Job? = null
    private var playJob: Job? = null
    private var recorder: AudioRecord? = null
    private var track:    AudioTrack? = null
    private val playQueue = ConcurrentLinkedQueue<ByteArray>()

    @Volatile var muted: Boolean = false
    @Volatile private var jarvisSpeaking: Boolean = false

    fun isJarvisSpeaking() = jarvisSpeaking

    @SuppressLint("MissingPermission")
    fun start() {
        startMic()
        startPlayer()
    }

    fun stop() {
        micJob?.cancel(); micJob = null
        playJob?.cancel(); playJob = null
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
        playQueue.clear()
    }

    fun enqueuePlayback(pcm: ByteArray) {
        if (pcm.isEmpty()) return
        playQueue += pcm
    }

    fun endOfTurn() {
        playQueue += END_OF_TURN
    }

    @SuppressLint("MissingPermission")
    private fun startMic() {
        val minBuf = AudioRecord.getMinBufferSize(MIC_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufSize = maxOf(minBuf, MIC_CHUNK * 4)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MIC_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            bufSize
        )
        recorder = rec
        rec.startRecording()
        micJob = micScope.launch {
            val buf = ByteArray(MIC_CHUNK)
            while (isActive(this)) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0 && !muted && !jarvisSpeaking) {
                    val out = if (n == buf.size) buf.copyOf() else buf.copyOf(n)
                    runCatching { onMicChunk(out) }
                }
            }
        }
    }

    private fun startPlayer() {
        val minBuf = AudioTrack.getMinBufferSize(PLAY_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(PLAY_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, 24000 * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        t.play()

        playJob = playScope.launch {
            while (isActive(this)) {
                val chunk = playQueue.poll()
                if (chunk == null) {
                    if (jarvisSpeaking) {
                        // small idle gap signals end-of-speech
                    }
                    Thread.sleep(20)
                    continue
                }
                if (chunk === END_OF_TURN) {
                    jarvisSpeaking = false
                    continue
                }
                jarvisSpeaking = true
                runCatching { t.write(chunk, 0, chunk.size) }
            }
        }
    }

    private fun isActive(scope: CoroutineScope) = scope.coroutineContext[Job]?.isActive == true

    companion object {
        const val MIC_RATE  = 16_000
        const val PLAY_RATE = 24_000
        const val MIC_CHUNK = 2048   // 64 ms @ 16kHz, 16-bit mono
        private val END_OF_TURN = ByteArray(0)
    }
}

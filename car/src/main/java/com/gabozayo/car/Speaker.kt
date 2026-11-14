package com.gabozayo.car

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.tts.listener.OnServerConnectionListener
import android.content.Context
import java.util.Locale

/**
 * 음성 안내 (Pleos TextToSpeech SDK).
 *  · 클라이언트 ID/시크릿이 있으면 HYBRID(서버 우선, 안 되면 On-device), 없으면 ON_DEVICE 로 동작해요.
 *  · 초기화가 준비되기 전에 요청한 문장은 하나만 기억했다가 준비되면 읽어요.
 *  · Pleos TTS 를 못 쓰는 환경이면 Android 기본 TextToSpeech 로 대신해요.
 */
class Speaker(private val ctx: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    private var fallback: android.speech.tts.TextToSpeech? = null
    private var fallbackReady = false

    init {
        val server = BuildConfig.PLEOS_CLIENT_ID.isNotBlank() && BuildConfig.PLEOS_CLIENT_SECRET.isNotBlank()
        runCatching {
            val t = TextToSpeech(ctx.applicationContext, if (server) Mode.HYBRID else Mode.ON_DEVICE)
            tts = t
            t.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    if (!server) { markReady(); return }
                    t.registerApp(
                        clientID = BuildConfig.PLEOS_CLIENT_ID,
                        clientSecret = BuildConfig.PLEOS_CLIENT_SECRET,
                        listener = object : OnServerConnectionListener {
                            override fun onConnected() { markReady() }
                            override fun onFailed(errorType: ErrorType) { startFallback() }
                        },
                    )
                }
                override fun onFailure(errorType: ErrorType) { startFallback() }
            })
        }.onFailure { startFallback() }
    }

    private fun markReady() { ready = true; pending?.let { pending = null; say(it) } }

    private fun startFallback() {
        if (fallback != null) return
        fallback = android.speech.tts.TextToSpeech(ctx.applicationContext) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                fallback?.language = Locale.KOREAN; fallbackReady = true
                pending?.let { pending = null; say(it) }
            }
        }
    }

    fun say(text: String) {
        when {
            ready -> runCatching { tts?.speak(text) }.onFailure { startFallback(); pending = text }
            fallbackReady -> fallback?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "gabozayo")
            else -> pending = text
        }
    }

    fun release() {
        runCatching { tts?.release() }; tts = null
        fallback?.stop(); fallback?.shutdown(); fallback = null
    }
}

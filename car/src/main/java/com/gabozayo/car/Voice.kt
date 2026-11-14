package com.gabozayo.car

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.OnServerConnectionListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context

/**
 * 음성 입력 (Pleos SpeechToText SDK). 운전 중에도 손을 쓰지 않고 "경복궁", "안내 시작"처럼 말해서 쓸 수 있어요.
 * 순서(문서): initialize → (서버 모드면 registerApp) → addListener → request() → 결과 onUpdated → stop() → removeListener → release()
 * ResultListener 의 import 경로는 문서에 없어서 추측했어요. 빨간 글씨는 Alt+Enter 로 import 하세요.
 */
class Voice(
    ctx: Context,
    private val onListening: (Boolean) -> Unit,
    private val onResult: (text: String, final: Boolean) -> Unit,
    private val onProblem: (String) -> Unit,
) {
    private var stt: SpeechToText? = null
    private var ready = false
    private var listening = false

    private val results = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() { listening = true; onListening(true) }
        override fun onUpdated(stt: String, completed: Boolean) { onResult(stt, completed) }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onEndedRecognition() { listening = false; onListening(false) }
        override fun onError(errorType: ErrorType) { listening = false; onListening(false); onProblem("음성 인식 오류: $errorType") }
    }

    init {
        val server = BuildConfig.PLEOS_CLIENT_ID.isNotBlank() && BuildConfig.PLEOS_CLIENT_SECRET.isNotBlank()
        runCatching {
            val s = SpeechToText(ctx.applicationContext, if (server) Mode.HYBRID else Mode.ON_DEVICE)
            stt = s
            s.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    s.addListener(results)
                    if (!server) { ready = true; return }
                    s.registerApp(
                        clientID = BuildConfig.PLEOS_CLIENT_ID,
                        clientSecret = BuildConfig.PLEOS_CLIENT_SECRET,
                        listener = object : OnServerConnectionListener {
                            override fun onConnected() { ready = true }
                            override fun onFailed(errorType: ErrorType) { onProblem("음성 서버 연결 실패: $errorType") }
                        },
                    )
                }
                override fun onFailure(errorType: ErrorType) { onProblem("음성 인식을 시작하지 못했어요: $errorType") }
            })
        }.onFailure { onProblem("음성 인식을 쓸 수 없어요: ${it.message}") }
    }

    /** 마이크 버튼: 듣는 중이면 멈추고, 아니면 듣기 시작해요 */
    fun toggle() {
        val s = stt ?: return onProblem("음성 인식을 쓸 수 없어요")
        if (listening) { runCatching { s.stop() }; return }
        if (!ready) return onProblem("음성 인식을 준비하는 중이에요. 잠시 후 다시 눌러 주세요")
        runCatching { s.request() }.onFailure { onProblem("음성 인식을 시작하지 못했어요") }
    }

    fun release() {
        runCatching { stt?.stop(); stt?.removeListener(results); stt?.release() }
        stt = null
    }
}

package com.gabozayo.car.link

import android.content.Context
import com.gabozayo.car.Ack
import com.gabozayo.car.Destination
import com.gabozayo.car.RouteType
import com.gabozayo.car.SavedPlace
import com.gabozayo.core.AckMsg
import com.gabozayo.core.FamilyApi
import com.gabozayo.core.InterestItem
import com.gabozayo.core.KidItem
import com.gabozayo.core.SavedItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 폰과 실제로 주고받는 통로 (Firebase 중계 + 6자리 코드).
 * 코드를 입력하면 3초마다 문서를 읽어서 폰의 찜 목록을 갱신하고, 새로 보낸 목적지가 있으면 화면에 올려요.
 * 차량은 진행 상태(ack)와 "연결됨" 표시만 써요.
 */
class FirebaseDestinationLink(ctx: Context, private val api: FamilyApi) : DestinationLink {
    private val prefs = ctx.applicationContext.getSharedPreferences("gabozayo_car", Context.MODE_PRIVATE)
    private val bus = MutableSharedFlow<Destination>(extraBufferCapacity = 4)
    override val incoming: Flow<Destination> = bus.asSharedFlow()
    private val savedFlow = MutableStateFlow<List<SavedPlace>>(emptyList())
    override val saved: StateFlow<List<SavedPlace>> = savedFlow.asStateFlow()
    private val interestFlow = MutableStateFlow<List<InterestItem>>(emptyList())
    override val interests: StateFlow<List<InterestItem>> = interestFlow.asStateFlow()
    private val recsFlow = MutableStateFlow<List<SavedItem>>(emptyList())
    override val recs: StateFlow<List<SavedItem>> = recsFlow.asStateFlow()
    private val eventsFlow = MutableStateFlow<List<SavedItem>>(emptyList())
    override val events: StateFlow<List<SavedItem>> = eventsFlow.asStateFlow()
    private val kidsFlow = MutableStateFlow<List<KidItem>>(emptyList())
    override val kids: StateFlow<List<KidItem>> = kidsFlow.asStateFlow()
    private fun apply(doc: com.gabozayo.core.FamilyDoc) {
        recsFlow.value = doc.recs; eventsFlow.value = doc.events; kidsFlow.value = doc.kids
    }
    private val codeFlow = MutableStateFlow(prefs.getString("family_code", null))
    override val linkedCode: StateFlow<String?> = codeFlow.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun connect(): Result<Unit> = runCatching {
        check(api.isConfigured) { "Firebase 주소가 없어요 (local.properties 의 FIREBASE_DB_URL)" }
        scope.launch { var n = 0; while (isActive) { pollOnce(); if (++n % 5 == 0) codeFlow.value?.let { c -> runCatching { api.beat(c) } }; delay(1000) } }
        Unit
    }

    private suspend fun pollOnce() {
        val code = codeFlow.value ?: return
        val r = runCatching { api.load(code) }
        if (r.isFailure) return
        val doc = r.getOrNull() ?: return
        savedFlow.value = doc.saved.map { SavedPlace(it.id, it.name, it.address, it.region, it.tag, it.lat, it.lng, it.img) }
        interestFlow.value = doc.interests
        apply(doc)
        val s = doc.sent ?: return
        // 새로 온 목적지만 받아요. 폰과 차량의 시계가 달라도 되도록 시간 대신 전송 번호(id)로 구분해요
        val seen = prefs.getString("last_sent_id", null)
        if (seen == null) { prefs.edit().putString("last_sent_id", s.id).apply(); return }   // 연결 전에 있던 예전 목적지는 무시
        if (s.id != seen) {
            prefs.edit().putString("last_sent_id", s.id).apply()
            bus.emit(Destination(s.id, s.name, s.address, s.lat, s.lng, RouteType.of(s.route), s.sentAt))
        }
    }

    override suspend fun link(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(code.length == 6 && code.all { it.isDigit() }) { "코드 6자리 숫자를 입력해 주세요" }
            check(api.isConfigured) { "Firebase 주소가 없어요 (local.properties 의 FIREBASE_DB_URL)" }
            val doc = api.load(code) ?: error("코드를 찾을 수 없어요. 휴대폰 앱의 번호를 다시 확인해 주세요")
            // 이 코드로 연결하기 전에 보낸 목적지는 다시 띄우지 않아요
            prefs.edit().putString("family_code", code).putString("last_sent_id", doc.sent?.id ?: "none").apply()
            savedFlow.value = doc.saved.map { SavedPlace(it.id, it.name, it.address, it.region, it.tag, it.lat, it.lng, it.img) }
            interestFlow.value = doc.interests
            apply(doc)
            codeFlow.value = code
            runCatching { api.markCarConnected(code) }
            Unit
        }
    }

    override fun unlink() {
        // 휴대폰이 "차량 연결 안 됨"으로 바로 보도록 알려요
        codeFlow.value?.let { c -> scope.launch { runCatching { api.markCarDisconnected(c) } } }
        prefs.edit().remove("family_code").apply()
        codeFlow.value = null
        savedFlow.value = emptyList()
        interestFlow.value = emptyList()
        recsFlow.value = emptyList(); eventsFlow.value = emptyList(); kidsFlow.value = emptyList()
    }

    override suspend fun ack(id: String, ack: Ack, message: String, etaSec: Int, distM: Int) {
        val code = codeFlow.value ?: return
        runCatching { api.writeAck(code, AckMsg(id, ack.key, message, System.currentTimeMillis(), etaSec, distM)) }
    }

    override fun close() { scope.cancel() }

}

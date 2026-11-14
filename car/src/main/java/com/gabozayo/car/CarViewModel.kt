package com.gabozayo.car

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gabozayo.car.navi.NaviEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.gabozayo.core.KidItem
import com.gabozayo.core.SavedItem

enum class Tab { HOME, SAVED, OPTIONS }

class CarViewModel(app: Application) : AndroidViewModel(app) {
    private val link = Gateways.link(app)
    private val navi = Gateways.navi(app)
    private val vehicle = Gateways.vehicle(app)
    private val speaker = Speaker(app)
    private val voice = Voice(
        app,
        onListening = { on -> _ui.update { it.copy(listening = on, heard = if (on) "" else it.heard) } },
        onResult = { text, final -> _ui.update { it.copy(heard = text) }; if (final) handleVoice(text) },
        onProblem = { msg -> _ui.update { it.copy(message = msg, listening = false) } },
    )
    private var pos: LatLng? = null

    // 내비에 시간·거리 묻기용 상태. init 블록(아래)이 코루틴을 바로 시작하므로 반드시 init 보다 위에서 만들어요
    private val tripPending = java.util.ArrayDeque<Pair<String, Destination>>()
    private val tripSignal = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
    private var tripWaiter: kotlinx.coroutines.CompletableDeferred<NaviEvent.Trip>? = null
    private val tripAsked = HashMap<String, Long>()
    private var tripFails = 0
    private var tripFailAt = 0L

    data class Ui(
        val stage: Stage = Stage.IDLE, val dest: Destination? = null,
        val linkOk: Boolean = false, val naviOk: Boolean = false, val vehicleOk: Boolean = false,
        val message: String = "", val gear: Gear = Gear.P, val distanceKm: Double? = null,
        val saved: List<SavedPlace> = emptyList(), val pos: LatLng? = null,
        val linkedCode: String? = null, val linkError: String = "",
        val listening: Boolean = false, val heard: String = "", val interests: List<com.gabozayo.core.InterestItem> = emptyList(),
        val recs: List<SavedItem> = emptyList(), val events: List<SavedItem> = emptyList(), val kids: List<KidItem> = emptyList(),
        val tab: Tab = Tab.HOME, val detail: SavedItem? = null, val kidFilter: String? = null,
        val proactive: Boolean = true, val suggestion: SavedItem? = null,
        val locationOk: Boolean = false, val notifOk: Boolean = false, val driveOk: Boolean = false,
        val lastSent: Destination? = null, val brake: String? = null,
        /** 내비가 계산한 도착까지 남은 시간(초) · 거리(m). 아직 못 받았으면 null */
        val etaSec: Int? = null, val tripDistM: Int? = null,
        /** 장소별로 내비에 물어본 시간·거리 (키: "p{장소번호}") */
        val trips: Map<String, TripBrief> = emptyMap(),
        /** 옵션: 차량에서 직접 출발할 때 쓸 경로 종류 · 근처 제안 반경(km) · 음성 안내 */
        val routeType: RouteType = RouteType.RECOMMEND, val radiusKm: Int = 3, val voiceOn: Boolean = true,
    ) {
        /** 주행 중(D·R)이면 조작은 막고 음성으로만 알려요 */
        val driving: Boolean get() = gear == Gear.D || gear == Gear.R
        /** 장소 카드에 보여줄 "약 12분 · 4.2km". 내비 답이 아직 없으면 빈 글자 */
        fun tripText(key: String): String = trips[key]?.let { formatTrip(it.etaSec, it.distM) }.orEmpty()
        /** 내비가 알려준 도착까지 시간(분, 올림) */
        val etaMin: Int? get() = etaSec?.let { ((it + 59) / 60).coerceAtLeast(1) }
    }
    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    private fun dist(p: LatLng?, d: Destination?): Double? = if (p != null && d != null) distanceKm(p, LatLng(d.lat, d.lng)) else null
    /** 안내가 끝나면 진행 상태만 비우고, 홈 화면 데이터와 설정은 그대로 둬요 */
    private fun fresh() = _ui.value.copy(stage = Stage.IDLE, dest = null, message = "", distanceKm = null, listening = false, heard = "", detail = null, suggestion = null, etaSec = null, tripDistM = null)

    private val prefs = app.getSharedPreferences("gabozayo_car", android.content.Context.MODE_PRIVATE)
    private val shownSuggestions = HashSet<Int>()

    init {
        _ui.update {
            it.copy(
                proactive = prefs.getBoolean("proactive", true), voiceOn = prefs.getBoolean("voice_on", true),
                routeType = RouteType.of(prefs.getString("route_type", null)), radiusKm = prefs.getInt("radius_km", 3),
            )
        }
        viewModelScope.launch { link.recs.collect { l -> _ui.update { it.copy(recs = l) } } }
        viewModelScope.launch { link.events.collect { l -> _ui.update { it.copy(events = l) } } }
        viewModelScope.launch { link.kids.collect { l -> _ui.update { it.copy(kids = l) } } }
        viewModelScope.launch {
            link.connect().onSuccess { _ui.update { it.copy(linkOk = true) } }
                .onFailure { e -> _ui.update { it.copy(message = "휴대폰 연결 실패: ${e.message}") } }
        }
        viewModelScope.launch {
            navi.connect().onSuccess { _ui.update { it.copy(naviOk = true) } }
                .onFailure { e -> _ui.update { it.copy(message = "내비 연결 실패: ${e.message}") } }
        }
        viewModelScope.launch { vehicle.brake.collect { st -> _ui.update { it.copy(brake = st) } } }
        viewModelScope.launch { vehicle.gear.collect { g -> _ui.update { it.copy(gear = g) } } }
        viewModelScope.launch { vehicle.position.collect { p -> pos = p; _ui.update { it.copy(pos = p, distanceKm = dist(p, it.dest)) }; maybeSuggest(p) } }
        viewModelScope.launch { link.linkedCode.collect { c -> _ui.update { it.copy(linkedCode = c, linkError = "") } } }
        viewModelScope.launch { link.interests.collect { list -> _ui.update { it.copy(interests = list) } } }
        viewModelScope.launch { link.saved.collect { list -> _ui.update { it.copy(saved = list) } } }
        // 휴대폰에서 목적지가 오면 확인 없이 바로 내비에 경로를 요청해요 (달리는 중에도 음성으로 알려요)
        viewModelScope.launch {
            link.incoming.collect { d ->
                _ui.update { it.copy(stage = Stage.RECEIVED, dest = d, lastSent = d, message = "", distanceKm = dist(pos, d)) }
                sendAck(d.id, Ack.RECEIVED)
                say("${d.name} 목적지가 도착했어요. 내비로 안내할게요.")
                startRoute(true)
            }
        }
        viewModelScope.launch { navi.events.collect { onNavi(it) } }
        viewModelScope.launch { tripWorker() }
        // 목적지가 정해지면 내비에 도착까지 걸리는 시간·거리를 물어봐요 (실제 값이 오면 화면과 폰에 보여줘요)
        viewModelScope.launch {
            _ui.map { it.dest }.distinctUntilChanged { a, b -> a?.id == b?.id }.collect { d ->
                _ui.update { it.copy(etaSec = null, tripDistM = null) }
                if (d != null) enqueueTrip("dest", d, front = true)
            }
        }
    }

    private var vehicleStarted = false
    /** 위치 권한(FUSED_LOCATION, 실행 중 허용이 필요한 권한)을 받은 뒤에 차량 데이터를 연결해요 */
    fun startVehicle() {
        if (vehicleStarted) return
        vehicleStarted = true
        viewModelScope.launch {
            vehicle.connect().onSuccess { _ui.update { it.copy(vehicleOk = true, message = "") } }
                .onFailure { e -> vehicleStarted = false; _ui.update { it.copy(message = "차량 데이터 연결 실패: ${e.message}") } }
        }
    }
    fun setPermissions(location: Boolean, notif: Boolean, drive: Boolean) = _ui.update { it.copy(locationOk = location, notifOk = notif, driveOk = drive) }
    /** 차량 데이터 권한을 허용받은 뒤 다시 읽어요 */
    fun refreshVehicle() = vehicle.refresh()

    // ---- 화면 이동 ----
    fun openTab(t: Tab) = _ui.update { it.copy(tab = t, detail = null) }
    fun openDetail(p: SavedItem?) = _ui.update { it.copy(detail = p) }
    fun selectKid(name: String?) = _ui.update { it.copy(kidFilter = if (it.kidFilter == name) null else name) }
    fun setProactive(on: Boolean) { prefs.edit().putBoolean("proactive", on).apply(); _ui.update { it.copy(proactive = on, suggestion = if (on) it.suggestion else null) } }
    fun setRoute(r: RouteType) { prefs.edit().putString("route_type", r.key).apply(); _ui.update { it.copy(routeType = r) } }
    fun setRadius(km: Int) { prefs.edit().putInt("radius_km", km).apply(); _ui.update { it.copy(radiusKm = km) } }
    fun setVoice(on: Boolean) { prefs.edit().putBoolean("voice_on", on).apply(); _ui.update { it.copy(voiceOn = on) } }
    /** 음성 안내를 껐으면 말하지 않아요 */
    private fun say(text: String) { if (_ui.value.voiceOn) speaker.say(text) }
    fun dismissSuggestion() { SuggestNotifier.cancel(getApplication()); _ui.update { it.copy(suggestion = null) } }
    /** 제안 카드·알림·음성의 [가보자요!] */
    fun acceptSuggestion() { _ui.value.suggestion?.let { goTo(it, true) } }

    /** 추천·축제 장소로 바로 출발: 목적지로 정하고 내비에 경로를 요청해요 */
    fun goTo(p: SavedItem, fromSuggestion: Boolean = false) {
        val d = Destination("rec-${p.id}-${System.currentTimeMillis()}", p.name, p.address, p.lat, p.lng, _ui.value.routeType, System.currentTimeMillis())
        _ui.update { it.copy(stage = Stage.RECEIVED, dest = d, message = "", distanceKm = dist(pos, d), suggestion = null, detail = null) }
        SuggestNotifier.cancel(getApplication())
        startRoute(fromSuggestion)   // 제안 카드의 [가보자요!]는 한 번 눌러 바로 출발해요
    }
    /** 찜 목록에서 바로 출발 */
    fun goToSaved(p: SavedPlace) {
        val d = Destination("saved-${p.id}-${System.currentTimeMillis()}", p.name, p.address, p.lat, p.lng, _ui.value.routeType, System.currentTimeMillis())
        goToDest(d)
    }
    fun goToDest(d: Destination) {
        _ui.update { it.copy(stage = Stage.RECEIVED, dest = d, message = "", distanceKm = dist(pos, d)) }
        startRoute(false)
    }

    /** 달리는 중 가까운(3km 안) 아이 취향 장소가 있으면 한 번만 제안하고 음성으로 알려요 */
    private fun maybeSuggest(p: LatLng?) {
        if (p == null) return
        val u = _ui.value
        if (!u.proactive || !u.driving || u.suggestion != null || u.stage != Stage.IDLE) return
        val hit = (u.recs + u.events).firstOrNull { it.id !in shownSuggestions && distanceKm(p, LatLng(it.lat, it.lng)) <= u.radiusKm.toDouble() } ?: return
        shownSuggestions += hit.id
        wantTrips(listOf(TripTarget("p${hit.id}", hit.name, hit.address, hit.lat, hit.lng)))
        _ui.update { it.copy(suggestion = hit) }
        SuggestNotifier.show(getApplication(), hit, distanceKm(p, LatLng(hit.lat, hit.lng)))
        say("근처에 ${hit.name}이 있어요. ${hit.why.ifBlank { "아이가 좋아할 만한 곳이에요" }}. 가보시겠어요? 좋아, 또는 다음에, 라고 말씀해 주세요.")
        // 말이 끝날 즈음 음성 인식(STT)을 켜서, 손을 쓰지 않고 대답할 수 있게 해요
        viewModelScope.launch {
            kotlinx.coroutines.delay(8_000)
            if (_ui.value.suggestion?.id == hit.id) voice.toggle()
        }
    }

    fun vehicleDenied() = _ui.update { it.copy(message = "위치 권한을 허용해 주세요. 차량 위치를 쓰려면 필요해요") }

    // ---- 내비에 시간·거리 묻기 ----
    // onTripInfo 에는 어느 장소의 답인지 표시가 없어서, 한 번에 하나씩 묻고 답이 올 때까지 기다려요 (목적지가 우선)

    private fun enqueueTrip(key: String, d: Destination, front: Boolean = false) {
        synchronized(tripPending) { if (front) tripPending.addFirst(key to d) else tripPending.addLast(key to d) }
        tripSignal.trySend(Unit)
    }

    /** 화면에 보이는 장소들의 시간·거리를 내비에 물어봐요 (5분 안에 물어본 곳은 다시 묻지 않아요) */
    fun wantTrips(items: List<TripTarget>) {
        val now = System.currentTimeMillis()
        items.forEach { t ->
            if (t.lat == 0.0 && t.lng == 0.0) return@forEach
            val last = tripAsked[t.key]
            if (last != null && now - last < 300_000) return@forEach
            tripAsked[t.key] = now
            enqueueTrip(t.key, Destination("trip-${t.key}", t.name, t.address, t.lat, t.lng, _ui.value.routeType, 0))
        }
    }

    private suspend fun tripWorker() {
        while (true) {
            val next = synchronized(tripPending) { tripPending.pollFirst() }
            if (next == null) { tripSignal.receive(); continue }
            val (key, d) = next
            // 내비가 계속 답하지 않으면(에뮬레이터 등) 잠시 장소 카드 묻기를 쉬어요. 목적지는 그대로 물어요
            if (key != "dest" && tripFails >= 3 && System.currentTimeMillis() - tripFailAt < 20_000) { tripAsked.remove(key); continue }
            val w = kotlinx.coroutines.CompletableDeferred<NaviEvent.Trip>()
            tripWaiter = w
            navi.requestTrip(d)
            val r = kotlinx.coroutines.withTimeoutOrNull(6_000) { w.await() }
            tripWaiter = null
            if (r == null) { tripFails++; tripFailAt = System.currentTimeMillis(); tripAsked.remove(key); continue }
            tripFails = 0
            if (key == "dest") {
                val cur = _ui.value.dest ?: continue
                _ui.update { it.copy(etaSec = r.etaSec, tripDistM = r.distM) }
                // 폰의 전송 화면에도 같은 값을 보여주도록 현재 상태와 함께 다시 보내요
                val ack = when (_ui.value.stage) { Stage.READY -> Ack.READY; Stage.GUIDING -> Ack.GUIDING; Stage.REQUESTING -> Ack.SEARCHING; else -> null }
                if (ack != null) link.ack(cur.id, ack, "", r.etaSec, r.distM)
            } else _ui.update { it.copy(trips = it.trips + (key to TripBrief(r.etaSec, r.distM))) }
        }
    }

    private suspend fun onNavi(ev: NaviEvent) {
        // 시간·거리 답은 목적지가 없어도 받아야 해요 (홈 카드용)
        if (ev is NaviEvent.Trip) { tripWaiter?.complete(ev); return }
        val d = _ui.value.dest ?: return
        when (ev) {
            is NaviEvent.Trip -> Unit
            NaviEvent.RouteStarted -> _ui.update { it.copy(message = "내비에서 경로를 계산하고 있어요") }
            NaviEvent.RouteReady -> {
                _ui.update { it.copy(stage = Stage.READY) }; sendAck(d.id, Ack.READY)
                // 경로가 만들어지면 시작 버튼을 기다리지 않고 바로 안내를 시작해요
                navi.startGuidance().onFailure {
                    say("경로가 준비됐어요. 내비 화면에서 안내 시작을 눌러 주세요.")
                    _ui.update { u -> u.copy(message = "내비 화면에서 [안내 시작]을 눌러 주세요") }
                }
            }
            NaviEvent.GuidanceStarted -> {
                _ui.update { it.copy(stage = Stage.GUIDING, message = "") }; sendAck(d.id, Ack.GUIDING)
                say("${d.name}까지 경로 안내를 시작합니다.")
            }
            NaviEvent.GuidanceEnded -> _ui.update { fresh() }
            is NaviEvent.Failed -> fail(d, ev.message)
        }
    }

    /** 폰에 진행 상태를 보낼 때 내비가 알려준 도착 정보도 같이 보내요 */
    private suspend fun sendAck(id: String, a: Ack, msg: String = "") = link.ack(id, a, msg, _ui.value.etaSec ?: 0, _ui.value.tripDistM ?: 0)

    private suspend fun fail(d: Destination, msg: String?) {
        _ui.update { it.copy(stage = Stage.FAILED, message = msg ?: "경로를 만들지 못했어요") }
        sendAck(d.id, Ack.FAILED, msg ?: "")
    }

    /** [안내 시작]: 정차(P) 상태에서만 Pleos 내비에 경로를 요청하고, 경로가 나오면 바로 안내를 시작해요 */
    fun start() = startRoute(false)

    private fun startRoute(force: Boolean) {
        val d = _ui.value.dest ?: return
        if (!force && _ui.value.driving) { _ui.update { it.copy(message = "주행 중에는 사용할 수 없어요") }; return }
        _ui.update { it.copy(stage = Stage.REQUESTING, message = "") }
        if (!force) say("${d.name}까지 경로를 찾을게요.")
        requestJob?.cancel()
        // 화면이 뒤로 가도(내비가 앞으로 나와도) 멈추지 않도록 메인 스레드와 상관없는 곳에서 돌려요
        requestJob = bgScope.launch {
            sendAck(d.id, Ack.SEARCHING)
            navi.requestRoute(d).onFailure { fail(d, it.message); return@launch }
            // 경로 완료 알림이 늦거나 오지 않을 수 있어요. 이미 내비가 받은 요청이라 취소하지 않고, 4초 뒤에는 내비 화면을 보라고만 알려요
            // (여기서 cancelRoute 를 부르면 운전자가 시작한 안내가 끊겨요)
            kotlinx.coroutines.delay(4_000)
            if (_ui.value.stage == Stage.REQUESTING && _ui.value.dest?.id == d.id) {
                _ui.update { it.copy(stage = Stage.READY, message = "내비 화면에서 경로와 안내를 확인해 주세요") }
                sendAck(d.id, Ack.READY)
            }
        }
    }

    private val bgScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    private var requestJob: kotlinx.coroutines.Job? = null

    /** 경로를 찾는 중에 [취소]: 내비 요청도 같이 취소해요 */
    fun cancelRequest() {
        requestJob?.cancel()
        navi.cancelRoute()
        _ui.value.dest?.let { d -> viewModelScope.launch { sendAck(d.id, Ack.FAILED, "취소했어요") } }
        _ui.update { fresh() }
    }

    fun dismiss() { requestJob?.cancel(); _ui.update { fresh() } }

    /** 휴대폰 앱에 보이는 6자리 코드로 연결 */
    fun connectCode(code: String) {
        viewModelScope.launch {
            _ui.update { it.copy(linkError = "") }
            link.link(code).onFailure { e -> _ui.update { it.copy(linkError = e.message ?: "연결하지 못했어요") } }
        }
    }
    fun unlink() = link.unlink()

    /** 찜 목록에서 하나를 고르면 그 곳이 목적지가 돼요 */
    fun pick(p: SavedPlace) {
        val d = Destination("saved-${p.id}-${System.currentTimeMillis()}", p.name, p.address, p.lat, p.lng, _ui.value.routeType, System.currentTimeMillis())
        _ui.update { it.copy(stage = Stage.RECEIVED, dest = d, message = "", distanceKm = dist(pos, d)) }
    }
    /** 마이크 버튼 */
    fun listen() = voice.toggle()

    /** 장소를 목적지로만 정해 둬요 (출발은 "안내 시작"이라고 말하면 해요) */
    private fun pickDest(d: Destination) = _ui.update { it.copy(stage = Stage.RECEIVED, dest = d, message = "", distanceKm = dist(pos, d), detail = null) }

    /**
     * 말한 내용을 처리해요. 주행 중에도 손을 쓰지 않으니 말로 하는 출발은 허용해요.
     *  · 제안이 떠 있을 때: "가보자요/좋아/갈래" → 출발, "다음에/아니/닫기" → 닫기
     *  · "안내 시작/출발/가자" → 정해 둔 목적지로 출발,  "취소/그만" → 닫기
     *  · 그 밖에는 찜·추천·행사의 장소 이름으로 목적지를 골라요
     */
    private fun handleVoice(raw: String) {
        val text = raw.replace(" ", "")
        if (text.isBlank()) return
        val u = _ui.value
        fun has(vararg w: String) = w.any { text.contains(it) }
        val sg = u.suggestion
        if (sg != null) {
            if (has("가보자", "좋아", "갈래", "그래", "응", "가자", "안내")) { say("${sg.name}으로 안내할게요."); acceptSuggestion(); return }
            if (has("다음에", "아니", "닫기", "싫어", "괜찮")) { dismissSuggestion(); return }
        }
        if (u.dest != null && u.stage == Stage.RECEIVED && has("안내시작", "안내해", "출발", "가자", "시작")) { startRoute(true); return }
        if (has("닫기", "취소", "그만")) { dismiss(); return }
        val cands = u.saved.map { Triple(it.name, it, null as SavedItem?) } + (u.recs + u.events).map { Triple(it.name, null as SavedPlace?, it as SavedItem?) }
        val hit = cands.firstOrNull { val n = it.first.replace(" ", ""); n.isNotEmpty() && (text.contains(n) || (text.length >= 2 && n.contains(text))) }
        if (hit == null) { say("찾지 못했어요. 다시 말씀해 주세요."); return }
        val (name, sp, si) = hit
        val d = if (sp != null) Destination("saved-${sp.id}-${System.currentTimeMillis()}", sp.name, sp.address, sp.lat, sp.lng, u.routeType, System.currentTimeMillis())
        else Destination("rec-${si!!.id}-${System.currentTimeMillis()}", si.name, si.address, si.lat, si.lng, u.routeType, System.currentTimeMillis())
        pickDest(d)
        say("${name}을 목적지로 정했어요. 안내 시작이라고 말씀해 주세요.")
    }

    override fun onCleared() { navi.release(); link.close(); vehicle.release(); speaker.release(); voice.release() }
}

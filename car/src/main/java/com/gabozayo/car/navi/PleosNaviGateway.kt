// ─────────────────────────────────────────────────────────────
// 실제 Pleos 내비 연동 (NaviHelper 3.0.5).  공식 문서 NaviHelper > 초기화 / 이벤트 리스너 / 목적지·경로 페이지 기준이에요.
//
// 문서로 확인한 것
//  · NaviHelper(context) → initialize(onConnected, onFailed) → addListener(listener) → requestRoute(RouteInfo(...))
//  · 끝낼 때  removeListener(listener) → release()
//  · 경로 탐색 시작 onRouteStarted(RouteStartInfo), 탐색 완료 onRoutingCompleted(), 오류 onError(NaviErrorCode)
//  · requestRoute 가 성공해도 탐색 완료가 아니에요. 결과는 콜백으로 와요
//
// 문서에 없는 것
//  · "안내 시작"을 코드로 바로 누르는 API 는 문서에 없어요. 그래서 startGuidance() 는 실패로 돌려주고,
//    차량 앱이 음성과 화면으로 "내비 화면에서 안내 시작을 눌러 주세요"라고 알려줘요.
//
// RouteInfo / RouteOption import 가 빨갛게 보이면 Alt+Enter 로 import 하세요 (패키지 이름을 문서에서 확인하지 못했어요)
// ─────────────────────────────────────────────────────────────
package com.gabozayo.car.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.TripInfo
import ai.pleos.playground.navi.data.TripRequest
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.gabozayo.car.Destination
import com.gabozayo.car.RouteType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PleosNaviGateway(private val ctx: Context) : NaviGateway {
    private var navi: NaviHelper? = null
    private val bus = MutableSharedFlow<NaviEvent>(extraBufferCapacity = 8)
    override val events: Flow<NaviEvent> = bus.asSharedFlow()

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) { Log.d("Gabozayo", "내비: onRouteStarted"); bus.tryEmit(NaviEvent.RouteStarted) }
        override fun onRoutingCompleted() { Log.d("Gabozayo", "내비: onRoutingCompleted"); bus.tryEmit(NaviEvent.RouteReady) }
        override fun onTripInfo(tripInfo: TripInfo) { Log.d("Gabozayo", "내비: onTripInfo eta=${tripInfo.eta}s distance=${tripInfo.distance}m"); bus.tryEmit(NaviEvent.Trip(tripInfo.eta, tripInfo.distance)) }
        override fun onError(errorCode: NaviErrorCode) { Log.d("Gabozayo", "내비: onError $errorCode"); bus.tryEmit(NaviEvent.Failed("내비 오류: $errorCode")) }
    }

    override suspend fun connect(): Result<Unit> = suspendCancellableCoroutine { c ->
        val n = NaviHelper(ctx.applicationContext)
        navi = n
        n.initialize(
            onConnected = { n.addListener(listener); if (c.isActive) c.resume(Result.success(Unit)) },
            onFailed = { err -> if (c.isActive) c.resume(Result.failure(IllegalStateException("Pleos 내비 연결 실패: ${err.message}"))) },
        )
    }

    override suspend fun requestRoute(d: Destination): Result<Unit> = runCatching {
        val option = when (d.route) {
            RouteType.FASTEST -> RouteOption.SHORT_TIME
            RouteType.SHORTEST -> RouteOption.SHORT_DISTANCE
            RouteType.FREE -> RouteOption.FREE
            RouteType.RECOMMEND -> RouteOption.RECOMMENDED
        }
        val n = checkNotNull(navi) { "Pleos 내비에 아직 연결되지 않았어요" }
        Log.d("Gabozayo", "내비: requestRoute ${d.name}")
        n.requestRoute(
            RouteInfo(
                latitude = d.lat, longitude = d.lng,
                displayLongitude = null, displayLatitude = null,
                poiName = d.name, poiId = d.id, poiSubId = "0", address = d.address,
                routeOption = option,
            )
        )
    }

    /**
     * 안내를 코드로 바로 시작하는 API 는 없어서, 내비 화면을 앞으로 가져와 운전자가 [안내 시작]만 누르면 되게 해요.
     * (NavigationScreenRequest.Foreground · NAVI_CUSTOM_MAP 권한). 그래서 결과는 항상 "눌러 주세요" 안내로 돌려줘요.
     */
    override suspend fun startGuidance(): Result<Unit> {
        runCatching { navi?.requestNavigationScreen(NavigationScreenRequest.Foreground) }
            .onFailure { Log.w("Gabozayo", "내비 화면 요청 실패: ${it.message}") }
        return Result.failure(UnsupportedOperationException("내비 화면에서 안내 시작을 눌러 주세요"))
    }

    /** 문서: requestTrip(TripRequest) → onTripInfo(TripInfo). eta 는 초, distance 는 미터 (NAVI_ROUTE 권한) */
    override fun requestTrip(d: Destination) {
        runCatching { navi?.requestTrip(TripRequest(latitude = d.lat, longitude = d.lng, poiId = d.id, poiName = d.name, poiSubId = "0")) }
            .onFailure { Log.w("Gabozayo", "내비 requestTrip 실패: ${it.message}") }
    }

    /** 문서: cancelRoute() 는 목적지를 취소해요 (NAVI_ROUTE_SEARCH 권한) */
    override fun cancelRoute() { runCatching { navi?.cancelRoute() } }

    override fun release() {
        navi?.removeListener(listener)
        navi?.release()
        navi = null
    }
}

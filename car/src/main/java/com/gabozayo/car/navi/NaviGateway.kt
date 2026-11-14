package com.gabozayo.car.navi

import com.gabozayo.car.Destination
import kotlinx.coroutines.flow.*

sealed interface NaviEvent {
    data object RouteStarted : NaviEvent      // 내비가 경로 탐색을 시작했어요
    data object RouteReady : NaviEvent        // 경로 탐색이 끝났어요
    data object GuidanceStarted : NaviEvent   // 운전자가 안내를 시작했어요
    data object GuidanceEnded : NaviEvent
    /** 내비가 계산한 도착 예정 정보: etaSec 초 · distM 미터 */
    data class Trip(val etaSec: Int, val distM: Int) : NaviEvent
    data class Failed(val message: String) : NaviEvent
}

/** Pleos 내비와 통신하는 부분은 이 인터페이스 뒤에 모아요. 화면 코드는 SDK 를 몰라도 돼요 */
interface NaviGateway {
    val events: Flow<NaviEvent>
    suspend fun connect(): Result<Unit>
    suspend fun requestRoute(d: Destination): Result<Unit>
    /** 경로가 준비되면 운전자가 시작 버튼을 누르지 않아도 바로 안내를 시작해요 */
    suspend fun startGuidance(): Result<Unit>
    /** 목적지까지 걸리는 시간·거리를 내비에 물어봐요. 답은 events 로 Trip 이 와요 */
    fun requestTrip(d: Destination)
    /** 요청 중이던 경로를 취소해요 */
    fun cancelRoute()
    fun release()
}

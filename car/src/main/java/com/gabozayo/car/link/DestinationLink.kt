package com.gabozayo.car.link

import com.gabozayo.car.Ack
import com.gabozayo.car.Destination
import com.gabozayo.car.SavedPlace
import com.gabozayo.core.InterestItem
import com.gabozayo.core.KidItem
import com.gabozayo.core.SavedItem
import kotlinx.coroutines.flow.*

/** 휴대폰 앱과 주고받는 통로 (Firebase 로 구현) */
interface DestinationLink {
    val incoming: Flow<Destination>
    /** 휴대폰에서 찜한 곳 목록 (바뀔 때마다 새로 들어와요) */
    val saved: StateFlow<List<SavedPlace>>
    /** 우리 아이 관심사 순위 (많이 고른 순서) */
    val interests: StateFlow<List<InterestItem>>
    /** 차량 홈 화면용: 아이 취향 추천 장소 · 오늘의 축제 · 아이 목록 */
    val recs: StateFlow<List<SavedItem>>
    val events: StateFlow<List<SavedItem>>
    val kids: StateFlow<List<KidItem>>
    /** 연결된 가족 코드 (없으면 null → 차량 화면에서 코드를 입력해요) */
    val linkedCode: StateFlow<String?>
    suspend fun link(code: String): Result<Unit>
    fun unlink()
    suspend fun connect(): Result<Unit>
    suspend fun ack(id: String, ack: Ack, message: String = "", etaSec: Int = 0, distM: Int = 0)
    fun close()
}

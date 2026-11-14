package com.gabozayo.car.vehicle

import com.gabozayo.car.Gear
import com.gabozayo.car.LatLng
import kotlinx.coroutines.flow.StateFlow

/** 차량이 알려주는 값(주행 상태, 현재 위치) */
interface VehicleGateway {
    val gear: StateFlow<Gear>
    val position: StateFlow<LatLng?>
    /** Vehicle SDK 로 읽은 브레이크 페달 상태 (읽지 못했으면 null) */
    val brake: StateFlow<String?>
    suspend fun connect(): Result<Unit>
    /** 권한을 나중에 허용받았을 때 차량 데이터를 다시 읽어요 */
    fun refresh()
    fun release()
}

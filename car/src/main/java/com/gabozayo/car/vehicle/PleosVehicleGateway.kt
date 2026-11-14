// ─────────────────────────────────────────────────────────────
// Pleos Vehicle SDK + FusedLocation SDK (공식 문서 기준)
//  · Vehicle(context) → initialize() → (getXxx() 도메인 객체) → release()
//  · FusedLocation(context) → isSupported() → initialize() → registerFusedLocationCallback(listener) → unregister → release()
//  · FusedLocationListener: onChanged(location: LocationData?) / onError(exception: Exception)
//
// 주의
//  · Vehicle 도메인 값을 읽으려면 CAR_* 권한이 필요해요 (브레이크·주행모드·주행거리는 CAR_DRIVE). 권한이 없으면 onFailed 로 와요.
//  · getCurrentBrakePedalStatus 의 콜백 매개변수 이름·타입은 문서 본문을 읽지 못해 추정했어요. 다르면 이 자리만 고치세요.
//  · Vehicle SDK 문서에 기어(P/R/N/D) 항목이 없어서, 주행 중 여부는 FusedLocation 의 위치 변화로 계산해요.
//  · FusedLocation / LocationData 의 import 경로와 필드 이름은 문서에서 확인하지 못했어요. 빨간 글씨는 Alt+Enter 로 import 하세요.
// ─────────────────────────────────────────────────────────────
package com.gabozayo.car.vehicle

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.vehicle.Vehicle
import android.content.Context
import android.util.Log
import com.gabozayo.car.Gear
import com.gabozayo.car.LatLng
import com.gabozayo.car.distanceKm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PleosVehicleGateway(private val ctx: Context) : VehicleGateway {
    private var vehicle: Vehicle? = null
    private var fused: FusedLocation? = null

    private val g = MutableStateFlow(Gear.UNKNOWN)
    private val p = MutableStateFlow<LatLng?>(null)
    override val gear: StateFlow<Gear> = g
    override val position: StateFlow<LatLng?> = p
    private val b = MutableStateFlow<String?>(null)
    override val brake: StateFlow<String?> = b

    private var lastPos: LatLng? = null
    private var lastAt = 0L
    private var speedKmh = 0.0

    private val listener = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {
            val loc = location ?: return
            val now = System.currentTimeMillis()
            val cur = LatLng(loc.latitude, loc.longitude)
            p.value = cur
            val prev = lastPos
            if (prev != null && now - lastAt >= 500) {
                val v = distanceKm(prev, cur) / ((now - lastAt) / 3_600_000.0)   // km/h
                speedKmh = speedKmh * 0.6 + v * 0.4                              // 흔들림 완화
                lastPos = cur; lastAt = now
            } else if (prev == null) { lastPos = cur; lastAt = now }
            // 시속 5km 이상이면 주행 중, 아니면 정차로 봐요
            g.value = if (speedKmh >= 5.0) Gear.D else Gear.P
        }
        override fun onError(exception: Exception) { Log.w("Gabozayo", "FusedLocation 오류: ${exception.message}") }
    }

    override suspend fun connect(): Result<Unit> = runCatching {
        // 차량 상태 (Vehicle SDK)
        val v = Vehicle(ctx.applicationContext)
        v.initialize()
        vehicle = v
        readBrake()

        // 위치 정보 (FusedLocation SDK)
        val f = FusedLocation(ctx.applicationContext)
        check(f.isSupported()) { "이 환경은 FusedLocation 을 지원하지 않아요" }
        f.initialize()
        f.registerFusedLocationCallback(listener)
        fused = f
    }

    /** 브레이크 페달 상태 (CAR_DRIVE 권한). 권한을 받지 못했으면 건너뛰어요 (앱이 멈추지 않게) */
    private fun readBrake() {
        val v = vehicle ?: return
        if (ctx.checkSelfPermission("pleos.car.permission.CAR_DRIVE") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w("Gabozayo", "Vehicle: CAR_DRIVE 권한이 아직 허용되지 않았어요"); return
        }
        runCatching {
            v.getBrake().getCurrentBrakePedalStatus(
                onComplete = { status -> b.value = status.toString(); Log.d("Gabozayo", "Vehicle 브레이크 페달: $status") },
                onFailed = { e -> b.value = null; Log.w("Gabozayo", "Vehicle 브레이크 읽기 실패: ${e.message}") },
            )
        }.onFailure { Log.w("Gabozayo", "Vehicle 브레이크 호출 실패: ${it.message}") }
    }

    override fun refresh() = readBrake()

    override fun release() {
        fused?.let { it.unregisterFusedLocationCallback(listener); it.release() }
        fused = null
        vehicle?.release()
        vehicle = null
    }
}

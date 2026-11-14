package com.gabozayo.car

import org.json.JSONObject

/** 휴대폰 앱의 경로 선택과 같은 이름이에요 */
enum class RouteType(val key: String, val label: String) {
    RECOMMEND("recommend", "추천"), FASTEST("fastest", "최소 시간"), SHORTEST("shortest", "최단 거리"), FREE("free", "무료도로");
    companion object { fun of(k: String?) = entries.firstOrNull { it.key == k } ?: RECOMMEND }
}

/** 휴대폰이 보내는 목적지 */
data class Destination(
    val id: String, val name: String, val address: String,
    val lat: Double, val lng: Double, val route: RouteType, val sentAt: Long,
) {
    companion object {
        /** 휴대폰 → 차량 JSON:  {"id","name","address","lat","lng","route","ts"} */
        fun fromJson(s: String): Result<Destination> = runCatching {
            val o = JSONObject(s)
            Destination(o.getString("id"), o.getString("name"), o.optString("address"), o.getDouble("lat"), o.getDouble("lng"), RouteType.of(o.optString("route")), o.optLong("ts"))
        }
    }
}

/** 차량이 휴대폰에 돌려주는 진행 상태. 휴대폰 전송 화면의 4단계와 맞아요 */
enum class Ack(val key: String) {
    RECEIVED("received"),   // 1 차량이 목적지를 받았어요
    SEARCHING("searching"), // 2 Pleos 내비가 경로를 탐색하는 중
    READY("ready"),         // 3 차량 화면에서 [안내 시작]을 눌러 주세요
    GUIDING("guiding"),     // 안내 중
    FAILED("failed"),
}

/** 차량 → 휴대폰 JSON:  {"id","ack","message","ts"} */
fun ackJson(id: String, ack: Ack, message: String = ""): String =
    JSONObject().put("id", id).put("ack", ack.key).put("message", message).put("ts", System.currentTimeMillis()).toString()

enum class Stage { IDLE, RECEIVED, REQUESTING, READY, GUIDING, FAILED }

/** 변속 단계. D/R 은 주행 중으로 봐요 */
enum class Gear(val label: String) { P("정차"), R("후진"), N("중립"), D("주행 중"), UNKNOWN("확인 중") }

data class LatLng(val lat: Double, val lng: Double)

/** 두 지점 사이 직선거리(km) */
fun distanceKm(a: LatLng, b: LatLng): Double {
    val r = 6371.0
    val dLat = Math.toRadians(b.lat - a.lat); val dLng = Math.toRadians(b.lng - a.lng)
    val h = Math.sin(dLat / 2).let { it * it } + Math.cos(Math.toRadians(a.lat)) * Math.cos(Math.toRadians(b.lat)) * Math.sin(dLng / 2).let { it * it }
    return 2 * r * Math.asin(Math.sqrt(h))
}

/** 휴대폰에서 찜한 곳 (휴대폰 → 차량).  {"ts","places":[{"id","name","address","region","tag","lat","lng"}]} */
data class SavedPlace(val id: Int, val name: String, val address: String, val region: String, val tag: String, val lat: Double, val lng: Double, val img: String = "")

fun parseSaved(json: String): Result<List<SavedPlace>> = runCatching {
    val a = JSONObject(json).getJSONArray("places")
    (0 until a.length()).map {
        val o = a.getJSONObject(it)
        SavedPlace(o.getInt("id"), o.getString("name"), o.optString("address"), o.optString("region"), o.optString("tag"), o.optDouble("lat", 0.0), o.optDouble("lng", 0.0))
    }
}

/** 내비가 계산해 준 시간·거리 (etaSec 초, distM 미터) */
data class TripBrief(val etaSec: Int, val distM: Int)

/** 카드마다 내비에 물어볼 장소 */
class TripTarget(val key: String, val name: String, val address: String, val lat: Double, val lng: Double)

/** "약 12분 · 4.2km" */
fun formatTrip(etaSec: Int, distM: Int): String {
    val min = ((etaSec + 59) / 60).coerceAtLeast(1)
    val t = if (min >= 60) "${min / 60}시간 ${min % 60}분" else "${min}분"
    val d = if (distM < 1000) "${distM}m" else "%.1fkm".format(distM / 1000.0)
    return "약 $t · $d"
}

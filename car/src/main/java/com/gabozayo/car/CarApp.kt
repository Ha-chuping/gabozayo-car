package com.gabozayo.car

import android.app.Application

/**
 * 앱 전체에서 하나만 쓰는 차량 엔진(CarViewModel).
 * 화면(Activity)이 닫혀도 휴대폰 연결 · 내비 · 음성 연결이 끊기지 않게, 화면이 아니라 앱(Application)이 들고 있어요.
 */
class CarApp : Application() {
    val engine: CarViewModel by lazy { CarViewModel(this) }
}

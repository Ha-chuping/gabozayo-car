package com.gabozayo.car

import android.content.Context
import com.gabozayo.car.link.DestinationLink
import com.gabozayo.car.link.FirebaseDestinationLink
import com.gabozayo.core.FamilyApi
import com.gabozayo.car.navi.PleosNaviGateway
import com.gabozayo.car.navi.NaviGateway
import com.gabozayo.car.vehicle.PleosVehicleGateway
import com.gabozayo.car.vehicle.VehicleGateway

object Gateways {
    fun navi(ctx: Context): NaviGateway = PleosNaviGateway(ctx)
    fun link(ctx: Context): DestinationLink = FirebaseDestinationLink(ctx.applicationContext, FamilyApi(BuildConfig.FIREBASE_DB_URL))
    /** 주행 상태 · 현재 위치 (Pleos Vehicle / FusedLocation SDK) */
    fun vehicle(ctx: Context): VehicleGateway = PleosVehicleGateway(ctx)
}

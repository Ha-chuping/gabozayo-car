package com.gabozayo.car

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 차량 시동이 켜지고 시스템이 부팅되면 앱 화면을 열지 않아도 휴대폰 연결을 시작해요 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) runCatching { context.startForegroundService(Intent(context, KeepAliveService::class.java)) }
    }
}

package com.gabozayo.car

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder

/**
 * 화면을 닫고 내비를 보고 있어도 휴대폰에서 온 목적지를 받을 수 있게 앱을 계속 살려 두는 서비스예요.
 * (이게 없으면 차량 시스템이 백그라운드 앱을 멈춰서, 앱 화면을 열어야 그제서야 목적지를 받아요)
 */
class KeepAliveService : Service() {
    override fun onCreate() {
        super.onCreate()
        (application as CarApp).engine   // 엔진(휴대폰 연결, 내비, 음성)을 미리 깨워 둬요
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("gabozayo_link", "휴대폰 연결", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "gabozayo_link")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("가보자요")
            .setContentText("휴대폰에서 보낸 목적지를 기다리는 중")
            .setOngoing(true)
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}

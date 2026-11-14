package com.gabozayo.car

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import com.gabozayo.core.SavedItem

/**
 * 내비 화면이 앞에 있어도 보이도록, 근처 장소 제안을 알림(상단 팝업)으로도 띄워요.
 * 화면 위에 직접 그리는 방식(오버레이)은 Pleos 에 허용 API 가 없어서 알림으로 해요.
 * [가보자요!] 를 누르면 바로 경로를 요청하고, [다음에] 를 누르면 닫아요.
 */
object SuggestNotifier {
    private const val CHANNEL = "gabozayo_suggest"
    private const val ID = 2
    const val ACTION_GO = "com.gabozayo.car.SUGGEST_GO"
    const val ACTION_NO = "com.gabozayo.car.SUGGEST_NO"

    fun show(ctx: Context, p: SavedItem, distKm: Double?) {
        if (ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "근처 장소 제안", NotificationManager.IMPORTANCE_HIGH))
        fun action(a: String, code: Int) = PendingIntent.getBroadcast(ctx, code, Intent(a).setPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val icon = Icon.createWithResource(ctx, android.R.drawable.ic_menu_directions)
        val text = listOfNotNull(distKm?.let { "%.1fkm".format(it) }, p.why.takeIf { it.isNotBlank() }).joinToString(" · ").ifBlank { "아이가 좋아할 만한 곳이에요" }
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("근처에 ${p.name}")
            .setContentText(text)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(icon, "가보자요!", action(ACTION_GO, 1)).build())
            .addAction(Notification.Action.Builder(icon, "다음에", action(ACTION_NO, 2)).build())
            .build()
        nm.notify(ID, n)
    }

    fun cancel(ctx: Context) { ctx.getSystemService(NotificationManager::class.java).cancel(ID) }
}

/** 알림의 버튼을 받아요 */
class SuggestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val engine = (context.applicationContext as CarApp).engine
        when (intent.action) {
            SuggestNotifier.ACTION_GO -> engine.acceptSuggestion()
            SuggestNotifier.ACTION_NO -> engine.dismissSuggestion()
        }
        SuggestNotifier.cancel(context)
    }
}

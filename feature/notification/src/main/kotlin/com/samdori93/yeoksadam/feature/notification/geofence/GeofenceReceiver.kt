package com.samdori93.yeoksadam.feature.notification.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/** 지오펜스 진입 트랜지션 → 로컬 알림 + 스토어 적재. */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val names = event.triggeringGeofences
            ?.joinToString(", ") { it.requestId }
            ?.ifBlank { "유적지" } ?: "유적지"
        val title = "유적지 도착"
        val body = "$names 에 도착했습니다. 역사 인물을 만나보세요."

        AppNotifications.post(context, title, body)
        NotificationStore.add(NotificationStore.Event(title, body, System.currentTimeMillis()))
    }
}

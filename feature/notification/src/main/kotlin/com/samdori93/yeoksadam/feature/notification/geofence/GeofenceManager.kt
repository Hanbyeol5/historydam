package com.samdori93.yeoksadam.feature.notification.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/** 유적지 지오펜스 등록 (CLAUDE.md §10). */
object GeofenceManager {

    data class Site(val name: String, val lat: Double, val lng: Double, val radiusM: Float = 150f)

    val sites = listOf(
        Site("경복궁", 37.579617, 126.977041),
        Site("창덕궁", 37.582604, 126.991987),
        Site("수원화성", 37.288323, 127.014053, 300f),
        Site("덕수궁", 37.565804, 126.975144, 120f),
    )

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** 모든 유적지 지오펜스 등록. 권한 없으면 false. */
    @SuppressLint("MissingPermission")
    fun register(context: Context, onResult: (Boolean) -> Unit) {
        if (!hasLocationPermission(context)) {
            onResult(false)
            return
        }
        val geofences = sites.map { site ->
            Geofence.Builder()
                .setRequestId(site.name)
                .setCircularRegion(site.lat, site.lng, site.radiusM)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        LocationServices.getGeofencingClient(context)
            .addGeofences(request, pendingIntent(context))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}

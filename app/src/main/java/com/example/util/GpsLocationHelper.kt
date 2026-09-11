package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Toast

object GpsLocationHelper {

    @SuppressLint("MissingPermission")
    fun fetchCurrentGps(
        context: Context,
        onSuccess: (lat: Double, lng: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onError("خدمة تحديد المواقع غير متوفرة على هذا الجهاز")
            return
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            onError("يرجى تفعيل الـ GPS من إعدادات الهاتف")
            return
        }

        try {
            // Check last known location first for immediate responsiveness
            val lastGps = if (isGpsEnabled) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            val lastNetwork = if (isNetworkEnabled) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null

            val bestLocation = when {
                lastGps != null && lastNetwork != null -> if (lastGps.time > lastNetwork.time) lastGps else lastNetwork
                lastGps != null -> lastGps
                else -> lastNetwork
            }

            if (bestLocation != null) {
                onSuccess(bestLocation.latitude, bestLocation.longitude)
                return
            }

            // Otherwise request single update
            val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
            locationManager.requestSingleUpdate(
                provider,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        onSuccess(location.latitude, location.longitude)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                },
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            onError("تعذر الحصول على إحداثيات الموقع: ${e.localizedMessage}")
        }
    }

    fun openInGoogleMaps(
        context: Context,
        latitude: Double,
        longitude: Double,
        placeLabel: String = ""
    ) {
        try {
            val encodedLabel = Uri.encode(placeLabel.ifBlank { "موقع العميل" })
            val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
            mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to web browser Google Maps
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الخرائط: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openInGoogleMapsByAddress(context: Context, address: String) {
        if (address.isBlank()) return
        try {
            val encoded = Uri.encode(address)
            val uri = Uri.parse("geo:0,0?q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح العنوان على الخريطة", Toast.LENGTH_SHORT).show()
        }
    }
}

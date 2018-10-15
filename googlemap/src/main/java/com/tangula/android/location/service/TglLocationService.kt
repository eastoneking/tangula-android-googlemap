package com.tangula.android.location.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.*
import android.os.*
import android.support.annotation.DrawableRes
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.tangula.android.utils.GpsPermissionsUtils
import com.tangula.android.utils.PermissionUtils
import com.tangula.android.utils.SdkVersionDecides
import com.tangula.android.utils.ToastUtils
import org.apache.commons.lang3.StringUtils

data class ForegroundInfo(val serviceId: Int, val iconRes: Int, val channelId: String, val channelName: String?)

enum class RunningStatus {
    STOPED, STARTING, RUNNING, STOPING, UNAVAILABLE, PROVIDER_DISABLED
}

enum class BatteryStatus {
    HIGH, LOW, MIDDLE
}

enum class Scene {
    IN_DOOR, OUT_DOOR, UNKNOWN
}

val START_LOCK = object {}

class TglLocationServiceImpl(val locationManager: LocationManager, val service: TglLocationService) : Binder(), IInterface, LocationListener {

    var runningStatus = RunningStatus.STOPED

    var batteryStatus = BatteryStatus.HIGH

    var sceneStatus = Scene.OUT_DOOR

    val locationListeners = mutableListOf<(Location) -> Unit>()

    override fun asBinder(): IBinder {
        return this
    }

    fun start() {
        when (runningStatus) {
            RunningStatus.STOPED, RunningStatus.UNAVAILABLE, RunningStatus.PROVIDER_DISABLED -> {
                synchronized(START_LOCK) {
                    runningStatus = RunningStatus.STARTING
                    startLocationService()
                    runningStatus = RunningStatus.RUNNING
                }
            }
        }
    }

    val FUNCTON_HIGH_POWER_PROVIDER_BUILD = {
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.isAltitudeRequired = false
        criteria.isBearingRequired = false
        criteria.isCostAllowed = true
        criteria.powerRequirement = Criteria.POWER_HIGH
        locationManager.getBestProvider(criteria, true)
    }

    val FUNCTON_LOW_POWER_PROVIDER_BUILD = {
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_COARSE
        criteria.isAltitudeRequired = false
        criteria.isBearingRequired = false
        criteria.isCostAllowed = false
        criteria.powerRequirement = Criteria.POWER_LOW
        locationManager.getBestProvider(criteria, true)
    }

    var FUNCTION_IN_DOOR_PROVIDER_BUILDER = { defaultProvider: String ->
        val providerList = locationManager.getProviders(true)
        when {
            providerList.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> defaultProvider
        }
    }

    var FUNCTION_OUT_DOOR_PROVIDER_BUILDER = { defaultProvider: String ->
        val providerList = locationManager.getProviders(true)
        when {
            providerList.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> defaultProvider
        }
    }

    var FUNCTION_UNKNOWN_SCENE_PROVIDER_BUILDER = { defaultProvider: String ->
        defaultProvider
    }

    var FUNCTION_ANY_PROVIDER_BUILDER: () -> String = {
        val providerList = locationManager.getProviders(true)
        var res = ""
        if (providerList.isNotEmpty()) {
            res = providerList[0]
        }
        res
    }

    var FUNCTION_ON_NO_PROVIDER = {
        ToastUtils.showToastLong("Please Open Your GPS or Location Service ")
    }


    @SuppressLint("MissingPermission")
    private fun startLocationService() {

        var provider = when (batteryStatus) {
            BatteryStatus.HIGH, BatteryStatus.MIDDLE -> FUNCTON_HIGH_POWER_PROVIDER_BUILD()
            else -> FUNCTON_LOW_POWER_PROVIDER_BUILD()
        }


        provider = when (sceneStatus) {
            Scene.IN_DOOR -> FUNCTION_IN_DOOR_PROVIDER_BUILDER(provider)
            Scene.OUT_DOOR -> FUNCTION_OUT_DOOR_PROVIDER_BUILDER(provider)
            else -> FUNCTION_UNKNOWN_SCENE_PROVIDER_BUILDER(provider)
        }

        if (StringUtils.isBlank(provider)) {
            provider = FUNCTION_ANY_PROVIDER_BUILDER()
        }

        if (StringUtils.isNotBlank(provider)) {
            GpsPermissionsUtils.whenHasGpsPermissionsNotWithRequestPermissions {
                locationManager.requestLocationUpdates(provider, 5000, 5.0f, this)
            }
        } else {
            FUNCTION_ON_NO_PROVIDER()
        }
    }

    fun onLowMemory() {
    }

    fun onTrimMemory(level: Int) {
    }

    fun onBatteryChange(old: BatteryStatus) {
            stop()
            start()
    }

    fun stop() {
        synchronized(START_LOCK) {
            runningStatus = RunningStatus.STOPING
            service.stopSelf()
            runningStatus = RunningStatus.STOPED
        }
    }

    fun bindMap(map: GoogleMap) {
        map.setLocationSource(object : LocationSource {
            var listenLoc: ((Location) -> Unit)? = null
            override fun deactivate() {
                listenLoc?.also {
                    locationListeners.remove(it)
                }
            }

            override fun activate(p0: LocationSource.OnLocationChangedListener?) {
                listenLoc = { loc ->
                    p0?.onLocationChanged(loc)
                }
                locationListeners.add(listenLoc!!)
            }
        })
    }

    // location listener


    override fun onLocationChanged(location: Location?) {
        location?.also { loc ->
            locationListeners.forEach { callback -> callback(loc) }
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        when (status) {
            LocationProvider.AVAILABLE -> {
                service.startService()
            }
            LocationProvider.OUT_OF_SERVICE, LocationProvider.TEMPORARILY_UNAVAILABLE -> {
                synchronized(START_LOCK) {
                    stop()
                    runningStatus = RunningStatus.UNAVAILABLE
                }
            }
        }
    }

    override fun onProviderEnabled(provider: String?) {
        service.startService()
    }

    override fun onProviderDisabled(provider: String?) {
        synchronized(START_LOCK) {
            stop()
            runningStatus = RunningStatus.PROVIDER_DISABLED
        }
    }

}

open class TglLocationService : Service() {

    /**
     * 是否显示在前台.
     */
    var foreGroundInfo: ForegroundInfo? = null

    var isConsideringBattery = true

    lateinit var instance: TglLocationServiceImpl

    var lowBatteryPercent = 15

    override fun onBind(intent: Intent?): IBinder {
        return instance.asBinder()
    }

    override fun onCreate() {
        super.onCreate()
        instance = TglLocationServiceImpl(applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager, this)

        if (isConsideringBattery) {

            this.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
                    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100

                    val percent = Math.ceil(level.toDouble() / scale.toDouble() * 100)
                    val old = instance.batteryStatus
                    when (percent) {
                        in 0..lowBatteryPercent -> instance.batteryStatus = BatteryStatus.LOW
                        else -> instance.batteryStatus = BatteryStatus.HIGH
                    }
                    if (old != instance.batteryStatus) {
                        instance.onBatteryChange(old)
                    }
                }
            }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        foreGroundInfo?.also {
            startForeground(it.serviceId, NotificationBuilder().buildNotification(applicationContext, it.iconRes, it.channelId, it.channelName))
        }
        instance.start()
        return START_STICKY
    }

    override fun onLowMemory() {
        super.onLowMemory()
        instance.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        instance.onTrimMemory(level)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance.stop()
    }

    fun startService() {
        GpsPermissionsUtils.whenHasGpsPermissionsNotWithRequestPermissions {
            applicationContext.startService(Intent(applicationContext, this::class.java))
        }
    }

}


class NotificationBuilder {
    @SuppressLint("NewApi")
    fun buildNotification(ctx: Context, @DrawableRes iconResId: Int, channelId: String, channelName: String?): Notification {

        val manager: NotificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder: Notification.Builder

        builder = if (SdkVersionDecides.afterSdk26A8d0()) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
            Notification.Builder(ctx, channelId)
        } else {
            Notification.Builder(ctx)
        }

        SdkVersionDecides.afterSdk20A4d4W {
            builder.setLocalOnly(true)
        }

        SdkVersionDecides.afterSdk17A4d2 {
            builder.setShowWhen(false)
        }

        builder.setContentTitle(channelId).setContentText(channelName)
                .setSmallIcon(iconResId).setAutoCancel(false)

        return builder.build()
    }
}
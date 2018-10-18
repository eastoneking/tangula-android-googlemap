package com.tangula.android.location.service

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.location.*
import android.os.BatteryManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.tangula.android.base.IServiceBinder
import com.tangula.android.base.TglLocalServiceBinder
import com.tangula.android.base.TglService
import com.tangula.android.googlemap.GMapUtils
import com.tangula.android.utils.ApplicationUtils
import com.tangula.android.utils.TaskUtils
import com.tangula.android.utils.ToastUtils
import org.apache.commons.lang3.StringUtils


/**
 * 服务运行状态.
 */
enum class RunningStatus {
    /**
     * 停止或尚未启动.
     */
    STOPED,
    /**
     * 启动中.
     */
    STARTING,
    /**
     * 正在运行.
     */
    RUNNING,
    /**
     * 正在停止.
     */
    STOPING,
    /**
     * 展示不可用.
     */
    UNAVAILABLE,
    /**
     * 功能提供者被禁用.
     */
    PROVIDER_DISABLED
}

/**
 * 电池状态.
 */
enum class BatteryStatus {
    /**
     * 高电量.
     */
    HIGH,
    /**
     * 中等电量.
     */
    MIDDLE,
    /**
     * 低电量.
     */
    LOW
}

/**
 * 应用场景.
 */
enum class Scene {
    /**
     * 室内使用.
     */
    IN_DOOR,
    /**
     * 户外使用.
     */
    OUT_DOOR,
    /**
     * 没有特殊特征.
     */
    UNKNOWN
}

/**
 * 服务启动过程的锁对象.
 */
val START_LOCK = object {}

interface ILocationService: IServiceBinder {
    /**
     * 运行状态.
     */
    var runningStatus: RunningStatus

    /**
     * 电池状态.
     */
    var batteryStatus: BatteryStatus

    /**
     * 应用的业务场景.
     */
    var sceneStatus: Scene

    /**
     * 位置变化回调函数.
     *
     * **Loc**ation **Ch**an**g**e**d** **C**all**b**ack **F**unction**s**.
     */
    val locChgdCbFs: MutableList<(Location) -> Unit>

    /**
     * 绑定地图中心到当前位置.
     */
    fun bindMapFlowingCurrentLocation(map: GoogleMap)

    /**
     * 接触地图中心和当前位置的绑定.
     */
    fun unbindMapFlowingCurrentLocation(map: GoogleMap)

}


class TglLocationServiceBinder(val locationManager: LocationManager)
    : TglLocalServiceBinder(), LocationListener, ILocationService {

    override var runningStatus = RunningStatus.STOPED

    override var batteryStatus = BatteryStatus.HIGH

    override var sceneStatus = Scene.OUT_DOOR

    override val locChgdCbFs = mutableListOf<(Location) -> Unit>()

    override fun onStart(): Int {

        when (runningStatus) {
            RunningStatus.STOPED, RunningStatus.UNAVAILABLE, RunningStatus.PROVIDER_DISABLED -> {
                synchronized(START_LOCK) {
                    when (runningStatus) {
                        RunningStatus.STOPED, RunningStatus.UNAVAILABLE, RunningStatus.PROVIDER_DISABLED -> {
                            runningStatus = RunningStatus.STARTING
                            startLocationUpdates()
                            runningStatus = RunningStatus.RUNNING
                        }
                        else->{}
                    }
                }
            }
            else->{}
        }

        return super.onStart()
    }


    /**
     * 高精度，高能耗，有成本的定位方式.
     */
    var FUNCTON_HIGH_POWER_PROVIDER_BUILD = {
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.isAltitudeRequired = false
        criteria.isBearingRequired = false
        criteria.isCostAllowed = true
        criteria.powerRequirement = Criteria.POWER_HIGH
        locationManager.getBestProvider(criteria, true)
    }

    /**
     * 低精度，低能耗，无成本的定位方式.
     */
    var FUNCTON_LOW_POWER_PROVIDER_BUILD = {
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_COARSE
        criteria.isAltitudeRequired = false
        criteria.isBearingRequired = false
        criteria.isCostAllowed = false
        criteria.powerRequirement = Criteria.POWER_LOW
        locationManager.getBestProvider(criteria, true)
    }

    /**
     * 室内定位方式.
     */
    var FUNCTION_IN_DOOR_PROVIDER_BUILDER = { defaultProvider: String ->
        val providerList = locationManager.getProviders(true)
        when {
            providerList.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> defaultProvider
        }
    }
    /**
     * 户外定位方式.
     */
    var FUNCTION_OUT_DOOR_PROVIDER_BUILDER = { defaultProvider: String ->
        val providerList = locationManager.getProviders(true)
        when {
            providerList.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> defaultProvider
        }
    }

    /**
     * 无特殊场景定位方式.
     */
    var FUNCTION_UNKNOWN_SCENE_PROVIDER_BUILDER = { defaultProvider: String ->
        defaultProvider
    }

    /**
     * 任何一种设备支持的定位方式.
     */
    var FUNCTION_ANY_PROVIDER_BUILDER: () -> String = {
        val providerList = locationManager.getProviders(true)
        var res = ""
        if (providerList.isNotEmpty()) {
            res = providerList[0]
        }
        res
    }

    /**
     * 任何定位方式都不支持是的处理方法.
     */
    var FUNCTION_ON_NO_PROVIDER = {
        ToastUtils.showToastLong("Please Open Your GPS or Location Service ")
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {

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
            locationManager.requestLocationUpdates(provider, 5000, 5.0f, this)
        } else {
            FUNCTION_ON_NO_PROVIDER()
        }
    }

    fun onBatteryChange(old: BatteryStatus) {
        close()
        onStart()
    }

    override fun close() {
        synchronized(START_LOCK) {
            runningStatus = RunningStatus.STOPING
            this.serviceInstance?.stopSelf()
            runningStatus = RunningStatus.STOPED
        }
    }


    var listenLoc: ((Location) -> Unit)? = null

    override fun bindMapFlowingCurrentLocation(map: GoogleMap) {
        map.setLocationSource(object : LocationSource {
            override fun deactivate() {
                listenLoc?.also {
                    locChgdCbFs.remove(it)
                }
            }

            override fun activate(p0: LocationSource.OnLocationChangedListener?) {
                listenLoc = { loc ->
                    TaskUtils.runInUiThread{
                        Log.v("console","bind map flowing listener loc:[${loc.latitude},${loc.longitude}]")
                        GMapUtils.moveTo(map, loc)
                    }
                    p0?.onLocationChanged(loc)
                }
                locChgdCbFs.add(listenLoc!!)
            }
        })
    }

    override fun unbindMapFlowingCurrentLocation(map: GoogleMap) {
        locChgdCbFs.remove(listenLoc)
    }

    // location listener
    /**
     * 位置更新时调用所有回调函数.
     */
    override fun onLocationChanged(location: Location?) {
        Log.v("console", "on location chaged:${location?.latitude}, ${location?.longitude}")
        location?.also { loc ->
            locChgdCbFs.forEach { callback -> callback(loc) }
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        when (status) {
            LocationProvider.AVAILABLE -> {
                TglService.startService(null, TglLocationService::class.java)
            }
            LocationProvider.OUT_OF_SERVICE, LocationProvider.TEMPORARILY_UNAVAILABLE -> {
                synchronized(START_LOCK) {
                    close()
                    runningStatus = RunningStatus.UNAVAILABLE
                }
            }
        }
    }

    override fun onProviderEnabled(provider: String?) {
        TglService.startService(null, TglLocationService::class.java)
    }

    override fun onProviderDisabled(provider: String?) {
        synchronized(START_LOCK) {
            close()
            runningStatus = RunningStatus.PROVIDER_DISABLED
        }
    }

}


open class TglLocationService : TglService<TglLocationServiceBinder>() {

    companion object {
        /**
         * 启动位置服务.
         *
         * @param[act] 如果为空，用[ApplicationUtils.APP]作为上下文启动服务.
         */
        fun startLocationService(act: Activity?){
            TglService.startService(act, TglLocationService::class.java)
        }

        /**
         * 绑定到定位服务.
         * @param[ctx] 如果未空使用ApplicationUtils.APP]作为上下文启动服务.
         * @param[onBindSuccess] 绑定成功时的回调函数，在回调函数中，可以通过参数获得服务对象.
         * @param[onDisConnnect] 断开服务时要执行的方法.
         * @param[onBindFail] 绑定时发生错误.
         */
        fun bindLocationService(ctx:Context?, onBindSuccess:(TglLocationServiceBinder)->Unit,
                                onDisConnnect:((ComponentName?)->Unit)?,
                                onBindFail:(()->Unit)?): ServiceConnection {
            return TglService.bind2NormalService(ctx?:ApplicationUtils.APP,
                    TglLocationService::class.java, onBindSuccess, onDisConnnect, onBindFail)
        }

    }

    /**
     * 是否考虑电池状态.
     */
    var isConsideringBattery = true
    /**
     * 电池电量小于此百分比时切换到省电模式.
     */
    var lowBatteryPercent = 15


    override fun onCreateBinder(intent: Intent?, flags: Int, startId: Int): TglLocationServiceBinder {
        return TglLocationServiceBinder(applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
    }

    override fun onCreate() {

        super.onCreate()

        if (isConsideringBattery) {

            this.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
                    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100

                    val percent = Math.ceil(level.toDouble() / scale.toDouble() * 100)
                    val old = instance?.batteryStatus
                    when (percent) {
                        in 0..lowBatteryPercent -> instance?.batteryStatus = BatteryStatus.LOW
                        else -> instance?.batteryStatus = BatteryStatus.HIGH
                    }
                    if (old != instance?.batteryStatus) {
                        this@TglLocationService.instance?.onBatteryChange(old!!)
                    }
                }
            }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.v("console", "service, onBind")
        return super.onBind(intent)
    }

}


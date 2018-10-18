package com.tangula.android.googlemap

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.tangula.android.base.TglBasicActivity
import com.tangula.android.location.service.ILocationService
import com.tangula.android.location.service.TglLocationService
import com.tangula.android.utils.LocationPermsUtils
import com.tangula.android.utils.ToastUtils

open class TglBasicGoogleMapActivity :  TglBasicActivity(), OnMapReadyCallback {

    companion object {
        var FUNC_NOT_GRANT_LOCATION_PREMS = {
            ToastUtils.showToastLong("Please grant GPS permissions.")
        }
    }

    /**
     * google map对象.
     */
    private lateinit var mMap: GoogleMap

    /**
     *
     */
    var enableMyLocationButton = true

    var enableZoomControllers = true

    var enableZoomGestures = true

    var enableScrollGestures = true

    var enableCompass = true

    var enableMapToolbar = true

    var enableIndoorLevelPicker = true

    var enableRotateGestures=true

    var enableTitleGestures = true


    /**
     * 跟随模式.
     * 地图的中心位置保持在用户当前位置.
     */
    var flowingMode = true

    /**
     * 地图就绪时是否启动定位服务.
     */
    var startLocationServiceWhenMapReady = true


    /**
     * 获取地图Acvitity的布局文件资源Id.
     */
    var supplier4ContentLayoutResId = {0}

    /**
     * 获取地图Fragment元素的资源Id.
     */
    var supplier4MapFragmentResId={0}

    var locationService: ILocationService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(supplier4ContentLayoutResId())
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(supplier4MapFragmentResId()) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateMapUiStings()

        if(startLocationServiceWhenMapReady&&!flowingMode){
            TglLocationService.startLocationService(this)
        }

        if(flowingMode){
            Log.v("console", "activity in flowing mode")
            TglLocationService.bindLocationService(this, {serv->
                    Log.v("console", "location service bind callback running")
                    locationService = serv
                    if(flowingMode) {
                        serv.bindMapFlowingCurrentLocation(mMap)
                    }
                    onLocationServiceBinded(serv)
            }, {_->
                Log.v("console", "location service unbinded")
                locationService?.unbindMapFlowingCurrentLocation(mMap)
            }, {
                Log.v("console", "bind fail")
            })

        }

    }

    protected fun onLocationServiceBinded(locationService: ILocationService) {
        Log.v("console", "after Location Service Binded")
    }

    @SuppressLint("MissingPermission")
    fun updateMapUiStings(){
        mMap.uiSettings.apply {
            isZoomControlsEnabled = enableZoomControllers
            isZoomGesturesEnabled = enableZoomGestures
            isScrollGesturesEnabled = enableScrollGestures
            isCompassEnabled = enableCompass
            isMyLocationButtonEnabled = enableMyLocationButton
            isMapToolbarEnabled = enableMapToolbar
            isIndoorLevelPickerEnabled = enableIndoorLevelPicker
            isRotateGesturesEnabled = enableRotateGestures
            isTiltGesturesEnabled = enableTitleGestures
        }

        LocationPermsUtils.canAccessLocation(this,{
            mMap.isMyLocationEnabled = enableMyLocationButton
        }, FUNC_NOT_GRANT_LOCATION_PREMS)

    }

    fun moveTo(pos:LatLng){
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pos))
    }

    fun moveTo(loc:Location){
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(loc.latitude, loc.longitude)))
    }

    fun distance(from:Location, to:Location): Float {
        return from.distanceTo(to)
    }

}
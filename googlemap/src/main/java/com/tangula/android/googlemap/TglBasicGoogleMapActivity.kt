package com.tangula.android.googlemap

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.tangula.android.base.TglBasicActivity
import com.tangula.android.utils.GpsPermissionsUtils
import com.tangula.android.utils.PermissionUtils

class TglBasicGoogleMapActivity :  TglBasicActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    var enableMyLocationButton = true
    var enableZoomControllers = true
    var enableZoomGestures = true
    var enableScrollGestures = true
    var enableCompass = true
    var enableMapToolbar = true
    var enableIndoorLevelPicker = true
    var enableRotateGestures=true
    var enableTitleGestures = true




    var contentLayoutResIdSupplier = {0}

    var mapFragmentResIdSupplier={0}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentLayoutResIdSupplier())
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(mapFragmentResIdSupplier()) as SupportMapFragment
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

        GpsPermissionsUtils.whenHasGpsPermissions(this){
            mMap.isMyLocationEnabled = enableMyLocationButton
        }

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
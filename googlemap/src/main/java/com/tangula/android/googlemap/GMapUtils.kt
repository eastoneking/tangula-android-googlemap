package com.tangula.android.googlemap

import android.location.Location
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

class GMapUtils {

    companion object {
        fun moveTo(mMap:GoogleMap, pos: LatLng){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(pos))
        }

        fun moveTo(mMap:GoogleMap, loc: Location){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(loc.latitude, loc.longitude)))
        }

        fun distance(mMap:GoogleMap, from: Location, to: Location): Float {
            return from.distanceTo(to)
        }
    }
}
package com.tangula.android.location.utils

import android.location.Location
import android.location.LocationManager
import com.google.android.gms.maps.model.LatLng

class LocationUtils{

    companion object {

        fun toLocation(loc:LatLng):Location{
            return Location(LocationManager.GPS_PROVIDER).apply {
                latitude = loc.latitude
                longitude = loc.longitude
            }
        }

        fun toLatLng(loc:Location):LatLng{
            return LatLng(loc.latitude, loc.longitude)
        }

        fun distance(from: Location, to: Location): Float {
            return from.distanceTo(to)
        }
    }

}
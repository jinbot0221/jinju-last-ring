package com.jinju.lastring

import kotlin.math.*

data class SimulatedPosition(val latitude: Double, val longitude: Double)
/** Great-circle movement; bearings: north=0, east=90, south=180, west=270. */
fun moveSimulatedPosition(position: SimulatedPosition, bearing: Double, meters: Double): SimulatedPosition {
    val angular = meters / 6371000.0
    val latitude = Math.toRadians(position.latitude)
    val longitude = Math.toRadians(position.longitude)
    val heading = Math.toRadians(bearing)
    val nextLatitude = asin((sin(latitude)*cos(angular)+cos(latitude)*sin(angular)*cos(heading)).coerceIn(-1.0,1.0))
    val nextLongitude = longitude + atan2(sin(heading)*sin(angular)*cos(latitude),cos(angular)-sin(latitude)*sin(nextLatitude))
    return SimulatedPosition(Math.toDegrees(nextLatitude),((Math.toDegrees(nextLongitude)+540)%360)-180)
}

package com.jinju.lastring

const val ARRIVAL_RADIUS_METERS = 80.0
const val MAX_LOCATION_ACCURACY_METERS = 50.0
const val MAX_LOCATION_AGE_NANOS = 60_000_000_000L

/** Shared by the field map and puzzle, so both use identical arrival rules. */
fun canArrive(precise: Boolean, distance: Double?, accuracy: Float?, ageNanos: Long?): Boolean =
    precise && distance != null && distance.isFinite() && distance >= 0 &&
        distance <= ARRIVAL_RADIUS_METERS && accuracy != null && accuracy.isFinite() &&
        accuracy >= 0 && accuracy <= MAX_LOCATION_ACCURACY_METERS &&
        ageNanos != null && ageNanos in 0..MAX_LOCATION_AGE_NANOS

package com.jinju.lastring

/** Only the next unfinished chapter can trigger; selection and GPS refreshes are irrelevant. */
fun spotEventToShow(completed: Int, spotCount: Int, lastShownChapter: Int, arrived: Boolean): Int? =
    completed.takeIf { it in 0 until spotCount && lastShownChapter != completed && arrived }

/** A genuine fresh outside fix rearms an unfinished event; GPS loss never does. */
fun canRearmSpotEvent(precise: Boolean, distance: Double?, accuracy: Float?, ageNanos: Long?): Boolean =
    canArrive(precise,0.0,accuracy,ageNanos) && distance != null && distance.isFinite() && distance>ARRIVAL_RADIUS_METERS

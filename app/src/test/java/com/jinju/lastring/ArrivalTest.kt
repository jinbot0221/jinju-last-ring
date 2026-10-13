package com.jinju.lastring

import org.junit.Assert.*
import org.junit.Test

class ArrivalTest {
    @Test fun acceptsFreshAccurateFixInsideBoundary() {
        assertTrue(canArrive(true, 80.0, 50f, MAX_LOCATION_AGE_NANOS))
        assertTrue(canArrive(true, 0.0, 5f, 0L))
    }
    @Test fun walkingAcrossBoundaryUnlocksAndLeavingLocksAgain() {
        val distances = listOf(120.0, 81.0, 79.0, 20.0, 81.0)
        assertEquals(listOf(false, false, true, true, false), distances.map { canArrive(true, it, 10f, 1_000_000_000L) })
    }
    @Test fun coarseStaleInaccurateAndMissingFixNeverUnlock() {
        assertFalse(canArrive(false, 0.0, 5f, 0L))
        assertFalse(canArrive(true, 0.0, 51f, 0L))
        assertFalse(canArrive(true, 0.0, 5f, MAX_LOCATION_AGE_NANOS + 1))
        assertFalse(canArrive(true, 0.0, 5f, -1L))
        assertFalse(canArrive(true, null, null, null))
        assertFalse(canArrive(true, Double.NaN, 5f, 0L))
    }
}

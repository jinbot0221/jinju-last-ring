package com.jinju.lastring

import org.junit.Assert.*
import org.junit.Test

class SimulatedPositionTest {
    @Test fun directionsMoveExpectedDistance() {
        val start=SimulatedPosition(35.1929,128.0827)
        for(bearing in listOf(0.0,90.0,180.0,270.0)) {
            val end=moveSimulatedPosition(start,bearing,25.0)
            assertEquals(25.0,distanceMeters(start.latitude,start.longitude,end.latitude,end.longitude),0.01)
            when(bearing) {
                0.0 -> assertTrue(end.latitude>start.latitude)
                90.0 -> assertTrue(end.longitude>start.longitude)
                180.0 -> assertTrue(end.latitude<start.latitude)
                270.0 -> assertTrue(end.longitude<start.longitude)
            }
        }
    }
    @Test fun buttonsCrossArrivalBoundaryAndCanLeaveAgain() {
        val target=SimulatedPosition(35.1929,128.0827)
        var player=moveSimulatedPosition(target,0.0,170.0)
        fun arrived()=canArrive(true,distanceMeters(player.latitude,player.longitude,target.latitude,target.longitude),5f,0L)
        assertFalse(arrived())
        repeat(4) { player=moveSimulatedPosition(player,180.0,25.0) }
        assertTrue(arrived())
        player=moveSimulatedPosition(player,0.0,25.0)
        assertFalse(arrived())
    }
}

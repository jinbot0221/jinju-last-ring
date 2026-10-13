package com.jinju.lastring

import org.junit.Assert.*
import org.junit.Test

class SpotEventTest {
    @Test fun enteringNextSpotTriggersExactlyOnce() {
        assertNull(spotEventToShow(0,4,-1,false))
        assertEquals(0,spotEventToShow(0,4,-1,true))
        assertNull(spotEventToShow(0,4,0,true))
        assertNull(spotEventToShow(0,4,0,false))
        assertNull(spotEventToShow(0,4,0,true))
    }
    @Test fun eachNewChapterCanTriggerButFinishedGameCannot() {
        for(index in 0..3) assertEquals(index,spotEventToShow(index,4,-1,true))
        assertNull(spotEventToShow(4,4,-1,true))
        assertNull(spotEventToShow(-1,4,-1,true))
    }
    @Test fun restoringPreviousChaptersEventDoesNotBlockNextSpot() {
        // Map saveable state from chapter 0 is restored after solving its quiz.
        assertEquals(1,spotEventToShow(1,4,0,true))
        assertEquals(2,spotEventToShow(2,4,1,true))
        assertEquals(3,spotEventToShow(3,4,2,true))
        assertNull(spotEventToShow(1,4,1,true))
    }
    @Test fun allFourActualSpotsTriggerAfterPreviousChapterWasShown() {
        var lastShown=-1
        spots.forEachIndexed { index, spot ->
            val center=SimulatedPosition(spot.latitude,spot.longitude)
            var player=moveSimulatedPosition(center,0.0,170.0)
            fun isInside()=canArrive(true,distanceMeters(player.latitude,player.longitude,center.latitude,center.longitude),5f,0L)
            assertNull(spotEventToShow(index,spots.size,lastShown,isInside()))
            repeat(4) { player=moveSimulatedPosition(player,180.0,25.0) }
            assertEquals(index,spotEventToShow(index,spots.size,lastShown,isInside()))
            lastShown=index
            assertNull(spotEventToShow(index,spots.size,lastShown,isInside()))
        }
    }
    @Test fun approximateStaleAndDistantFixesNeverTriggerEvents() {
        for(arrived in listOf(canArrive(false,0.0,5f,0L),canArrive(true,0.0,5f,MAX_LOCATION_AGE_NANOS+1),canArrive(true,81.0,5f,0L))) {
            assertNull(spotEventToShow(0,4,-1,arrived))
        }
    }
}

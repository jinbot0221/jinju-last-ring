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
    @Test fun allActualSpotsTriggerAfterPreviousChapterWasShown() {
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
    @Test fun unfinishedEventFiresAgainAfterLeavingAndReturning() {
        var lastShown=-1
        repeat(3) {
            assertEquals(0,spotEventToShow(0,spots.size,lastShown,true))
            lastShown=0 // dismissing the event leaves progress unfinished
            assertNull(spotEventToShow(0,spots.size,lastShown,true))
            assertTrue(canRearmSpotEvent(true,81.0,5f,0L))
            if(canRearmSpotEvent(true,81.0,5f,0L)) lastShown=-1
            assertNull(spotEventToShow(0,spots.size,lastShown,false))
        }
    }
    @Test fun missingOrBadGpsDoesNotCountAsLeaving() {
        assertFalse(canRearmSpotEvent(false,120.0,5f,0L))
        assertFalse(canRearmSpotEvent(true,120.0,51f,0L))
        assertFalse(canRearmSpotEvent(true,120.0,5f,MAX_LOCATION_AGE_NANOS+1))
        assertFalse(canRearmSpotEvent(true,null,null,null))
        assertFalse(canRearmSpotEvent(true,80.0,5f,0L))
        assertFalse(canRearmSpotEvent(true,Double.NaN,5f,0L))
    }
    @Test fun completedSpotCannotReopenWhenRevisited() {
        val old=spots[0]; val next=spots[1]
        val arrivedAtNext=canArrive(true,distanceMeters(old.latitude,old.longitude,next.latitude,next.longitude),5f,0L)
        assertFalse(arrivedAtNext)
        assertNull(spotEventToShow(1,spots.size,-1,arrivedAtNext))
        assertEquals(1,spotEventToShow(1,spots.size,-1,true))
    }
    @Test fun approximateStaleAndDistantFixesNeverTriggerEvents() {
        for(arrived in listOf(canArrive(false,0.0,5f,0L),canArrive(true,0.0,5f,MAX_LOCATION_AGE_NANOS+1),canArrive(true,81.0,5f,0L))) {
            assertNull(spotEventToShow(0,4,-1,arrived))
        }
    }
}

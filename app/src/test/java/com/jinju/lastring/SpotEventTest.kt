package com.jinju.lastring

import org.junit.Assert.*
import org.junit.Test

class SpotEventTest {
    @Test fun enteringNextSpotTriggersExactlyOnce() {
        assertNull(spotEventToShow(0,4,false,false))
        assertEquals(0,spotEventToShow(0,4,false,true))
        assertNull(spotEventToShow(0,4,true,true))
        assertNull(spotEventToShow(0,4,true,false))
        assertNull(spotEventToShow(0,4,true,true))
    }
    @Test fun eachNewChapterCanTriggerButFinishedGameCannot() {
        for(index in 0..3) assertEquals(index,spotEventToShow(index,4,false,true))
        assertNull(spotEventToShow(4,4,false,true))
        assertNull(spotEventToShow(-1,4,false,true))
    }
    @Test fun approximateStaleAndDistantFixesNeverTriggerEvents() {
        for(arrived in listOf(canArrive(false,0.0,5f,0L),canArrive(true,0.0,5f,MAX_LOCATION_AGE_NANOS+1),canArrive(true,81.0,5f,0L))) {
            assertNull(spotEventToShow(0,4,false,arrived))
        }
    }
}

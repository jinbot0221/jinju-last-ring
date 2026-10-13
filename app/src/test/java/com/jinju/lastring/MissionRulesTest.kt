package com.jinju.lastring

import org.junit.Assert.*
import org.junit.Test

class MissionRulesTest {
    @Test fun orderingPreservesCardsAndCanReachHistoricalOrder() {
        val original=listOf(2,1,0)
        val reordered=swapMissionItems(original,0,2)
        assertEquals(listOf(0,1,2),reordered)
        assertEquals(listOf(2,1,0),original)
        assertTrue(answerMatches(reordered.joinToString("|"),spots[2].answer))
    }
    @Test fun deliveryRequiresTheEndOfTheSlider() {
        assertFalse(canDeliverRing(0f)); assertFalse(canDeliverRing(0.94f))
        assertTrue(canDeliverRing(0.95f)); assertTrue(canDeliverRing(1f))
        assertFalse(canDeliverRing(Float.NaN)); assertFalse(canDeliverRing(2f))
    }
    @Test fun ocrKeywordGateRejectsMissingOrUnrelatedText() {
        assertTrue(isMissionSignText("진 주 성 안내"))
        assertTrue(isMissionSignText("성벽은 방어 시설"))
        assertFalse(isMissionSignText("")); assertFalse(isMissionSignText("식당 메뉴"))
    }
}

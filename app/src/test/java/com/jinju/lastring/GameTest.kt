package com.jinju.lastring

import org.junit.Assert.*
import org.junit.Test

class GameTest {
    @Test fun distanceUsesMeters() {
        assertEquals(0.0, distanceMeters(35.19,128.08,35.19,128.08),0.01)
        assertEquals(111.2, distanceMeters(0.0,0.0,0.001,0.0),0.2)
        assertTrue(distanceMeters(35.19,128.08,35.2,128.08)>80)
    }
    @Test fun answersAllowWhitespaceButRejectWrongOrEmptyAnswers() {
        assertTrue(answerMatches(" 용 기 약 ","용기약"))
        assertFalse(answerMatches("기억","약속"))
        assertFalse(answerMatches("","약속"))
    }
    @Test fun everyChapterCanBeSolvedAndHasAReward() {
        assertEquals(4,spots.size)
        spots.forEach { assertTrue(answerMatches(it.answer,it.answer)); assertTrue(it.hint.isNotBlank()); assertTrue(it.relic.isNotBlank()) }
    }
}

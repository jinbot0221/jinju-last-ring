package com.jinju.lastring

/** Only the next unfinished chapter can trigger; selection and GPS refreshes are irrelevant. */
fun spotEventToShow(completed: Int, spotCount: Int, lastShownChapter: Int, arrived: Boolean): Int? =
    completed.takeIf { it in 0 until spotCount && lastShownChapter != completed && arrived }

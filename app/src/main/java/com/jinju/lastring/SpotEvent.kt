package com.jinju.lastring

/** Only the next unfinished chapter can trigger; selection and GPS refreshes are irrelevant. */
fun spotEventToShow(completed: Int, spotCount: Int, alreadyShown: Boolean, arrived: Boolean): Int? =
    completed.takeIf { it in 0 until spotCount && !alreadyShown && arrived }

package com.jinju.lastring

fun swapMissionItems(items: List<Int>, from: Int, to: Int): List<Int> {
    require(from in items.indices && to in items.indices)
    return items.toMutableList().apply { val saved=this[from]; this[from]=this[to]; this[to]=saved }
}
fun canDeliverRing(progress: Float) = progress.isFinite() && progress in 0.95f..1f

fun isMissionSignText(text: String): Boolean {
    val normalized=text.filter(Char::isLetterOrDigit)
    return listOf("진주성","성벽","성곽").any { it in normalized }
}

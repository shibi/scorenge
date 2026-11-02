package com.bzcode.scorenge.domain.match

/**
 * Points for finishing places: with N players or teams, 1st earns N points, 2nd N-1, down to 1 for
 * last. Stored as the round score, so totals, rankings and charts work as for typed scores.
 */
object FinishingOrder {
    fun points(place: Int, count: Int): Long {
        require(count >= 1 && place in 1..count) { "Place $place out of $count" }
        return (count - place + 1).toLong()
    }

    /** The place that earned [points], or null if the points don't match a place. */
    fun placeOf(points: Long?, count: Int): Int? {
        if (points == null) return null
        val place = count - points.toInt() + 1
        return place.takeIf { it in 1..count }
    }
}

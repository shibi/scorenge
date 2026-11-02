package com.bzcode.scorenge.domain.match

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FinishingOrderTest {
    @Test
    fun firstEarnsOnePointPerPlayerAndLastEarnsOne() {
        assertEquals(listOf(4L, 3L, 2L, 1L), (1..4).map { FinishingOrder.points(it, 4) })
        assertEquals(1L, FinishingOrder.points(1, 1))
    }

    @Test
    fun pointsMapBackToPlaces() {
        (1..5).forEach { place -> assertEquals(place, FinishingOrder.placeOf(FinishingOrder.points(place, 5), 5)) }
        assertNull(FinishingOrder.placeOf(null, 5))
        assertNull(FinishingOrder.placeOf(9L, 5), "not a place with 5 players")
        assertNull(FinishingOrder.placeOf(0L, 5))
    }

    @Test
    fun rejectsAPlaceOutsideTheField() {
        assertFailsWith<IllegalArgumentException> { FinishingOrder.points(5, 4) }
    }
}

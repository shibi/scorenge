package com.bzcode.scorenge.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
        assertNull("not a place with 5 players", FinishingOrder.placeOf(9, 5))
        assertNull(FinishingOrder.placeOf(0, 5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAPlaceOutsideTheField() {
        FinishingOrder.points(5, 4)
    }
}

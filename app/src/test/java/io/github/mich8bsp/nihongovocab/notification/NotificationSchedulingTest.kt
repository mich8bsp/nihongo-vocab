package io.github.mich8bsp.nihongovocab.notification

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSchedulingTest {
    @Test
    fun staysAsIsWhenTargetFallsWithinActiveHours() {
        val now = LocalDateTime.of(2026, 7, 25, 14, 0)
        val delay = computeNextDelayMillis(now, randomMinutes = 30)
        assertEquals(now.plusMinutes(30), now.plusNanos(delay * 1_000_000))
    }

    @Test
    fun clampsToActiveStartWhenTargetLandsBeforeIt() {
        val now = LocalDateTime.of(2026, 7, 25, 7, 0)
        val delay = computeNextDelayMillis(now, randomMinutes = 20)
        val target = now.plusNanos(delay * 1_000_000)
        assertEquals(LocalDateTime.of(2026, 7, 25, 8, 0), target)
    }

    @Test
    fun clampsToNextDayActiveStartWhenTargetLandsAfterActiveEnd() {
        val now = LocalDateTime.of(2026, 7, 25, 21, 45)
        val delay = computeNextDelayMillis(now, randomMinutes = 90)
        val target = now.plusNanos(delay * 1_000_000)
        assertEquals(LocalDateTime.of(2026, 7, 26, 8, 0), target)
    }

    @Test
    fun neverReturnsNegativeDelay() {
        val now = LocalDateTime.of(2026, 7, 25, 21, 59)
        val delay = computeNextDelayMillis(now, randomMinutes = 90)
        assert(delay >= 0)
    }
}

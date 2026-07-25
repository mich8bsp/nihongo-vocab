package io.github.mich8bsp.nihongovocab.notification

import java.time.Duration
import java.time.LocalDateTime
import kotlin.random.Random

const val ACTIVE_START_HOUR = 8
const val ACTIVE_END_HOUR = 22
const val MIN_INTERVAL_MINUTES = 20
const val MAX_INTERVAL_MINUTES = 90

/**
 * Random interval within active hours (DESIGN.md "Notifications"): picks a
 * random gap, then clamps into the [ACTIVE_START_HOUR, ACTIVE_END_HOUR)
 * window if that would otherwise land outside it.
 */
fun computeNextDelayMillis(
    now: LocalDateTime = LocalDateTime.now(),
    randomMinutes: Int = Random.nextInt(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES + 1),
): Long {
    var target = now.plusMinutes(randomMinutes.toLong())
    val activeStart = target.toLocalDate().atTime(ACTIVE_START_HOUR, 0)
    val activeEnd = target.toLocalDate().atTime(ACTIVE_END_HOUR, 0)

    if (target.isBefore(activeStart)) {
        target = activeStart
    } else if (target.isAfter(activeEnd)) {
        target = activeStart.plusDays(1)
    }

    return Duration.between(now, target).toMillis().coerceAtLeast(0)
}

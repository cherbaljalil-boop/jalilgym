package com.chdjalil.jalilgym.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** A running rest period. Times are wall-clock epoch millis. */
data class RestTimerState(
    val startedAtMillis: Long,
    val endsAtMillis: Long,
    /** What comes next, e.g. "Bench Press · set 3 of 4". */
    val label: String,
) {
    val totalMillis: Long get() = endsAtMillis - startedAtMillis

    fun remainingMillis(now: Long): Long = (endsAtMillis - now).coerceAtLeast(0)
}

/** App-wide rest timer shared by the workout screen and the workout notification. */
interface RestTimer {
    val state: StateFlow<RestTimerState?>

    fun start(seconds: Int, label: String)

    /** Adds (or with a negative value, removes) time; stops the timer if none is left. */
    fun add(seconds: Int)

    fun skip()
}

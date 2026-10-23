package com.chdjalil.jalilgym.core.data.workout

import com.chdjalil.jalilgym.core.domain.repository.RestTimer
import com.chdjalil.jalilgym.core.domain.repository.RestTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide rest timer. It only holds the end time; the workout screen and
 * the workout service each tick against the clock, so nothing runs in between.
 */
@Singleton
internal class InMemoryRestTimer @Inject constructor() : RestTimer {

    private val _state = MutableStateFlow<RestTimerState?>(null)
    override val state: StateFlow<RestTimerState?> = _state.asStateFlow()

    override fun start(seconds: Int, label: String) {
        val now = System.currentTimeMillis()
        _state.value = RestTimerState(now, now + seconds * 1000L, label)
    }

    override fun add(seconds: Int) {
        val now = System.currentTimeMillis()
        _state.update { current ->
            current ?: return@update null
            val ends = maxOf(current.endsAtMillis, now) + seconds * 1000L
            if (ends <= now) null else current.copy(endsAtMillis = ends)
        }
    }

    override fun skip() {
        _state.value = null
    }
}

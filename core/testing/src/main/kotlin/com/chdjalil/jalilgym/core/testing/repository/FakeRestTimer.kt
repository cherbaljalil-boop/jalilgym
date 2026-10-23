package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.RestTimer
import com.chdjalil.jalilgym.core.domain.repository.RestTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Rest timer with a controllable clock. */
class FakeRestTimer(var now: Long = 0L) : RestTimer {
    private val _state = MutableStateFlow<RestTimerState?>(null)
    override val state: StateFlow<RestTimerState?> = _state

    override fun start(seconds: Int, label: String) {
        _state.value = RestTimerState(now, now + seconds * 1000L, label)
    }

    override fun add(seconds: Int) {
        val current = _state.value ?: return
        val ends = current.endsAtMillis + seconds * 1000L
        _state.value = if (ends <= now) null else current.copy(endsAtMillis = ends)
    }

    override fun skip() {
        _state.value = null
    }
}

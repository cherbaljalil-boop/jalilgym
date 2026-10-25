package com.chdjalil.jalilgym.feature.workout.wear

import android.content.Context
import android.util.Log
import com.chdjalil.jalilgym.core.domain.repository.RestTimer
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.domain.workout.CompleteSetUseCase
import com.chdjalil.jalilgym.core.wear.WearJson
import com.chdjalil.jalilgym.core.wear.WearPaths
import com.chdjalil.jalilgym.core.wear.WearSetCompletion
import com.chdjalil.jalilgym.core.wear.WearWorkoutState
import com.chdjalil.jalilgym.feature.workout.R
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Shares the workout with the Jalilgym watch app as one data item. Phones
 * without a paired watch (or without Play services) just get a logged failure.
 */
@Singleton
class WearPublisher @Inject constructor(@ApplicationContext private val context: Context) {
    private var last: WearWorkoutState? = null

    fun publish(state: WearWorkoutState) {
        if (state == last) return
        last = state
        runCatching {
            val request = PutDataMapRequest.create(WearPaths.STATE).apply {
                dataMap.putString(WearPaths.STATE_KEY, WearJson.encodeToString(WearWorkoutState.serializer(), state))
                // Changes every time, so the watch hears about each update.
                dataMap.putLong(KEY_SENT_AT, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(request)
                .addOnFailureListener { Log.d(TAG, "No watch to update: ${it.message}") }
        }.onFailure { Log.d(TAG, "Wearable API unavailable", it) }
    }

    companion object {
        const val KEY_SENT_AT = "sentAt"
        private const val TAG = "WearPublisher"
    }
}

/** Receives sets logged on the watch. Android starts it when a message arrives. */
@AndroidEntryPoint
class WorkoutWearListenerService : WearableListenerService() {

    @Inject lateinit var workoutRepository: WorkoutRepository

    @Inject lateinit var completeSet: CompleteSetUseCase

    @Inject lateinit var restTimer: RestTimer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            WearPaths.COMPLETE_SET -> {
                val completion = runCatching {
                    WearJson.decodeFromString(WearSetCompletion.serializer(), event.data.decodeToString())
                }.getOrNull() ?: return
                scope.launch {
                    workoutRepository.editSet(completion.setId) { set ->
                        set.copy(weightKg = completion.weightKg ?: set.weightKg, reps = completion.reps ?: set.reps)
                    }
                    // WorkoutService is already running for the active workout and picks up the new rest timer.
                    completeSet(completion.setId) { name, set, total -> getString(R.string.feature_workout_rest_label, name, set, total) }
                }
            }
            WearPaths.SKIP_REST -> restTimer.skip()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

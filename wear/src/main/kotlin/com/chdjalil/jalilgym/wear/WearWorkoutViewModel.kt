package com.chdjalil.jalilgym.wear

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.chdjalil.jalilgym.core.wear.WearJson
import com.chdjalil.jalilgym.core.wear.WearPaths
import com.chdjalil.jalilgym.core.wear.WearSetCompletion
import com.chdjalil.jalilgym.core.wear.WearWorkoutState
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WearUiState(
    /** Null until the phone's state has been read. */
    val workout: WearWorkoutState? = null,
    val draft: SetDraft? = null,
    /** A set was sent and the phone hasn't confirmed it yet. */
    val sending: Boolean = false,
    /** No phone reachable when last sending. */
    val phoneUnreachable: Boolean = false,
)

/**
 * Mirrors the phone's workout (a Data Layer item the phone app keeps up to date)
 * and sends logged sets back as messages.
 */
class WearWorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val dataClient = Wearable.getDataClient(application)
    private val messageClient = Wearable.getMessageClient(application)
    private val nodeClient = Wearable.getNodeClient(application)

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState: StateFlow<WearUiState> = _uiState.asStateFlow()

    private val listener = DataClient.OnDataChangedListener { events ->
        events.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == WearPaths.STATE }
            .mapNotNull { parse(it.dataItem) }
            .lastOrNull()
            ?.let(::show)
    }

    init {
        dataClient.addListener(listener)
        dataClient.dataItems
            .addOnSuccessListener { items ->
                val state = items.filter { it.uri.path == WearPaths.STATE }.mapNotNull(::parse).lastOrNull()
                items.release()
                show(state ?: WearWorkoutState())
            }
            .addOnFailureListener {
                Log.w(TAG, "Couldn't read the workout", it)
                show(WearWorkoutState())
            }
    }

    private fun parse(item: DataItem): WearWorkoutState? = runCatching {
        val json = DataMapItem.fromDataItem(item).dataMap.getString(WearPaths.STATE_KEY) ?: return null
        WearJson.decodeFromString(WearWorkoutState.serializer(), json)
    }.getOrNull()

    private fun show(workout: WearWorkoutState) = _uiState.update { current ->
        val next = workout.next
        // Keep the user's adjustments while it's still the same set.
        val draft = when {
            next == null -> null
            current.draft?.set?.setId == next.setId && current.draft.units == workout.units -> current.draft
            else -> SetDraft.from(next, workout.units)
        }
        current.copy(workout = workout, draft = draft, sending = current.sending && draft?.set?.setId == current.draft?.set?.setId)
    }

    fun changeWeight(steps: Int) = _uiState.update { it.copy(draft = it.draft?.changeWeight(steps)) }

    fun changeReps(steps: Int) = _uiState.update { it.copy(draft = it.draft?.changeReps(steps)) }

    fun completeSet() {
        val draft = _uiState.value.draft ?: return
        _uiState.update { it.copy(sending = true) }
        send(WearPaths.COMPLETE_SET, WearJson.encodeToString(WearSetCompletion.serializer(), draft.completion()).encodeToByteArray())
    }

    fun skipRest() = send(WearPaths.SKIP_REST, ByteArray(0))

    private fun send(path: String, body: ByteArray) {
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                _uiState.update { it.copy(phoneUnreachable = nodes.isEmpty(), sending = it.sending && nodes.isNotEmpty()) }
                nodes.forEach { node -> messageClient.sendMessage(node.id, path, body) }
            }
            .addOnFailureListener {
                Log.w(TAG, "Couldn't reach the phone", it)
                _uiState.update { state -> state.copy(phoneUnreachable = true, sending = false) }
            }
    }

    override fun onCleared() {
        dataClient.removeListener(listener)
    }

    private companion object {
        const val TAG = "JalilgymWear"
    }
}

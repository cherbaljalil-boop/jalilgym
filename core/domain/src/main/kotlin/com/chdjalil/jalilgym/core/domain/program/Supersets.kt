package com.chdjalil.jalilgym.core.domain.program

/**
 * Supersets are runs of consecutive exercises sharing a group id. Given the
 * current group ids (one per exercise, in order) and the exercise ids, toggles
 * the link between [index] and [index] + 1 and returns the new group ids.
 *
 * Each run is identified by its first exercise's id, so the result is stable and
 * a run broken in the middle becomes two runs (or single exercises).
 */
fun toggleSupersetLink(exerciseIds: List<String>, groups: List<String?>, index: Int): List<String?> {
    require(exerciseIds.size == groups.size) { "One group per exercise" }
    if (index !in 0 until exerciseIds.size - 1) return groups

    val links = BooleanArray(exerciseIds.size - 1) { i -> groups[i] != null && groups[i] == groups[i + 1] }
    links[index] = !links[index]

    val result = arrayOfNulls<String>(exerciseIds.size)
    var runStart = 0
    for (i in exerciseIds.indices) {
        val linkedToNext = i < links.size && links[i]
        if (!linkedToNext) {
            if (i > runStart) for (j in runStart..i) result[j] = exerciseIds[runStart]
            runStart = i + 1
        }
    }
    return result.toList()
}

/** True when the exercise at [index] and the next one are in the same superset. */
fun List<String?>.isLinkedToNext(index: Int): Boolean =
    index in 0 until size - 1 && this[index] != null && this[index] == this[index + 1]

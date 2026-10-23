package com.chdjalil.jalilgym.core.domain.workout

/**
 * Plates to load on each side of the bar to reach [target]; everything is in
 * the same unit (kg or lb). Uses each plate size as often as needed (assumes
 * enough pairs), largest first.
 */
data class PlateLoad(
    val perSide: List<Double>,
    /** The weight actually loadable: bar + 2 × plates. */
    val achieved: Double,
) {
    /** How far the loadable weight is from what was asked (positive = lighter). */
    fun shortfall(target: Double): Double = target - achieved
}

private const val EPSILON = 1e-6

fun calculatePlates(target: Double, bar: Double, plates: List<Double>): PlateLoad {
    if (target <= bar) return PlateLoad(emptyList(), bar)
    var remainingPerSide = (target - bar) / 2
    val perSide = mutableListOf<Double>()
    for (plate in plates.filter { it > 0 }.sortedDescending()) {
        while (remainingPerSide + EPSILON >= plate) {
            perSide += plate
            remainingPerSide -= plate
        }
    }
    return PlateLoad(perSide, bar + 2 * perSide.sum())
}

/** Closest loadable weight to [target] (rounding down when exactly between). */
fun nearestLoadable(target: Double, bar: Double, plates: List<Double>): Double {
    val smallest = plates.filter { it > 0 }.minOrNull() ?: return bar
    val down = calculatePlates(target, bar, plates).achieved
    val up = calculatePlates(target + 2 * smallest - EPSILON, bar, plates).achieved
    return if (up - target < target - down - EPSILON) up else down
}

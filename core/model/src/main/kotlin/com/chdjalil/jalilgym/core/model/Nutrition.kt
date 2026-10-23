package com.chdjalil.jalilgym.core.model

enum class DietGoal { CUT, LEAN_BULK, MAINTAIN }

/** High-protein is a modifier on any goal (docs/BLUEPRINT.md §3.7). */
enum class DietStyle { BALANCED, HIGH_PROTEIN }

enum class FoodSource { SEED, OPEN_FOOD_FACTS, CUSTOM }

/** Never inferred as certified without a label. */
enum class HalalStatus { CERTIFIED, FLAGGED, USER_MARKED, UNKNOWN }

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

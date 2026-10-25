package com.chdjalil.jalilgym.feature.nutrition

import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.FoodServing
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.Nutrients

internal val oats = Food(
    id = "oats",
    name = "Rolled oats",
    brand = null,
    barcode = null,
    source = FoodSource.SEED,
    per100g = Nutrients(379.0, 13.2, 67.7, 6.5, fiberG = 10.1),
    halalStatus = HalalStatus.UNKNOWN,
    servings = listOf(FoodServing("1/2 cup", 40.0)),
)

internal val gummies = Food(
    id = "gummies",
    name = "Gummy bears",
    brand = "Acme",
    barcode = "4001",
    source = FoodSource.OPEN_FOOD_FACTS,
    per100g = Nutrients(340.0, 6.0, 77.0, 0.5),
    halalStatus = HalalStatus.FLAGGED,
)

internal val yogurt = Food(
    id = "yogurt",
    name = "Greek yogurt",
    brand = "Acme",
    barcode = "4002",
    source = FoodSource.OPEN_FOOD_FACTS,
    per100g = Nutrients(97.0, 9.0, 4.0, 5.0),
    halalStatus = HalalStatus.CERTIFIED,
)

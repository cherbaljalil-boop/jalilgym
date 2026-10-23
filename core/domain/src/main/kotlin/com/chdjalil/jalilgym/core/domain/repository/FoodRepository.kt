package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.FoodDraft
import kotlinx.coroutines.flow.Flow

sealed interface OnlineSearch {
    data class Results(val foods: List<Food>) : OnlineSearch

    /** No connection (or Open Food Facts is down); local results still work. */
    data object Offline : OnlineSearch
}

sealed interface BarcodeLookup {
    data class Found(val food: Food) : BarcodeLookup

    data class NotFound(val barcode: String) : BarcodeLookup

    /** Not cached and Open Food Facts couldn't be reached. */
    data class Offline(val barcode: String) : BarcodeLookup
}

/** Foods from the bundled seed, Open Food Facts (cached in Room) and the user's own. */
interface FoodRepository {
    /** Cached and local foods matching [query]; favorites and recents first. Works offline. */
    fun searchLocal(query: String): Flow<List<Food>>

    fun observeRecent(): Flow<List<Food>>

    fun observeFavorites(): Flow<List<Food>>

    fun observeFood(id: String): Flow<Food?>

    /** Searches Open Food Facts and caches what it finds. */
    suspend fun searchOnline(query: String): OnlineSearch

    /** Local cache first, then Open Food Facts. */
    suspend fun lookupBarcode(barcode: String): BarcodeLookup

    /** Creates or updates a custom food and returns its id. */
    suspend fun saveCustomFood(draft: FoodDraft): String

    suspend fun setFavorite(id: String, favorite: Boolean)

    /** Marks or unmarks a food as halal by the user's own judgement. */
    suspend fun setUserMarkedHalal(id: String, marked: Boolean)
}

package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.BarcodeLookup
import com.chdjalil.jalilgym.core.domain.repository.FoodRepository
import com.chdjalil.jalilgym.core.domain.repository.OnlineSearch
import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.FoodDraft
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Local foods live in [foods]; [online] is what "Open Food Facts" returns (null = offline). */
class FakeFoodRepository(initial: List<Food> = emptyList()) : FoodRepository {
    val foods = MutableStateFlow(initial)
    var online: List<Food>? = emptyList()
    val searchedOnline = mutableListOf<String>()
    private val recentIds = MutableStateFlow<List<String>>(emptyList())
    private var nextId = 1

    fun markUsed(id: String) = recentIds.update { listOf(id) + (it - id) }

    override fun searchLocal(query: String): Flow<List<Food>> =
        foods.map { list -> list.filter { it.name.contains(query.trim(), ignoreCase = true) } }

    override fun observeRecent(): Flow<List<Food>> =
        combine(foods, recentIds) { list, ids -> ids.mapNotNull { id -> list.find { it.id == id } } }

    override fun observeFavorites(): Flow<List<Food>> = foods.map { list -> list.filter { it.isFavorite } }

    override fun observeFood(id: String): Flow<Food?> = foods.map { list -> list.find { it.id == id } }

    override suspend fun searchOnline(query: String): OnlineSearch {
        searchedOnline += query
        val results = online ?: return OnlineSearch.Offline
        val matches = results.filter { it.name.contains(query.trim(), ignoreCase = true) }
        foods.update { list -> list + matches.filter { m -> list.none { it.id == m.id } } }
        return OnlineSearch.Results(matches)
    }

    override suspend fun lookupBarcode(barcode: String): BarcodeLookup {
        foods.value.find { it.barcode == barcode }?.let { return BarcodeLookup.Found(it) }
        val results = online ?: return BarcodeLookup.Offline(barcode)
        val match = results.find { it.barcode == barcode } ?: return BarcodeLookup.NotFound(barcode)
        foods.update { it + match }
        return BarcodeLookup.Found(match)
    }

    override suspend fun saveCustomFood(draft: FoodDraft): String {
        val id = draft.id ?: "custom-food-${nextId++}"
        val food = Food(
            id = id,
            name = draft.name.trim(),
            brand = draft.brand,
            barcode = draft.barcode,
            source = FoodSource.CUSTOM,
            per100g = draft.per100g,
            halalStatus = if (draft.markedHalal) HalalStatus.USER_MARKED else HalalStatus.UNKNOWN,
            servings = draft.servings,
        )
        foods.update { list -> list.filterNot { it.id == id } + food }
        return id
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) =
        foods.update { list -> list.map { if (it.id == id) it.copy(isFavorite = favorite) else it } }

    override suspend fun setUserMarkedHalal(id: String, marked: Boolean) = foods.update { list ->
        list.map { if (it.id == id) it.copy(halalStatus = if (marked) HalalStatus.USER_MARKED else HalalStatus.UNKNOWN) else it }
    }
}

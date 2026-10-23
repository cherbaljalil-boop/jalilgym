package com.chdjalil.jalilgym.feature.nutrition.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.nutrition.allows
import com.chdjalil.jalilgym.core.domain.repository.FoodRepository
import com.chdjalil.jalilgym.core.domain.repository.OnlineSearch
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.feature.nutrition.navigation.DATE_ARG
import com.chdjalil.jalilgym.feature.nutrition.navigation.MEAL_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Minimum characters before searching. */
internal const val MIN_QUERY_LENGTH = 2

enum class OnlineStatus { IDLE, LOADING, DONE, OFFLINE }

data class FoodSearchUiState(
    val date: LocalDate,
    val meal: MealType,
    val query: String = "",
    /** Matches for [query], or favorites and recents when it's empty. */
    val results: List<Food> = emptyList(),
    val favorites: List<Food> = emptyList(),
    val recents: List<Food> = emptyList(),
    val online: OnlineStatus = OnlineStatus.IDLE,
    val halalFilter: HalalFilter = HalalFilter.ALL,
    /** Matches hidden by the halal filter. */
    val hiddenByFilter: Int = 0,
) {
    val isSearching: Boolean get() = query.trim().length >= MIN_QUERY_LENGTH
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val date: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle.get<String>(DATE_ARG)))
    private val meal: MealType = MealType.valueOf(checkNotNull(savedStateHandle.get<String>(MEAL_ARG)))

    private val query = MutableStateFlow("")
    private val online = MutableStateFlow(OnlineStatus.IDLE)

    /** Ids in Open Food Facts' order, for results not matched by the local name search. */
    private val onlineResults = MutableStateFlow<List<Food>>(emptyList())

    private val matches = query.flatMapLatest { q ->
        if (q.trim().length < MIN_QUERY_LENGTH) flowOf(emptyList()) else foodRepository.searchLocal(q)
    }

    val uiState: StateFlow<FoodSearchUiState> = combine(
        combine(query, matches, onlineResults) { q, local, remote -> Triple(q, local, remote) },
        foodRepository.observeFavorites(),
        foodRepository.observeRecent(),
        online,
        preferencesRepository.userPreferences,
    ) { (q, local, remote), favorites, recents, status, prefs ->
        val filter = prefs.halalFilter
        val all = (local + remote.filter { r -> local.none { it.id == r.id } })
        val shown = all.filter { filter.allows(it.halalStatus) }
        FoodSearchUiState(
            date = date,
            meal = meal,
            query = q,
            results = shown,
            favorites = favorites.filter { filter.allows(it.halalStatus) },
            recents = recents.filter { filter.allows(it.halalStatus) },
            online = status,
            halalFilter = filter,
            hiddenByFilter = all.size - shown.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodSearchUiState(date, meal))

    fun setQuery(value: String) {
        query.value = value
        online.value = OnlineStatus.IDLE
        onlineResults.value = emptyList()
    }

    /** Asks Open Food Facts; only on explicit search, to respect its rate limits. */
    fun searchOnline() {
        val q = query.value.trim()
        if (q.length < MIN_QUERY_LENGTH) return
        online.value = OnlineStatus.LOADING
        viewModelScope.launch {
            when (val result = foodRepository.searchOnline(q)) {
                is OnlineSearch.Results -> {
                    onlineResults.value = result.foods
                    online.value = OnlineStatus.DONE
                }
                OnlineSearch.Offline -> online.value = OnlineStatus.OFFLINE
            }
        }
    }

    fun setHalalFilter(filter: HalalFilter) {
        viewModelScope.launch { preferencesRepository.setHalalFilter(filter) }
    }
}

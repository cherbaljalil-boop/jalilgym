package com.chdjalil.jalilgym.feature.nutrition

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.domain.nutrition.GroceryLine
import com.chdjalil.jalilgym.core.model.GroceryItem
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeGroceryRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.chdjalil.jalilgym.core.uitest.assertActionsHaveLabels
import com.chdjalil.jalilgym.feature.nutrition.groceries.GroceryContent
import com.chdjalil.jalilgym.feature.nutrition.groceries.GroceryUiState
import com.chdjalil.jalilgym.feature.nutrition.groceries.GroceryViewModel
import com.chdjalil.jalilgym.feature.nutrition.ui.formatGroceryAmount
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class GroceryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `items move to the basket when ticked and clear out`() = runTest {
        val groceries = FakeGroceryRepository()
        val viewModel = GroceryViewModel(groceries, FakeUserPreferencesRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        groceries.addLines(listOf(GroceryLine("Rice", "rice", 160.0), GroceryLine("Chicken", "chicken", 300.0)))
        groceries.addLines(listOf(GroceryLine("Rice", "rice", 80.0)))
        viewModel.add("Milk")
        viewModel.add("  ")
        assertThat(viewModel.uiState.value.toBuy.map { it.name }).containsExactly("Rice", "Chicken", "Milk").inOrder()
        assertThat(viewModel.uiState.value.toBuy.first().grams).isEqualTo(240.0)

        viewModel.setChecked(viewModel.uiState.value.toBuy.first().id, true)
        assertThat(viewModel.uiState.value.inCart.map { it.name }).containsExactly("Rice")
        viewModel.clearChecked()
        assertThat(viewModel.uiState.value.inCart).isEmpty()
        assertThat(viewModel.uiState.value.toBuy).hasSize(2)
    }

    @Test
    fun `amounts read naturally in both unit systems`() {
        assertThat(formatGroceryAmount(450.0, UnitSystem.METRIC)).isEqualTo("450 g")
        assertThat(formatGroceryAmount(1_250.0, UnitSystem.METRIC)).isEqualTo("1.3 kg")
        assertThat(formatGroceryAmount(340.0, UnitSystem.IMPERIAL)).isEqualTo("12 oz")
        assertThat(formatGroceryAmount(1_000.0, UnitSystem.IMPERIAL)).isEqualTo("2.2 lb")
    }

    @Test
    fun groceryListIsLabeled() {
        composeRule.setContent {
            JalilgymTheme {
                GroceryContent(
                    GroceryUiState(
                        isLoading = false,
                        toBuy = listOf(GroceryItem("1", "Rice", "rice", 240.0, checked = false)),
                        inCart = listOf(GroceryItem("2", "Milk", null, null, checked = true)),
                    ),
                    onBack = {},
                )
            }
        }
        composeRule.assertActionsHaveLabels()
    }
}

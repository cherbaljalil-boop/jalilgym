package com.chdjalil.jalilgym.core.network

import com.chdjalil.jalilgym.core.common.Dispatcher
import com.chdjalil.jalilgym.core.common.JgDispatchers
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** A product as Open Food Facts describes it; nutrients are per 100 g. */
data class OffProduct(
    val code: String,
    val name: String,
    val brand: String?,
    val kcal: Double?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val fiberG: Double?,
    val sugarG: Double?,
    val satFatG: Double?,
    val sodiumMg: Double?,
    val labelTags: List<String>,
    val ingredientsText: String?,
    /** e.g. "1 bar (40 g)". */
    val servingLabel: String?,
    val servingGrams: Double?,
) {
    /** Usable for logging: it has a name and at least an energy value. */
    val isLoggable: Boolean get() = name.isNotBlank() && kcal != null
}

/** Open Food Facts lookups. Network failures throw [IOException]. */
interface OpenFoodFactsDataSource {
    suspend fun search(query: String, pageSize: Int = 25): List<OffProduct>

    /** Null when the barcode isn't in Open Food Facts. */
    suspend fun product(barcode: String): OffProduct?
}

internal const val OFF_FIELDS =
    "code,product_name,product_name_en,brands,nutriments,labels_tags,ingredients_text,ingredients_text_en,serving_size,serving_quantity"

internal class OkHttpOpenFoodFacts @Inject constructor(
    private val client: OkHttpClient,
    @Named(OFF_BASE_URL) private val baseUrl: HttpUrl,
    @Dispatcher(JgDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : OpenFoodFactsDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: String, pageSize: Int): List<OffProduct> {
        val url = baseUrl.newBuilder()
            .addPathSegments("cgi/search.pl")
            .addQueryParameter("search_terms", query)
            .addQueryParameter("search_simple", "1")
            .addQueryParameter("action", "process")
            .addQueryParameter("json", "1")
            .addQueryParameter("page_size", "$pageSize")
            .addQueryParameter("fields", OFF_FIELDS)
            .build()
        val body = get(url) ?: return emptyList()
        val products = json.parseToJsonElement(body).jsonObject["products"] as? JsonArray ?: return emptyList()
        return products.mapNotNull { (it as? JsonObject)?.toProduct() }
    }

    override suspend fun product(barcode: String): OffProduct? {
        val url = baseUrl.newBuilder()
            .addPathSegments("api/v2/product")
            .addPathSegment(barcode)
            .addQueryParameter("fields", OFF_FIELDS)
            .build()
        val body = get(url) ?: return null
        val root = json.parseToJsonElement(body).jsonObject
        if (root["status"].number()?.toInt() != 1) return null
        return (root["product"] as? JsonObject)?.toProduct(fallbackCode = barcode)
    }

    /** The response body, or null for 404 (unknown product). */
    private suspend fun get(url: HttpUrl): String? = withContext(ioDispatcher) {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            when {
                response.code == 404 -> null
                !response.isSuccessful -> throw IOException("Open Food Facts returned ${response.code}")
                else -> response.body.string()
            }
        }
    }
}

/** Numbers sometimes arrive as strings ("12.5") in Open Food Facts data. */
private fun JsonElement?.number(): Double? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.doubleOrNull ?: primitive.content.trim().replace(',', '.').toDoubleOrNull()
}

private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }

/** kJ to kcal, for products that only state energy in kJ. */
private const val KJ_PER_KCAL = 4.184

internal fun JsonObject.toProduct(fallbackCode: String? = null): OffProduct? {
    val code = this["code"].text() ?: fallbackCode ?: return null
    val nutriments = this["nutriments"] as? JsonObject ?: JsonObject(emptyMap())
    fun n(key: String) = nutriments["${key}_100g"].number()
    val kcal = n("energy-kcal") ?: n("energy")?.let { it / KJ_PER_KCAL }
    return OffProduct(
        code = code,
        name = (this["product_name"].text() ?: this["product_name_en"].text()).orEmpty(),
        brand = this["brands"].text()?.split(',')?.firstOrNull()?.trim(),
        kcal = kcal,
        proteinG = n("proteins"),
        carbsG = n("carbohydrates"),
        fatG = n("fat"),
        fiberG = n("fiber"),
        sugarG = n("sugars"),
        satFatG = n("saturated-fat"),
        // Open Food Facts stores sodium in grams.
        sodiumMg = n("sodium")?.times(1000),
        labelTags = (this["labels_tags"] as? JsonArray)?.mapNotNull { it.text() }.orEmpty(),
        ingredientsText = this["ingredients_text"].text() ?: this["ingredients_text_en"].text(),
        servingLabel = this["serving_size"].text(),
        servingGrams = this["serving_quantity"].number()?.takeIf { it > 0 },
    )
}

package com.chdjalil.jalilgym.core.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OpenFoodFactsTest {

    private val server = MockWebServer()
    private lateinit var api: OkHttpOpenFoodFacts

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().addInterceptor(userAgentInterceptor("1.0-test")).build()
        api = OkHttpOpenFoodFacts(client, server.url("/"), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = server.close()

    private fun respond(body: String, code: Int = 200) = server.enqueue(MockResponse.Builder().code(code).body(body).build())

    @Test
    fun parsesAProductWithStringNumbersAndSodiumInGrams() = runTest {
        respond(
            """
            {"status":1,"product":{
              "code":"3017620422003","product_name":"Hazelnut spread","brands":"Brand A, Brand B",
              "nutriments":{"energy-kcal_100g":539,"proteins_100g":"6.3","carbohydrates_100g":57.5,"fat_100g":30.9,
                            "sugars_100g":56.3,"saturated-fat_100g":10.6,"sodium_100g":0.0428},
              "labels_tags":["en:halal","en:no-palm-oil"],
              "ingredients_text":"Sugar, palm oil, hazelnuts 13%",
              "serving_size":"15 g","serving_quantity":"15"}}
            """.trimIndent(),
        )

        val product = api.product("3017620422003")!!

        assertThat(product.name).isEqualTo("Hazelnut spread")
        assertThat(product.brand).isEqualTo("Brand A")
        assertThat(product.kcal).isEqualTo(539.0)
        assertThat(product.proteinG).isEqualTo(6.3)
        assertThat(product.sodiumMg).isWithin(1e-9).of(42.8)
        assertThat(product.fiberG).isNull()
        assertThat(product.labelTags).containsExactly("en:halal", "en:no-palm-oil")
        assertThat(product.servingGrams).isEqualTo(15.0)
        assertThat(product.isLoggable).isTrue()

        val request = server.takeRequest()
        assertThat(request.url.encodedPath).isEqualTo("/api/v2/product/3017620422003")
        assertThat(request.url.queryParameter("fields")).contains("nutriments")
        assertThat(request.headers["User-Agent"]).startsWith("Jalilgym/1.0-test")
    }

    @Test
    fun energyInKilojoulesIsConverted() = runTest {
        respond("""{"status":1,"product":{"code":"1","product_name":"Bar","nutriments":{"energy_100g":418.4}}}""")
        assertThat(api.product("1")!!.kcal).isWithin(1e-9).of(100.0)
    }

    @Test
    fun unknownProductsAreNull() = runTest {
        respond("""{"status":0,"status_verbose":"product not found"}""", code = 404)
        assertThat(api.product("000")).isNull()
        respond("""{"status":0}""")
        assertThat(api.product("000")).isNull()
    }

    @Test(expected = IOException::class)
    fun serverErrorsThrow() = runTest {
        respond("oops", code = 503)
        api.product("1")
    }

    @Test
    fun searchSendsTermsAndSkipsBrokenEntries() = runTest {
        respond(
            """
            {"count":2,"products":[
              {"code":"111","product_name":"Greek yogurt","nutriments":{"energy-kcal_100g":97,"proteins_100g":9}},
              {"product_name":"No code"},
              {"code":"222","product_name":"","nutriments":{}}
            ]}
            """.trimIndent(),
        )

        val results = api.search("greek yogurt")

        assertThat(results.map { it.code }).containsExactly("111", "222").inOrder()
        assertThat(results.filter { it.isLoggable }.map { it.name }).containsExactly("Greek yogurt")
        val request = server.takeRequest()
        assertThat(request.url.encodedPath).isEqualTo("/cgi/search.pl")
        assertThat(request.url.queryParameter("search_terms")).isEqualTo("greek yogurt")
        assertThat(request.url.queryParameter("json")).isEqualTo("1")
    }
}

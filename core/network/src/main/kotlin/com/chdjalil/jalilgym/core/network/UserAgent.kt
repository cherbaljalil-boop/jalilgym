package com.chdjalil.jalilgym.core.network

import okhttp3.Interceptor

/** Qualifier for the Open Food Facts base URL (swapped for MockWebServer in tests). */
const val OFF_BASE_URL = "offBaseUrl"

/**
 * Open Food Facts asks every app to identify itself with a descriptive
 * User-Agent. No personal data is sent.
 */
fun userAgentInterceptor(versionName: String) = Interceptor { chain ->
    chain.proceed(
        chain.request().newBuilder()
            .header("User-Agent", "Jalilgym/$versionName (Android; https://github.com/cherbaljalil-boop/jalilgym)")
            .build(),
    )
}

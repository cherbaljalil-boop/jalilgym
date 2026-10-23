package com.chdjalil.jalilgym.feature.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.feature.profile.licenses.NOTICES
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LicensesTest {

    @Test
    fun everyBundledLicenseTextExists() {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        NOTICES.mapNotNull { it.textAsset }.distinct().forEach { path ->
            val text = assets.open(path).bufferedReader().use { it.readText() }
            assertThat(text.length).isGreaterThan(500)
        }
    }

    @Test
    fun noticesCoverTheRequiredAttributions() {
        val names = NOTICES.joinToString { it.name }
        listOf("Open Food Facts", "free-exercise-db", "Inter", "Bebas Neue", "ML Kit", "OkHttp").forEach {
            assertThat(names).contains(it)
        }
    }
}

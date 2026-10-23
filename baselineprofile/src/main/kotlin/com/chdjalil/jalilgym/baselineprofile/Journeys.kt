package com.chdjalil.jalilgym.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val PACKAGE_NAME = "com.chdjalil.jalilgym"

private const val TIMEOUT_MS = 5_000L

/** Taps the first element whose text is [text], if it shows up. */
private fun MacrobenchmarkScope.tapText(text: String): Boolean {
    val node = device.wait(Until.findObject(By.text(text)), TIMEOUT_MS) ?: return false
    node.click()
    device.waitForIdle()
    return true
}

/** On a fresh install the app opens on onboarding; skip it to reach the tabs. */
fun MacrobenchmarkScope.skipOnboardingIfShown() {
    tapText("I'll set it up later")
}

/** The screens people open most: every tab, then back to Today. */
fun MacrobenchmarkScope.browseTabs() {
    listOf("Train", "Nutrition", "Progress", "Profile", "Today").forEach { tab ->
        tapText(tab)
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIMEOUT_MS)
    }
}

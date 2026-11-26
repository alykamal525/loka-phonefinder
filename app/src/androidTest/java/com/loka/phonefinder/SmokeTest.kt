package com.loka.phonefinder

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        rule.waitForIdle()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(ctx.getExternalFilesDir(null), "screenshots")
        dir.mkdirs()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun search(number: String, tag: String) {
        rule.onNodeWithTag("input").performTextClearance()
        rule.onNodeWithTag("input").performTextInput(number)
        rule.onNodeWithTag(tag).performClick()
    }

    @Test
    fun a_homeScreenShows() {
        rule.onNodeWithTag("input").assertIsDisplayed()
        rule.onNodeWithTag("btn_quick").assertIsDisplayed()
        shot("01_home")
    }

    @Test
    fun b_validEgyptianNumberQuickSearch() {
        search("01012345678", "btn_quick")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("localcard").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("intl").assertIsDisplayed()
        shot("02_results_quick")
        rule.onNodeWithTag("results_list").performScrollToNode(hasTestTag("card_whatsapp"))
        rule.onNodeWithTag("card_whatsapp").assertIsDisplayed()
        shot("03_results_scrolled")
    }

    @Test
    fun c_invalidNumberShowsError() {
        search("0101", "btn_quick")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("invalid").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("invalid").assertIsDisplayed()
        assertEquals(0, rule.onAllNodesWithTag("localcard").fetchSemanticsNodes().size)
        shot("04_invalid")
    }

    @Test
    fun d_deepAndEnginesModes() {
        search("01112345678", "btn_deep")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("localcard").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("results_list").performScrollToNode(hasTestTag("card_nocall"))
        rule.onNodeWithTag("card_nocall").assertIsDisplayed()
        shot("05_deep")
        rule.onNodeWithTag("back").performClick()
        search("01212345678", "btn_engines")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("localcard").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("results_list").performScrollToNode(hasTestTag("card_brave"))
        rule.onNodeWithTag("card_brave").assertIsDisplayed()
        shot("06_engines")
    }

    @Test
    fun e_historyAndProviders() {
        search("01512345678", "btn_quick")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("localcard").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("back").performClick()
        rule.onNodeWithTag("nav_history").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTag("history_row").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(rule.onAllNodesWithTag("history_row").fetchSemanticsNodes().isNotEmpty())
        shot("07_history")
        rule.onNodeWithTag("history_clear").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTag("history_empty").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("back").performClick()
        rule.onNodeWithTag("nav_providers").performClick()
        rule.onNodeWithTag("registry_info").assertIsDisplayed()
        shot("08_providers")
    }
}

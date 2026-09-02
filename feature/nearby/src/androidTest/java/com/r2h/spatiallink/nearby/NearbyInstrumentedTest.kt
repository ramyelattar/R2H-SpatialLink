package com.r2h.spatiallink.nearby

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NearbyInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun idle_field_is_readable_and_inspectable() {
        render(NearbyUiState.Idle)

        composeRule.onNodeWithText("NEARBY FIELD").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Nearby field status IDLE").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Anonymous presence count 0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("OPEN FIELD").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close nearby field").assertIsDisplayed()
    }

    @Test
    fun unavailable_field_remains_readable_without_platform_prompt() {
        render(NearbyUiState.Unavailable)

        composeRule.onNodeWithContentDescription("Nearby field status UNAVAILABLE").assertIsDisplayed()
        composeRule.onNodeWithText("NO ANONYMOUS PRESENCE").assertIsDisplayed()
        composeRule.onNodeWithText("BLUETOOTH").assertDoesNotExist()
    }

    @Test
    fun active_field_exposes_only_bounded_anonymous_presence() {
        render(
            NearbyUiState.Active(
                remainingDurationMs = 12_000L,
                peerCount = 3,
                strongestPeerRssiDbm = -48,
            ),
        )

        composeRule.onNodeWithContentDescription("Nearby field status ACTIVE").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Anonymous presence count 3").assertIsDisplayed()
        composeRule.onNodeWithText("3 ANONYMOUS PRESENCES").assertIsDisplayed()
        composeRule.onNodeWithText("PRESENCE STRENGTH  -48 dBm").assertIsDisplayed()
        composeRule.onNodeWithText("Spatial ID").assertDoesNotExist()
        composeRule.onNodeWithText("Bluetooth address").assertDoesNotExist()
        composeRule.onNodeWithText("Device name").assertDoesNotExist()
    }

    @Test
    fun active_field_exposes_an_end_command() {
        var stopped = false
        render(
            state = NearbyUiState.Active(12_000L, peerCount = 1, strongestPeerRssiDbm = null),
            onStop = { stopped = true },
        )

        composeRule.onNodeWithContentDescription("END FIELD").assertIsDisplayed().performClick()
        assertTrue(stopped)
    }

    @Test
    fun close_command_is_exposed_without_identity_or_device_metadata() {
        var closed = false
        render(NearbyUiState.Idle, onRequestClose = { closed = true })

        composeRule.onNodeWithContentDescription("Close nearby field").performClick()
        assert(closed)
        composeRule.onNodeWithText("MAC").assertDoesNotExist()
        composeRule.onNodeWithText("RSSI").assertDoesNotExist()
        composeRule.onNodeWithText("Session ID").assertDoesNotExist()
    }

    private fun render(
        state: NearbyUiState,
        onStop: () -> Unit = {},
        onRequestClose: () -> Unit = {},
    ) {
        composeRule.setContent {
            NearbyScreen(
                state = state,
                onStart = {},
                onStop = onStop,
                onRequestClose = onRequestClose,
            )
        }
        composeRule.waitForIdle()
    }
}

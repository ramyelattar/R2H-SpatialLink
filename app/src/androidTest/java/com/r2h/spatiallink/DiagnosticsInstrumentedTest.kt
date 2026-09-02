package com.r2h.spatiallink

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

class DiagnosticsInstrumentedTest {
    private val shellActivityRule = ShellActivityLaunchRule()
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rule: TestRule = RuleChain
        .outerRule(composeRule)
        .around(shellActivityRule)

    @Test
    fun overview_opens_as_a_spatial_product_surface() {
        composeRule.onNodeWithText("Spatial field").assertIsDisplayed()
        composeRule.onAllNodesWithText("READY").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("OPEN FIELD").assertIsDisplayed()
        composeRule.onNodeWithText("FIELD").assertIsDisplayed()
        composeRule.onNodeWithText("IDENTITY").assertIsDisplayed()
        composeRule.onNodeWithText("SYSTEM").assertDoesNotExist()
    }

    @Test
    fun overview_gateway_uses_product_language_not_implementation_language() {
        composeRule.onNodeWithText("Spatial systems").assertIsDisplayed()
        composeRule.onNodeWithText("Capability surface · inspection only").assertDoesNotExist()
    }

    @Test
    fun overview_field_is_dominant_and_statuses_are_anchored() {
        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val fieldBounds = composeRule
            .onNodeWithContentDescription("Spatial field ready")
            .getUnclippedBoundsInRoot()

        assertTrue(
            "the spatial field must be larger than the phone viewport",
            fieldBounds.right - fieldBounds.left > rootBounds.right - rootBounds.left,
        )
        composeRule.onNodeWithContentDescription("Secure link TRUSTED").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Local network READY").assertIsDisplayed()

        val commandBounds = composeRule
            .onNodeWithContentDescription("OPEN FIELD")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "the field command must span most of the usable width",
            commandBounds.right - commandBounds.left >=
            (rootBounds.right - rootBounds.left) * 0.78f,
        )
    }

    @Test
    fun overview_trust_seal_has_artifact_scale() {
        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val sealBounds = composeRule
            .onNodeWithContentDescription("Spatial field trust seal")
            .getUnclippedBoundsInRoot()

        assertTrue(
            "the Overview trust seal must read as a primary artifact",
            sealBounds.right - sealBounds.left >=
                (rootBounds.right - rootBounds.left) * 0.48f,
        )
    }

    @Test
    fun overview_gateway_has_a_quiet_transition_to_the_rail() {
        val gatewayBounds = composeRule
            .onNodeWithContentDescription("OPEN FIELD")
            .getUnclippedBoundsInRoot()
        val railBounds = composeRule
            .onNodeWithContentDescription("OVERVIEW")
            .getUnclippedBoundsInRoot()

        assertTrue(
            "the gateway must have a quiet transition before the instrumentation rail",
            railBounds.top - gatewayBounds.bottom >= 28.dp,
        )
    }

    @Test
    fun diagnostics_title_survives_activity_recreation() {
        val activityToRecreate = checkNotNull(shellActivityRule.resumedMainActivity)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activityToRecreate.recreate()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Spatial field").assertIsDisplayed()
    }

    @Test
    fun identity_surface_exposes_only_safe_identity_metadata() {
        composeRule.onNodeWithText("IDENTITY").performClick()

        composeRule.onNodeWithText("Spatial identity").assertIsDisplayed()
        composeRule.onNodeWithText("ECDSA P-256 / SHA-256")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Signing verification")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun identity_id_is_presented_outside_the_trust_seal() {
        composeRule.onNodeWithText("IDENTITY").performClick()

        val sealBounds = composeRule
            .onNodeWithContentDescription("Trust seal trusted")
            .getUnclippedBoundsInRoot()
        val idBounds = composeRule
            .onNodeWithContentDescription("Spatial ID value")
            .getUnclippedBoundsInRoot()

        assertTrue(
            "the safe Spatial ID must sit beside, not inside, the trust seal",
            idBounds.left >= sealBounds.right,
        )
    }

    @Test
    fun system_surface_groups_capabilities_and_permissions() {
        composeRule.onNodeWithText("FIELD").performClick()

        composeRule.onNodeWithText("Field instrumentation").assertIsDisplayed()
        composeRule.onNodeWithText("SYSTEM READOUT").assertIsDisplayed()
        composeRule.onNodeWithText("Spatial capabilities").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Runtime permissions").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Runtime permissions").performScrollTo().performClick()
        composeRule.onNodeWithText("Bluetooth LE").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Local network").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ranging").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun field_surface_remains_capability_only() {
        composeRule.onNodeWithText("FIELD").performClick()

        composeRule.onNodeWithText("Field instrumentation").assertIsDisplayed()
        composeRule.onNodeWithText("No active spatial session").assertIsDisplayed()
    }

    @Test
    fun open_field_gateway_reaches_anonymous_nearby_surface() {
        composeRule.onNodeWithText("OPEN FIELD").performClick()

        composeRule.onNodeWithContentDescription("Nearby field route").assertIsDisplayed()
        composeRule.onNodeWithText("NEARBY FIELD").assertIsDisplayed()
    }

    @Test
    fun field_status_surface_keeps_title_and_explanation_separated() {
        composeRule.onNodeWithText("FIELD").performClick()

        val title = composeRule
            .onNodeWithText("No active spatial session")
            .performScrollTo()
            .getUnclippedBoundsInRoot()
        val explanation = composeRule
            .onNodeWithText(
                "This surface reports local readiness only. Peer discovery and ranging sessions are not active in this foundation.",
            )
            .performScrollTo()
            .getUnclippedBoundsInRoot()

        assertTrue(
            "field status title and explanation must not overlap",
            explanation.top >= title.bottom || title.top >= explanation.bottom,
        )
    }

    @Test
    fun long_permission_row_keeps_label_and_value_separated() {
        composeRule.onNodeWithText("FIELD").performClick()
        composeRule.onNodeWithText("Runtime permissions").performScrollTo().performClick()
        composeRule.onNodeWithText("Local network").performScrollTo().assertIsDisplayed()

        val labelBounds = composeRule
            .onNodeWithText("Local network")
            .getUnclippedBoundsInRoot()
        val valueBounds = composeRule
            .onAllNodesWithText("NOT REQUIRED")
            .onFirst()
            .getUnclippedBoundsInRoot()
        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()

        assertTrue(
            "long permission status must have visible vertical separation",
            valueBounds.top >= labelBounds.bottom || labelBounds.top >= valueBounds.bottom,
        )
        assertTrue(
            "long permission status must remain inside the viewport",
            valueBounds.right <= rootBounds.right,
        )
    }
}

private class ShellActivityLaunchRule : TestRule {
    @Volatile
    var resumedMainActivity: Activity? = null

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val application = instrumentation.targetContext.applicationContext as Application
                val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                    override fun onActivityStarted(activity: Activity) = Unit

                    override fun onActivityResumed(activity: Activity) {
                        if (activity.componentName.className == MainActivity::class.java.name) {
                            resumedMainActivity = activity
                        }
                    }

                    override fun onActivityPaused(activity: Activity) = Unit

                    override fun onActivityStopped(activity: Activity) = Unit

                    override fun onActivitySaveInstanceState(
                        activity: Activity,
                        outState: Bundle,
                    ) = Unit

                    override fun onActivityDestroyed(activity: Activity) {
                        if (resumedMainActivity === activity) {
                            resumedMainActivity = null
                        }
                    }
                }
                application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
                try {
                    val command = instrumentation.uiAutomation.executeShellCommand(
                        "am start -W -n com.r2h.spatiallink/.MainActivity",
                    )
                    command.use { descriptor ->
                        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
                            it.readBytes()
                        }
                    }
                    instrumentation.waitForIdleSync()
                    base.evaluate()
                } finally {
                    application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
                }
            }
        }
}

package de.digitalService.useID.util

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import de.digitalService.useID.analytics.IssueTrackerManagerType
import de.digitalService.useID.analytics.TrackerManagerType
import de.digitalService.useID.hilt.ConfigModule
import io.getunleash.UnleashClient
import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.polling.PollingModes
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import javax.inject.Named
import kotlin.coroutines.resume

class AbTestManager constructor(
    @Named(ConfigModule.UNLEASH_API_URL) private val url: String,
    @Named(ConfigModule.UNLEASH_API_KEY) private val apiKey: String,
    private val trackerManager: TrackerManagerType,
    private val issueTrackerManager: IssueTrackerManagerType
) {

    private lateinit var unleashClient: UnleashClient

    private val _isSetupIntroTestVariation = mutableStateOf(false)
    val isSetupIntroTestVariation: State<Boolean> = _isSetupIntroTestVariation

    suspend fun initialise() = suspendCancellableCoroutine { continuation ->
        val config = UnleashConfig.newBuilder()
            .proxyUrl(url)
            .clientKey(apiKey)
            .pollingMode(PollingModes.autoPoll(Int.MAX_VALUE.toLong()))
            .build()

        val supportedAbTests = AbTest.values().joinToString(",") { it.testName }
        val context = UnleashContext.newBuilder()
            .appName("bundesIdent.Android")
            .sessionId(UUID.randomUUID().toString())
            .properties(mutableMapOf(Pair("supportedToggles", supportedAbTests)))
            .build()

        unleashClient = UnleashClient(config, context)

        unleashClient.addTogglesUpdatedListener {
            _isSetupIntroTestVariation.value =
                isVariationActivatedFor(AbTest.SETUP_INTRODUCTION_EXPLANATION)
            continuation.resume(Unit)
        }
        unleashClient.addTogglesErroredListener {
            issueTrackerManager.capture(it)
            continuation.resume(Unit)
        }

        continuation.invokeOnCancellation {
            issueTrackerManager.capture(UnleashException("Timed out while fetching toggles."))
            unleashClient.close()
        }
    }

    private fun isVariationActivatedFor(test: AbTest): Boolean =
        if (unleashClient.isEnabled(test.testName)) {
            val variantName = unleashClient.getVariant(test.testName).name
            val testNameForTracking = test.testName.replace(".", "_")
            trackerManager.trackEvent("abtesting", testNameForTracking, variantName)
            issueTrackerManager.addInfoBreadcrumb("abtest", "${test.testName}: $variantName")
            variantName == "Variation"
        } else {
            false
        }
}

enum class AbTest(val testName: String) {
    SETUP_INTRODUCTION_EXPLANATION("bundesIdent.setup_introduction_explanation_03_23")
}

class UnleashException(override val message: String) : Exception()

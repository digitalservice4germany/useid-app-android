package de.digitalService.useID.userFlowTests.identFlows.success

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.intent.rule.IntentsTestRule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.digitalService.useID.MainActivity
import de.digitalService.useID.StorageManager
import de.digitalService.useID.analytics.TrackerManagerType
import de.digitalService.useID.hilt.CoroutineContextProviderModule
import de.digitalService.useID.hilt.NfcInterfaceMangerModule
import de.digitalService.useID.hilt.SingletonModule
import de.digitalService.useID.idCardInterface.*
import de.digitalService.useID.models.NfcAvailability
import de.digitalService.useID.ui.UseIDApp
import de.digitalService.useID.ui.coordinators.AppCoordinatorType
import de.digitalService.useID.ui.navigation.Navigator
import de.digitalService.useID.userFlowTests.utils.TestScreen
import de.digitalService.useID.util.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject


@UninstallModules(SingletonModule::class, CoroutineContextProviderModule::class, NfcInterfaceMangerModule::class)
@HiltAndroidTest
class IdentSuccessfulNavigationTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val intentsTestRule = IntentsTestRule(MainActivity::class.java)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var trackerManager: TrackerManagerType

    @Inject
    lateinit var appCoordinator: AppCoordinatorType

    @BindValue
    val mockEidInteractionManager: EidInteractionManager = mockk(relaxed = true)

    @BindValue
    val mockStorageManager: StorageManager = mockk(relaxed = true) {
        every { firstTimeUser } returns false
    }

    @BindValue
    val mockCoroutineContextProvider: CoroutineContextProviderType = mockk {
        every { Main } returns Dispatchers.Main
    }

    @BindValue
    val mockNfcInterfaceManager: NfcInterfaceManagerType = mockk(relaxed = true){
        every { nfcAvailability } returns MutableStateFlow(NfcAvailability.Available)
    }

    @Before
    fun before() {
        hiltRule.inject()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testIdentSuccessfulNavigation() = runTest {
        every { mockCoroutineContextProvider.IO } returns StandardTestDispatcher(testScheduler)
        every { mockCoroutineContextProvider.Default } returns StandardTestDispatcher(testScheduler)

        val eidFlow = MutableStateFlow<EidInteractionEvent>(EidInteractionEvent.Idle)
        every { mockEidInteractionManager.eidFlow } returns eidFlow

        composeTestRule.activity.setContentUsingUseIdTheme {
            UseIDApp(
                nfcAvailability = NfcAvailability.Available,
                navigator = navigator,
                trackerManager = trackerManager
            )
        }

        val deepLink = Uri.parse("bundesident://127.0.0.1:24727/eID-Client?tcTokenURL=https%3A%2F%2Feid.digitalservicebund.de%2Fapi%2Fv1%2Fidentification%2Fsessions%2F30d20d97-cf31-4f01-ab27-35dea918bb83%2Ftc-token")
        val redirectUrl = "test.url.com"
        val personalPin = "123456"

        // Define screens to be tested
        val identificationFetchMetaData = TestScreen.IdentificationFetchMetaData(composeTestRule)
        val identificationAttributeConsent = TestScreen.IdentificationAttributeConsent(composeTestRule)
        val identificationPersonalPin = TestScreen.IdentificationPersonalPin(composeTestRule)
        val identificationScan = TestScreen.Scan(composeTestRule)

        composeTestRule.waitForIdle()

        appCoordinator.handleDeepLink(deepLink)
        advanceUntilIdle()

        eidFlow.value = EidInteractionEvent.IdentificationStarted
        advanceUntilIdle()

        identificationFetchMetaData.assertIsDisplayed()
        identificationFetchMetaData.cancel.click()
        identificationFetchMetaData.navigationConfirmDialog.assertIsDisplayed()
        identificationFetchMetaData.navigationConfirmDialog.dismiss()

        eidFlow.value = EidInteractionEvent.IdentificationRequestConfirmationRequested(
            IdentificationRequest(
                TestScreen.IdentificationAttributeConsent.RequestData.requiredAttributes,
                TestScreen.IdentificationAttributeConsent.RequestData.transactionInfo
            )
        )

        advanceUntilIdle()

        eidFlow.value = EidInteractionEvent.CertificateDescriptionReceived(
            CertificateDescription(
                TestScreen.IdentificationAttributeConsent.CertificateDescription.issuerName,
                TestScreen.IdentificationAttributeConsent.CertificateDescription.issuerUrl,
                TestScreen.IdentificationAttributeConsent.CertificateDescription.purpose,
                TestScreen.IdentificationAttributeConsent.CertificateDescription.subjectName,
                TestScreen.IdentificationAttributeConsent.CertificateDescription.subjectUrl,
                TestScreen.IdentificationAttributeConsent.CertificateDescription.termsOfUsage,
            )
        )

        advanceUntilIdle()

        identificationAttributeConsent.assertIsDisplayed()
        identificationAttributeConsent.moreInformationBtn.scrollToAndClick()
        identificationAttributeConsent.infoDialog.assertIsDisplayed()
        identificationAttributeConsent.infoDialogCloseBtn.click()

        advanceUntilIdle()

        identificationAttributeConsent.assertIsDisplayed()
        identificationAttributeConsent.cancel.click()
        identificationAttributeConsent.navigationConfirmDialog.assertIsDisplayed()
        identificationAttributeConsent.navigationConfirmDialog.dismiss()

        identificationAttributeConsent.assertIsDisplayed()
        identificationAttributeConsent.continueBtn.click()

        advanceUntilIdle()

        identificationPersonalPin.assertIsDisplayed()
        identificationPersonalPin.back.click()

        advanceUntilIdle()

        identificationAttributeConsent.assertIsDisplayed()
        identificationAttributeConsent.continueBtn.click()

        advanceUntilIdle()

        identificationPersonalPin.assertIsDisplayed()
        identificationPersonalPin.personalPinField.assertLength(0)
        composeTestRule.performPinInput(personalPin)
        identificationPersonalPin.personalPinField.assertLength(personalPin.length)
        identificationPersonalPin.back.click()

        advanceUntilIdle()

        identificationAttributeConsent.assertIsDisplayed()
        identificationAttributeConsent.continueBtn.click()

        advanceUntilIdle()

        identificationPersonalPin.assertIsDisplayed()
        identificationPersonalPin.personalPinField.assertLength(0)
        composeTestRule.performPinInput(personalPin)
        identificationPersonalPin.personalPinField.assertLength(personalPin.length)
        composeTestRule.pressReturn()

        advanceUntilIdle()

        identificationScan.setIdentPending(true).setBackAllowed(false).assertIsDisplayed()
        identificationScan.cancel.click()
        identificationScan.navigationConfirmDialog.assertIsDisplayed()
        identificationScan.navigationConfirmDialog.dismiss()

        eidFlow.value = EidInteractionEvent.CardRecognized
        advanceUntilIdle()

        identificationScan.setProgress(true).assertIsDisplayed()

        intending(allOf(
            hasAction(Intent.ACTION_VIEW),
            hasData(redirectUrl),
            hasFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
        )).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                null
            )
        )

        eidFlow.value = EidInteractionEvent.IdentificationSucceededWithRedirect(redirectUrl)
        advanceUntilIdle()
    }
}

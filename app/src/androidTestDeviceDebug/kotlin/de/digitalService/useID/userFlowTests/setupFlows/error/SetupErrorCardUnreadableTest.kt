//package de.digitalService.useID.userFlowTests.setupFlows.error
//
//import androidx.compose.ui.test.*
//import androidx.compose.ui.test.junit4.createAndroidComposeRule
//import dagger.hilt.android.testing.BindValue
//import dagger.hilt.android.testing.HiltAndroidRule
//import dagger.hilt.android.testing.HiltAndroidTest
//import dagger.hilt.android.testing.UninstallModules
//import de.digitalService.useID.MainActivity
//import de.digitalService.useID.StorageManager
//import de.digitalService.useID.analytics.TrackerManagerType
//import de.digitalService.useID.hilt.CoroutineContextProviderModule
//import de.digitalService.useID.hilt.SingletonModule
//import de.digitalService.useID.idCardInterface.EidInteractionEvent
//import de.digitalService.useID.idCardInterface.IdCardInteractionException
//import de.digitalService.useID.idCardInterface.IdCardManager
//import de.digitalService.useID.models.NfcAvailability
//import de.digitalService.useID.ui.UseIDApp
//import de.digitalService.useID.ui.navigation.Navigator
//import de.digitalService.useID.userFlowTests.utils.TestScreen
//import de.digitalService.useID.util.*
//import io.mockk.every
//import io.mockk.mockk
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.test.StandardTestDispatcher
//import kotlinx.coroutines.test.advanceUntilIdle
//import kotlinx.coroutines.test.runTest
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.openecard.mobile.activation.ActivationResultCode
//import javax.inject.Inject
//
//@UninstallModules(SingletonModule::class, CoroutineContextProviderModule::class)
//@HiltAndroidTest
//class SetupErrorCardUnreadableTest {
//
//    @get:Rule(order = 0)
//    var hiltRule = HiltAndroidRule(this)
//
//    @get:Rule(order = 1)
//    val composeTestRule = createAndroidComposeRule<MainActivity>()
//
//    @Inject
//    lateinit var navigator: Navigator
//
//    @Inject
//    lateinit var trackerManager: TrackerManagerType
//
//    @BindValue
//    val mockIdCardManager: IdCardManager = mockk(relaxed = true)
//
//    @BindValue
//    val mockStorageManager: StorageManager = mockk(relaxed = true) {
//        every { firstTimeUser } returns false
//    }
//
//    @BindValue
//    val mockCoroutineContextProvider: CoroutineContextProviderType = mockk {
//        every { Main } returns Dispatchers.Main
//    }
//
//    @Before
//    fun before() {
//        hiltRule.inject()
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testSetupErrorCardUnreadable() = runTest {
//        every { mockCoroutineContextProvider.IO } returns StandardTestDispatcher(testScheduler)
//        every { mockCoroutineContextProvider.Default } returns StandardTestDispatcher(testScheduler)
//
//        val eidFlow = MutableStateFlow<EidInteractionEvent>(EidInteractionEvent.Idle)
//        every { mockIdCardManager.eidFlow } returns eidFlow
//
//        composeTestRule.activity.setContentUsingUseIdTheme {
//            UseIDApp(
//                nfcAvailability = NfcAvailability.Available,
//                navigator = navigator,
//                trackerManager = trackerManager
//            )
//        }
//
//        val transportPin = "12345"
//        val personalPin = "123456"
//
//        // Define screens to be tested
//        val setupIntro = TestScreen.SetupIntro(composeTestRule)
//        val setupPinLetter = TestScreen.SetupPinLetter(composeTestRule)
//        val setupTransportPin = TestScreen.SetupTransportPin(composeTestRule)
//        val setupPersonalPinIntro = TestScreen.SetupPersonalPinIntro(composeTestRule)
//        val setupPersonalPinInput = TestScreen.SetupPersonalPinInput(composeTestRule)
//        val setupPersonalPinConfirm = TestScreen.SetupPersonalPinConfirm(composeTestRule)
//        val setupScan = TestScreen.Scan(composeTestRule)
//        val errorCardUnreadable = TestScreen.ErrorCardUnreadable(composeTestRule)
//        val setupFinish = TestScreen.SetupFinish(composeTestRule)
//        val home = TestScreen.Home(composeTestRule)
//
//        home.assertIsDisplayed()
//        home.setupButton.click()
//        advanceUntilIdle()
//
//        setupIntro.assertIsDisplayed()
//        setupIntro.setupIdBtn.click()
//        advanceUntilIdle()
//
//        setupPinLetter.assertIsDisplayed()
//        setupPinLetter.letterPresentBtn.click()
//        advanceUntilIdle()
//
//        setupTransportPin.assertIsDisplayed()
//        setupTransportPin.transportPinField.assertLength(0)
//        composeTestRule.performPinInput(transportPin)
//        setupTransportPin.transportPinField.assertLength(transportPin.length)
//        composeTestRule.pressReturn()
//        advanceUntilIdle()
//
//        setupPersonalPinIntro.assertIsDisplayed()
//        setupPersonalPinIntro.continueBtn.click()
//        advanceUntilIdle()
//
//        setupPersonalPinInput.assertIsDisplayed()
//        setupPersonalPinInput.personalPinField.assertLength(0)
//        composeTestRule.performPinInput(personalPin)
//        setupPersonalPinInput.personalPinField.assertLength(personalPin.length)
//        composeTestRule.pressReturn()
//        advanceUntilIdle()
//
//        setupPersonalPinConfirm.assertIsDisplayed()
//        setupPersonalPinConfirm.personalPinField.assertLength(0)
//        composeTestRule.performPinInput(personalPin)
//        setupPersonalPinConfirm.personalPinField.assertLength(personalPin.length)
//        composeTestRule.pressReturn()
//        advanceUntilIdle()
//
//        eidFlow.value = EidInteractionEvent.CardInsertionRequested
//        advanceUntilIdle()
//
//        setupScan.assertIsDisplayed()
//
//        eidFlow.value = EidInteractionEvent.CardRecognized
//        advanceUntilIdle()
//
//        setupScan.setProgress(true).assertIsDisplayed()
//
//        eidFlow.value = EidInteractionEvent.CardRemoved
//        advanceUntilIdle()
//
//        eidFlow.value = EidInteractionEvent.Error(
//            IdCardInteractionException.ProcessFailed(
//                resultCode = ActivationResultCode.CLIENT_ERROR,
//                redirectUrl = null,
//                resultMinor = null
//            )
//        )
//
//        advanceUntilIdle()
//
//        errorCardUnreadable.assertIsDisplayed()
//        errorCardUnreadable.closeBtn.click()
//
//        eidFlow.value = EidInteractionEvent.RequestCardInsertion
//        advanceUntilIdle()
//
//        setupScan.setProgress(false).setBackAllowed(true).assertIsDisplayed() // TODO: navigating back should be possible here. Ticket: https://digitalservicebund.atlassian.net/browse/USEID-907
//
//        eidFlow.value = EidInteractionEvent.CardRecognized
//        advanceUntilIdle()
//
//        setupScan.setProgress(true).assertIsDisplayed()
//
//        eidFlow.value = EidInteractionEvent.CardRemoved
//        advanceUntilIdle()
//
//        eidFlow.value = EidInteractionEvent.Error(
//            IdCardInteractionException.ProcessFailed(
//                resultCode = ActivationResultCode.INTERNAL_ERROR,
//                redirectUrl = null,
//                resultMinor = null
//            )
//        )
//
//        advanceUntilIdle()
//
//        errorCardUnreadable.assertIsDisplayed()
//        errorCardUnreadable.cancel.click()
//
//        eidFlow.value = EidInteractionEvent.RequestCardInsertion
//        advanceUntilIdle()
//
//        setupScan.setProgress(false).assertIsDisplayed()
//
//        eidFlow.value = EidInteractionEvent.CardRecognized
//        advanceUntilIdle()
//
//        setupScan.setProgress(true).assertIsDisplayed()
//
//        eidFlow.value = EidInteractionEvent.CardRemoved
//        advanceUntilIdle()
//
//        eidFlow.value = EidInteractionEvent.Error(
//            IdCardInteractionException.ProcessFailed(
//                resultCode = ActivationResultCode.BAD_REQUEST,
//                redirectUrl = null,
//                resultMinor = null
//            )
//        )
//
//        advanceUntilIdle()
//
//        errorCardUnreadable.assertIsDisplayed()
//        errorCardUnreadable.closeBtn.click()
//
//        eidFlow.value = EidInteractionEvent.RequestCardInsertion
//        advanceUntilIdle()
//
//        setupScan.setProgress(false).assertIsDisplayed()
//        setupScan.back.click()
//
//        setupPersonalPinInput.assertIsDisplayed()
//        setupPersonalPinInput.personalPinField.assertLength(0)
//        composeTestRule.performPinInput(personalPin)
//        setupPersonalPinInput.personalPinField.assertLength(personalPin.length)
//        composeTestRule.pressReturn()
//
//        setupPersonalPinConfirm.assertIsDisplayed()
//        setupPersonalPinConfirm.personalPinField.assertLength(0)
//        composeTestRule.performPinInput(personalPin)
//        setupPersonalPinConfirm.personalPinField.assertLength(personalPin.length)
//        composeTestRule.pressReturn()
//
//        eidFlow.value = EidInteractionEvent.RequestCardInsertion
//        advanceUntilIdle()
//
//        setupScan.setProgress(false).assertIsDisplayed()
//
//        eidFlow.value = EidInteractionEvent.CardRecognized
//        advanceUntilIdle()
//
//        setupScan.setProgress(true).assertIsDisplayed()
//
//        eidFlow.value = EidInteractionEvent.RequestChangedPin(null) {_, _ -> }
//        advanceUntilIdle()
//
//        eidFlow.value = EidInteractionEvent.ProcessCompletedSuccessfullyWithoutResult
//        advanceUntilIdle()
//
//        setupFinish.assertIsDisplayed()
//        setupFinish.finishSetupBtn.click()
//
//        home.assertIsDisplayed()
//    }
//}

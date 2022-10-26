package de.digitalService.useID

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.digitalService.useID.ui.screens.setup.SetupPersonalPIN
import de.digitalService.useID.ui.screens.setup.SetupPersonalPINViewModelInterface
import de.digitalService.useID.util.MockNfcAdapterUtil
import de.digitalService.useID.util.NfcAdapterUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SetupPersonalPINTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    val mockNfcAdapterUtil: NfcAdapterUtil = MockNfcAdapterUtil()

    @Test
    fun correctUsage() {
        val testShouldShowErrorState = mutableStateOf(false)
        val testShouldShowPIN2EntryFieldState = mutableStateOf(false)
        val testPin1State = mutableStateOf("")
        val testPin2State = mutableStateOf("")
        val testFocusPinState = mutableStateOf(SetupPersonalPINViewModelInterface.PINEntryFieldFocus.PIN_1)

        val testPinInput = "123456"
        val testPinInput2 = "1234"

        val mockViewModel: SetupPersonalPINViewModelInterface = mockk(relaxed = true)

        every { mockViewModel.pin1 } returns ""
        every { mockViewModel.pin2 } returns ""
        every { mockViewModel.shouldShowError } answers { testShouldShowErrorState.value }
        every { mockViewModel.shouldShowPIN2EntryField } answers { testShouldShowPIN2EntryFieldState.value }
        every { mockViewModel.pin1 } answers { testPin1State.value }
        every { mockViewModel.pin2 } answers { testPin2State.value }
        every { mockViewModel.focus } answers { testFocusPinState.value }

        composeTestRule.activity.setContent {
            SetupPersonalPIN(viewModel = mockViewModel)
        }

        val pinEntryFieldTag = "PINEntryField"
        val obfuscationTestTag = "Obfuscation"

        composeTestRule.onNodeWithTag(pinEntryFieldTag).assertIsFocused()

        composeTestRule.onNodeWithTag(pinEntryFieldTag).performTextInput(testPinInput)
        testPin1State.value = testPinInput
        composeTestRule.onAllNodesWithTag(obfuscationTestTag).assertCountEquals(6)

        composeTestRule.onAllNodesWithTag(pinEntryFieldTag).assertCountEquals(1)

        testShouldShowPIN2EntryFieldState.value = true
        testFocusPinState.value = SetupPersonalPINViewModelInterface.PINEntryFieldFocus.PIN_2

        composeTestRule.onAllNodesWithTag(pinEntryFieldTag).assertCountEquals(2)

        composeTestRule.onAllNodesWithTag(pinEntryFieldTag)[1].assertIsFocused()

        composeTestRule.onAllNodesWithTag(pinEntryFieldTag)[1].performTextInput(testPinInput2)
        testPin2State.value = testPinInput2

        composeTestRule.onAllNodesWithTag(obfuscationTestTag).assertCountEquals(10)

        val transportPinDialogTitleText = composeTestRule.activity.getString(R.string.firstTimeUser_personalPIN_error_mismatch_title)
        composeTestRule.onNodeWithText(transportPinDialogTitleText).assertDoesNotExist()

        testShouldShowErrorState.value = true

        composeTestRule.onNodeWithText(transportPinDialogTitleText).performScrollTo()
        composeTestRule.onNodeWithText(transportPinDialogTitleText).assertIsDisplayed()

        verify(exactly = 1) { mockViewModel.userInputPIN1(testPinInput) }
        verify(exactly = 1) { mockViewModel.userInputPIN2(testPinInput2) }
    }
}

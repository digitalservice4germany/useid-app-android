package de.digitalService.useID.ui.composables.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import de.digitalService.useID.R
import de.digitalService.useID.ui.composables.PINEntryField
import de.digitalService.useID.ui.theme.UseIDTheme

@Composable
fun SetPINScreen(viewModel: SetPINScreenViewModelInterface, modifier: Modifier = Modifier) {
    val focusRequesterPIN1 = remember { FocusRequester() }
    val focusRequesterPIN2 = remember { FocusRequester() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            stringResource(id = R.string.firstTimeUser_personalPIN_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PINEntryField(
                value = viewModel.pin1,
                onValueChanged = viewModel::userInputPIN1,
                focusRequester = focusRequesterPIN1
            )
            AnimatedVisibility(viewModel.shouldShowPIN2EntryField) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(id = R.string.firstTimeUser_personalPIN_confirmation),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    PINEntryField(
                        value = viewModel.pin2,
                        onValueChanged = viewModel::userInputPIN2,
                        focusRequester = focusRequesterPIN2
                    )
                }
            }
            AnimatedVisibility(viewModel.shouldShowError) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(id = R.string.firstTimeUser_personalPIN_error_mismatch_title),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    Text(
                        stringResource(id = R.string.firstTimeUser_personalPIN_error_mismatch_body),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }

    LaunchedEffect(viewModel.focus) {
        when(viewModel.focus) {
            SetPINScreenViewModelInterface.PINEntryFieldFocus.PIN_1 -> focusRequesterPIN1.requestFocus()
            SetPINScreenViewModelInterface.PINEntryFieldFocus.PIN_2 -> focusRequesterPIN2.requestFocus()
        }
    }
}

interface SetPINScreenViewModelInterface {
    enum class PINEntryFieldFocus {
        PIN_1, PIN_2
    }

    val pin1: String
    val pin2: String

    val focus: PINEntryFieldFocus

    val shouldShowPIN2EntryField: Boolean
    val shouldShowError: Boolean

    fun userInputPIN1(value: String)
    fun userInputPIN2(value: String)
}

class SetPINScreenViewModel: ViewModel(), SetPINScreenViewModelInterface {
    override var pin1 by mutableStateOf("")
        private set

    override var pin2 by mutableStateOf("")
        private set

    override var focus: SetPINScreenViewModelInterface.PINEntryFieldFocus by mutableStateOf(SetPINScreenViewModelInterface.PINEntryFieldFocus.PIN_1)
        private set

    override var shouldShowPIN2EntryField by mutableStateOf(false)
        private set

    override var shouldShowError by mutableStateOf(false)
        private set

    override fun userInputPIN1(value: String) {
        pin1 = value
        shouldShowError = false

        val pinComplete = pin1.length >= 6

        shouldShowPIN2EntryField = pinComplete || pin2.isNotEmpty()

        if (pinComplete) {
            focus = SetPINScreenViewModelInterface.PINEntryFieldFocus.PIN_2
        }
    }

    override fun userInputPIN2(value: String) {
        pin2 = value
        if (pin2.length > 5) {
            handlePINInput()
        }
    }

    private fun handlePINInput() {
        if (pin1 == pin2) {
            // Proceed to next screen
        } else {
            pin1 = ""
            pin2 = ""
            shouldShowError = true
            focus = SetPINScreenViewModelInterface.PINEntryFieldFocus.PIN_1
        }
    }
}

//region Preview
private class PreviewSetPINScreenViewModel(
    override val pin1: String,
    override val pin2: String,
    override val focus: SetPINScreenViewModelInterface.PINEntryFieldFocus,
    override val shouldShowPIN2EntryField: Boolean,
    override val shouldShowError: Boolean
) : SetPINScreenViewModelInterface {
    override fun userInputPIN1(value: String) { }
    override fun userInputPIN2(value: String) { }
}

@Preview(device = Devices.PIXEL_3A)
@Composable
fun PreviewSetPINScreen() {
    UseIDTheme {
        SetPINScreen(PreviewSetPINScreenViewModel("12", "", SetPINScreenViewModelInterface.PINEntryFieldFocus.PIN_1, false, false))
    }
}
//endregion
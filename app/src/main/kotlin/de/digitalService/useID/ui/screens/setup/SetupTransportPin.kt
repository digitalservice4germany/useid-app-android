package de.digitalService.useID.ui.screens.setup

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ramcosta.composedestinations.annotation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import de.digitalService.useID.R
import de.digitalService.useID.ui.components.Flow
import de.digitalService.useID.ui.components.NavigationButton
import de.digitalService.useID.ui.components.NavigationIcon
import de.digitalService.useID.ui.components.pin.InputType
import de.digitalService.useID.ui.components.pin.StandardNumberEntryScreen
import de.digitalService.useID.ui.coordinators.ChangePinCoordinator
import de.digitalService.useID.ui.screens.destinations.SetupTransportPinDestination
import de.digitalService.useID.ui.theme.UseIdTheme
import javax.inject.Inject

@Destination(navArgsDelegate = SetupTransportPinNavArgs::class)
@Composable
fun SetupTransportPin(
    modifier: Modifier = Modifier,
    viewModel: SetupTransportPinViewModelInterface = hiltViewModel<SetupTransportPinViewModel>()
) {
    val icon = if (viewModel.retry) {
        NavigationIcon.Cancel
    } else {
        NavigationIcon.Back
    }

    val titleString = if (viewModel.retry) {
        stringResource(id = R.string.firstTimeUser_incorrectTransportPIN_title)
    } else {
        stringResource(id = R.string.firstTimeUser_transportPIN_title)
    }

    StandardNumberEntryScreen(
        title = titleString,
        body = stringResource(id = R.string.firstTimeUser_transportPIN_body),
        attempts = null,
        navigationButton = NavigationButton(
            icon = icon,
            onClick = viewModel::onNavigationButtonClicked,
            confirmation = (if (viewModel.identificationPending) Flow.Identification else Flow.Setup).takeIf { viewModel.retry },
            contentDescription = titleString
        ),
        inputType = InputType.TransportPin,
        entryFieldDescription = "",
        onDone = viewModel::onDoneClicked
    )
}

data class SetupTransportPinNavArgs(
    val retry: Boolean,
    val identificationPending: Boolean
)

interface SetupTransportPinViewModelInterface {
    val retry: Boolean
    val identificationPending: Boolean

    fun onDoneClicked(pin: String)
    fun onNavigationButtonClicked()
}

@HiltViewModel
class SetupTransportPinViewModel @Inject constructor(
    private val changePinCoordinator: ChangePinCoordinator,
    savedStateHandle: SavedStateHandle
) :
    ViewModel(), SetupTransportPinViewModelInterface {
    override val retry: Boolean
    override val identificationPending: Boolean

    init {
        retry = SetupTransportPinDestination.argsFrom(savedStateHandle).retry
        identificationPending = SetupTransportPinDestination.argsFrom(savedStateHandle).identificationPending
    }

    override fun onDoneClicked(pin: String) {
        changePinCoordinator.onOldPinEntered(pin)
    }

    override fun onNavigationButtonClicked() {
        if (retry) {
            changePinCoordinator.cancelPinManagement()
        } else {
            changePinCoordinator.onBack()
        }
    }
}

private class PreviewSetupTransportPinViewModel(
    override val retry: Boolean,
    override val identificationPending: Boolean
) : SetupTransportPinViewModelInterface {
    override fun onDoneClicked(pin: String) {}
    override fun onNavigationButtonClicked() {}
}

@Preview(widthDp = 300, showBackground = true)
@Composable
private fun PreviewSetupTransportPinWithoutAttemptsNarrowDevice() {
    UseIdTheme {
        SetupTransportPin(viewModel = PreviewSetupTransportPinViewModel(false, false))
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetupTransportPinWithoutAttempts() {
    UseIdTheme {
        SetupTransportPin(viewModel = PreviewSetupTransportPinViewModel(false, false))
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSetupTransportPinRetry() {
    UseIdTheme {
        SetupTransportPin(viewModel = PreviewSetupTransportPinViewModel(true, false))
    }
}

package de.digitalService.useID.ui.screens.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ramcosta.composedestinations.annotation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import de.digitalService.useID.R
import de.digitalService.useID.getLogger
import de.digitalService.useID.ui.components.NavigationButton
import de.digitalService.useID.ui.components.NavigationIcon
import de.digitalService.useID.ui.components.ScreenWithTopBar
import de.digitalService.useID.ui.components.pin.TransportPinEntryField
import de.digitalService.useID.ui.coordinators.SetupCoordinator
import de.digitalService.useID.ui.screens.destinations.SetupTransportPinDestination
import de.digitalService.useID.ui.theme.UseIDTheme
import kotlinx.coroutines.delay
import javax.inject.Inject

@Destination(navArgsDelegate = SetupTransportPinNavArgs::class)
@Composable
fun SetupTransportPin(
    modifier: Modifier = Modifier,
    viewModel: SetupTransportPinViewModelInterface = hiltViewModel<SetupTransportPinViewModel>()
) {
    val resources = LocalContext.current.resources

    val icon = if (viewModel.attempts == null) {
        NavigationIcon.Back
    } else {
        NavigationIcon.Cancel
    }

    val titleString = if (viewModel.attempts == null) {
        stringResource(id = R.string.firstTimeUser_transportPIN_title)
    } else {
        stringResource(id = R.string.firstTimeUser_incorrectTransportPIN_title)
    }

    val attemptString = viewModel.attempts?.let { attempts ->
        resources.getQuantityString(
            R.plurals.firstTimeUser_transportPIN_remainingAttempts,
            attempts,
            attempts
        )
    }

    ScreenWithTopBar(
        navigationButton = NavigationButton(
            icon = icon,
            onClick = if (viewModel.attempts == null) viewModel::onBackButtonTapped else viewModel::onCancelTapped,
            shouldShowConfirmDialog = viewModel.attempts != null,
            contentDescription = "$titleString $attemptString"
        )
    ) { topPadding ->
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }

        Column(
            modifier = modifier
                .padding(horizontal = 20.dp)
                .padding(top = topPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = titleString,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.firstTimeUser_transportPIN_body),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            TransportPinEntryField(
                value = viewModel.transportPin,
                onValueChanged = viewModel::onInputChanged,
                onDone = viewModel::onDoneTapped,
                focusRequester = focusRequester
            )

            Spacer(modifier = Modifier.height(24.dp))

            attemptString?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
                )
            }
        }
    }
}

data class SetupTransportPinNavArgs(
    val attempts: Int?
)

interface SetupTransportPinViewModelInterface {
    val transportPin: String
    val attempts: Int?

    fun onInputChanged(value: String)
    fun onDoneTapped()
    fun onCancelTapped()
    fun onBackButtonTapped()
}

@HiltViewModel
class SetupTransportPinViewModel @Inject constructor(
    private val coordinator: SetupCoordinator,
    savedStateHandle: SavedStateHandle
) :
    ViewModel(), SetupTransportPinViewModelInterface {
    private val logger by getLogger()

    override val attempts: Int?

    override var transportPin: String by mutableStateOf("")
        private set

    init {
        attempts = SetupTransportPinDestination.argsFrom(savedStateHandle).attempts
    }

    override fun onInputChanged(value: String) {
        transportPin = value
    }

    override fun onDoneTapped() {
        if (transportPin.length == 5) {
            coordinator.onTransportPinEntered(transportPin)
            transportPin = ""
        } else {
            logger.debug("Transport PIN too short.")
        }
    }

    override fun onCancelTapped() {
        coordinator.cancelSetup()
    }

    override fun onBackButtonTapped() {
        coordinator.onBackTapped()
    }
}

//region Preview
private class PreviewSetupTransportPinViewModel(
    override val transportPin: String,
    override val attempts: Int?
) : SetupTransportPinViewModelInterface {
    override fun onInputChanged(value: String) {}
    override fun onDoneTapped() {}
    override fun onCancelTapped() {}
    override fun onBackButtonTapped() {}
}

@Preview(widthDp = 300, showBackground = true)
@Composable
fun PreviewSetupTransportPinWithoutAttemptsNarrowDevice() {
    UseIDTheme {
        SetupTransportPin(viewModel = PreviewSetupTransportPinViewModel("12", null))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSetupTransportPinWithoutAttempts() {
    UseIDTheme {
        SetupTransportPin(viewModel = PreviewSetupTransportPinViewModel("12", null))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSetupTransportPinOneAttempt() {
    UseIDTheme {
        SetupTransportPin(viewModel = PreviewSetupTransportPinViewModel("12", attempts = 1))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSetupTransportPinTwoAttempts() {
    UseIDTheme {
        SetupTransportPin(viewModel = PreviewSetupTransportPinViewModel("12", attempts = 2))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSetupTransportPinCancelDialog() {
    UseIDTheme {
        SetupTransportPin(viewModel = PreviewSetupTransportPinViewModel("12", attempts = 2))
    }
}
//endregion
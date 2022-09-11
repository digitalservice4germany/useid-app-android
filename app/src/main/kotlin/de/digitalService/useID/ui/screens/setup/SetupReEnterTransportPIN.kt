package de.digitalService.useID.ui.screens.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import de.digitalService.useID.R
import de.digitalService.useID.getLogger
import de.digitalService.useID.ui.components.pin.TransportPINEntryField
import de.digitalService.useID.ui.theme.UseIDTheme

@Composable
fun SetupReEnterTransportPIN(
    viewModel: SetupReEnterTransportPINViewModelInterface,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val resources = LocalContext.current.resources

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(id = R.string.firstTimeUser_transportPIN_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.firstTimeUser_transportPIN_body),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        TransportPINEntryField(
            value = viewModel.transportPIN,
            onValueChanged = viewModel::onInputChanged,
            onDone = viewModel::onDoneTapped,
            focusRequester = focusRequester
        )

        Spacer(modifier = Modifier.height(40.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(id = R.string.firstTimeUser_incorrectTransportPIN_title),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            val attemptString = if (viewModel.attempts > 0) {
                resources.getQuantityString(
                    R.plurals.firstTimeUser_transportPIN_remainingAttempts,
                    viewModel.attempts,
                    viewModel.attempts
                )
            } else {
                stringResource(id = R.string.firstTimeUser_incorrectTransportPIN_noAttemptLeft)
            }
            Text(
                attemptString,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

interface SetupReEnterTransportPINViewModelInterface {
    val transportPIN: String
    val attempts: Int

    fun onInputChanged(value: String)
    fun onDoneTapped()
}

class SetupReEnterTransportPINViewModel(
    override val attempts: Int,
    private val onDone: (String) -> Unit
) :
    ViewModel(), SetupReEnterTransportPINViewModelInterface {
    private val logger by getLogger()

    override var transportPIN: String by mutableStateOf("")
        private set

    override fun onInputChanged(value: String) {
        transportPIN = value
    }

    override fun onDoneTapped() {
        if (transportPIN.length == 5) {
            onDone(transportPIN)
        } else {
            logger.debug("Transport PIN too short.")
        }
    }
}

//region Preview
private class PreviewSetupReEnterTransportPINViewModel(
    override val transportPIN: String,
    override val attempts: Int
) : SetupReEnterTransportPINViewModelInterface {
    override fun onInputChanged(value: String) {}
    override fun onDoneTapped() {}
}

@Preview(widthDp = 300, showBackground = true)
@Composable
fun PreviewSetupReEnterTransportPINWithoutAttemptsNarrowDevice() {
    UseIDTheme {
        SetupReEnterTransportPIN(PreviewSetupReEnterTransportPINViewModel("12", 0))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSetupReEnterTransportPINWithoutAttempts() {
    UseIDTheme {
        SetupReEnterTransportPIN(PreviewSetupReEnterTransportPINViewModel("12", 0))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSetupReEnterTransportPINNullAttempts() {
    UseIDTheme {
        SetupReEnterTransportPIN(PreviewSetupReEnterTransportPINViewModel("12", 0))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSetupReEnterTransportPINOneAttempt() {
    UseIDTheme {
        SetupReEnterTransportPIN(PreviewSetupReEnterTransportPINViewModel("12", 1))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSetupReEnterTransportPINTwoAttempts() {
    UseIDTheme {
        SetupReEnterTransportPIN(PreviewSetupReEnterTransportPINViewModel("12", 2))
    }
}
//endregion

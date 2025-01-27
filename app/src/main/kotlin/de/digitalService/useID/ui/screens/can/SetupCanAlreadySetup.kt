package de.digitalService.useID.ui.screens.can

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ramcosta.composedestinations.annotation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import de.digitalService.useID.R
import de.digitalService.useID.ui.components.*
import de.digitalService.useID.ui.coordinators.CanCoordinator
import de.digitalService.useID.ui.screens.destinations.SetupCanAlreadySetupDestination
import de.digitalService.useID.ui.theme.UseIdTheme
import javax.inject.Inject

@Destination(navArgsDelegate = SetupCanAlreadySetupNavArgs::class)
@Composable
fun SetupCanAlreadySetup(viewModel: SetupCanAlreadySetupViewModelInterface = hiltViewModel<SetupCanAlreadySetupViewModel>()) {
    val bodyString = if (viewModel.identificationPending) {
        stringResource(id = R.string.firstTimeUser_can_alreadySetup_body_ident)
    } else {
        stringResource(id = R.string.firstTimeUser_can_alreadySetup_body_setup)
    }

    val buttonLabelString = if (viewModel.identificationPending) {
        stringResource(id = R.string.firstTimeUser_done_identify)
    } else {
        stringResource(id = R.string.firstTimeUser_done_close)
    }

    ScreenWithTopBar(
        navigationButton = NavigationButton(
            icon = NavigationIcon.Back,
            confirmation = null,
            onClick = viewModel::onBack
        )
    ) { topPadding ->
        StandardButtonScreen(
            primaryButton = BundButtonConfig(title = buttonLabelString, viewModel::onFinish),
            modifier = Modifier.padding(top = topPadding)
        ) { bottomPadding ->
            Column(
                modifier = Modifier
                    .padding(horizontal = UseIdTheme.spaces.m)
                    .padding(bottom = bottomPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(id = R.string.firstTimeUser_can_alreadySetup_title),
                    style = UseIdTheme.typography.headingXl,
                    color = UseIdTheme.colors.black,
                    modifier = Modifier.semantics { heading() }
                )

                Spacer(modifier = Modifier.height(UseIdTheme.spaces.m))

                Text(
                    bodyString,
                    style = UseIdTheme.typography.bodyLRegular
                )

                Spacer(modifier = Modifier.height(UseIdTheme.spaces.m))

                BundInformationButton(onClick = viewModel::onPinNotAvailable, label = stringResource(id = R.string.firstTimeUser_can_alreadySetup_personalPINNotAvailable))
            }
        }
    }
}

data class SetupCanAlreadySetupNavArgs(
    val identificationPending: Boolean
)

interface SetupCanAlreadySetupViewModelInterface {
    val identificationPending: Boolean

    fun onBack()
    fun onPinNotAvailable()
    fun onFinish()
}

@HiltViewModel
class SetupCanAlreadySetupViewModel @Inject constructor(
    private val canCoordinator: CanCoordinator,
    savedStateHandle: SavedStateHandle
) : ViewModel(), SetupCanAlreadySetupViewModelInterface {
    override val identificationPending: Boolean

    init {
        identificationPending = SetupCanAlreadySetupDestination.argsFrom(savedStateHandle).identificationPending
    }

    override fun onBack() {
        canCoordinator.onBack()
    }

    override fun onPinNotAvailable() {
        canCoordinator.onResetPin()
    }

    override fun onFinish() {
        canCoordinator.skipCanFlow()
    }
}

private class PreviewSetupCanAlreadySetupViewModel : SetupCanAlreadySetupViewModelInterface {
    override val identificationPending: Boolean = false
    override fun onPinNotAvailable() {}
    override fun onBack() {}
    override fun onFinish() {}
}

@Preview
@Composable
private fun Preview() {
    UseIdTheme {
        SetupCanAlreadySetup(PreviewSetupCanAlreadySetupViewModel())
    }
}

package de.digitalService.useID.ui.composables.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.digitalService.useID.ui.AppCoordinator
import de.digitalService.useID.ui.composables.screens.identification.IdentificationScan
import de.digitalService.useID.ui.composables.screens.identification.IdentificationScanViewModelInterface
import de.digitalService.useID.ui.coordinators.IdentificationCoordinator
import de.digitalService.useID.ui.theme.UseIDTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun EmulatorIdentificationScan(viewModel: EmulatorIdentificationScanViewModel) {
    Column(verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxSize()) {
        IdentificationScan(modifier = Modifier.fillMaxHeight(0.9f), viewModel.innerViewModel)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Button(onClick = { viewModel.simulateSuccess() }) { Text("✅") }
            Button(onClick = { viewModel.simulateIncorrectPIN() }) { Text("❌") }
            Button(onClick = { viewModel.simulateCANRequired() }) { Text("CAN") }
            Button(onClick = { viewModel.simulatePUKRequired() }) { Text("PUK") }
        }
    }
}

@HiltViewModel
class EmulatorIdentificationScanViewModel @Inject constructor(private val coordinator: IdentificationCoordinator) : ViewModel() {
    fun simulateSuccess() {
        viewModelScope.launch {
            innerViewModel.injectShouldShowProgress(true)
            delay(3000L)
            innerViewModel.injectShouldShowProgress(false)
            coordinator.onIDInteractionFinishedSuccessfully()
        }
    }
    fun simulateIncorrectPIN() {  }
    fun simulateCANRequired() {  }
    fun simulatePUKRequired() {  }

    val innerViewModel = object : IdentificationScanViewModelInterfaceExtension {
        override var shouldShowProgress by mutableStateOf(false)
        override fun onHelpButtonTapped() {}

        override fun injectShouldShowProgress(show: Boolean) {
            shouldShowProgress = show
        }
    }

    interface IdentificationScanViewModelInterfaceExtension : IdentificationScanViewModelInterface {
        fun injectShouldShowProgress(show: Boolean)
    }
}

@Preview(device = Devices.PIXEL_3A)
@Composable
fun PreviewEmulatorIdentificationScan() {
    UseIDTheme {
        EmulatorIdentificationScan(
            EmulatorIdentificationScanViewModel(
                IdentificationCoordinator(
                    AppCoordinator()
                )
            )
        )
    }
}
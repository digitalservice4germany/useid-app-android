package de.digitalService.useID.ui.screens.error

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import de.digitalService.useID.R
import de.digitalService.useID.ui.components.ScanErrorScreen
import de.digitalService.useID.ui.screens.error.viewModel.SetupCardErrorViewModel

@Destination
@Composable
fun SetupOtherError(viewModel: SetupCardErrorViewModel = hiltViewModel()) {
    ScanErrorScreen(
        titleResId = R.string.scanError_unknown_title,
        bodyResId = R.string.scanError_unknown_body,
        buttonTitleResId = R.string.scanError_close,
        confirmNavigationButtonDialog = false,
        onNavigationButtonClicked = viewModel::onNavigationButtonClicked,
        onButtonClicked = viewModel::onButtonClicked
    )
}

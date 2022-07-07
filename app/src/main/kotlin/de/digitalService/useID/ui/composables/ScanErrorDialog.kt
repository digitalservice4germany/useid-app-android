package de.digitalService.useID.ui.composables

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.digitalService.useID.ui.ScanError

@Composable
fun ScanErrorAlertDialog(error: ScanError, onButtonTap: () -> Unit) {
    StandardDialog(
        title = { Text(stringResource(id = error.titleResID), style = MaterialTheme.typography.titleMedium) },
        text = { Text(stringResource(id = error.textResID), style = MaterialTheme.typography.bodySmall) },
        onButtonTap = onButtonTap
    )
}
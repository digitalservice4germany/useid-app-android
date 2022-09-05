package de.digitalService.useID.ui.screens.information

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import de.digitalService.useID.R
import de.digitalService.useID.ui.components.HtmlText
import de.digitalService.useID.ui.theme.UseIDTheme
import dev.jeziellago.compose.markdowntext.MarkdownText

@Destination
@Composable
fun TermsOfUseScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        MarkdownText(markdown = stringResource(id = R.string.termsOfUse_html_text))
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    UseIDTheme {
        TermsOfUseScreen()
    }
}

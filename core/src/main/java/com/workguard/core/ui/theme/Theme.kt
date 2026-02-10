package com.workguard.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.workguard.core.R

@Composable
fun WorkGuardTheme(content: @Composable () -> Unit) {
    val poppins = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_semibold, FontWeight.SemiBold)
    )

    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography(defaultFontFamily = poppins),
        shapes = Shapes(),
        content = content
    )
}

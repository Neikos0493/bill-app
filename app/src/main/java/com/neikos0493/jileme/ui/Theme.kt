package com.neikos0493.jileme.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light= lightColorScheme(primary=Color(0xFF315C4C),secondary=Color(0xFF6B7F76),tertiary=Color(0xFF9A6B38),background=Color(0xFFFAFAF8),surface=Color(0xFFFFFFFF),surfaceVariant=Color(0xFFEFF2EE))
private val Dark= darkColorScheme(primary=Color(0xFF9BCBB4),secondary=Color(0xFFB0C5B9),tertiary=Color(0xFFE1B989))
@Composable fun JilemeTheme(mode: String, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=if(mode=="DARK" || (mode=="SYSTEM" && isSystemInDarkTheme())) Dark else Light, content=content)
}

package com.xmvisio.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun PlatformInfo() {
    val os = System.getProperty("os.name")
    val icon = when {
        os.contains("Mac") -> "🍎"
        os.contains("Windows") -> "🪟"
        else -> "🐧"
    }
    
    Card(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "$icon Running on Desktop: $os",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

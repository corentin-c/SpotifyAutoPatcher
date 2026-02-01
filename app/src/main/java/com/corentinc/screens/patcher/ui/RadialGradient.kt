package com.corentinc.screens.patcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun VerticalGradientBox() {
    Surface {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .alpha(0.3f)
        )
    }
}

@Preview
@Composable
fun VerticalBoxPreview() {
    VerticalGradientBox()
}


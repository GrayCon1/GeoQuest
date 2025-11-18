package com.prog7314.geoquest.components.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen loading overlay with circular progress indicator.
 * Displays over content while loading is in progress.
 *
 * @param isLoading Whether to show the loading overlay
 * @param message Optional loading message to display
 * @param modifier Optional modifier for the overlay
 * @param backgroundColor Background color of the overlay (default: semi-transparent black)
 * @param indicatorColor Color of the loading indicator (default: primary blue)
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    backgroundColor: Color = Color.Black.copy(alpha = 0.5f),
    indicatorColor: Color = Color(0xFF4A90E2)
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = indicatorColor,
                    modifier = Modifier.size(48.dp)
                )

                if (message != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}

/**
 * Centered loading indicator without overlay background.
 * Useful for loading states within a specific area.
 *
 * @param modifier Optional modifier for the indicator
 * @param color Color of the loading indicator (default: primary blue)
 * @param message Optional loading message to display below indicator
 */
@Composable
fun CenteredLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4A90E2),
    message: String? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = color,
                modifier = Modifier.size(48.dp)
            )

            if (message != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

/**
 * Inline loading indicator for buttons or small spaces.
 *
 * @param modifier Optional modifier for the indicator
 * @param color Color of the loading indicator (default: white)
 * @param size Size of the indicator (default: 24dp)
 */
@Composable
fun InlineLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Int = 24
) {
    CircularProgressIndicator(
        color = color,
        modifier = modifier.size(size.dp)
    )
}


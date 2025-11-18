package com.prog7314.geoquest.components.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorText(
    message: String,
    modifier: Modifier = Modifier
) {
    if (message.isNotEmpty()) {
        Text(
            text = message,
            color = Color.Red,
            fontSize = 14.sp,
            modifier = modifier.padding(bottom = 16.dp)
        )
    }
}


package com.prog7314.geoquest.components.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prog7314.geoquest.data.data.LocationData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable card component for displaying location entries in lists.
 * Used in LogbookScreen and other location listing screens.
 *
 * @param location The location data to display
 * @param onClick Callback when the card is clicked
 * @param modifier Optional modifier for the card
 * @param maxDescriptionLength Maximum characters to show in description (default: 100)
 */
@Composable
fun LocationCard(
    location: LocationData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxDescriptionLength: Int = 100
) {
    val formattedDate = remember(location.dateAdded) {
        val date = Date(location.dateAdded)
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Location name
            Text(
                text = location.name,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Location description (truncated if too long)
            Text(
                text = if (location.description.length > maxDescriptionLength) {
                    location.description.take(maxDescriptionLength) + "..."
                } else {
                    location.description
                },
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date added
            Text(
                text = "Added on: $formattedDate",
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}


package com.prog7314.geoquest.components.cards

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prog7314.geoquest.data.data.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Load image bitmap when imageUri changes
    LaunchedEffect(location.imageUri) {
        imageBitmap = if (location.imageUri != null && location.imageUri.isNotEmpty()) {
            try {
                val uri = Uri.parse(location.imageUri)
                // Load bitmap on IO dispatcher to avoid blocking UI thread
                val bitmap = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val decodedBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    decodedBitmap
                }
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Image preview if available
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = location.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Location name
                Text(
                    text = location.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF2C3E50),
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location description (truncated if too long)
                Text(
                    text = if (location.description.length > maxDescriptionLength) {
                        location.description.take(maxDescriptionLength) + "..."
                    } else {
                        location.description
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date added
                Text(
                    text = "Added on: $formattedDate",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.align(Alignment.End),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}


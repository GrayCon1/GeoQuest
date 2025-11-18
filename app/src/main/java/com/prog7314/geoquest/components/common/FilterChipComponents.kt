package com.prog7314.geoquest.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable filter chip component for selection filters.
 *
 * @param label The text to display on the chip
 * @param selected Whether the chip is currently selected
 * @param onClick Callback when the chip is clicked
 * @param modifier Optional modifier for the chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Selected",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
        modifier = modifier
    )
}

/**
 * Row of filter chips for common filter patterns.
 * Useful for date filters, category filters, etc.
 *
 * @param items List of filter items to display
 * @param selectedItem Currently selected item
 * @param onItemSelected Callback when an item is selected
 * @param modifier Optional modifier for the row
 * @param itemLabel Lambda to convert item to display label
 */
@Composable
fun <T> FilterChipRow(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String = { it.toString() }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            StyledFilterChip(
                label = itemLabel(item),
                selected = selectedItem == item,
                onClick = { onItemSelected(item) }
            )
        }
    }
}


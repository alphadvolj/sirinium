package com.dlab.sirinium.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Shapes
 * Distinctive curves with oversized radii for hero containers and tactile pill shapes.
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),        // Standard lesson cards & modals
    extraLarge = RoundedCornerShape(32.dp)    // Hero active pair banner & floating sheets
)

package com.dlab.sirinium.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import sirinium_dev.shared.generated.resources.Res
import sirinium_dev.shared.generated.resources.app_logo

@Composable
fun SiriniumAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    cornerRadius: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.app_logo),
            contentDescription = "Sirinium Logo",
            modifier = Modifier.size(size)
        )
    }
}

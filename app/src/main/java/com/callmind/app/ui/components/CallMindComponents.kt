package com.callmind.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callmind.app.ui.theme.AvatarColors
import com.callmind.app.ui.theme.CallImported
import com.callmind.app.ui.theme.CallIncoming
import com.callmind.app.ui.theme.CallMissed
import com.callmind.app.ui.theme.CallOutgoing
import com.callmind.app.ui.theme.TextSecondary
import kotlin.math.absoluteValue

fun avatarColor(name: String): Color {
    val index = name.hashCode().absoluteValue % AvatarColors.size
    return AvatarColors[index]
}

@Composable
fun ContactAvatar(
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val displayName = name?.ifBlank { "?" } ?: "?"
    val initial = displayName.first().uppercaseChar()
    val color = avatarColor(displayName)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            color = Color.White,
            style = when {
                size >= 80.dp -> MaterialTheme.typography.headlineMedium
                size >= 48.dp -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CallTypeIcon(
    callType: String,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (callType.uppercase()) {
        "INCOMING" -> Icons.AutoMirrored.Filled.CallReceived to CallIncoming
        "OUTGOING" -> Icons.AutoMirrored.Filled.CallMade to CallOutgoing
        "MISSED" -> Icons.AutoMirrored.Filled.CallMissed to CallMissed
        "IMPORTED" -> Icons.Default.FileDownload to CallImported
        else -> Icons.Default.Call to TextSecondary
    }

    Icon(
        imageVector = icon,
        contentDescription = callType,
        tint = color,
        modifier = modifier.size(18.dp)
    )
}

fun callTypeColor(callType: String): Color = when (callType.uppercase()) {
    "MISSED" -> CallMissed
    "IMPORTED" -> CallImported
    else -> Color.White
}

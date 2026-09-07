package com.vacart.presentation.home.util

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.leftLine (): Modifier {
    return drawWithCache {
        onDrawWithContent {
            // draw behind the content the vertical line on the left
            drawLine(
                color = Color.Black,
                start = Offset.Zero,
                end = Offset(0f, this.size.height),
                strokeWidth= 1f
            )

            // draw the content
            drawContent()
        }
    }
}
fun Modifier.rightLine (): Modifier {
    return drawWithCache {
        onDrawWithContent {
            // draw behind the content the vertical line on the left
            drawLine(
                color = Color.Black,
                start = Offset(this.size.width, 0f),
                end = Offset(this.size.width, this.size.height),
                strokeWidth= 1f
            )
            // draw the content
            drawContent()
        }
    }
}

// ---------------------------------------------------------------------------
// Offline / saved data UI components
// ---------------------------------------------------------------------------

@Composable
fun OfflineDataBanner(cachedAt: Long, isStale: Boolean = false) {
    val timeAgo = formatTimeAgo(cachedAt)
    val label = if (isStale) "⚠️ Stale data · cached $timeAgo" else "Cached data · $timeAgo"
    val containerColor = if (isStale)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.tertiaryContainer
    val contentColor = if (isStale)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onTertiaryContainer

    Surface(
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

@Composable
fun SavedDataBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkAdded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "📌 Saved snapshot · available offline",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

fun formatTimeAgo(epochMs: Long): String {
    val diffMs = System.currentTimeMillis() - epochMs
    val minutes = diffMs / 60_000
    val hours = minutes / 60
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "${hours / 24}d ago"
    }
}

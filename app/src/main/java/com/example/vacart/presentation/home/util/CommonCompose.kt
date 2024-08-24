package com.example.vacart.presentation.home.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

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
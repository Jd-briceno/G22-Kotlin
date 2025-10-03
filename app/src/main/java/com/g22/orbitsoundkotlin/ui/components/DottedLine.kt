package com.g22.orbitsoundkotlin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Composable
fun DottedLine(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFB4B1B8),
    strokeWidth: Float = 2f,
    dashWidth: Float = 6f,
    dashSpace: Float = 4f
) {
    Canvas(
        modifier = modifier
            .width(320.dp)
            .height(2.dp)
    ) {
        drawDottedLine(
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            color = color,
            strokeWidth = strokeWidth,
            dashWidth = dashWidth,
            dashSpace = dashSpace
        )
    }
}

private fun DrawScope.drawDottedLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    dashWidth: Float,
    dashSpace: Float
) {
    val distance = (end - start).getDistance()
    val dashCount = (distance / (dashWidth + dashSpace)).toInt()

    for (i in 0 until dashCount) {
        val startX = start.x + i * (dashWidth + dashSpace)
        val endX = startX + dashWidth

        drawLine(
            color = color,
            start = Offset(startX, start.y),
            end = Offset(endX, start.y),
            strokeWidth = strokeWidth
        )
    }
}

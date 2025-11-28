package com.g22.orbitsoundkotlin.ui.screens.activitystats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityItem
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityStatsPeriod
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityStatChip
import com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry

/**
 * Selector de período con chips.
 */
@Composable
fun PeriodSelectorChips(
    selectedPeriod: ActivityStatsPeriod,
    onPeriodSelected: (ActivityStatsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActivityStatsPeriod.values().forEach { period ->
            PeriodChip(
                period = period,
                isSelected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) }
            )
        }
    }
}

@Composable
private fun PeriodChip(
    period: ActivityStatsPeriod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = Color(0xFF5099BA),
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                color = if (isSelected) Color(0xFF5099BA) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = period.displayName,
            style = TextStyle(
                fontSize = 12.sp,
                color = if (isSelected) Color.White else Color(0xFF5099BA),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

/**
 * Card de estadística simple.
 */
@Composable
fun ActivityStatCard(
    title: String,
    value: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF24292E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF5099BA),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5099BA)
                )
            )
        }
    }
}

/**
 * Item de lista de actividad reciente.
 */
@Composable
fun ActivityListItem(
    activity: ActivityItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF24292E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = activity.dateTime,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = activity.summary,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                activity.stats.forEach { stat ->
                    ActivityStatChip(stat = stat)
                }
            }
        }
    }
}

@Composable
private fun ActivityStatChip(
    stat: ActivityStatChip,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color(0xFF5099BA),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${stat.value} ${stat.label}",
            style = TextStyle(
                fontSize = 10.sp,
                color = Color(0xFF5099BA)
            )
        )
    }
}

/**
 * Card de entrada de diario previa.
 */
@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF24292E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = entry.date,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (entry.text.length > 100) {
                    entry.text.take(100) + "..."
                } else {
                    entry.text
                },
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                ),
                maxLines = 3
            )
        }
    }
}


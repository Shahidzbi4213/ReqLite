package com.learn.reqlite.ui.response

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResponseStatusSummary(
    response: HttpResponseUiModel,
    modifier: Modifier = Modifier
) {
    val statusColors = getStatusBadgeColors(response.statusCategory)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status Code & Reason Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColors.backgroundColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = response.statusDescription,
                    color = statusColors.contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Duration Pill
            ResponsePill(
                label = response.formattedDuration,
                icon = "⏱"
            )

            // Size Pill
            ResponsePill(
                label = response.formattedSize,
                icon = "📦"
            )

            // Content Type Pill
            response.contentType?.let { cType ->
                val simplifiedType = when {
                    cType.contains("json", ignoreCase = true) -> "JSON"
                    cType.contains("html", ignoreCase = true) -> "HTML"
                    cType.contains("xml", ignoreCase = true) -> "XML"
                    cType.contains("text", ignoreCase = true) -> "TEXT"
                    else -> cType.substringBefore(";").trim()
                }
                ResponsePill(
                    label = simplifiedType,
                    icon = "📄"
                )
            }
        }
    }
}

@Composable
fun ResponsePill(
    label: String,
    icon: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Text(text = icon, fontSize = 11.sp)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class StatusBadgeColors(
    val backgroundColor: Color,
    val contentColor: Color
)

@Composable
fun getStatusBadgeColors(category: HttpStatusCategory): StatusBadgeColors {
    return when (category) {
        HttpStatusCategory.SUCCESS -> StatusBadgeColors(
            backgroundColor = Color(0xFF2E7D32),
            contentColor = Color.White
        )
        HttpStatusCategory.REDIRECT -> StatusBadgeColors(
            backgroundColor = Color(0xFF0288D1),
            contentColor = Color.White
        )
        HttpStatusCategory.CLIENT_ERROR -> StatusBadgeColors(
            backgroundColor = Color(0xFFE65100),
            contentColor = Color.White
        )
        HttpStatusCategory.SERVER_ERROR -> StatusBadgeColors(
            backgroundColor = Color(0xFFC62828),
            contentColor = Color.White
        )
        HttpStatusCategory.UNKNOWN -> StatusBadgeColors(
            backgroundColor = MaterialTheme.colorScheme.outline,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

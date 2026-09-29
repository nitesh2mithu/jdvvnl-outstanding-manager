package com.example.outstandingmanager.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.outstandingmanager.model.AppStatus

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val appStatus = AppStatus.fromString(status)
    val (bgColor, textColor, borderColor) = when (appStatus) {
        AppStatus.PAYMENT_PROMISED -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFFFFB74D))
        AppStatus.VISITED -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), Color(0xFF90CAF9))
        AppStatus.PART_PAYMENT -> Triple(Color(0xFFE0F2F1), Color(0xFF00695C), Color(0xFF80CBC4))
        AppStatus.PAID -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFFA5D6A7))
        AppStatus.DISCONNECTED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Color(0xFFEF9A9A))
        AppStatus.DISPUTE -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), Color(0xFFCE93D8))
        AppStatus.OTHERS -> Triple(Color(0xFFEFEBE9), Color(0xFF4E342E), Color(0xFFBCAAA4))
        AppStatus.NO_STATUS -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), Color(0xFFE0E0E0))
    }

    val displayText = if (status.isBlank()) AppStatus.NO_STATUS.displayName else status

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = displayText,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

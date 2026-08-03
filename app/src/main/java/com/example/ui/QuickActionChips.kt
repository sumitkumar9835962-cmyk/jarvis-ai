package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBorderCyan
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.TechSurfaceVariant
import com.example.ui.theme.TextCyanLight

data class QuickCommand(
    val label: String,
    val prompt: String
)

val defaultQuickCommands = listOf(
    QuickCommand("👋 Hello Jarvis", "Hello Jarvis"),
    QuickCommand("🎵 Play Song", "Play Roshan Rohi song"),
    QuickCommand("💬 WhatsApp", "Send WhatsApp message to Ajay saying Main ja raha hoon."),
    QuickCommand("▶️ YouTube", "Open YouTube Roshan Rohi song"),
    QuickCommand("💡 Flashlight ON", "Turn on flashlight"),
    QuickCommand("🔌 Flashlight OFF", "Turn off flashlight"),
    QuickCommand("📷 Camera", "Open Camera"),
    QuickCommand("📱 Open App", "Open Calculator")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionChips(
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(defaultQuickCommands) { cmd ->
            FilterChip(
                selected = false,
                onClick = { onCommandClick(cmd.prompt) },
                label = {
                    Text(
                        text = cmd.label,
                        color = TextCyanLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = TechSurfaceVariant,
                    labelColor = TextCyanLight
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = CardBorderCyan,
                    borderWidth = 1.dp
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

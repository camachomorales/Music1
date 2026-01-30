package com.example.music.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music.data.model.AudioQuality
import com.example.music.data.model.EqualizerPreset
import com.example.music.data.model.UserPreferences

@Composable
fun PlaybackSettingsScreen(
    userPreferences: UserPreferences,
    onAudioQualityChange: (AudioQuality) -> Unit,
    onStreamingQualityChange: (AudioQuality) -> Unit,
    onDownloadQualityChange: (AudioQuality) -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    onToggleGapless: () -> Unit,
    onToggleNormalize: () -> Unit,
    onEqualizerChange: (EqualizerPreset) -> Unit,
    onBackClick: () -> Unit
) {
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showStreamingQualityDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Playback",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Audio Quality Section
        Text(
            text = "Audio Quality",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        SettingsCard(
            icon = Icons.Default.HighQuality,
            title = "Local Music Quality",
            subtitle = userPreferences.audioQuality.displayName,
            onClick = { showAudioQualityDialog = true }
        )

        SettingsCard(
            icon = Icons.Default.CloudQueue,
            title = "Streaming Quality",
            subtitle = "${userPreferences.streamingQuality.displayName} • While online",
            onClick = { showStreamingQualityDialog = true }
        )

        SettingsCard(
            icon = Icons.Default.Download,
            title = "Download Quality",
            subtitle = "${userPreferences.downloadQuality.displayName} • For offline",
            onClick = { showDownloadQualityDialog = true }
        )

        // Playback Features
        Text(
            text = "Playback Features",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        SettingsCardWithToggle(
            icon = Icons.Default.Merge,
            title = "Gapless Playback",
            subtitle = "Seamless transitions between songs",
            isEnabled = userPreferences.gaplessPlayback,
            onToggle = { onToggleGapless() }
        )

        SettingsCardWithToggle(
            icon = Icons.Default.VolumeUp,
            title = "Normalize Volume",
            subtitle = "Balance volume across different songs",
            isEnabled = userPreferences.normalizeVolume,
            onToggle = { onToggleNormalize() }
        )

        // Crossfade
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = Color(0xFF00D9FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Crossfade",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "${userPreferences.crossfadeDuration}s",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = userPreferences.crossfadeDuration.toFloat(),
                onValueChange = { onCrossfadeChange(it.toInt()) },
                valueRange = 0f..12f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF00D9FF),
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0s", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                Text("12s", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }

        // Equalizer
        Text(
            text = "Sound",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        SettingsCard(
            icon = Icons.Default.Equalizer,
            title = "Equalizer",
            subtitle = userPreferences.equalizerPreset.displayName,
            onClick = { showEqualizerDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Dialogs
    if (showAudioQualityDialog) {
        QualitySelectionDialog(
            title = "Local Music Quality",
            currentQuality = userPreferences.audioQuality,
            onDismiss = { showAudioQualityDialog = false },
            onSelect = {
                onAudioQualityChange(it)
                showAudioQualityDialog = false
            }
        )
    }

    if (showStreamingQualityDialog) {
        QualitySelectionDialog(
            title = "Streaming Quality",
            subtitle = "Quality while listening online",
            currentQuality = userPreferences.streamingQuality,
            onDismiss = { showStreamingQualityDialog = false },
            onSelect = {
                onStreamingQualityChange(it)
                showStreamingQualityDialog = false
            }
        )
    }

    if (showDownloadQualityDialog) {
        QualitySelectionDialog(
            title = "Download Quality",
            subtitle = "Quality for offline downloads",
            currentQuality = userPreferences.downloadQuality,
            onDismiss = { showDownloadQualityDialog = false },
            onSelect = {
                onDownloadQualityChange(it)
                showDownloadQualityDialog = false
            }
        )
    }

    if (showEqualizerDialog) {
        EqualizerDialog(
            currentPreset = userPreferences.equalizerPreset,
            onDismiss = { showEqualizerDialog = false },
            onSelect = {
                onEqualizerChange(it)
                showEqualizerDialog = false
            }
        )
    }
}

@Composable
private fun QualitySelectionDialog(
    title: String,
    subtitle: String? = null,
    currentQuality: AudioQuality,
    onDismiss: () -> Unit,
    onSelect: (AudioQuality) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A2F3D),
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (quality == currentQuality)
                                    Color(0xFF00D9FF).copy(alpha = 0.2f)
                                else
                                    Color.Transparent
                            )
                            .clickable { onSelect(quality) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quality.displayName,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        if (quality == currentQuality) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00D9FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun EqualizerDialog(
    currentPreset: EqualizerPreset,
    onDismiss: () -> Unit,
    onSelect: (EqualizerPreset) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A2F3D),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Equalizer Preset",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqualizerPreset.entries.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (preset == currentPreset)
                                    Color(0xFF00D9FF).copy(alpha = 0.2f)
                                else
                                    Color.Transparent
                            )
                            .clickable { onSelect(preset) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preset.displayName,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        if (preset == currentPreset) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00D9FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}
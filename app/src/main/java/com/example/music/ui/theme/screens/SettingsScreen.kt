package com.example.music.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*  // ¡IMPORTANTE!
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music.data.model.AccountType
import com.example.music.data.model.AppMode
import com.example.music.data.model.UserPreferences

@Composable
fun SettingsScreen(
    appMode: AppMode,
    userPreferences: UserPreferences,
    accountType: AccountType,
    onToggleMode: () -> Unit,
    onAccountClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onStorageClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onToggleDownload: () -> Unit,
    onClearCache: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        Text(
            text = "Settings",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Online/Offline Mode Toggle
        SettingsCardWithToggle(
            icon = if (appMode == AppMode.STREAMING) Icons.Default.Cloud else Icons.Default.CloudOff,
            title = if (appMode == AppMode.STREAMING) "Online Mode" else "Offline Mode",
            subtitle = if (appMode == AppMode.STREAMING)
                "Streaming from YouTube Music & JioSaavn"
            else
                "Playing local music from device",
            isEnabled = appMode == AppMode.STREAMING,
            onToggle = { onToggleMode() }
        )

        // Account Section
        SettingsCard(
            icon = when (accountType) {
                AccountType.ADMIN -> Icons.Default.AdminPanelSettings
                AccountType.LOCAL -> Icons.Default.AccountCircle
                AccountType.GUEST -> Icons.Default.PersonOutline
            },
            title = "Account",
            subtitle = when (accountType) {
                AccountType.ADMIN -> "👑 ${userPreferences.userName} (Developer)"
                AccountType.LOCAL -> userPreferences.userEmail ?: "Manage account"
                AccountType.GUEST -> "Sign in to sync your data"
            },
            onClick = onAccountClick
        )

        // Playback Settings
        SettingsCard(
            icon = Icons.Default.MusicNote,
            title = "Playback",
            subtitle = "${userPreferences.audioQuality.displayName} • ${if (userPreferences.gaplessPlayback) "Gapless on" else "Gapless off"}",
            onClick = onPlaybackClick
        )

        // Storage
        SettingsCard(
            icon = Icons.Default.Storage,
            title = "Storage",
            subtitle = "Manage downloads and cache",
            onClick = onStorageClick
        )

        // Notifications
        SettingsCard(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = if (userPreferences.showNotifications) "Enabled" else "Disabled",
            onClick = onNotificationsClick
        )

        // Download over mobile data toggle
        SettingsCardWithToggle(
            icon = Icons.Default.Download,
            title = "Download over mobile data",
            subtitle = "Allow downloads without WiFi",
            isEnabled = !userPreferences.downloadOnlyOnWifi,
            onToggle = { onToggleDownload() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.2f),
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Appearance
        SettingsCardWithToggle(
            icon = Icons.Default.DarkMode,
            title = "Dark theme",
            subtitle = "Use dark theme throughout the app",
            isEnabled = userPreferences.isDarkTheme,
            onToggle = { onToggleDarkTheme() }
        )

        // About
        SettingsCard(
            icon = Icons.Default.Info,
            title = "About",
            subtitle = "Version 1.0.0 • Licenses & Credits",
            onClick = onAboutClick
        )

        // Clear Cache
        SettingsCard(
            icon = Icons.Default.DeleteOutline,
            title = "Clear cache",
            subtitle = "Free up ${userPreferences.maxCacheSize}MB of storage",
            onClick = onClearCache
        )

        // Admin Section
        if (accountType == AccountType.ADMIN) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Developer Options",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFBE0B)
            )

            SettingsCardWithToggle(
                icon = Icons.Default.Code,
                title = "Developer Mode",
                subtitle = "Show debug information",
                isEnabled = userPreferences.developerMode,
                onToggle = { /* TODO */ }
            )

            SettingsCardWithToggle(
                icon = Icons.Default.Science,
                title = "Experimental Features",
                subtitle = "Enable beta features",
                isEnabled = userPreferences.allowExperimentalFeatures,
                onToggle = { /* TODO */ }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Reutilizar SettingsCard y SettingsCardWithToggle del original
@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF00D9FF),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SettingsCardWithToggle(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF00D9FF),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00D9FF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
    }
}
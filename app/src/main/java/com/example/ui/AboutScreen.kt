package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TileBorderDark
import com.example.ui.theme.TileBorderLight
import `in`.sreerajp.vault_files.BuildConfig
import `in`.sreerajp.vault_files.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tileBorder = BorderStroke(1.dp, if (isDark) TileBorderDark else TileBorderLight)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("about_screen"),
        contentPadding = PaddingValues(top = statusBarTop + 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSubPageHeader(
                title = stringResource(R.string.about_title),
                onBack = onBack,
                border = tileBorder,
                backTestTag = "about_back_btn"
            )
        }

        item {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        item { SettingsSectionLabel(stringResource(R.string.about_build_info), Modifier.padding(top = 12.dp)) }

        item { AboutInfoRow(Icons.Default.Person, stringResource(R.string.about_author), BuildConfig.AUTHOR, tileBorder) }
        item { AboutInfoRow(Icons.Default.Code, stringResource(R.string.about_ide_used), BuildConfig.IDE, tileBorder) }
        item { AboutInfoRow(Icons.Default.SmartToy, stringResource(R.string.about_ai_version), BuildConfig.AI_VERSION, tileBorder) }
        item { AboutInfoRow(Icons.Default.Build, stringResource(R.string.about_last_build_date), BuildConfig.BUILD_DATE, tileBorder) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.about_made_with),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.cd_love),
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.about_from_india),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun AboutInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    border: BorderStroke
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsIconChip(icon, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

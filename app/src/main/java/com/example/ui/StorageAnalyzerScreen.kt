package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StorageStats
import `in`.sreerajp.vault_files.R
import kotlin.math.roundToInt

fun hasAllFilesPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

// --- "Zen Bento" design palette (light / dark aware) ---

private val CatImages = Color(0xFF5B8DEF)
private val CatVideos = Color(0xFFF2994A)
private val CatAudio = Color(0xFF2FB39A)
private val CatDocuments = Color(0xFFE0A93B)
private val CatArchives = Color(0xFF9B59B6)
private val CatOther = Color(0xFF95A5A6)

private data class CategoryData(
    @param:androidx.annotation.StringRes val titleRes: Int,
    val bytes: Long,
    val color: Color,
    val icon: ImageVector,
    val testTag: String,
    // Canonical category string used by the repository/filter ("Image", "Video", ...).
    val category: String
)

@Composable
private fun ringGradientColors(): List<Color> = if (isSystemInDarkTheme()) {
    listOf(Color(0xFFE08763), Color(0xFFD2A491), Color(0xFFE0A93B))
} else {
    listOf(Color(0xFFBF4A2E), Color(0xFFCF8A6B), Color(0xFFE0A93B))
}

@Composable
private fun vaultGradientColors(): List<Color> = if (isSystemInDarkTheme()) {
    listOf(Color(0xFFE08763), Color(0xFFB85A3C))
} else {
    listOf(Color(0xFFCF6B47), Color(0xFFBF4A2E))
}

@Composable
private fun accentColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFE08763) else Color(0xFFBF4A2E)

@Composable
fun StorageAnalyzerScreen(
    viewModel: StorageViewModel,
    modifier: Modifier = Modifier,
    onOpenVault: () -> Unit = {},
    onOpenFilesWithCategory: (String) -> Unit = {}
) {
    val statsState by viewModel.storageStats.collectAsState()
    val currentMode by viewModel.storageSourceMode.collectAsState()
    val vaultFiles by viewModel.vaultFiles.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasAllFilesPermission(context)) }

    // Recheck permission status when app returns to focus
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = hasAllFilesPermission(context)
                viewModel.refreshStorageStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val stats = statsState

    // Keep content below the system status bar while the background still fills edge-to-edge.
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp + statusBarTop, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Scrolling header
        item {
            Column {
                Text(
                    text = stringResource(R.string.storage_analysis_title),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (currentMode == "device") stringResource(R.string.storage_scope_device) else stringResource(R.string.storage_scope_sandbox),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Source selector pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("storage_source_card"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SourcePill(
                    label = stringResource(R.string.source_app_sandbox),
                    selected = currentMode == "sandbox",
                    icon = Icons.Default.GridView,
                    onClick = { viewModel.updateStorageSourceMode("sandbox") },
                    modifier = Modifier.weight(1f).testTag("select_sandbox_chip")
                )
                SourcePill(
                    label = stringResource(R.string.source_entire_device),
                    selected = currentMode == "device",
                    icon = Icons.Default.PhoneAndroid,
                    onClick = { viewModel.updateStorageSourceMode("device") },
                    modifier = Modifier.weight(1f).testTag("select_device_chip")
                )
            }
        }

        if (currentMode == "device" && !hasPermission) {
            // 3. Permission Prompt Card (functionally unchanged)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("permission_request_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.cd_permission_required),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = stringResource(R.string.perm_all_files_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.perm_all_files_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            hasPermission = isGranted
                            viewModel.refreshStorageStats()
                        }

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    try {
                                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                            context.startActivity(intent)
                                        } catch (ex: Exception) {
                                            viewModel.dispatchMessage(context.getString(R.string.msg_cannot_launch_settings_files))
                                        }
                                    }
                                } else {
                                    launcher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("grant_permission_btn")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.perm_grant_storage_access))
                        }
                    }
                }
            }
        } else if (stats == null) {
            // 4. Loading Indicator
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // 5. Bento top row: storage ring tile + vault tile
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .testTag("storage_overview_card"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StorageRingTile(
                        stats = stats,
                        modifier = Modifier.weight(1.25f)
                    )
                    VaultTile(
                        fileCount = vaultFiles.size,
                        encryptedBytes = vaultFiles.sumOf { it.fileSize },
                        onOpenVault = onOpenVault,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }

            // 6. Breakdown heading
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.storage_breakdown),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.storage_see_all),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor()
                    )
                }
            }

            // 7. Category stat-tile grid (2 columns)
            val categories = listOf(
                CategoryData(R.string.cat_images, stats.imageBytes, CatImages, Icons.Default.Image, "images_metric_row", "Image"),
                CategoryData(R.string.cat_videos, stats.videoBytes, CatVideos, Icons.Default.Videocam, "videos_metric_row", "Video"),
                CategoryData(R.string.cat_audio_tracks, stats.audioBytes, CatAudio, Icons.Default.AudioFile, "audio_metric_row", "Audio"),
                CategoryData(R.string.cat_documents, stats.documentBytes, CatDocuments, Icons.Default.Description, "documents_metric_row", "Document"),
                CategoryData(R.string.cat_archives, stats.archiveBytes, CatArchives, Icons.Default.FolderZip, "archives_metric_row", "Archive"),
                CategoryData(R.string.cat_other_formats, stats.otherBytes, CatOther, Icons.Default.Extension, "others_metric_row", "Other")
            )
            val maxCategoryBytes = categories.maxOf { it.bytes }.coerceAtLeast(1L)

            categories.chunked(2).forEach { pair ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { cat ->
                            CategoryStatTile(
                                data = cat,
                                totalDeviceBytes = stats.totalLimitBytes,
                                maxCategoryBytes = maxCategoryBytes,
                                onClick = { onOpenFilesWithCategory(cat.category) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcePill(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = accentColor()
    val containerModifier = if (selected) {
        if (isSystemInDarkTheme()) {
            // Dark mode already reads clearly as selected — leave it as-is.
            Modifier.background(accent.copy(alpha = 0.16f))
        } else {
            // Light mode: the faint tint alone looks disabled, so add a stronger tint and
            // an accent border to make the selected pill clearly identifiable.
            Modifier
                .background(accent.copy(alpha = 0.18f))
                .border(1.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
        }
    } else {
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    }
    val contentColor = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(containerModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.Check else icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun StorageRingTile(
    stats: StorageStats,
    modifier: Modifier = Modifier
) {
    val used = stats.usedBytes
    val total = stats.totalLimitBytes.coerceAtLeast(1L)
    val free = (stats.totalLimitBytes - stats.usedBytes).coerceAtLeast(0L)
    val percent = (used.toFloat() / total).coerceIn(0f, 1f)

    val animatedSweep by animateFloatAsState(
        targetValue = percent * 360f,
        animationSpec = tween(durationMillis = 700),
        label = "ring_sweep"
    )

    val ringColors = ringGradientColors()
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val accent = accentColor()

    // In light mode the outline color is already very light; reducing its alpha makes the
    // border vanish against the white tile. Use the full outline in light mode, keep the
    // softer alpha in dark mode (where it has enough contrast).
    val tileBorderColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, tileBorderColor, RoundedCornerShape(24.dp))
            .padding(vertical = 18.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val strokeWidth = 12.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    brush = Brush.linearGradient(ringColors),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = formatBytesToGBorMB(used),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.storage_percent_used, (percent * 100).roundToInt()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
        }
        Text(
            text = stringResource(R.string.storage_free_of, formatBytesToGBorMB(free), formatBytesToGBorMB(stats.totalLimitBytes)),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VaultTile(
    fileCount: Int,
    encryptedBytes: Long,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(vaultGradientColors()))
            .clickable(onClick = onOpenVault)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }
        Column {
            Text(
                text = "$fileCount",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.vault_files_count_label),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = stringResource(R.string.vault_encrypted_label, formatBytesToGBorMB(encryptedBytes)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.vault_open),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun CategoryStatTile(
    data: CategoryData,
    totalDeviceBytes: Long,
    maxCategoryBytes: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val (numberPart, unitPart) = categorySizeParts(data.bytes)

    val devicePercent = if (totalDeviceBytes > 0) data.bytes * 100f / totalDeviceBytes else 0f
    val percentLabel = when {
        data.bytes <= 0L -> "0%"
        devicePercent > 0f && devicePercent < 1f -> "<1%"
        else -> "${devicePercent.roundToInt()}%"
    }

    val barFraction = (data.bytes.toFloat() / maxCategoryBytes).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = barFraction,
        animationSpec = tween(durationMillis = 600),
        label = "category_bar"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(data.color.copy(alpha = if (dark) 0.12f else 0.07f))
            .border(
                1.dp,
                data.color.copy(alpha = if (dark) 0.22f else 0.14f),
                RoundedCornerShape(20.dp)
            )
            .padding(15.dp)
            .testTag(data.testTag),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(data.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = percentLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = data.color,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(data.color.copy(alpha = if (dark) 0.18f else 0.13f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            )
        }

        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = numberPart,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unitPart,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Text(
                text = stringResource(data.titleRes),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(data.color.copy(alpha = if (dark) 0.16f else 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(data.color)
            )
        }
    }
}

fun formatBytesToGBorMB(bytes: Long): String {
    val mb = bytes / (1024 * 1024f)
    return if (mb >= 1024) {
        String.format("%.1f GB", mb / 1024)
    } else {
        String.format("%.0f MB", mb)
    }
}

private fun categorySizeParts(bytes: Long): Pair<String, String> {
    val mb = bytes / (1024 * 1024f)
    return if (mb >= 1024) {
        String.format("%.2f", mb / 1024) to "GB"
    } else {
        String.format("%.2f", mb) to "MB"
    }
}

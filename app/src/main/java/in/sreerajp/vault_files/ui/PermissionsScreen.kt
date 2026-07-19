package `in`.sreerajp.vault_files.ui

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.sreerajp.vault_files.ui.theme.TileBorderDark
import `in`.sreerajp.vault_files.ui.theme.TileBorderLight
import `in`.sreerajp.vault_files.R

/** A single permission resolved for display. */
private data class PermissionEntry(
    val constant: String,
    val label: String,
    val granted: Boolean
)

/** Permissions our app declares directly in its own AndroidManifest. */
private val EXPLICIT_PERMISSIONS = setOf(
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.REQUEST_INSTALL_PACKAGES"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tileBorder = BorderStroke(1.dp, if (isDark) TileBorderDark else TileBorderLight)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Special-access (appop) permissions can be toggled in system settings while we're
    // backgrounded; recompute on resume so the displayed status stays accurate.
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Read every permission present in the merged manifest at runtime, plus its granted state.
    // This naturally covers both the permissions we declare and any pulled in by libraries.
    val (explicit, implicit) = remember(context, refreshKey) {
        val pm = context.packageManager
        val entries: List<PermissionEntry> = try {
            @Suppress("DEPRECATION")
            val info: PackageInfo =
                pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            val requested = info.requestedPermissions ?: emptyArray()
            val flags = info.requestedPermissionsFlags ?: IntArray(0)
            requested.mapIndexed { i, perm ->
                val flagGranted = i < flags.size &&
                    (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                PermissionEntry(
                    constant = perm,
                    label = friendlyLabel(context, perm),
                    granted = resolveGranted(context, perm, flagGranted)
                )
            }.sortedBy { it.constant }
        } catch (e: Exception) {
            emptyList()
        }
        entries.partition { it.constant in EXPLICIT_PERMISSIONS }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("permissions_screen"),
        contentPadding = PaddingValues(top = statusBarTop + 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSubPageHeader(
                title = stringResource(R.string.permissions_title),
                onBack = onBack,
                border = tileBorder,
                backTestTag = "permissions_back_btn"
            )
        }

        item {
            Text(
                text = stringResource(R.string.permissions_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
        }

        if (explicit.isEmpty() && implicit.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.permissions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (explicit.isNotEmpty()) {
            item {
                SettingsSectionLabel(
                    stringResource(R.string.permissions_section_declared),
                    Modifier.padding(top = 6.dp)
                )
            }
            items(explicit.size) { idx ->
                PermissionRow(explicit[idx], tileBorder)
            }
        }

        if (implicit.isNotEmpty()) {
            item {
                SettingsSectionLabel(
                    stringResource(R.string.permissions_section_implicit),
                    Modifier.padding(top = 12.dp)
                )
            }
            items(implicit.size) { idx ->
                PermissionRow(implicit[idx], tileBorder)
            }
        }
    }
}

/**
 * Resolves the true granted state for a permission. Special-access (appop) permissions are not
 * tracked by [PackageInfo.REQUESTED_PERMISSION_GRANTED], so they're queried via their dedicated
 * APIs; everything else falls back to the manifest flag.
 */
private fun resolveGranted(
    context: android.content.Context,
    permission: String,
    flagGranted: Boolean
): Boolean = when (permission) {
    "android.permission.MANAGE_EXTERNAL_STORAGE" ->
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
    "android.permission.REQUEST_INSTALL_PACKAGES" ->
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.canRequestPackageInstalls()
    // On Android 11+ the app's effective storage access is All-files access
    // (MANAGE_EXTERNAL_STORAGE); the legacy READ/WRITE runtime permissions are not
    // granted there (WRITE is maxSdkVersion=29), so report them as covered when it's held.
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE" ->
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) || flagGranted
    else -> flagGranted
}

/**
 * Friendly, human-readable name for a permission. Our four declared permissions get curated
 * strings; anything else falls back to the system-provided label, then the short constant name.
 */
private fun friendlyLabel(
    context: android.content.Context,
    permission: String
): String {
    val curated = when (permission) {
        "android.permission.READ_EXTERNAL_STORAGE" -> R.string.perm_read_storage
        "android.permission.WRITE_EXTERNAL_STORAGE" -> R.string.perm_write_storage
        "android.permission.MANAGE_EXTERNAL_STORAGE" -> R.string.perm_manage_storage
        "android.permission.REQUEST_INSTALL_PACKAGES" -> R.string.perm_install_packages
        else -> null
    }
    if (curated != null) return context.getString(curated)

    val pm = context.packageManager
    return try {
        val info = pm.getPermissionInfo(permission, 0)
        info.loadLabel(pm).toString().replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        permission.substringAfterLast('.')
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun PermissionRow(entry: PermissionEntry, border: BorderStroke) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = entry.constant,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            GrantedChip(entry.granted)
        }
    }
}

@Composable
private fun GrantedChip(granted: Boolean) {
    val color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.RemoveCircleOutline,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = stringResource(
                    if (granted) R.string.permissions_status_granted
                    else R.string.permissions_status_denied
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

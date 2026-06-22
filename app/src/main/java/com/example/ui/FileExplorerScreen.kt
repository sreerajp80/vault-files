package com.example.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.FileItem
import com.example.utils.BiometricHelper
import `in`.sreerajp.vault_files.R
import java.io.File

enum class FileSortMode(@param:androidx.annotation.StringRes val labelRes: Int) {
    NAME(R.string.sort_name),
    SIZE(R.string.sort_size),
    DATE(R.string.sort_date)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: StorageViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDir by viewModel.currentDirectory.collectAsState()
    val filesList by viewModel.currentDirectoryFiles.collectAsState()
    val activeCategoryFilter by viewModel.activeCategoryFilter.collectAsState()
    val categoryFilteredFiles by viewModel.categoryFilteredFiles.collectAsState()
    val isCategoryLoading by viewModel.isCategoryLoading.collectAsState()
    val isPhoneLockDeleteEnabled by viewModel.phoneLockDeleteEnabled.collectAsState()
    val customPinValue by viewModel.customPin.collectAsState()
    val unlockedFolderSessions by viewModel.unlockedFolderSessions.collectAsState()

    val storageSourceMode by viewModel.storageSourceMode.collectAsState()
    var hasPermission by remember { mutableStateOf(hasAllFilesPermission(context)) }

    // Recheck permission when app returns from background
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = hasAllFilesPermission(context)
                viewModel.loadFilesInDirectory(viewModel.currentDirectory.value)
                viewModel.loadCategoryFilteredFiles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val showHiddenItems by viewModel.showHiddenItems.collectAsState()
    val passwordProtectHidden by viewModel.passwordProtectHidden.collectAsState()
    val isHiddenUnlocked by viewModel.isHiddenUnlocked.collectAsState()

    val shouldShowHiddenUnlockBanner = showHiddenItems && passwordProtectHidden && !isHiddenUnlocked

    // Dialog trigger states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showZipDialogForFile by remember { mutableStateOf<FileItem?>(null) }
    var activeActionPendingValidation by remember { mutableStateOf<PendingAction?>(null) }

    // Dropdown state for contextual action menus
    var expandedMenuForFileItem by remember { mutableStateOf<FileItem?>(null) }
    var showDetailsForFileItem by remember { mutableStateOf<FileItem?>(null) }

    // Screen-local search + sort state (client-side over the loaded directory list)
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(FileSortMode.NAME) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val isAtRoot = currentDir.absolutePath == viewModel.userStorageRoot.absolutePath
    val needsPermission = storageSourceMode == "device" && !hasPermission
    val isCategoryFiltered = activeCategoryFilter != null

    // Intercept the system back gesture/button while there's somewhere to go within the explorer:
    // clear an active category filter first, otherwise navigate to the parent folder. When at the
    // root with no filter the handler is disabled, so back falls through and leaves the app.
    BackHandler(enabled = isCategoryFiltered || !isAtRoot) {
        if (isCategoryFiltered) {
            viewModel.clearCategoryFilter()
        } else {
            viewModel.navigateUp()
        }
    }

    // In category-filter mode the list comes from the flat recursive collection; otherwise
    // it's the current directory listing.
    val baseList = if (isCategoryFiltered) categoryFilteredFiles else filesList

    val displayedFiles = remember(baseList, searchQuery, sortMode) {
        val filtered = if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
        val comparator = when (sortMode) {
            FileSortMode.NAME -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
            FileSortMode.SIZE -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size }
            FileSortMode.DATE -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.file.lastModified() }
        }
        filtered.sortedWith(comparator)
    }

    val folderCount = filesList.count { it.isDirectory }
    val totalSize = baseList.sumOf { it.size }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ---------------- Header ----------------
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.nav_files),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (isCategoryFiltered) {
                            // Dismissible chip describing the active type filter.
                            Surface(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .testTag("files_category_filter_chip"),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                onClick = { viewModel.clearCategoryFilter() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.files_filter_chip, categoryDisplayLabel(activeCategoryFilter), baseList.size),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cd_clear_filter),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .then(
                                        if (!isAtRoot) Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.navigateUp() }
                                        else Modifier
                                    )
                                    .testTag("files_breadcrumb"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (!isAtRoot) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = stringResource(R.string.cd_navigate_up),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = getDisplayPath(viewModel.userStorageRoot, currentDir),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(
                                        R.string.files_breadcrumb_summary,
                                        pluralStringResource(R.plurals.folder_count, folderCount, folderCount),
                                        formatBytes(totalSize)
                                    ),
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        HeaderActionButton(
                            icon = Icons.Default.CreateNewFolder,
                            contentDescription = stringResource(R.string.cd_new_folder),
                            filled = false,
                            onClick = { showCreateFolderDialog = true }
                        )
                        HeaderActionButton(
                            icon = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = stringResource(R.string.cd_new_text_file),
                            filled = true,
                            onClick = { showCreateFileDialog = true }
                        )
                    }
                }

                // ---------------- Search field ----------------
                if (!needsPermission) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .testTag("files_search_field"),
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        stringResource(R.string.files_search_placeholder),
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                    }
                }

                // ---------------- Source pills ----------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("files_storage_source_card"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SourcePill(
                        selected = storageSourceMode == "sandbox",
                        icon = Icons.Default.GridView,
                        label = stringResource(R.string.source_app_sandbox),
                        onClick = { viewModel.updateStorageSourceMode("sandbox") },
                        modifier = Modifier.weight(1f).testTag("files_select_sandbox_chip")
                    )
                    SourcePill(
                        selected = storageSourceMode == "device",
                        icon = Icons.Default.PhoneAndroid,
                        label = stringResource(R.string.source_entire_device),
                        onClick = { viewModel.updateStorageSourceMode("device") },
                        modifier = Modifier.weight(1f).testTag("files_select_device_chip")
                    )
                }
            }

            // ---------------- Hidden-items unlock banner ----------------
            if (shouldShowHiddenUnlockBanner) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .testTag("hidden_unlock_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    onClick = {
                        activeActionPendingValidation = PendingAction(
                            title = context.getString(R.string.hidden_unlock_title),
                            subtitle = context.getString(R.string.hidden_unlock_subtitle),
                            onValidated = {
                                viewModel.markHiddenUnlockedState(true)
                            }
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.hidden_locked_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                stringResource(R.string.hidden_locked_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ---------------- Section header + sort control ----------------
            if (!needsPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCategoryFiltered) stringResource(R.string.files_all_category_header, categoryDisplayLabel(activeCategoryFilter).uppercase()) else stringResource(R.string.files_all_items),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { sortMenuExpanded = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("files_sort_control"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(R.string.cd_sort),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(sortMode.labelRes),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            FileSortMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_by, stringResource(mode.labelRes))) },
                                    onClick = {
                                        sortMode = mode
                                        sortMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (sortMode == mode) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.testTag("files_sort_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // ---------------- Content area ----------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (needsPermission) {
                    // All Files Permission Request State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("explorer_permission_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = stringResource(R.string.cd_permission_required),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.perm_device_access_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.perm_device_access_message),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            hasPermission = isGranted
                            viewModel.loadFilesInDirectory(viewModel.currentDirectory.value)
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
                                            viewModel.dispatchMessage(context.getString(R.string.msg_cannot_launch_settings))
                                        }
                                    }
                                } else {
                                    launcher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.perm_open_storage_settings))
                        }
                    }
                } else if (isCategoryFiltered && isCategoryLoading) {
                    // Scanning state: the recursive category walk is still running. Show a
                    // spinner rather than the previous category's stale list or an empty state.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("category_filter_loading_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.files_scanning_category, categoryDisplayLabel(activeCategoryFilter).lowercase()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (displayedFiles.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("empty_explorer_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isBlank() && !isCategoryFiltered) Icons.Default.FolderOpen else Icons.Default.SearchOff,
                            contentDescription = stringResource(R.string.cd_empty_folder),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> stringResource(R.string.files_no_matching)
                                isCategoryFiltered -> stringResource(R.string.files_no_category_found, categoryDisplayLabel(activeCategoryFilter).lowercase())
                                else -> stringResource(R.string.files_empty_folder)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> stringResource(R.string.files_no_match_detail, searchQuery)
                                isCategoryFiltered -> stringResource(R.string.files_no_category_detail)
                                else -> stringResource(R.string.files_empty_folder_detail)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        items(displayedFiles, key = { it.absolutePath }) { item ->
                            FileRowItem(
                                item = item,
                                onItemClick = {
                                    if (item.isSecured && !unlockedFolderSessions.contains(item.absolutePath)) {
                                        activeActionPendingValidation = PendingAction(
                                            title = context.getString(R.string.unlock_folder_title),
                                            subtitle = context.getString(R.string.unlock_folder_subtitle, item.name),
                                            onValidated = {
                                                viewModel.unlockSecuredFolderSession(item.absolutePath)
                                                if (item.isDirectory) {
                                                    viewModel.navigateToDirectory(item.file)
                                                }
                                            }
                                        )
                                    } else {
                                        if (item.isDirectory) {
                                            viewModel.navigateToDirectory(item.file)
                                        } else if (item.name.lowercase().endsWith(".zip")) {
                                            viewModel.decompressZip(item)
                                        } else {
                                            viewModel.dispatchMessage(context.getString(R.string.msg_viewing_file, item.name, formatBytes(item.size)))
                                        }
                                    }
                                },
                                onActionMenuOpen = { expandedMenuForFileItem = item },
                                isSecuredLocked = item.isSecured && !unlockedFolderSessions.contains(item.absolutePath)
                            )
                        }
                    }
                }
            }
        }

        // --- Floating Action Dropdowns for items ---
        expandedMenuForFileItem?.let { selectedItem ->
            CardItemMenu(
                item = selectedItem,
                onDismiss = { expandedMenuForFileItem = null },
                onDetailsClick = {
                    showDetailsForFileItem = selectedItem
                    expandedMenuForFileItem = null
                },
                onZipClick = {
                    showZipDialogForFile = selectedItem
                    expandedMenuForFileItem = null
                },
                onExtractClick = {
                    viewModel.decompressZip(selectedItem)
                    expandedMenuForFileItem = null
                },
                onShieldToggle = {
                    viewModel.toggleFolderShield(selectedItem)
                    expandedMenuForFileItem = null
                },
                onEncryptToVault = {
                    expandedMenuForFileItem = null
                    if (isPhoneLockDeleteEnabled) {
                        activeActionPendingValidation = PendingAction(
                            title = context.getString(R.string.confirm_move_title),
                            subtitle = context.getString(R.string.confirm_move_subtitle, selectedItem.name),
                            onValidated = {
                                viewModel.secureFileInVault(selectedItem)
                            }
                        )
                    } else {
                        viewModel.secureFileInVault(selectedItem)
                    }
                },
                onDeleteClick = {
                    expandedMenuForFileItem = null
                    if (isPhoneLockDeleteEnabled) {
                        activeActionPendingValidation = PendingAction(
                            title = context.getString(R.string.confirm_delete_title),
                            subtitle = context.getString(R.string.confirm_delete_subtitle, selectedItem.name),
                            onValidated = {
                                viewModel.deleteFileItem(selectedItem)
                            }
                        )
                    } else {
                        viewModel.deleteFileItem(selectedItem)
                    }
                }
            )
        }

        // --- File/Folder Details Dialog ---
        showDetailsForFileItem?.let { detailsItem ->
            FileDetailsDialog(
                item = detailsItem,
                onDismiss = { showDetailsForFileItem = null }
            )
        }

        // --- Folder Creator Dialog ---
        if (showCreateFolderDialog) {
            var folderNameInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                title = { Text(stringResource(R.string.dialog_create_folder_title)) },
                text = {
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        label = { Text(stringResource(R.string.dialog_folder_name_label)) },
                        placeholder = { Text(stringResource(R.string.dialog_folder_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth().testTag("folder_input_field"),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (folderNameInput.isNotBlank()) {
                                viewModel.createFolder(folderNameInput.trim())
                                showCreateFolderDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_create_folder_btn")
                    ) {
                        Text(stringResource(R.string.action_create))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolderDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // --- Text File Creator Dialog ---
        if (showCreateFileDialog) {
            var filenameInput by remember { mutableStateOf("") }
            var textContentInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showCreateFileDialog = false },
                title = { Text(stringResource(R.string.dialog_new_note_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = filenameInput,
                            onValueChange = { filenameInput = it },
                            label = { Text(stringResource(R.string.dialog_filename_label)) },
                            placeholder = { Text(stringResource(R.string.dialog_filename_placeholder)) },
                            modifier = Modifier.fillMaxWidth().testTag("note_filename_field"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = textContentInput,
                            onValueChange = { textContentInput = it },
                            label = { Text(stringResource(R.string.dialog_note_content_label)) },
                            placeholder = { Text(stringResource(R.string.dialog_note_content_placeholder)) },
                            modifier = Modifier.fillMaxWidth().height(120.dp).testTag("note_content_field")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (filenameInput.isNotBlank()) {
                                viewModel.createTextFile(filenameInput.trim(), textContentInput)
                                showCreateFileDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_create_note_btn")
                    ) {
                        Text(stringResource(R.string.action_save_file))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFileDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // --- ZIP Compress Options Dialog ---
        showZipDialogForFile?.let { pendingZipFile ->
            var zipNameInput by remember { mutableStateOf(pendingZipFile.name + ".zip") }
            AlertDialog(
                onDismissRequest = { showZipDialogForFile = null },
                title = { Text(stringResource(R.string.dialog_compress_title)) },
                text = {
                    OutlinedTextField(
                        value = zipNameInput,
                        onValueChange = { zipNameInput = it },
                        label = { Text(stringResource(R.string.dialog_archive_name_label)) },
                        modifier = Modifier.fillMaxWidth().testTag("zip_name_field"),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (zipNameInput.isNotBlank()) {
                                viewModel.compressFolderOrFile(pendingZipFile, zipNameInput.trim())
                                showZipDialogForFile = null
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_compress))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showZipDialogForFile = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // --- Security / Biometric Verification System Fallback Dialog ---
        activeActionPendingValidation?.let { pendingAction ->
            val activity = context as? FragmentActivity
            if (activity != null && BiometricHelper.isBiometricHardwareAvailable(context)) {
                LaunchedEffect(pendingAction) {
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = pendingAction.title,
                        subtitle = pendingAction.subtitle,
                        onSuccess = {
                            pendingAction.onValidated()
                            activeActionPendingValidation = null
                        },
                        onError = { err ->
                            if (customPinValue == null) {
                                viewModel.dispatchMessage(context.getString(R.string.msg_configure_pin_tab))
                                activeActionPendingValidation = null
                            }
                        }
                    )
                }
            }

            if (customPinValue != null) {
                var enteredPin by remember { mutableStateOf("") }
                var hasPinError by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { activeActionPendingValidation = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.auth_passcode_title))
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = pendingAction.subtitle,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = enteredPin,
                                onValueChange = {
                                    enteredPin = it
                                    hasPinError = false
                                },
                                label = { Text(stringResource(R.string.auth_enter_app_pin)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().testTag("auth_pin_input"),
                                singleLine = true,
                                isError = hasPinError,
                                supportingText = {
                                    if (hasPinError) {
                                        Text(stringResource(R.string.auth_incorrect_passcode), color = Color.Red)
                                    }
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (enteredPin == customPinValue) {
                                    pendingAction.onValidated()
                                    activeActionPendingValidation = null
                                } else {
                                    hasPinError = true
                                }
                            },
                            modifier = Modifier.testTag("auth_pin_confirm_btn")
                        ) {
                            Text(stringResource(R.string.action_verify))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { activeActionPendingValidation = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            } else if (activity == null || !BiometricHelper.isBiometricHardwareAvailable(context)) {
                AlertDialog(
                    onDismissRequest = { activeActionPendingValidation = null },
                    title = { Text(stringResource(R.string.auth_credentials_required_title)) },
                    text = {
                        Text(stringResource(R.string.auth_credentials_required_message))
                    },
                    confirmButton = {
                        Button(onClick = { activeActionPendingValidation = null }) {
                            Text(stringResource(R.string.action_ok_understand))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderActionButton(
    icon: ImageVector,
    contentDescription: String,
    filled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = if (filled) 4.dp else 0.dp,
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun SourcePill(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FileRowItem(
    item: FileItem,
    onItemClick: () -> Unit,
    onActionMenuOpen: () -> Unit,
    isSecuredLocked: Boolean
) {
    val fileIcon = getIconForFileCategory(item)
    val iconTint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val dateText = remember(item.file) {
        val lastMod = item.file.lastModified()
        val format = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        format.format(java.util.Date(lastMod))
    }
    val metaText = if (item.isDirectory) {
        "$dateText · " + pluralStringResource(R.plurals.item_count, item.itemCount, item.itemCount)
    } else {
        dateText
    }

    Surface(
        onClick = onItemClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("file_row_${item.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                if (isSecuredLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.cd_locked_folder),
                        tint = Color(0xFFE74C3C),
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = item.category,
                        tint = iconTint,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (item.isSecured) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = stringResource(R.string.cd_shielded_folder),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = metaText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (item.isDirectory && !item.sizeComputed) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    text = formatBytes(item.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            IconButton(
                onClick = onActionMenuOpen,
                modifier = Modifier
                    .size(30.dp)
                    .testTag("options_${item.name}")
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_menu_actions),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Dialog options popup Card
@Composable
fun CardItemMenu(
    item: FileItem,
    onDismiss: () -> Unit,
    onDetailsClick: () -> Unit,
    onZipClick: () -> Unit,
    onExtractClick: () -> Unit,
    onShieldToggle: () -> Unit,
    onEncryptToVault: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onDetailsClick,
                    modifier = Modifier.fillMaxWidth().testTag("menu_details")
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.menu_details))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (item.isDirectory) {
                    TextButton(
                        onClick = onShieldToggle,
                        modifier = Modifier.fillMaxWidth().testTag("menu_shield_toggle")
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF2ECC71))
                            Text(if (item.isSecured) stringResource(R.string.menu_remove_shield) else stringResource(R.string.menu_lock_folder))
                        }
                    }
                } else {
                    TextButton(
                        onClick = onEncryptToVault,
                        modifier = Modifier.fillMaxWidth().testTag("menu_move_vault")
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFF1C40F))
                            Text(stringResource(R.string.menu_move_vault))
                        }
                    }
                }

                if (item.name.lowercase().endsWith(".zip")) {
                    TextButton(
                        onClick = onExtractClick,
                        modifier = Modifier.fillMaxWidth().testTag("menu_extract")
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.menu_decompress))
                        }
                    }
                } else {
                    TextButton(
                        onClick = onZipClick,
                        modifier = Modifier.fillMaxWidth().testTag("menu_zip")
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Compress, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.menu_compress))
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                TextButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("menu_delete")
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.menu_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

// Read-only properties dialog for a file or folder
@Composable
fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    val modifiedText = remember(item.file) {
        val format = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        format.format(java.util.Date(item.file.lastModified()))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(stringResource(R.string.detail_type), if (item.isDirectory) stringResource(R.string.detail_folder) else item.category)
                DetailRow(stringResource(R.string.detail_path), item.absolutePath)
                DetailRow(stringResource(R.string.detail_size), formatBytes(item.size))
                if (item.isDirectory) {
                    DetailRow(stringResource(R.string.detail_items), pluralStringResource(R.plurals.item_count, item.itemCount, item.itemCount))
                }
                DetailRow(stringResource(R.string.detail_modified), modifiedText)
                if (item.isDirectory) {
                    DetailRow(stringResource(R.string.detail_secured), if (item.isSecured) stringResource(R.string.common_yes) else stringResource(R.string.common_no))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Helpers

fun getIconForFileCategory(item: FileItem): ImageVector {
    if (item.isDirectory) return Icons.Default.Folder
    return when (item.category) {
        "Image" -> Icons.Default.Image
        "Video" -> Icons.Default.Videocam
        "Audio" -> Icons.Default.AudioFile
        "Document" -> Icons.Default.Description
        "Archive" -> Icons.Default.FolderZip
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

/** Maps a canonical category string to a friendly localized plural label for the Files filter UI. */
@Composable
fun categoryDisplayLabel(category: String?): String = when (category) {
    "Image" -> stringResource(R.string.filter_label_images)
    "Video" -> stringResource(R.string.filter_label_videos)
    "Audio" -> stringResource(R.string.filter_label_audio)
    "Document" -> stringResource(R.string.filter_label_documents)
    "Archive" -> stringResource(R.string.filter_label_archives)
    "Other" -> stringResource(R.string.filter_label_other)
    else -> stringResource(R.string.filter_label_files)
}

@Composable
fun getDisplayPath(root: File, current: File): String {
    if (root.absolutePath == current.absolutePath) return stringResource(R.string.path_main_storage)
    val diff = current.absolutePath.substring(root.absolutePath.length)
    return stringResource(R.string.path_storage_prefix) + diff.replace(File.separatorChar, '›')
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return if (digitGroups == 0) "$bytes B" else String.format("%.1f %s", value, units[digitGroups])
}

// Representation matching active prompt validation
data class PendingAction(
    val title: String,
    val subtitle: String,
    val onValidated: () -> Unit
)

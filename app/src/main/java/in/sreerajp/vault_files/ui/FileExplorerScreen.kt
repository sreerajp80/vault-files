package `in`.sreerajp.vault_files.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import `in`.sreerajp.vault_files.data.FileItem
import `in`.sreerajp.vault_files.utils.BiometricHelper
import `in`.sreerajp.vault_files.R
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class FileSortMode(@param:androidx.annotation.StringRes val labelRes: Int) {
    NAME(R.string.sort_name),
    SIZE(R.string.sort_size),
    DATE(R.string.sort_date)
}

/** Files-explorer layout. [storageKey] is the value persisted in settings. */
enum class FileViewMode(
    val storageKey: String,
    val icon: ImageVector,
    @param:androidx.annotation.StringRes val contentDescRes: Int
) {
    LIST("list", Icons.AutoMirrored.Filled.ViewList, R.string.cd_view_list),
    GRID("grid", Icons.Default.GridView, R.string.cd_view_grid),
    COMPACT("compact", Icons.Default.ViewHeadline, R.string.cd_view_compact);

    companion object {
        fun fromKey(key: String): FileViewMode = values().firstOrNull { it.storageKey == key } ?: LIST
    }
}

/** Actions emitted by the selection toolbar / overflow menu, handled by the screen. */
enum class SelectionAction {
    DELETE, INFO, RENAME, SHARE, HIDE, CREATE_SHORTCUT, COPY_PATH, SET_AS,
    OPEN_WITH, OPEN_AS, COPY_TO, MOVE_TO, COMPRESS, EXTRACT, SHIELD_TOGGLE,
    MOVE_VAULT, SELECT_ALL
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
    val openNote by viewModel.openNote.collectAsState()
    val imagePreviewEnabled by viewModel.imagePreviewEnabled.collectAsState()
    val imagePreview by viewModel.imagePreview.collectAsState()
    val textPreviewEnabled by viewModel.textPreviewEnabled.collectAsState()
    val textPreview by viewModel.textPreview.collectAsState()
    val fileViewModeKey by viewModel.fileViewMode.collectAsState()
    val fileViewMode = FileViewMode.fromKey(fileViewModeKey)

    val storageSourceMode by viewModel.storageSourceMode.collectAsState()
    val isPickMode by viewModel.isPickMode.collectAsState()
    val allowMultiplePick by viewModel.allowMultiplePick.collectAsState()
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
    var showCreateDialog by remember { mutableStateOf(false) }
    var showViewTypeDialog by remember { mutableStateOf(false) }
    var showSortByDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showZipDialogForItems by remember { mutableStateOf<List<FileItem>?>(null) }
    var activeActionPendingValidation by remember { mutableStateOf<PendingAction?>(null) }
    // Selection-menu dialog states.
    var pendingConfirm by remember { mutableStateOf<ConfirmSpec?>(null) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var openAsTarget by remember { mutableStateOf<FileItem?>(null) }

    // Multi-select state: absolute paths of items the user has long-pressed to select. While
    // non-empty the screen is in "selection mode" and the search field is replaced by a
    // contextual action toolbar.
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var showDetailsForFileItem by remember { mutableStateOf<FileItem?>(null) }
    var showDetailsForSelection by remember { mutableStateOf<List<FileItem>?>(null) }

    // Screen-local search + sort state (client-side over the loaded directory list)
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(FileSortMode.NAME) }
    var sortAscending by remember { mutableStateOf(true) }
    var optionsMenuExpanded by remember { mutableStateOf(false) }

    val isAtRoot = currentDir.absolutePath == viewModel.userStorageRoot.absolutePath
    val needsPermission = storageSourceMode == "device" && !hasPermission
    val isCategoryFiltered = activeCategoryFilter != null

    // Pull-to-refresh: the ViewModel's load functions are fire-and-forget (they launch their
    // own jobs), so the screen drives this flag directly — set true on pull, cleared after a
    // short delay so the spinner stays visible briefly while the reload runs.
    val refreshScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val onRefresh: () -> Unit = {
        isRefreshing = true
        if (isCategoryFiltered) {
            viewModel.loadCategoryFilteredFiles()
        } else {
            viewModel.loadFilesInDirectory(currentDir)
        }
        viewModel.refreshStorageStats()
        refreshScope.launch {
            delay(600)
            isRefreshing = false
        }
    }

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
    val selectionMode = selectedPaths.isNotEmpty() || (isPickMode && allowMultiplePick)
    val selectedItems = remember(baseList, selectedPaths) {
        baseList.filter { it.absolutePath in selectedPaths }
    }
    val toggleSelection: (FileItem) -> Unit = { item ->
        if (isPickMode && !allowMultiplePick) {
            // No multi-select in single pick mode
        } else {
            selectedPaths = if (item.absolutePath in selectedPaths) {
                selectedPaths - item.absolutePath
            } else {
                selectedPaths + item.absolutePath
            }
        }
    }

    // Leaving the current directory or changing the category filter clears the selection, since
    // the selected items are no longer on screen.
    LaunchedEffect(currentDir, activeCategoryFilter) {
        selectedPaths = emptySet()
    }

    // While in selection mode, system Back exits selection first (takes priority over the
    // category/navigate-up handler above because it is registered later).
    BackHandler(enabled = selectionMode) {
        selectedPaths = emptySet()
    }

    val displayedFiles = remember(baseList, searchQuery, sortMode, sortAscending) {
        val filtered = if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
        val comparator = when (sortMode) {
            FileSortMode.NAME -> {
                if (sortAscending) {
                    compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
                } else {
                    compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() }
                }
            }
            FileSortMode.SIZE -> {
                if (sortAscending) {
                    compareBy<FileItem> { !it.isDirectory }.thenBy { it.size }
                } else {
                    compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size }
                }
            }
            FileSortMode.DATE -> {
                if (sortAscending) {
                    compareBy<FileItem> { !it.isDirectory }.thenBy { it.file.lastModified() }
                } else {
                    compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.file.lastModified() }
                }
            }
        }
        filtered.sortedWith(comparator)
    }

    val folderCount = filesList.count { it.isDirectory }
    val fileCount = filesList.count { !it.isDirectory }
    val totalSize = baseList.sumOf { it.size }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ---------------- Header ----------------
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp)) {
                if (isPickMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (allowMultiplePick) "Pick Multiple Files" else "Pick a File",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = {
                                viewModel.exitPickMode()
                                (context as? android.app.Activity)?.let {
                                    it.setResult(android.app.Activity.RESULT_CANCELED)
                                    it.finish()
                                }
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                } else {
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
                            }
                        }
                    }
                }

                // ---------------- Source pills ----------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
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

                // ---------------- Search field / selection toolbar ----------------
                if (!needsPermission && selectionMode && !isPickMode) {
                    // Single handler for every selection action. Actions that have their own dialog
                    // (rename / open-as / copy / move / compress) open it directly; the rest are gated
                    // by a generic OK/Cancel confirmation dialog (pendingConfirm) so every action is
                    // confirmed before it runs. Delete / Move-to-Vault keep their phone-lock
                    // validation, which itself serves as the confirmation when enabled.
                    val clear = { selectedPaths = emptySet() }
                    val onSelectionAction: (SelectionAction) -> Unit = handler@{ action ->
                        val items = selectedItems
                        if (items.isEmpty()) return@handler
                        val single = items.singleOrNull()
                        when (action) {
                            SelectionAction.INFO -> {
                                if (items.size == 1) showDetailsForFileItem = items.first()
                                else showDetailsForSelection = items
                            }
                            SelectionAction.RENAME -> { renameTarget = single }
                            SelectionAction.OPEN_AS -> { openAsTarget = single }
                            SelectionAction.COPY_TO -> {
                                if (isCategoryFiltered) viewModel.clearCategoryFilter()
                                viewModel.setPendingMoveCopy(MoveCopyRequest(items, isMove = false))
                                clear()
                            }
                            SelectionAction.MOVE_TO -> {
                                if (isCategoryFiltered) viewModel.clearCategoryFilter()
                                viewModel.setPendingMoveCopy(MoveCopyRequest(items, isMove = true))
                                clear()
                            }
                            SelectionAction.COMPRESS -> { showZipDialogForItems = items }
                            SelectionAction.DELETE -> {
                                val runDelete = { items.forEach { viewModel.deleteFileItem(it) }; clear() }
                                if (isPhoneLockDeleteEnabled) {
                                    activeActionPendingValidation = PendingAction(
                                        title = context.getString(R.string.confirm_delete_title),
                                        subtitle = if (items.size == 1) context.getString(R.string.confirm_delete_subtitle, items.first().name)
                                            else context.getString(R.string.confirm_delete_subtitle_multi, items.size),
                                        onValidated = { items.forEach { viewModel.deleteFileItem(it) }; clear() }
                                    )
                                } else {
                                    pendingConfirm = ConfirmSpec(
                                        message = if (items.size == 1) context.getString(R.string.confirm_delete_subtitle, items.first().name)
                                            else context.getString(R.string.confirm_delete_subtitle_multi, items.size),
                                        onConfirm = runDelete
                                    )
                                }
                            }
                            SelectionAction.MOVE_VAULT -> {
                                val runMove = { items.forEach { viewModel.secureFileInVault(it) }; clear() }
                                if (isPhoneLockDeleteEnabled) {
                                    activeActionPendingValidation = PendingAction(
                                        title = context.getString(R.string.confirm_move_title),
                                        subtitle = if (items.size == 1) context.getString(R.string.confirm_move_subtitle, items.first().name)
                                            else context.getString(R.string.confirm_move_subtitle_multi, items.size),
                                        onValidated = { items.forEach { viewModel.secureFileInVault(it) }; clear() }
                                    )
                                } else {
                                    pendingConfirm = ConfirmSpec(
                                        message = if (items.size == 1) context.getString(R.string.confirm_move_vault_action, items.first().name)
                                            else context.getString(R.string.confirm_move_vault_action_multi, items.size),
                                        onConfirm = runMove
                                    )
                                }
                            }
                            SelectionAction.EXTRACT -> pendingConfirm = ConfirmSpec(
                                message = context.getString(R.string.confirm_decompress, single?.name ?: ""),
                                onConfirm = { single?.let { viewModel.decompressZip(it) }; clear() }
                            )
                            SelectionAction.SHIELD_TOGGLE -> {
                                val secured = items.all { it.isSecured }
                                pendingConfirm = ConfirmSpec(
                                    message = context.getString(
                                        if (secured) R.string.confirm_remove_shield else R.string.confirm_lock_folder,
                                        single?.name ?: "${items.size}"
                                    ),
                                    onConfirm = { items.forEach { viewModel.toggleFolderShield(it) }; clear() }
                                )
                            }
                            SelectionAction.SHARE -> { shareItems(context, items, viewModel); clear() }
                            SelectionAction.HIDE -> {
                                val unhide = single != null && single.name.startsWith(".")
                                pendingConfirm = ConfirmSpec(
                                    message = when {
                                        unhide -> context.getString(R.string.confirm_unhide, single!!.name)
                                        items.size == 1 -> context.getString(R.string.confirm_hide, items.first().name)
                                        else -> context.getString(R.string.confirm_hide_multi, items.size)
                                    },
                                    onConfirm = { viewModel.hideOrUnhideItems(items); clear() }
                                )
                            }
                            SelectionAction.CREATE_SHORTCUT -> pendingConfirm = ConfirmSpec(
                                message = context.getString(R.string.confirm_create_shortcut, single?.name ?: ""),
                                onConfirm = { single?.let { createPinnedShortcut(context, it, viewModel) }; clear() }
                            )
                            SelectionAction.COPY_PATH -> { copyPathToClipboard(context, items, viewModel); clear() }
                            SelectionAction.SET_AS -> pendingConfirm = ConfirmSpec(
                                message = context.getString(R.string.confirm_set_as, single?.name ?: ""),
                                onConfirm = { single?.let { setAsImage(context, it, viewModel) }; clear() }
                            )
                            SelectionAction.OPEN_WITH -> { single?.let { openWithChooser(context, it, viewModel) }; clear() }
                            SelectionAction.SELECT_ALL -> pendingConfirm = ConfirmSpec(
                                message = context.getString(R.string.confirm_select_all),
                                onConfirm = { selectedPaths = baseList.map { it.absolutePath }.toSet() }
                            )
                        }
                    }
                    SelectionToolbar(
                        selectedItems = selectedItems,
                        modifier = Modifier.padding(top = 12.dp),
                        onClear = clear,
                        onAction = onSelectionAction
                    )
                } else if (!needsPermission) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
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

            // ---------------- Section header + options (view/sort) menu ----------------
            if (!needsPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 10.dp, top = 6.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCategoryFiltered) {
                        Text(
                            text = stringResource(R.string.files_all_category_header, categoryDisplayLabel(activeCategoryFilter).uppercase()),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Two-line breadcrumb: line 1 = path, line 2 = folder count · size.
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (!isAtRoot) Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.navigateUp() }
                                    else Modifier
                                )
                                .testTag("files_breadcrumb")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.files_breadcrumb_summary,
                                    pluralStringResource(R.plurals.folder_count, folderCount, folderCount),
                                    pluralStringResource(R.plurals.file_count, fileCount, fileCount),
                                    formatBytes(totalSize)
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!selectionMode) Box {
                        IconButton(
                            onClick = { optionsMenuExpanded = true },
                            modifier = Modifier.testTag("files_options_menu")
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_files_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = optionsMenuExpanded,
                            onDismissRequest = { optionsMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_view_type)) },
                                onClick = {
                                    optionsMenuExpanded = false
                                    showViewTypeDialog = true
                                },
                                leadingIcon = { Icon(fileViewMode.icon, contentDescription = null) },
                                modifier = Modifier.testTag("files_menu_view_type")
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_sort_by)) },
                                onClick = {
                                    optionsMenuExpanded = false
                                    showSortByDialog = true
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                                modifier = Modifier.testTag("files_menu_sort_by")
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_create)) },
                                onClick = {
                                    optionsMenuExpanded = false
                                    showCreateDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                modifier = Modifier.testTag("files_menu_create")
                            )
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
                } else {
                  // Pull-to-refresh wraps every non-permission state (lists, empty, and the
                  // category-scanning state) so the drag-down gesture reloads the current folder
                  // or category from any of them.
                  PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                  ) {
                    if (isCategoryFiltered && isCategoryLoading) {
                    // Scanning state: the recursive category walk is still running. Show a
                    // spinner rather than the previous category's stale list or an empty state.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
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
                            .verticalScroll(rememberScrollState())
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
                    // Shared tap behaviour for every view mode: unlock prompt for secured
                    // folders, otherwise navigate / extract / open note / preview / fallback.
                    val handleItemClick: (FileItem) -> Unit = { item ->
                        if (item.isSecured && !unlockedFolderSessions.contains(item.absolutePath)) {
                            activeActionPendingValidation = PendingAction(
                                title = context.getString(R.string.unlock_folder_title),
                                subtitle = context.getString(R.string.unlock_folder_subtitle, item.name),
                                onValidated = {
                                    viewModel.unlockSecuredFolderSession(item.absolutePath)
                                    if (item.isDirectory) {
                                        viewModel.navigateToDirectory(item.file)
                                    } else {
                                        if (isPickMode) {
                                            if (allowMultiplePick) {
                                                toggleSelection(item)
                                            } else {
                                                val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)
                                                viewModel.finishPicking(listOf(fileUri))
                                            }
                                        } else {
                                            if (item.name.lowercase().endsWith(".zip")) {
                                                viewModel.decompressZip(item)
                                            } else if (item.name.lowercase().endsWith(".securenote")) {
                                                viewModel.openNote(item)
                                            } else if (item.category == "Image" && imagePreviewEnabled) {
                                                viewModel.openImagePreview(item)
                                            } else if (textPreviewEnabled && viewModel.isTextPreviewable(item)) {
                                                viewModel.openTextPreview(item)
                                            }
                                        }
                                    }
                                }
                            )
                        } else {
                            if (item.isDirectory) {
                                viewModel.navigateToDirectory(item.file)
                            } else if (isPickMode) {
                                if (allowMultiplePick) {
                                    toggleSelection(item)
                                } else {
                                    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)
                                    viewModel.finishPicking(listOf(fileUri))
                                }
                            } else {
                                if (item.name.lowercase().endsWith(".zip")) {
                                    viewModel.decompressZip(item)
                                } else if (item.name.lowercase().endsWith(".securenote")) {
                                    viewModel.openNote(item)
                                } else if (item.category == "Image" && imagePreviewEnabled) {
                                    viewModel.openImagePreview(item)
                                } else if (textPreviewEnabled && viewModel.isTextPreviewable(item)) {
                                    viewModel.openTextPreview(item)
                                } else {
                                    viewModel.dispatchMessage(context.getString(R.string.msg_viewing_file, item.name, formatBytes(item.size)))
                                }
                            }
                        }
                    }

                    when (fileViewMode) {
                        FileViewMode.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 104.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(displayedFiles, key = { it.absolutePath }) { item ->
                                    FileGridItem(
                                        item = item,
                                        onItemClick = { handleItemClick(item) },
                                        onToggleSelection = { toggleSelection(item) },
                                        onOpenExternal = { openFileExternally(context, item, viewModel) },
                                        selectionMode = selectionMode && (!isPickMode || !item.isDirectory),
                                        isSelected = item.absolutePath in selectedPaths,
                                        isSecuredLocked = item.isSecured && !unlockedFolderSessions.contains(item.absolutePath)
                                    )
                                }
                            }
                        }
                        FileViewMode.COMPACT -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                items(displayedFiles, key = { it.absolutePath }) { item ->
                                    FileCompactRow(
                                        item = item,
                                        onItemClick = { handleItemClick(item) },
                                        onToggleSelection = { toggleSelection(item) },
                                        onOpenExternal = { openFileExternally(context, item, viewModel) },
                                        selectionMode = selectionMode && (!isPickMode || !item.isDirectory),
                                        isSelected = item.absolutePath in selectedPaths,
                                        isSecuredLocked = item.isSecured && !unlockedFolderSessions.contains(item.absolutePath)
                                    )
                                }
                            }
                        }
                        FileViewMode.LIST -> {
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
                                        onItemClick = { handleItemClick(item) },
                                        onToggleSelection = { toggleSelection(item) },
                                        onOpenExternal = { openFileExternally(context, item, viewModel) },
                                        selectionMode = selectionMode && (!isPickMode || !item.isDirectory),
                                        isSelected = item.absolutePath in selectedPaths,
                                        isSecuredLocked = item.isSecured && !unlockedFolderSessions.contains(item.absolutePath)
                                    )
                                }
                            }
                        }
                    }
                    }
                  }
                }
            }
        }

        // If in multi-pick mode, show a FloatingActionButton at the bottom end to confirm picking.
        // Sits directly in the outer Box (not the Column) so `align` gets its BoxScope.
        if (isPickMode && allowMultiplePick && selectedPaths.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    val pickedUris = selectedItems.map {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it.file)
                    }
                    viewModel.finishPicking(pickedUris)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("confirm_multi_pick_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm Selection")
                    Text("Done (${selectedPaths.size})", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- File/Folder Details Dialog ---
        showDetailsForFileItem?.let { detailsItem ->
            FileDetailsDialog(
                item = detailsItem,
                onDismiss = { showDetailsForFileItem = null }
            )
        }

        // --- Multi-selection Details Dialog ---
        showDetailsForSelection?.let { detailsItems ->
            SelectionDetailsDialog(
                items = detailsItems,
                onDismiss = { showDetailsForSelection = null }
            )
        }

        // --- Generic confirmation dialog (gates every action without its own dialog) ---
        pendingConfirm?.let { spec ->
            ConfirmActionDialog(
                message = spec.message,
                onConfirm = {
                    val run = spec.onConfirm
                    pendingConfirm = null
                    run()
                },
                onDismiss = { pendingConfirm = null }
            )
        }

        // --- Rename dialog ---
        renameTarget?.let { target ->
            RenameDialog(
                item = target,
                onConfirm = { newName ->
                    viewModel.renameFileItem(target, newName)
                    renameTarget = null
                    selectedPaths = emptySet()
                },
                onDismiss = { renameTarget = null }
            )
        }

        // --- Open as dialog ---
        openAsTarget?.let { target ->
            OpenAsDialog(
                onPick = { mime ->
                    openAs(context, target, mime, viewModel)
                    openAsTarget = null
                    selectedPaths = emptySet()
                },
                onDismiss = { openAsTarget = null }
            )
        }


        // --- Full-screen Image Preview ---
        imagePreview?.let { previewItem ->
            ImagePreviewDialog(
                item = previewItem,
                onDismiss = { viewModel.closeImagePreview() }
            )
        }

        // --- Full-screen Text Preview ---
        textPreview?.let { state ->
            TextPreviewDialog(
                state = state,
                onDismiss = { viewModel.closeTextPreview() }
            )
        }

        // --- Create Dialog (Folder name + Secure Note option) ---
        if (showCreateDialog) {
            var folderNameInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text(stringResource(R.string.menu_create)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = folderNameInput,
                            onValueChange = { folderNameInput = it },
                            label = { Text(stringResource(R.string.dialog_folder_name_label)) },
                            placeholder = { Text(stringResource(R.string.dialog_folder_name_placeholder)) },
                            modifier = Modifier.fillMaxWidth().testTag("folder_input_field"),
                            singleLine = true
                        )
                        Surface(
                            onClick = {
                                showCreateDialog = false
                                showCreateFileDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth().testTag("create_secure_note_option")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.NoteAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    stringResource(R.string.create_option_secure_note),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (folderNameInput.isNotBlank()) {
                                viewModel.createFolder(folderNameInput.trim())
                                showCreateDialog = false
                            }
                        },
                        enabled = folderNameInput.isNotBlank(),
                        modifier = Modifier.testTag("confirm_create_folder_btn")
                    ) {
                        Text(stringResource(R.string.create_option_folder))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // --- View Type Dialog ---
        if (showViewTypeDialog) {
            AlertDialog(
                onDismissRequest = { showViewTypeDialog = false },
                title = { Text(stringResource(R.string.menu_view_type)) },
                text = {
                    Column {
                        FileViewMode.values().forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.updateFileViewMode(mode.storageKey)
                                        showViewTypeDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                                    .testTag("files_view_${mode.storageKey}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    mode.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(stringResource(mode.contentDescRes), modifier = Modifier.weight(1f))
                                if (mode == fileViewMode) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showViewTypeDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // --- Sort By Dialog ---
        if (showSortByDialog) {
            var tempSortMode by remember(showSortByDialog) { mutableStateOf(sortMode) }
            var tempSortAscending by remember(showSortByDialog) { mutableStateOf(sortAscending) }

            AlertDialog(
                onDismissRequest = { showSortByDialog = false },
                title = { Text(stringResource(R.string.menu_sort_by)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FileSortMode.values().forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (tempSortMode == mode) {
                                            tempSortAscending = !tempSortAscending
                                        } else {
                                            tempSortMode = mode
                                            tempSortAscending = (mode == FileSortMode.NAME)
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                                    .testTag("files_sort_${mode.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(stringResource(mode.labelRes), modifier = Modifier.weight(1f))
                                if (mode == tempSortMode) {
                                    Icon(
                                        imageVector = if (tempSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = if (tempSortAscending) stringResource(R.string.sort_ascending) else stringResource(R.string.sort_descending),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = tempSortAscending,
                                onClick = { tempSortAscending = true },
                                label = { Text(stringResource(R.string.sort_ascending)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("files_sort_ascending")
                            )
                            FilterChip(
                                selected = !tempSortAscending,
                                onClick = { tempSortAscending = false },
                                label = { Text(stringResource(R.string.sort_descending)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("files_sort_descending")
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            sortMode = tempSortMode
                            sortAscending = tempSortAscending
                            showSortByDialog = false
                        },
                        modifier = Modifier.testTag("files_sort_ok")
                    ) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSortByDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // --- Secure Note Creator Bottom Sheet ---
        if (showCreateFileDialog) {
            var filenameInput by remember { mutableStateOf("") }
            var textContentInput by remember { mutableStateOf("") }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val sheetScope = rememberCoroutineScope()

            fun dismissSheet() {
                sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) showCreateFileDialog = false
                }
            }

            ModalBottomSheet(
                onDismissRequest = { showCreateFileDialog = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialog_new_note_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
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
                        modifier = Modifier.fillMaxWidth().weight(1f).testTag("note_content_field")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { dismissSheet() }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (filenameInput.isNotBlank()) {
                                    viewModel.createTextFile(filenameInput.trim(), textContentInput)
                                    dismissSheet()
                                }
                            },
                            modifier = Modifier.testTag("confirm_create_note_btn")
                        ) {
                            Text(stringResource(R.string.action_save_file))
                        }
                    }
                }
            }
        }

        // --- Secure Note Viewer/Editor Bottom Sheet ---
        openNote?.let { note ->
            val viewerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val viewerScope = rememberCoroutineScope()
            var isEditing by remember(note.file.absolutePath) { mutableStateOf(false) }
            var editContent by remember(note.file.absolutePath) { mutableStateOf(note.content) }

            fun dismissViewer() {
                viewerScope.launch { viewerSheetState.hide() }.invokeOnCompletion {
                    if (!viewerSheetState.isVisible) viewModel.closeNote()
                }
            }

            ModalBottomSheet(
                onDismissRequest = { viewModel.closeNote() },
                sheetState = viewerSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = note.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isEditing) {
                        OutlinedTextField(
                            value = editContent,
                            onValueChange = { editContent = it },
                            label = { Text(stringResource(R.string.dialog_note_content_label)) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("note_edit_field")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                editContent = note.content
                                isEditing = false
                            }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.saveNoteEdits(editContent)
                                    isEditing = false
                                },
                                modifier = Modifier.testTag("note_save_edit_btn")
                            ) {
                                Text(stringResource(R.string.action_update))
                            }
                        }
                    } else {
                        SelectionContainer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    val target = note
                                    viewModel.closeNote()
                                    if (isPhoneLockDeleteEnabled) {
                                        activeActionPendingValidation = PendingAction(
                                            title = context.getString(R.string.confirm_delete_title),
                                            subtitle = context.getString(R.string.confirm_delete_subtitle, target.name),
                                            onValidated = { viewModel.deleteOpenNoteFile(target.file, target.name) }
                                        )
                                    } else {
                                        viewModel.deleteOpenNoteFile(target.file, target.name)
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.action_delete))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { dismissViewer() }) {
                                Text(stringResource(R.string.action_close))
                            }
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.testTag("note_edit_btn")
                            ) {
                                Text(stringResource(R.string.action_edit))
                            }
                        }
                    }
                }
            }
        }

        // --- ZIP Compress Options Dialog (single or multiple items) ---
        showZipDialogForItems?.let { pendingZipItems ->
            val defaultName = if (pendingZipItems.size == 1) {
                pendingZipItems.first().name + ".zip"
            } else {
                "archive.zip"
            }
            var zipNameInput by remember { mutableStateOf(defaultName) }
            AlertDialog(
                onDismissRequest = { showZipDialogForItems = null },
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
                                if (pendingZipItems.size == 1) {
                                    viewModel.compressFolderOrFile(pendingZipItems.first(), zipNameInput.trim())
                                } else {
                                    viewModel.compressItems(pendingZipItems, zipNameInput.trim())
                                }
                                showZipDialogForItems = null
                                selectedPaths = emptySet()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_compress))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showZipDialogForItems = null }) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRowItem(
    item: FileItem,
    onItemClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onOpenExternal: () -> Unit,
    selectionMode: Boolean,
    isSelected: Boolean,
    isSecuredLocked: Boolean
) {
    val fileIcon = getIconForFileCategory(item)
    val iconTint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val isApk = !item.isDirectory && item.name.substringAfterLast('.', "").lowercase() == "apk"
    val showThumbnail = !isSecuredLocked && !item.isDirectory && item.category == "Image"
    val dateText = remember(item.file) {
        val lastMod = item.file.lastModified()
        val format = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        format.format(java.util.Date(lastMod))
    }
    // Size now lives on the meta line (freeing the full row width for the name). For directories
    // the size is appended only once it has finished computing; otherwise a small inline spinner
    // is shown at the end of the meta row instead.
    val metaText = if (item.isDirectory) {
        val items = pluralStringResource(R.plurals.item_count, item.itemCount, item.itemCount)
        if (item.sizeComputed) "$dateText · $items · ${formatBytes(item.size)}" else "$dateText · $items"
    } else {
        "$dateText · ${formatBytes(item.size)}"
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else onItemClick() },
                onDoubleClick = if (!item.isDirectory && !selectionMode) onOpenExternal else null,
                onLongClick = onToggleSelection
            )
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
                when {
                    isSecuredLocked -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.cd_locked_folder),
                        tint = Color(0xFFE74C3C),
                        modifier = Modifier.size(22.dp)
                    )
                    showThumbnail -> SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.file)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Icon(
                                imageVector = fileIcon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(23.dp)
                            )
                        },
                        error = {
                            Icon(
                                imageVector = fileIcon,
                                contentDescription = item.category,
                                tint = iconTint,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    )
                    isApk -> ApkIcon(
                        apkPath = item.absolutePath,
                        contentDescription = item.name,
                        fallbackTint = iconTint
                    )
                    else -> Icon(
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
                    MiddleEllipsisText(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
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

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = metaText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Directory size is still being summed: show a small inline spinner after the
                    // item count instead of a size on the (now removed) right-hand column.
                    if (item.isDirectory && !item.sizeComputed) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.cd_selected_item),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Grid/thumbnail tile. Image files render a real Coil thumbnail (falling back to the category
 * icon while loading or on decode failure); folders, APKs, and other types show their icon.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    item: FileItem,
    onItemClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onOpenExternal: () -> Unit,
    selectionMode: Boolean,
    isSelected: Boolean,
    isSecuredLocked: Boolean
) {
    val fileIcon = getIconForFileCategory(item)
    val iconTint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val isApk = !item.isDirectory && item.name.substringAfterLast('.', "").lowercase() == "apk"
    val showThumbnail = !isSecuredLocked && !item.isDirectory && item.category == "Image"

    val metaText = if (item.isDirectory) {
        if (item.sizeComputed) formatBytes(item.size) else null
    } else {
        formatBytes(item.size)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else onItemClick() },
                onDoubleClick = if (!item.isDirectory && !selectionMode) onOpenExternal else null,
                onLongClick = onToggleSelection
            )
            .testTag("file_grid_${item.name}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(iconTint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isSecuredLocked -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.cd_locked_folder),
                        tint = Color(0xFFE74C3C),
                        modifier = Modifier.size(30.dp)
                    )
                    showThumbnail -> SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.file)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Icon(
                                imageVector = fileIcon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(34.dp)
                            )
                        },
                        error = {
                            Icon(
                                imageVector = fileIcon,
                                contentDescription = item.category,
                                tint = iconTint,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    )
                    isApk -> ApkIcon(
                        apkPath = item.absolutePath,
                        contentDescription = item.name,
                        fallbackTint = iconTint
                    )
                    else -> Icon(
                        imageVector = fileIcon,
                        contentDescription = item.category,
                        tint = iconTint,
                        modifier = Modifier.size(34.dp)
                    )
                }

                if (item.isSecured) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = stringResource(R.string.cd_shielded_folder),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(15.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("selected_${item.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.cd_selected_item),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (metaText != null) {
                        Text(
                            text = metaText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (item.isDirectory && !item.sizeComputed) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Dense single-line row used by the compact view mode. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCompactRow(
    item: FileItem,
    onItemClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onOpenExternal: () -> Unit,
    selectionMode: Boolean,
    isSelected: Boolean,
    isSecuredLocked: Boolean
) {
    val fileIcon = getIconForFileCategory(item)
    val iconTint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val isApk = !item.isDirectory && item.name.substringAfterLast('.', "").lowercase() == "apk"
    val showThumbnail = !isSecuredLocked && !item.isDirectory && item.category == "Image"
    val sizeText = if (item.isDirectory) {
        if (item.sizeComputed) formatBytes(item.size) else null
    } else {
        formatBytes(item.size)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else onItemClick() },
                onDoubleClick = if (!item.isDirectory && !selectionMode) onOpenExternal else null,
                onLongClick = onToggleSelection
            )
            .testTag("file_compact_${item.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isSecuredLocked -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.cd_locked_folder),
                        tint = Color(0xFFE74C3C),
                        modifier = Modifier.size(16.dp)
                    )
                    showThumbnail -> SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.file)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Icon(
                                imageVector = fileIcon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        error = {
                            Icon(
                                imageVector = fileIcon,
                                contentDescription = item.category,
                                tint = iconTint,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    )
                    isApk -> ApkIcon(
                        apkPath = item.absolutePath,
                        contentDescription = item.name,
                        fallbackTint = iconTint
                    )
                    else -> Icon(
                        imageVector = fileIcon,
                        contentDescription = item.category,
                        tint = iconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            MiddleEllipsisText(
                text = item.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (item.isSecured) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = stringResource(R.string.cd_shielded_folder),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }

            if (sizeText != null) {
                Text(
                    text = sizeText,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (item.isDirectory && !item.sizeComputed) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(11.dp)
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.cd_selected_item),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Contextual action bar shown in place of the search field while one or more items are selected
 * (multi-select via long-press). Shows icon actions that adapt to the current selection:
 * Details/Compress/Extract are single-selection only (they drive name dialogs); Lock-shield is
 * shown when every selected item is a folder, Move-to-vault when every item is a file; Delete is
 * always available and acts on the whole selection.
 */
@Composable
private fun SelectionToolbar(
    selectedItems: List<FileItem>,
    onClear: () -> Unit,
    onAction: (SelectionAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = selectedItems.size
    val single = selectedItems.singleOrNull()
    val isSingle = single != null
    val isSingleFile = single != null && !single.isDirectory
    val isSingleFolder = single != null && single.isDirectory
    val isImage = isSingleFile && single!!.category == "Image"
    val isSingleZip = isSingleFile && single!!.name.lowercase().endsWith(".zip")
    val allDirs = selectedItems.isNotEmpty() && selectedItems.all { it.isDirectory }
    val allFiles = selectedItems.isNotEmpty() && selectedItems.all { !it.isDirectory }
    val allSecured = allDirs && selectedItems.all { it.isSecured }

    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("files_selection_toolbar"),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear selection (acts as the "back"; no separate back arrow per design).
            IconButton(onClick = onClear, modifier = Modifier.testTag("selection_clear")) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_clear_selection),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            // Count shown as just the number (e.g. "1").
            Text(
                text = count.toString(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // Always-visible quick actions: Delete, Info, and (single only) Rename.
            IconButton(onClick = { onAction(SelectionAction.DELETE) }, modifier = Modifier.testTag("selection_action_delete")) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.menu_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = { onAction(SelectionAction.INFO) }, modifier = Modifier.testTag("selection_action_details")) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = stringResource(R.string.menu_details),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (isSingle) {
                IconButton(onClick = { onAction(SelectionAction.RENAME) }, modifier = Modifier.testTag("selection_action_rename")) {
                    Icon(
                        Icons.Default.DriveFileRenameOutline,
                        contentDescription = stringResource(R.string.menu_rename),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            // Overflow: everything else, gated by selection type.
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag("selection_overflow")) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_actions),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    @Composable
                    fun item(@androidx.annotation.StringRes labelRes: Int, icon: ImageVector, action: SelectionAction) {
                        DropdownMenuItem(
                            text = { Text(stringResource(labelRes)) },
                            leadingIcon = { Icon(icon, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAction(action)
                            }
                        )
                    }

                    item(R.string.menu_share, Icons.Default.Share, SelectionAction.SHARE)
                    item(R.string.menu_hide, Icons.Default.VisibilityOff, SelectionAction.HIDE)
                    if (isSingle) {
                        item(R.string.menu_create_shortcut, Icons.Default.AddLink, SelectionAction.CREATE_SHORTCUT)
                        item(R.string.menu_copy_path, Icons.Default.ContentCopy, SelectionAction.COPY_PATH)
                    }
                    if (isImage) {
                        item(R.string.menu_set_as, Icons.Default.Wallpaper, SelectionAction.SET_AS)
                    }
                    if (isSingleFile) {
                        item(R.string.menu_open_with, Icons.Default.OpenInNew, SelectionAction.OPEN_WITH)
                        item(R.string.menu_open_as, Icons.Default.FileOpen, SelectionAction.OPEN_AS)
                    }
                    item(R.string.menu_copy_to, Icons.Default.FileCopy, SelectionAction.COPY_TO)
                    item(R.string.menu_move_to, Icons.AutoMirrored.Filled.DriveFileMove, SelectionAction.MOVE_TO)
                    item(R.string.menu_compress, Icons.Default.Compress, SelectionAction.COMPRESS)
                    if (isSingleZip) {
                        item(R.string.menu_decompress, Icons.Default.FolderZip, SelectionAction.EXTRACT)
                    }
                    // Kept secure-app actions (per design): folders → lock/shield, files → vault.
                    if (allDirs) {
                        item(
                            if (allSecured) R.string.menu_remove_shield else R.string.menu_lock_folder,
                            Icons.Default.Security,
                            SelectionAction.SHIELD_TOGGLE
                        )
                    }
                    if (allFiles) {
                        item(R.string.menu_move_vault, Icons.Default.VpnKey, SelectionAction.MOVE_VAULT)
                    }
                    item(R.string.menu_select_all, Icons.Default.SelectAll, SelectionAction.SELECT_ALL)
                }
            }
        }
    }
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

/**
 * Aggregate read-only properties dialog for a multi-selection. Shows the combined type, the shared
 * parent folder, the total size, and the number of selected items. There is no single "modified"
 * timestamp across a selection, so that row is rendered as a placeholder dash.
 */
@Composable
fun SelectionDetailsDialog(
    items: List<FileItem>,
    onDismiss: () -> Unit
) {
    val hasFiles = items.any { !it.isDirectory }
    val hasFolders = items.any { it.isDirectory }
    val typeText = when {
        hasFiles && hasFolders -> stringResource(R.string.detail_files_and_folders)
        hasFolders -> stringResource(R.string.detail_folders)
        else -> stringResource(R.string.detail_files)
    }
    val parentPath = items.firstOrNull()?.file?.parent ?: ""
    val totalSize = items.sumOf { it.size }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.files_selected_count, items.size)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(stringResource(R.string.detail_type), typeText)
                DetailRow(stringResource(R.string.detail_path), parentPath)
                DetailRow(stringResource(R.string.detail_size), formatBytes(totalSize))
                DetailRow(stringResource(R.string.detail_count), items.size.toString())
                DetailRow(stringResource(R.string.detail_modified), "—")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

/**
 * Full-screen image preview rendered in a [Dialog]. Hands the local file straight to Coil with
 * the GIF/animated (ImageDecoder/GifDecoder) and SVG decoders registered, so the platform raster
 * decoders plus these add-ons cover effectively every decodable format. Anything Coil cannot
 * decode (e.g. RAW/TIFF without an embedded preview) shows the graceful error slot rather than
 * crashing. Tapping the scrim, the close button, or the system back gesture dismisses.
 */
@Composable
private fun ImagePreviewDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .build()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
                .testTag("image_preview_dialog")
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.file)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                loading = {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                },
                error = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.image_preview_error),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("image_preview_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close_preview),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TextPreviewDialog(
    state: TextPreviewUi,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .testTag("text_preview_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top bar: filename + close button.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.item.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("text_preview_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close_preview),
                            tint = Color.White
                        )
                    }
                }

                when (state) {
                    is TextPreviewUi.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    is TextPreviewUi.Failed -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.text_preview_error),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is TextPreviewUi.Ready -> {
                        if (state.truncated) {
                            Text(
                                text = stringResource(R.string.text_preview_truncated),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        SelectionContainer(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = state.text,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
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

/**
 * Opens [item] with an external app. Regular files use `ACTION_VIEW` (the OS opens the user's
 * default app, or shows the app chooser when none is set). APKs are routed to the system package
 * installer, which handles both fresh installs and updates — on Android O+ this first requires the
 * per-app "install unknown apps" permission, so when it is not yet granted the user is sent to that
 * settings screen and asked to re-tap the APK.
 */
fun openFileExternally(context: Context, item: FileItem, viewModel: StorageViewModel) {
    val uri: Uri = try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)
    } catch (e: Exception) {
        viewModel.dispatchMessage(context.getString(R.string.msg_open_failed, item.name))
        return
    }

    val extension = item.name.substringAfterLast('.', "").lowercase()

    if (extension == "apk") {
        // Android O+ gates APK installs behind a per-app "install unknown apps" permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            viewModel.dispatchMessage(context.getString(R.string.msg_install_permission_needed))
            try {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            } catch (e: Exception) {
                viewModel.dispatchMessage(context.getString(R.string.msg_cannot_launch_settings))
            }
            return
        }
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: Exception) {
            viewModel.dispatchMessage(context.getString(R.string.msg_install_failed, item.name))
        }
        return
    }

    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (e: ActivityNotFoundException) {
        viewModel.dispatchMessage(context.getString(R.string.msg_no_app_to_open, item.name))
    } catch (e: Exception) {
        viewModel.dispatchMessage(context.getString(R.string.msg_open_failed, item.name))
    }
}

// ---- Selection-menu support: state holders, intent helpers, and dialogs ----

/** A pending generic confirmation: [message] is shown with OK/Cancel; OK runs [onConfirm]. */
data class ConfirmSpec(val message: String, val onConfirm: () -> Unit)


private fun mimeTypeOf(item: FileItem): String = mimeTypeForName(item.name)

private fun uriFor(context: Context, item: FileItem): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)

/** Shares the file(s) of [items] through the system share sheet. Folders cannot be shared. */
fun shareItems(context: Context, items: List<FileItem>, viewModel: StorageViewModel) {
    val files = items.filter { !it.isDirectory }
    if (files.isEmpty()) {
        viewModel.dispatchMessage(context.getString(R.string.msg_cannot_share_folder))
        return
    }
    try {
        val uris = ArrayList(files.map { uriFor(context, it) })
        val shareType = shareTypeFor(files.map { it.name })
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = shareType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = shareType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Bluetooth's intent filter lists only a handful of MIME types, so it drops out of the
            // sheet for APKs, archives and the like. Add it back explicitly when it is missing.
            bluetoothInitialIntents(context, intent, uris)?.let {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, it)
            }
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        viewModel.dispatchMessage(context.getString(R.string.msg_share_failed, files.first().name))
    }
}

/** Launches the "Set as" flow (wallpaper, contact photo, …) for an image via ACTION_ATTACH_DATA. */
fun setAsImage(context: Context, item: FileItem, viewModel: StorageViewModel) {
    if (item.category != "Image") {
        viewModel.dispatchMessage(context.getString(R.string.msg_set_as_image_only))
        return
    }
    try {
        val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            setDataAndType(uriFor(context, item), "image/*")
            putExtra("mimeType", "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (e: Exception) {
        viewModel.dispatchMessage(context.getString(R.string.msg_set_as_failed, item.name))
    }
}

/** Opens [item] forcing the system app-chooser bottom sheet (never the silent default). */
fun openWithChooser(context: Context, item: FileItem, viewModel: StorageViewModel) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(context, item), mimeTypeOf(item))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (e: ActivityNotFoundException) {
        viewModel.dispatchMessage(context.getString(R.string.msg_no_app_to_open, item.name))
    } catch (e: Exception) {
        viewModel.dispatchMessage(context.getString(R.string.msg_open_failed, item.name))
    }
}

/** Opens [item] forcing the chosen [mime] type (the "Open as" dialog's selection). */
fun openAs(context: Context, item: FileItem, mime: String, viewModel: StorageViewModel) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(context, item), mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (e: ActivityNotFoundException) {
        viewModel.dispatchMessage(context.getString(R.string.msg_no_app_to_open, item.name))
    } catch (e: Exception) {
        viewModel.dispatchMessage(context.getString(R.string.msg_open_failed, item.name))
    }
}

/** Copies the absolute path(s) of [items] to the clipboard (newline-separated for many). */
fun copyPathToClipboard(context: Context, items: List<FileItem>, viewModel: StorageViewModel) {
    val text = items.joinToString("\n") { it.absolutePath }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("path", text))
    viewModel.dispatchMessage(context.getString(R.string.msg_path_copied))
}

/** Requests a pinned home-screen shortcut that re-opens the app, labelled with the item name. */
fun createPinnedShortcut(context: Context, item: FileItem, viewModel: StorageViewModel) {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        viewModel.dispatchMessage(context.getString(R.string.msg_shortcut_unsupported))
        return
    }
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: Intent(Intent.ACTION_MAIN)
    val shortcut = ShortcutInfoCompat.Builder(context, "file_${item.absolutePath.hashCode()}")
        .setShortLabel(item.name.take(24))
        .setLongLabel(item.name)
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(launch)
        .build()
    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    viewModel.dispatchMessage(context.getString(R.string.msg_shortcut_created, item.name))
}

@Composable
fun ConfirmActionDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_generic_title)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        modifier = Modifier.testTag("confirm_action_dialog")
    )
}

@Composable
fun RenameDialog(item: FileItem, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.rename_dialog_label)) },
                modifier = Modifier.testTag("rename_field")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name.trim() != item.name
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun OpenAsDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        R.string.open_as_text to "text/*",
        R.string.open_as_image to "image/*",
        R.string.open_as_audio to "audio/*",
        R.string.open_as_video to "video/*",
        R.string.open_as_other to "*/*"
    )
    var selected by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.open_as_dialog_title)) },
        text = {
            Column {
                options.forEachIndexed { index, (labelRes, _) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = index }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == index, onClick = { selected = index })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(labelRes))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(options[selected].second) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}


fun getIconForFileCategory(item: FileItem): ImageVector {
    if (item.isDirectory) return Icons.Default.Folder
    when (item.name.substringAfterLast('.', "").lowercase()) {
        "apk", "apks", "xapk" -> return Icons.Default.Android
    }
    return when (item.category) {
        "Image" -> Icons.Default.Image
        "Video" -> Icons.Default.Videocam
        "Audio" -> Icons.Default.AudioFile
        "Document" -> Icons.Default.Description
        "Archive" -> Icons.Default.FolderZip
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

/**
 * Renders a file name keeping both the start and the end visible. Compose's
 * [TextOverflow.MiddleEllipsis] needs Compose 1.8+, so this composes a head (which ellipsizes at
 * the end) with a fixed-length tail that is never truncated, e.g. `brahma_muhurta-v…3_1-release.apk`.
 */
@Composable
private fun MiddleEllipsisText(
    text: String,
    fontWeight: FontWeight,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    tailLength: Int = 14
) {
    val tail = if (text.length > tailLength + 1) text.takeLast(tailLength) else ""
    val head = if (tail.isEmpty()) text else text.dropLast(tailLength)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = head,
            fontWeight = fontWeight,
            fontSize = fontSize,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (tail.isNotEmpty()) {
            Text(
                text = tail,
                fontWeight = fontWeight,
                fontSize = fontSize,
                color = color,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Loads and shows the launcher icon embedded in an APK. Reads the icon off the main thread via
 * [android.content.pm.PackageManager.getPackageArchiveInfo]; while loading or if the APK can't be
 * read (no access / corrupt) it falls back to a generic Android-app vector icon.
 */
@Composable
private fun ApkIcon(
    apkPath: String,
    contentDescription: String,
    fallbackTint: Color
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, apkPath) {
        value = withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val pkg = pm.getPackageArchiveInfo(apkPath, 0)
                pkg?.applicationInfo?.let { appInfo ->
                    appInfo.sourceDir = apkPath
                    appInfo.publicSourceDir = apkPath
                    appInfo.loadIcon(pm).toBitmap().asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = contentDescription,
            tint = fallbackTint,
            modifier = Modifier.size(23.dp)
        )
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

package com.example.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import `in`.sreerajp.vault_files.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/** A decrypted secure note surfaced to the viewer. [file] is the backing `.securenote` to save into. */
data class OpenNote(val name: String, val content: String, val file: File)

/** UI state for the full-screen text-file preview; null when no preview is open. */
sealed interface TextPreviewUi {
    val item: FileItem
    /** Read in progress. */
    data class Loading(override val item: FileItem) : TextPreviewUi
    /** Content decoded successfully; [truncated] is true when the file exceeded the read cap. */
    data class Ready(override val item: FileItem, val text: String, val truncated: Boolean) : TextPreviewUi
    /** Read failed or the content is not displayable as text (e.g. binary). */
    data class Failed(override val item: FileItem) : TextPreviewUi
}

class StorageViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = StorageRepository(
        context = application,
        settingsDao = db.settingsDao(),
        securedFolderDao = db.securedFolderDao(),
        vaultFileDao = db.vaultFileDao()
    )

    // --- State declarations ---

    private val _storageSourceMode = MutableStateFlow("sandbox")
    val storageSourceMode: StateFlow<String> = _storageSourceMode.asStateFlow()

    // Selected Files-explorer layout: "list", "grid", or "compact". Persisted in settings.
    private val _fileViewMode = MutableStateFlow("list")
    val fileViewMode: StateFlow<String> = _fileViewMode.asStateFlow()

    val userStorageRoot: File
        get() = if (_storageSourceMode.value == "device") {
            android.os.Environment.getExternalStorageDirectory()
        } else {
            repository.userStorageRoot
        }

    private val _currentDirectory = MutableStateFlow<File>(repository.userStorageRoot)
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()

    private val _currentDirectoryFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val currentDirectoryFiles: StateFlow<List<FileItem>> = _currentDirectoryFiles.asStateFlow()

    // True while the background folder-size pass is running, so each folder row can show a small
    // spinner where its size will appear.
    private val _isComputingDirectorySizes = MutableStateFlow(false)
    val isComputingDirectorySizes: StateFlow<Boolean> = _isComputingDirectorySizes.asStateFlow()

    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()

    // Active category filter (canonical: "Image", "Video", "Audio", "Document", "Archive", "Other"),
    // or null when the Files screen is in normal directory-browsing mode.
    private val _activeCategoryFilter = MutableStateFlow<String?>(null)
    val activeCategoryFilter: StateFlow<String?> = _activeCategoryFilter.asStateFlow()

    // Flat, recursive list of files matching the active category filter.
    private val _categoryFilteredFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val categoryFilteredFiles: StateFlow<List<FileItem>> = _categoryFilteredFiles.asStateFlow()

    // True while the recursive category scan is running, so the UI can show a loading state
    // instead of the previous category's stale results.
    private val _isCategoryLoading = MutableStateFlow(false)
    val isCategoryLoading: StateFlow<Boolean> = _isCategoryLoading.asStateFlow()

    val securedFolders: StateFlow<List<SecuredFolder>> = repository.getSecuredFoldersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultFiles: StateFlow<List<VaultFile>> = repository.getVaultFilesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _phoneLockDeleteEnabled = MutableStateFlow(true)
    val phoneLockDeleteEnabled: StateFlow<Boolean> = _phoneLockDeleteEnabled.asStateFlow()

    // null until loaded from storage, so the first-launch request effect doesn't fire on the
    // default value before the persisted flag is read.
    private val _storagePermissionRequested = MutableStateFlow<Boolean?>(null)
    val storagePermissionRequested: StateFlow<Boolean?> = _storagePermissionRequested.asStateFlow()

    private val _customPin = MutableStateFlow<String?>(null)
    val customPin: StateFlow<String?> = _customPin.asStateFlow()

    private val _themePreference = MutableStateFlow("system")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    private val _showHiddenItems = MutableStateFlow(false)
    val showHiddenItems: StateFlow<Boolean> = _showHiddenItems.asStateFlow()

    private val _passwordProtectApp = MutableStateFlow(false)
    val passwordProtectApp: StateFlow<Boolean> = _passwordProtectApp.asStateFlow()

    private val _passwordProtectHidden = MutableStateFlow(false)
    val passwordProtectHidden: StateFlow<Boolean> = _passwordProtectHidden.asStateFlow()

    // When true, tapping an image file opens the in-app preview; when false images fall back
    // to the generic "viewing file" message. Defaults to enabled.
    private val _imagePreviewEnabled = MutableStateFlow(true)
    val imagePreviewEnabled: StateFlow<Boolean> = _imagePreviewEnabled.asStateFlow()

    /** Image file currently shown in the full-screen preview, or null when none is open. */
    private val _imagePreview = MutableStateFlow<FileItem?>(null)
    val imagePreview: StateFlow<FileItem?> = _imagePreview.asStateFlow()

    // When true, tapping a detected text file opens the in-app text viewer; when false text
    // files fall back to the generic "viewing file" message. Defaults to enabled.
    private val _textPreviewEnabled = MutableStateFlow(true)
    val textPreviewEnabled: StateFlow<Boolean> = _textPreviewEnabled.asStateFlow()

    /** Text file currently shown in the full-screen preview, or null when none is open. */
    private val _textPreview = MutableStateFlow<TextPreviewUi?>(null)
    val textPreview: StateFlow<TextPreviewUi?> = _textPreview.asStateFlow()

    // Flag representing whether hidden files have been unlocked in this active sessions
    private val _isHiddenUnlocked = MutableStateFlow(false)
    val isHiddenUnlocked: StateFlow<Boolean> = _isHiddenUnlocked.asStateFlow()

    // Authentication session states
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    // Temporary list of folder paths the user unlocked in this session
    private val _unlockedFolderSessions = MutableStateFlow<Set<String>>(emptySet())
    val unlockedFolderSessions: StateFlow<Set<String>> = _unlockedFolderSessions.asStateFlow()

    // Simple Snack message dispatcher for Compose scaffold to display toasts
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            // Remove any fabricated/seeded demo data left by earlier versions (one-time).
            repository.purgeFabricatedDataIfNeeded()
            
            // Sync preferences on start
            _storageSourceMode.value = repository.getStorageSourceMode()
            _phoneLockDeleteEnabled.value = repository.isPhoneLockDeleteEnabled()
            _storagePermissionRequested.value = repository.isStoragePermissionRequested()
            _customPin.value = repository.getCustomPin()
            _themePreference.value = repository.getThemePreference()
            _showHiddenItems.value = repository.isShowHiddenItems()
            _passwordProtectApp.value = repository.isPasswordProtectApp()
            _passwordProtectHidden.value = repository.isPasswordProtectHidden()
            _imagePreviewEnabled.value = repository.isImagePreviewEnabled()
            _textPreviewEnabled.value = repository.isTextPreviewEnabled()
            _fileViewMode.value = repository.getFileViewMode()

            _currentDirectory.value = userStorageRoot

            // Storage stats are a full-device recursive scan and don't change as the user
            // navigates folders, so compute them once here rather than on every directory change.
            refreshStorageStats()

            // Observe settings flows for live adjustments
            repository.getPhoneLockDeleteSettingFlow().collect { setting ->
                if (setting != null) {
                    _phoneLockDeleteEnabled.value = setting.value.toBoolean()
                }
            }
        }

        // Keep local listings updated based on directory, show setting, or session unlock changes
        viewModelScope.launch {
            combine(
                _currentDirectory,
                _showHiddenItems,
                _passwordProtectHidden,
                _isHiddenUnlocked
            ) { dir, _, _, _ -> dir }.collect { dir ->
                loadFilesInDirectory(dir)
            }
        }
    }

    // --- Directory Navigation Operations ---

    fun navigateToDirectory(dir: File) {
        _currentDirectory.value = dir
    }

    fun navigateUp(): Boolean {
        val parent = _currentDirectory.value.parentFile
        if (parent != null && parent.absolutePath.startsWith(userStorageRoot.absolutePath)) {
            _currentDirectory.value = parent
            return true
        }
        return false
    }

    // Tracks the in-flight directory load (both the progressive listing and the background
    // folder-size pass) so a new navigation cancels the previous, possibly slow, work instead of
    // letting it overwrite the current listing.
    private var loadJob: Job? = null
    // Monotonic token: each load increments it, and only the latest generation is allowed to
    // write listing/loading state, so rapid navigation can't leave stale results or a stuck spinner.
    private var loadGeneration = 0

    fun loadFilesInDirectory(dir: File) {
        val generation = ++loadGeneration
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val showHidden = _showHiddenItems.value && (!_passwordProtectHidden.value || _isHiddenUnlocked.value)

            // Phase 1: progressive listing (no recursive folder sizes) — emit items as they arrive
            // so large directories fill in gradually instead of after one pause.
            var listing: List<FileItem> = emptyList()
            repository.getFilesAndFoldersFlow(dir, showHidden).collect { partial ->
                listing = partial
                if (generation == loadGeneration) {
                    _currentDirectoryFiles.value = partial
                }
            }
            if (generation != loadGeneration) return@launch

            // Phase 2: fill in folder sizes one folder at a time, re-emitting progressively. Each
            // folder row keeps its own spinner (sizeComputed = false) until its size lands, so small
            // folders show first and large ones fill in later. Skip entirely if nothing is pending.
            val hasPendingFolders = listing.any { it.isDirectory && !it.sizeComputed }
            if (!hasPendingFolders) {
                _isComputingDirectorySizes.value = false
                return@launch
            }
            _isComputingDirectorySizes.value = true
            try {
                repository.computeDirectorySizesFlow(listing).collect { withSizes ->
                    if (generation == loadGeneration) {
                        _currentDirectoryFiles.value = withSizes
                    }
                }
            } finally {
                if (generation == loadGeneration) {
                    _isComputingDirectorySizes.value = false
                }
            }
        }
    }

    // --- Category Filter (Storage Analysis tile -> Files) ---

    fun openCategoryFilter(category: String) {
        _activeCategoryFilter.value = category
        // Drop the previous category's results immediately so the new chip is never shown over
        // a stale list while the recursive scan runs.
        _categoryFilteredFiles.value = emptyList()
        loadCategoryFilteredFiles()
    }

    fun clearCategoryFilter() {
        _activeCategoryFilter.value = null
        _categoryFilteredFiles.value = emptyList()
        _isCategoryLoading.value = false
    }

    fun loadCategoryFilteredFiles() {
        val category = _activeCategoryFilter.value ?: return
        viewModelScope.launch {
            _isCategoryLoading.value = true
            try {
                val showHidden = _showHiddenItems.value && (!_passwordProtectHidden.value || _isHiddenUnlocked.value)
                _categoryFilteredFiles.value = repository.getFilesByCategoryRecursive(userStorageRoot, category, showHidden)
            } finally {
                _isCategoryLoading.value = false
            }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repository.getStorageUsageStats(
                userStorageRoot,
                isDeviceSource = _storageSourceMode.value == "device"
            )
        }
    }

    // --- File Action Operations ---

    fun createFolder(name: String) {
        viewModelScope.launch {
            val success = repository.createNewFolder(_currentDirectory.value, name)
            if (success) {
                dispatchMessage(string(R.string.msg_folder_created, name))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_folder_create_failed))
            }
        }
    }

    fun createTextFile(name: String, content: String) {
        viewModelScope.launch {
            val success = repository.createEncryptedNote(_currentDirectory.value, name, content)
            if (success) {
                dispatchMessage(string(R.string.msg_file_created, name))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_file_create_failed))
            }
        }
    }

    /** Decrypted secure note currently shown in the read-only viewer, or null when none is open. */
    private val _openNote = MutableStateFlow<OpenNote?>(null)
    val openNote: StateFlow<OpenNote?> = _openNote.asStateFlow()

    fun openNote(item: FileItem) {
        viewModelScope.launch {
            val content = repository.readEncryptedNote(item.file)
            if (content != null) {
                _openNote.value = OpenNote(item.name, content, item.file)
            } else {
                dispatchMessage(string(R.string.msg_note_open_failed))
            }
        }
    }

    fun saveNoteEdits(newContent: String) {
        val note = _openNote.value ?: return
        viewModelScope.launch {
            val success = repository.overwriteEncryptedNote(note.file, newContent)
            if (success) {
                _openNote.value = note.copy(content = newContent)
                dispatchMessage(string(R.string.msg_note_updated))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_note_update_failed))
            }
        }
    }

    /** Deletes a secure note's backing file. Takes file+name directly because the viewer closes
     *  (clearing [openNote]) before the optional phone-lock confirmation runs. */
    fun deleteOpenNoteFile(file: File, name: String) {
        viewModelScope.launch {
            val success = repository.deleteFile(file)
            if (success) {
                dispatchMessage(string(R.string.msg_deleted, name))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_delete_failed, name))
            }
        }
    }

    fun closeNote() {
        _openNote.value = null
    }

    // --- Image Preview ---

    fun openImagePreview(item: FileItem) {
        _imagePreview.value = item
    }

    fun closeImagePreview() {
        _imagePreview.value = null
    }

    // --- Text Preview ---

    /** True if [item] is eligible for the in-app text viewer (extension-based, cheap). */
    fun isTextPreviewable(item: FileItem): Boolean = repository.isLikelyTextFile(item.file)

    fun openTextPreview(item: FileItem) {
        _textPreview.value = TextPreviewUi.Loading(item)
        viewModelScope.launch {
            val content = repository.readTextFilePreview(item.file)
            // Ignore the result if the user already dismissed or opened another preview.
            if (_textPreview.value?.item?.absolutePath != item.absolutePath) return@launch
            _textPreview.value = if (content != null) {
                TextPreviewUi.Ready(item, content.text, content.truncated)
            } else {
                TextPreviewUi.Failed(item)
            }
        }
    }

    fun closeTextPreview() {
        _textPreview.value = null
    }

    fun deleteFileItem(item: FileItem) {
        viewModelScope.launch {
            // Check if folder is secure and remove secured reference from DB first
            if (item.isDirectory) {
                // Remove folder from secured paths if deleting
                repository.unsecureFolder(item.absolutePath)
                // Also remove sub-sessions
                val cleanSet = _unlockedFolderSessions.value.filter { it != item.absolutePath }.toSet()
                _unlockedFolderSessions.value = cleanSet
            }
            
            val success = repository.deleteFile(item.file)
            if (success) {
                dispatchMessage(string(R.string.msg_deleted, item.name))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_delete_failed, item.name))
            }
        }
    }

    // --- General file actions (rename / hide / copy / move) ---

    /** Roots the Copy-to / Move-to picker can browse. */
    val appStorageRoot: File get() = repository.appStorageRoot
    val deviceStorageRoot: File get() = repository.deviceStorageRoot

    suspend fun subdirectoriesOf(dir: File): List<File> = repository.listSubdirectories(dir)

    fun renameFileItem(item: FileItem, newName: String) {
        viewModelScope.launch {
            val success = repository.renameFile(item.file, newName)
            if (success) {
                dispatchMessage(string(R.string.msg_renamed, item.name, newName.trim()))
                loadFilesInDirectory(_currentDirectory.value)
            } else {
                dispatchMessage(string(R.string.msg_rename_failed, item.name))
            }
        }
    }

    /**
     * Toggles the Android "hidden" convention (a leading ".") on each item by renaming it. Items
     * whose name already starts with "." are un-hidden; the rest are hidden.
     */
    fun hideOrUnhideItems(items: List<FileItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            var hidden = 0
            var shown = 0
            items.forEach { item ->
                val isHidden = item.name.startsWith(".")
                val newName = if (isHidden) item.name.removePrefix(".") else "." + item.name
                if (newName.isNotEmpty() && repository.renameFile(item.file, newName)) {
                    if (isHidden) shown++ else hidden++
                }
            }
            when {
                hidden > 0 && shown == 0 -> dispatchMessage(string(R.string.msg_hidden, hidden))
                shown > 0 && hidden == 0 -> dispatchMessage(string(R.string.msg_unhidden, shown))
                hidden > 0 || shown > 0 -> dispatchMessage(string(R.string.msg_hidden_mixed, hidden, shown))
                else -> dispatchMessage(string(R.string.msg_hide_failed))
            }
            loadFilesInDirectory(_currentDirectory.value)
        }
    }

    fun copyItemsTo(items: List<FileItem>, destDir: File) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val ok = items.count { repository.copyFileOrFolder(it.file, destDir) }
            if (ok > 0) {
                dispatchMessage(string(R.string.msg_copied, ok, destDir.name))
            } else {
                dispatchMessage(string(R.string.msg_copy_failed))
            }
            loadFilesInDirectory(_currentDirectory.value)
            refreshStorageStats()
        }
    }

    fun moveItemsTo(items: List<FileItem>, destDir: File) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val ok = items.count { repository.moveFileOrFolder(it.file, destDir) }
            if (ok > 0) {
                dispatchMessage(string(R.string.msg_moved, ok, destDir.name))
            } else {
                dispatchMessage(string(R.string.msg_move_failed))
            }
            loadFilesInDirectory(_currentDirectory.value)
            refreshStorageStats()
        }
    }

    // --- Archiving Zip Tasks ---

    fun compressFolderOrFile(item: FileItem, zipName: String) {
        viewModelScope.launch {
            val success = repository.compressFileOrFolder(item.file, zipName)
            if (success) {
                dispatchMessage(string(R.string.msg_zip_success, item.name, zipName))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_zip_failed))
            }
        }
    }

    fun compressItems(items: List<FileItem>, zipName: String) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val success = repository.compressMultiple(items.map { it.file }, zipName)
            if (success) {
                dispatchMessage(string(R.string.msg_zip_success_multi, items.size, zipName))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_zip_failed))
            }
        }
    }

    fun decompressZip(item: FileItem) {
        viewModelScope.launch {
            val success = repository.decompressZipFile(item.file)
            if (success) {
                dispatchMessage(string(R.string.msg_extract_success, item.name))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_extract_failed))
            }
        }
    }

    // --- Secure Folder Operation ---

    fun toggleFolderShield(item: FileItem) {
        viewModelScope.launch {
            val isSecured = repository.isFolderSecured(item.absolutePath)
            if (isSecured) {
                repository.unsecureFolder(item.absolutePath)
                val cleanSet = _unlockedFolderSessions.value.filter { it != item.absolutePath }.toSet()
                _unlockedFolderSessions.value = cleanSet
                dispatchMessage(string(R.string.msg_shield_removed, item.name))
            } else {
                repository.secureFolder(item.absolutePath)
                dispatchMessage(string(R.string.msg_folder_secured, item.name))
            }
            loadFilesInDirectory(_currentDirectory.value)
        }
    }

    fun unlockSecuredFolderSession(path: String) {
        val nextSessions = _unlockedFolderSessions.value.toMutableSet()
        nextSessions.add(path)
        _unlockedFolderSessions.value = nextSessions
        loadFilesInDirectory(_currentDirectory.value)
    }

    // --- Vault Management ---

    fun secureFileInVault(item: FileItem) {
        viewModelScope.launch {
            val success = repository.moveFileToVault(item.file)
            if (success) {
                dispatchMessage(string(R.string.msg_moved_to_vault, item.name))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_move_vault_failed, item.name))
            }
        }
    }

    fun restoreFileFromVault(vaultFile: VaultFile) {
        viewModelScope.launch {
            val success = repository.restoreFileFromVault(vaultFile)
            if (success) {
                dispatchMessage(string(R.string.msg_restored, vaultFile.originalName))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_restore_failed))
            }
        }
    }

    fun deleteFileFromVault(vaultFile: VaultFile) {
        viewModelScope.launch {
            val success = repository.deleteFileFromVault(vaultFile)
            if (success) {
                dispatchMessage(string(R.string.msg_vault_deleted, vaultFile.originalName))
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_vault_delete_failed))
            }
        }
    }

    fun markVaultUnlockedState(unlocked: Boolean) {
        _isVaultUnlocked.value = unlocked
    }

    // --- Preference Handling ---

    fun updateDeletePhoneLockSetting(enabled: Boolean) {
        viewModelScope.launch {
            repository.savePhoneLockDeleteSetting(enabled)
            _phoneLockDeleteEnabled.value = enabled
            dispatchMessage(string(if (enabled) R.string.msg_delete_lock_enabled else R.string.msg_delete_lock_disabled))
        }
    }

    fun createOrUpdatePin(pin: String) {
        viewModelScope.launch {
            repository.saveCustomPin(pin)
            _customPin.value = pin
            dispatchMessage(string(R.string.msg_pin_updated))
        }
    }

    fun updateThemePreference(theme: String) {
        viewModelScope.launch {
            repository.saveThemePreference(theme)
            _themePreference.value = theme
            dispatchMessage(string(when (theme) {
                "light" -> R.string.msg_theme_light
                "dark" -> R.string.msg_theme_dark
                else -> R.string.msg_theme_system
            }))
        }
    }

    fun updateShowHiddenItemsSetting(show: Boolean) {
        viewModelScope.launch {
            repository.saveShowHiddenItems(show)
            _showHiddenItems.value = show
            loadFilesInDirectory(_currentDirectory.value)
            refreshStorageStats()
            dispatchMessage(string(if (show) R.string.msg_hidden_listed else R.string.msg_hidden_hidden))
        }
    }

    fun markStoragePermissionRequested() {
        viewModelScope.launch {
            repository.saveStoragePermissionRequested(true)
            _storagePermissionRequested.value = true
        }
    }

    fun updatePasswordProtectAppSetting(protect: Boolean) {
        viewModelScope.launch {
            repository.savePasswordProtectApp(protect)
            _passwordProtectApp.value = protect
            dispatchMessage(string(if (protect) R.string.msg_app_lock_enabled else R.string.msg_app_lock_disabled))
        }
    }

    fun updateImagePreviewEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveImagePreviewEnabled(enabled)
            _imagePreviewEnabled.value = enabled
            dispatchMessage(string(if (enabled) R.string.msg_image_preview_enabled else R.string.msg_image_preview_disabled))
        }
    }

    fun updateTextPreviewEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveTextPreviewEnabled(enabled)
            _textPreviewEnabled.value = enabled
            dispatchMessage(string(if (enabled) R.string.msg_text_preview_enabled else R.string.msg_text_preview_disabled))
        }
    }

    fun updatePasswordProtectHiddenSetting(protect: Boolean) {
        viewModelScope.launch {
            repository.savePasswordProtectHidden(protect)
            _passwordProtectHidden.value = protect
            dispatchMessage(string(if (protect) R.string.msg_hidden_lock_enabled else R.string.msg_hidden_lock_disabled))
        }
    }

    fun markHiddenUnlockedState(unlocked: Boolean) {
        _isHiddenUnlocked.value = unlocked
    }

    fun updateFileViewMode(mode: String) {
        if (_fileViewMode.value == mode) return
        _fileViewMode.value = mode
        viewModelScope.launch {
            repository.saveFileViewMode(mode)
        }
    }

    fun updateStorageSourceMode(mode: String) {
        viewModelScope.launch {
            repository.saveStorageSourceMode(mode)
            _storageSourceMode.value = mode
            // A category filter is tied to the previous source's tree; clear it so the
            // Files screen returns to normal browsing of the newly selected source.
            clearCategoryFilter()
            // Clear stale stats immediately so the loading spinner replaces the
            // previous source's tiles right away instead of showing them until
            // the (potentially slow) recursive scan of the new source finishes.
            _storageStats.value = null
            _currentDirectory.value = userStorageRoot
            loadFilesInDirectory(userStorageRoot)
            refreshStorageStats()
            dispatchMessage(string(if (mode == "device") R.string.msg_source_device else R.string.msg_source_sandbox))
        }
    }

    // Resolves a localized string (with optional format args) using the application context, so
    // user messages follow the same OS locale as the Compose UI.
    private fun string(@StringRes resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    // Helper message utility
    fun dispatchMessage(msg: String) {
        viewModelScope.launch {
            _userMessage.emit(msg)
        }
    }
}

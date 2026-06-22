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
            _customPin.value = repository.getCustomPin()
            _themePreference.value = repository.getThemePreference()
            _showHiddenItems.value = repository.isShowHiddenItems()
            _passwordProtectApp.value = repository.isPasswordProtectApp()
            _passwordProtectHidden.value = repository.isPasswordProtectHidden()

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
            _storageStats.value = repository.getStorageUsageStats(userStorageRoot)
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
            val success = repository.createNewTextFile(_currentDirectory.value, name, content)
            if (success) {
                dispatchMessage(string(R.string.msg_file_created, name))
                loadFilesInDirectory(_currentDirectory.value)
                refreshStorageStats()
            } else {
                dispatchMessage(string(R.string.msg_file_create_failed))
            }
        }
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

    fun updatePasswordProtectAppSetting(protect: Boolean) {
        viewModelScope.launch {
            repository.savePasswordProtectApp(protect)
            _passwordProtectApp.value = protect
            dispatchMessage(string(if (protect) R.string.msg_app_lock_enabled else R.string.msg_app_lock_disabled))
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

package com.example.data

import android.content.Context
import com.example.utils.ZipUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// Number of entries to accumulate before emitting a progressive directory-listing update.
private const val LISTING_CHUNK = 50
// Minimum gap between progressive folder-size emissions, to bound recomposition cost on
// directories with many subfolders. A final emission is always sent regardless.
private const val SIZE_EMIT_INTERVAL_MS = 100L

// Upper bound on how many bytes the text previewer reads from a file. Bounds memory and keeps
// the UI responsive for huge logs; anything larger is shown truncated.
private const val TEXT_PREVIEW_MAX_BYTES = 512 * 1024

// Generous allowlist of file extensions treated as previewable text. Covers plain text, markup,
// config/data, logs, and common source code. Files outside this set keep the generic toast.
private val TEXT_PREVIEW_EXTENSIONS = setOf(
    "txt", "text", "log", "md", "markdown", "csv", "tsv",
    "json", "xml", "yaml", "yml", "ini", "cfg", "conf", "properties", "toml", "env",
    "gradle", "kt", "kts", "java", "js", "ts", "jsx", "tsx", "py", "rb", "go", "rs",
    "c", "h", "cpp", "hpp", "cs", "php", "sh", "bash", "bat", "ps1", "sql",
    "html", "htm", "css", "scss", "gitignore", "gitattributes", "lst"
)

/** Result of a text-file preview read: the (possibly truncated) UTF-8 [text] and a [truncated] flag. */
data class TextPreviewContent(val text: String, val truncated: Boolean)

class StorageRepository(
    private val context: Context,
    private val settingsDao: SettingsDao,
    private val securedFolderDao: SecuredFolderDao,
    private val vaultFileDao: VaultFileDao
) {
    // Root directory for user storage (isolated to app filesDir to guarantee access)
    val userStorageRoot: File = File(context.filesDir, "Storage")
    // Safe biometric / PIN file encryption storage
    private val vaultStorageRoot: File = File(context.filesDir, "Vault")
    // Keystore-backed AES-GCM encryption for secure notes
    private val cryptoManager = CryptoManager()

    init {
        if (!userStorageRoot.exists()) {
            userStorageRoot.mkdirs()
        }
        if (!vaultStorageRoot.exists()) {
            vaultStorageRoot.mkdirs()
        }
    }

    /**
     * One-time cleanup of fabricated/seeded demo data left on existing installs.
     * Removes the virtual cache, the seeded sandbox files, and the dummy files that earlier
     * versions wrote into the user's real external folders. Real files are matched by exact
     * name + seeded byte-size so genuine user content is never removed.
     */
    suspend fun purgeFabricatedDataIfNeeded() = withContext(Dispatchers.IO) {
        val alreadyPurged = settingsDao.getSetting("fabricated_data_purged_v1")?.value?.toBoolean() ?: false
        if (alreadyPurged) return@withContext

        // 1. The virtual_external cache is pure fabricated junk — remove it entirely.
        try {
            File(context.cacheDir, "virtual_external").deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Seeded sandbox files under filesDir/Storage, matched by exact size.
        val sandboxSeed = listOf(
            Triple("Images", "vacation_mountains.jpg", 2_400_000L),
            Triple("Images", "avatar_hq.png", 850_000L),
            Triple("Videos", "graduation_clip.mp4", 15_300_000L),
            Triple("Videos", "nature_loop_4k.mp4", 42_000_000L),
            Triple("Audio", "acoustic_tune.mp3", 6_400_000L),
            Triple("Audio", "voice_meeting_record.wav", 11_100_000L),
            Triple("Documents", "yearly_tax_statement_2025.pdf", 1_600_000L),
            Triple("Documents", "grocery_list.txt", 1500L),
            Triple("Documents", "inventory_sheets.xlsx", 720_000L),
            Triple("Archives", "old_photos_backup.zip", 14_900_000L),
            Triple("Others", "app_workspace_config.json", 45_000L)
        )
        deleteSeededFiles(userStorageRoot, sandboxSeed)
        // Remove the seeded category folders only when they are now empty.
        listOf("Images", "Videos", "Audio", "Documents", "Archives", "Others").forEach { name ->
            val dir = File(userStorageRoot, name)
            if (dir.isDirectory && dir.listFiles().isNullOrEmpty()) {
                dir.delete()
            }
        }

        // 3. Dummy files written into the user's real external folders, matched by exact size.
        try {
            val externalRoot = android.os.Environment.getExternalStorageDirectory()
            val externalSeed = listOf(
                Triple("DCIM", "family_vacation_skyline_2026.jpg", 33_400_000L),
                Triple("DCIM", "camera_shot_hdr.png", 14_850_000L),
                Triple("Pictures", "scenery_gathering.jpg", 10_900_000L),
                Triple("Pictures", "minimalist_workspace.jpg", 5_200_000L),
                Triple("Pictures", "tax_receipt_scan.png", 1_450_000L),
                Triple("Download", "report_yearly_template.pdf", 4_980_000L),
                Triple("Download", "invoice_48291_rev.pdf", 1_240_000L),
                Triple("Download", "archived_assets_backup.zip", 64_400_000L),
                Triple("Documents", "personal_manifesto_notes.txt", 120_000L),
                Triple("Documents", "salary_sheet_may_jun.xlsx", 7_250_000L),
                Triple("Music", "lofi_coding_ambient.mp3", 17_200_000L),
                Triple("Music", "orchestral_symphony.mp3", 16_100_000L),
                Triple("Movies", "drone_footage_beach_4k.mp4", 118_500_000L),
                Triple("Movies", "cat_funny_moments_hd.mp4", 54_400_000L)
            )
            deleteSeededFiles(externalRoot, externalSeed)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        settingsDao.saveSetting(AppSetting("fabricated_data_purged_v1", "true"))
    }

    /**
     * Deletes each (subDir, name) under [root] only when the file exists and its length matches
     * the seeded size exactly — protecting any genuine user file that shares a name.
     */
    private fun deleteSeededFiles(root: File, entries: List<Triple<String, String, Long>>) {
        for ((subDir, name, size) in entries) {
            try {
                val f = File(File(root, subDir), name)
                if (f.isFile && f.length() == size) {
                    f.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- File Storage Listing & Actions ---

    /**
     * Maps a raw [File] to a [FileItem] with only cheap metadata: file length and a one-level
     * item count. Folder sizes are intentionally NOT computed here — a recursive subtree walk per
     * folder is the dominant cost. Directories are reported with `size = 0` and
     * `sizeComputed = false`; call [computeDirectorySizesFlow] afterwards to fill them in.
     */
    private fun mapToFileItem(file: File, securedFoldersList: List<String>): FileItem {
        val isSecured = securedFoldersList.any { securedPath ->
            file.absolutePath == securedPath || file.absolutePath.startsWith(securedPath + File.separator)
        }
        val isDir = file.isDirectory
        return FileItem(
            name = file.name,
            absolutePath = file.absolutePath,
            file = file,
            isDirectory = isDir,
            size = if (isDir) 0L else file.length(),
            isSecured = isSecured,
            category = getCategoryForFile(file),
            itemCount = if (isDir) (file.listFiles()?.size ?: 0) else 0,
            sizeComputed = !isDir
        )
    }

    /**
     * Incremental directory listing. Emits a growing list in chunks of [LISTING_CHUNK] entries
     * (plus a final emission with everything) so large directories appear progressively instead of
     * after a single pause. Folder sizes are not computed here (see [computeDirectorySizesFlow]).
     * Emissions are unsorted — callers sort for display. Cooperatively cancellable.
     */
    fun getFilesAndFoldersFlow(directory: File, showHidden: Boolean): Flow<List<FileItem>> = flow {
        val files = if (directory.exists()) directory.listFiles() else null
        if (files == null) {
            emit(emptyList())
            return@flow
        }
        val securedFoldersList = securedFolderDao.getAllSecuredFolders().map { it.path }
        val visible = files.filter { showHidden || !it.name.startsWith(".") }
        val acc = ArrayList<FileItem>(visible.size)
        var lastEmitted = 0
        for (file in visible) {
            currentCoroutineContext().ensureActive()
            acc.add(mapToFileItem(file, securedFoldersList))
            if (acc.size % LISTING_CHUNK == 0) {
                emit(acc.toList())
                lastEmitted = acc.size
            }
        }
        if (lastEmitted != acc.size || acc.isEmpty()) {
            emit(acc.toList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Recursively fills in each directory's [FileItem.size] one folder at a time, emitting an
     * updated copy of the full list after each folder resolves (throttled to at most one emission
     * per [SIZE_EMIT_INTERVAL_MS], with a guaranteed final emission). This lets small/fast folders
     * show their size immediately while large folders fill in later and independently. Each updated
     * folder is marked `sizeComputed = true`. Cooperatively cancellable between folders so fast
     * navigation aborts a slow scan promptly.
     */
    fun computeDirectorySizesFlow(items: List<FileItem>): Flow<List<FileItem>> = flow {
        val working = items.toMutableList()
        var dirtySinceEmit = false
        var lastEmit = 0L
        for (i in working.indices) {
            currentCoroutineContext().ensureActive()
            val item = working[i]
            if (item.isDirectory && !item.sizeComputed) {
                working[i] = item.copy(size = getDirectorySize(item.file), sizeComputed = true)
                dirtySinceEmit = true
                val now = System.currentTimeMillis()
                if (now - lastEmit >= SIZE_EMIT_INTERVAL_MS) {
                    emit(working.toList())
                    lastEmit = now
                    dirtySinceEmit = false
                }
            }
        }
        if (dirtySinceEmit) {
            emit(working.toList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Recursively collects every non-directory file under [root] whose category matches
     * [category] (canonical strings: "Image", "Video", "Audio", "Document", "Archive", "Other").
     * Honors the same hidden-file rule as [getFilesAndFoldersFlow] and evaluates
     * [FileItem.isSecured] against the secured-folder paths.
     */
    suspend fun getFilesByCategoryRecursive(root: File, category: String, showHidden: Boolean): List<FileItem> = withContext(Dispatchers.IO) {
        val securedFoldersList = securedFolderDao.getAllSecuredFolders().map { it.path }
        val matches = mutableListOf<FileItem>()

        fun walk(file: File) {
            try {
                if (!showHidden && file.name.startsWith(".")) return
                if (file.isDirectory) {
                    val children = file.listFiles() ?: return
                    for (child in children) {
                        walk(child)
                    }
                } else if (getCategoryForFile(file) == category) {
                    val isSecured = securedFoldersList.any { securedPath ->
                        file.absolutePath == securedPath || file.absolutePath.startsWith(securedPath + File.separator)
                    }
                    matches.add(
                        FileItem(
                            name = file.name,
                            absolutePath = file.absolutePath,
                            file = file,
                            isDirectory = false,
                            size = file.length(),
                            isSecured = isSecured,
                            category = category,
                            itemCount = 0
                        )
                    )
                }
            } catch (e: Exception) {
                // Squelch permission issues gracefully
            }
        }

        walk(root)
        return@withContext matches
    }

    private fun getDirectorySize(dir: File): Long {
        var size: Long = 0
        val files = dir.listFiles() ?: return 0
        for (file in files) {
            size += if (file.isDirectory) getDirectorySize(file) else file.length()
        }
        return size
    }

    fun getCategoryForFile(file: File): String {
        if (file.isDirectory) return "Folder"
        val ext = file.extension.lowercase()
        return when (ext) {
            // Generous image set: standard raster, modern (heic/heif/avif), vector (svg),
            // icons, and common camera RAW formats. Preview attempts to decode anything here
            // and degrades gracefully when no decoder is available (e.g. RAW/TIFF).
            "jpg", "jpeg", "jpe", "jfif", "png", "gif", "webp", "bmp", "svg",
            "heic", "heif", "avif", "ico", "tif", "tiff",
            "dng", "cr2", "nef", "arw", "orf", "rw2" -> "Image"
            "mp4", "mkv", "avi", "mov", "3gp", "webm" -> "Video"
            "mp3", "wav", "ogg", "flac", "m4a", "aac" -> "Audio"
            "pdf", "docx", "doc", "txt", "pptx", "xlsx", "epub", "csv", "json" -> "Document"
            "zip", "rar", "7z", "tar", "gz" -> "Archive"
            else -> "Other"
        }
    }

    // --- File Type Wise Usage Calculation ---

    suspend fun getStorageUsageStats(storageRoot: File): StorageStats = withContext(Dispatchers.IO) {
        var imageSize = 0L
        var videoSize = 0L
        var audioSize = 0L
        var docSize = 0L
        var archiveSize = 0L
        var otherSize = 0L
        var totalSize = 0L

        fun scanRecursive(file: File) {
            try {
                if (file.isDirectory) {
                    val files = file.listFiles() ?: return
                    for (child in files) {
                        scanRecursive(child)
                    }
                } else {
                    val size = file.length()
                    totalSize += size
                    when (getCategoryForFile(file)) {
                        "Image" -> imageSize += size
                        "Video" -> videoSize += size
                        "Audio" -> audioSize += size
                        "Document" -> docSize += size
                        "Archive" -> archiveSize += size
                        else -> otherSize += size
                    }
                }
            } catch (e: Exception) {
                // Squelch permission issues gracefully
            }
        }

        scanRecursive(storageRoot)

        // Report real filesystem capacity for the scanned root (sandbox or device).
        val stat = android.os.StatFs(storageRoot.path)
        val totalInternalMax = stat.blockCountLong * stat.blockSizeLong
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytesResult = totalInternalMax - freeBytes

        // Space used on the partition that we did not itemize is reported as "Other".
        val unscannedSpace = (usedBytesResult - totalSize).coerceAtLeast(0L)
        val finalOtherBytes = otherSize + unscannedSpace

        return@withContext StorageStats(
            imageBytes = imageSize,
            videoBytes = videoSize,
            audioBytes = audioSize,
            documentBytes = docSize,
            archiveBytes = archiveSize,
            otherBytes = finalOtherBytes,
            usedBytes = usedBytesResult,
            totalLimitBytes = totalInternalMax
        )
    }

    // --- General File Management Commands ---

    suspend fun createNewFolder(parentDir: File, name: String): Boolean = withContext(Dispatchers.IO) {
        val newFolder = File(parentDir, name)
        if (newFolder.exists()) return@withContext false
        return@withContext newFolder.mkdirs()
    }

    /**
     * Creates a secure note whose content is encrypted at rest with AES-GCM (Android Keystore).
     * The file is stored with a `.securenote` extension because its bytes are ciphertext, not text.
     */
    suspend fun createEncryptedNote(parentDir: File, name: String, textContent: String): Boolean = withContext(Dispatchers.IO) {
        var fileName = name
        if (!fileName.endsWith(".securenote")) {
            fileName += ".securenote"
        }
        val newFile = File(parentDir, fileName)
        return@withContext try {
            val encrypted = cryptoManager.encrypt(textContent.toByteArray())
            FileOutputStream(newFile).use { fos ->
                fos.write(encrypted)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads and decrypts a `.securenote` file, returning its plaintext, or `null` if the file is
     * missing, corrupt, or was sealed with a different (e.g. another device's) Keystore key.
     */
    suspend fun readEncryptedNote(file: File): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            cryptoManager.decrypt(file.readBytes()).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Re-encrypts [content] and overwrites an existing `.securenote` file in place, used when the
     * user edits a note. Returns false if the file is gone or the write fails.
     */
    suspend fun overwriteEncryptedNote(file: File, content: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val encrypted = cryptoManager.encrypt(content.toByteArray())
            FileOutputStream(file).use { fos ->
                fos.write(encrypted)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteFile(file: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    // --- Compression (Zip) Tasks ---

    suspend fun compressFileOrFolder(source: File, targetZipName: String): Boolean = withContext(Dispatchers.IO) {
        var zipName = targetZipName
        if (!zipName.endsWith(".zip")) {
            zipName += ".zip"
        }
        val destFile = File(source.parentFile ?: userStorageRoot, zipName)
        return@withContext ZipUtility.zip(source, destFile)
    }

    suspend fun decompressZipFile(zipFile: File): Boolean = withContext(Dispatchers.IO) {
        // Extract into a sub-folder matching the zip file's prefix name
        val destFolder = File(zipFile.parentFile ?: userStorageRoot, zipFile.nameWithoutExtension + "_extracted")
        if (!destFolder.exists()) {
            destFolder.mkdirs()
        }
        return@withContext ZipUtility.unzip(zipFile, destFolder)
    }

    // --- Secure Folder Shieling ---

    fun getSecuredFoldersFlow(): Flow<List<SecuredFolder>> = securedFolderDao.getAllSecuredFoldersFlow()

    suspend fun secureFolder(path: String) = withContext(Dispatchers.IO) {
        securedFolderDao.secureFolder(SecuredFolder(path))
    }

    suspend fun unsecureFolder(path: String) = withContext(Dispatchers.IO) {
        securedFolderDao.unsecureFolder(path)
    }

    suspend fun isFolderSecured(path: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext securedFolderDao.isFolderSecured(path)
    }

    // --- Sensitive SECURE VAULT Operations ---

    fun getVaultFilesFlow(): Flow<List<VaultFile>> = vaultFileDao.getAllVaultFilesFlow()

    suspend fun moveFileToVault(originalFile: File): Boolean = withContext(Dispatchers.IO) {
        if (!originalFile.exists() || originalFile.isDirectory) return@withContext false

        val vaultFileName = "vault_" + UUID.randomUUID().toString() + ".secured"
        val destinationInVault = File(vaultStorageRoot, vaultFileName)

        return@withContext try {
            // Copy file bytes securely to vault folder
            originalFile.inputStream().use { input ->
                destinationInVault.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Delete original file from local storage
            val deleted = originalFile.delete()
            if (deleted) {
                val category = getCategoryForFile(originalFile)
                val vaultFile = VaultFile(
                    originalName = originalFile.name,
                    vaultFileName = vaultFileName,
                    fileSize = originalFile.length(),
                    category = category
                )
                vaultFileDao.insertVaultFile(vaultFile)
                true
            } else {
                destinationInVault.delete() // Clean up vault file if delete failed
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFileFromVault(vaultFile: VaultFile): Boolean = withContext(Dispatchers.IO) {
        val vaultPhysicalFile = File(vaultStorageRoot, vaultFile.vaultFileName)
        if (!vaultPhysicalFile.exists()) return@withContext false

        // Place recovered files inside a "Restored" folder of standard user storage
        val restoredDirectory = File(userStorageRoot, "Restored")
        if (!restoredDirectory.exists()) {
            restoredDirectory.mkdirs()
        }

        val destinationFile = File(restoredDirectory, vaultFile.originalName)

        return@withContext try {
            vaultPhysicalFile.inputStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Clean vault file
            vaultPhysicalFile.delete()
            vaultFileDao.deleteVaultFile(vaultFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteFileFromVault(vaultFile: VaultFile): Boolean = withContext(Dispatchers.IO) {
        val vaultPhysicalFile = File(vaultStorageRoot, vaultFile.vaultFileName)
        return@withContext try {
            if (vaultPhysicalFile.exists()) {
                vaultPhysicalFile.delete()
            }
            vaultFileDao.deleteVaultFile(vaultFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- Settings Persistence Helpers ---

    fun getPhoneLockDeleteSettingFlow(): Flow<AppSetting?> = settingsDao.getSettingFlow("phone_lock_delete_enabled")

    suspend fun isPhoneLockDeleteEnabled(): Boolean = withContext(Dispatchers.IO) {
        val setting = settingsDao.getSetting("phone_lock_delete_enabled")
        return@withContext setting?.value?.toBoolean() ?: true
    }

    suspend fun savePhoneLockDeleteSetting(enabled: Boolean) = withContext(Dispatchers.IO) {
        settingsDao.saveSetting(AppSetting("phone_lock_delete_enabled", enabled.toString()))
    }

    suspend fun getCustomPin(): String? = withContext(Dispatchers.IO) {
        return@withContext settingsDao.getSetting("app_passcode_pin")?.value
    }

    suspend fun saveCustomPin(pin: String) = withContext(Dispatchers.IO) {
        settingsDao.saveSetting(AppSetting("app_passcode_pin", pin))
    }

    fun getThemePreferenceFlow(): Flow<AppSetting?> = settingsDao.getSettingFlow("theme_preference")
    suspend fun getThemePreference(): String = withContext(Dispatchers.IO) {
        return@withContext settingsDao.getSetting("theme_preference")?.value ?: "system"
    }
    suspend fun saveThemePreference(theme: String) = withContext(Dispatchers.IO) {
        settingsDao.saveSetting(AppSetting("theme_preference", theme))
    }

    fun getShowHiddenItemsFlow(): Flow<AppSetting?> = settingsDao.getSettingFlow("show_hidden_items")
    suspend fun isShowHiddenItems(): Boolean = withContext(Dispatchers.IO) {
        return@withContext settingsDao.getSetting("show_hidden_items")?.value?.toBoolean() ?: false
    }
    suspend fun saveShowHiddenItems(show: Boolean) = withContext(Dispatchers.IO) {
        settingsDao.saveSetting(AppSetting("show_hidden_items", show.toString()))
    }

    fun getPasswordProtectAppFlow(): Flow<AppSetting?> = settingsDao.getSettingFlow("password_protect_app")
    suspend fun isPasswordProtectApp(): Boolean = withContext(Dispatchers.IO) {
        return@withContext settingsDao.getSetting("password_protect_app")?.value?.toBoolean() ?: false
    }
    suspend fun savePasswordProtectApp(protect: Boolean) = withContext(Dispatchers.IO) {
        settingsDao.saveSetting(AppSetting("password_protect_app", protect.toString()))
    }

    fun getPasswordProtectHiddenFlow(): Flow<AppSetting?> = settingsDao.getSettingFlow("password_protect_hidden")
    suspend fun isPasswordProtectHidden(): Boolean = withContext(Dispatchers.IO) {
        return@withContext settingsDao.getSetting("password_protect_hidden")?.value?.toBoolean() ?: false
    }
    suspend fun savePasswordProtectHidden(protect: Boolean) = withContext(Dispatchers.IO) {
        settingsDao.saveSetting(AppSetting("password_protect_hidden", protect.toString()))
    }

    fun getImagePreviewEnabledFlow(): Flow<AppSetting?> = settingsDao.getSettingFlow("image_preview_enabled")
    suspend fun isImagePreviewEnabled(): Boolean = withContext(Dispatchers.IO) {
        return@withContext settingsDao.getSetting("image_preview_enabled")?.value?.toBoolean() ?: true
    }
    suspend fun saveImagePreviewEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        settingsDao.saveSetting(AppSetting("image_preview_enabled", enabled.toString()))
    }

    fun getTextPreviewEnabledFlow(): Flow<AppSetting?> = settingsDao.getSettingFlow("text_preview_enabled")
    suspend fun isTextPreviewEnabled(): Boolean = withContext(Dispatchers.IO) {
        return@withContext settingsDao.getSetting("text_preview_enabled")?.value?.toBoolean() ?: true
    }
    suspend fun saveTextPreviewEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        settingsDao.saveSetting(AppSetting("text_preview_enabled", enabled.toString()))
    }

    /** True if [file]'s extension is in the previewable-text allowlist (cheap, used for tap routing). */
    fun isLikelyTextFile(file: File): Boolean =
        !file.isDirectory && file.extension.lowercase() in TEXT_PREVIEW_EXTENSIONS

    /**
     * Reads up to [TEXT_PREVIEW_MAX_BYTES] from [file] and decodes it as UTF-8 for preview.
     * Returns null when the read fails or the content looks binary (contains a NUL byte in the
     * sampled bytes), so the viewer can show a graceful "can't display as text" state.
     */
    suspend fun readTextFilePreview(file: File): TextPreviewContent? = withContext(Dispatchers.IO) {
        try {
            val totalLength = file.length()
            val buffer = ByteArray(TEXT_PREVIEW_MAX_BYTES)
            val read = file.inputStream().use { input ->
                var off = 0
                while (off < buffer.size) {
                    val n = input.read(buffer, off, buffer.size - off)
                    if (n < 0) break
                    off += n
                }
                off
            }
            // Binary guard: a NUL byte in the sampled region almost certainly means non-text.
            for (i in 0 until read) {
                if (buffer[i].toInt() == 0) return@withContext null
            }
            val text = String(buffer, 0, read, Charsets.UTF_8)
            TextPreviewContent(text = text, truncated = totalLength > read.toLong())
        } catch (e: Exception) {
            null
        }
    }

    fun getStorageSourceModeFlow(): Flow<AppSetting?> = settingsDao.getSettingFlow("storage_source_mode")
    suspend fun getStorageSourceMode(): String = withContext(Dispatchers.IO) {
        return@withContext settingsDao.getSetting("storage_source_mode")?.value ?: "sandbox"
    }
    suspend fun saveStorageSourceMode(mode: String) = withContext(Dispatchers.IO) {
        settingsDao.saveSetting(AppSetting("storage_source_mode", mode))
    }

    fun getFileViewModeFlow(): Flow<AppSetting?> = settingsDao.getSettingFlow("file_view_mode")
    suspend fun getFileViewMode(): String = withContext(Dispatchers.IO) {
        return@withContext settingsDao.getSetting("file_view_mode")?.value ?: "list"
    }
    suspend fun saveFileViewMode(mode: String) = withContext(Dispatchers.IO) {
        settingsDao.saveSetting(AppSetting("file_view_mode", mode))
    }
}

// --- Data Containers ---

data class FileItem(
    val name: String,
    val absolutePath: String,
    val file: File,
    val isDirectory: Boolean,
    val size: Long,
    val isSecured: Boolean,
    val category: String,
    val itemCount: Int = 0,
    // False for a directory whose recursive size has not yet been computed (shows a spinner in the
    // listing until [StorageRepository.computeDirectorySizesFlow] fills it in). Always true for files.
    val sizeComputed: Boolean = true
)

data class StorageStats(
    val imageBytes: Long,
    val videoBytes: Long,
    val audioBytes: Long,
    val documentBytes: Long,
    val archiveBytes: Long,
    val otherBytes: Long,
    val usedBytes: Long,
    val totalLimitBytes: Long
)

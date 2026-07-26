package `in`.sreerajp.vault_files.data

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import `in`.sreerajp.vault_files.R
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException

/**
 * Exposes the app's user storage to the rest of the system through the Storage Access Framework,
 * so "Vault Files" appears as a browsable source inside the system file picker.
 *
 * Only [StorageRepository.userStorageRoot] is published. The vault and every password/biometric
 * protected folder are deliberately hidden: SAF has no way to run the app's unlock prompt, so a
 * caller reaching those paths would bypass the protection entirely. [isExposable] enforces that on
 * every entry point, not only on listings, so a guessed document id cannot reach a hidden path.
 *
 * Provider callbacks arrive on binder threads, so all work here is synchronous and never touches
 * the ViewModel or a coroutine scope.
 */
class VaultDocumentsProvider : DocumentsProvider() {

    private lateinit var appContext: Context
    private lateinit var storageRoot: File
    private lateinit var vaultRoot: File

    /** Secured-folder paths, re-read at most once per [SECURED_CACHE_MS] to keep listings cheap. */
    private var securedPaths: List<String> = emptyList()
    private var securedPathsReadAt: Long = 0L

    private val authority: String get() = "${appContext.packageName}.documents"

    override fun onCreate(): Boolean {
        appContext = context?.applicationContext ?: return false
        storageRoot = File(appContext.filesDir, "Storage")
        vaultRoot = File(appContext.filesDir, "Vault")
        if (!storageRoot.exists()) storageRoot.mkdirs()
        return true
    }

    // ---- Roots ----

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        cursor.newRow().apply {
            add(Root.COLUMN_ROOT_ID, ROOT_ID)
            add(Root.COLUMN_SUMMARY, appContext.getString(R.string.documents_root_summary))
            add(
                Root.COLUMN_FLAGS,
                Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_IS_CHILD or Root.FLAG_LOCAL_ONLY
            )
            add(Root.COLUMN_TITLE, appContext.getString(R.string.documents_root_title))
            add(Root.COLUMN_DOCUMENT_ID, toDocumentId(storageRoot))
            add(Root.COLUMN_MIME_TYPES, "*/*")
            add(Root.COLUMN_AVAILABLE_BYTES, storageRoot.usableSpace)
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        }
        return cursor
    }

    // ---- Reading ----

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val file = resolveDocumentId(documentId)
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        includeFile(cursor, file)
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val parent = resolveDocumentId(parentDocumentId)
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        parent.listFiles()?.forEach { child ->
            if (isExposable(child)) includeFile(cursor, child)
        }
        // Lets the picker refresh itself when the app changes this folder.
        cursor.setNotificationUri(
            appContext.contentResolver,
            DocumentsContract.buildChildDocumentsUri(authority, parentDocumentId)
        )
        return cursor
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = resolveDocumentId(documentId)
        if (file.isDirectory) throw FileNotFoundException("Not a file: $documentId")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): android.content.res.AssetFileDescriptor {
        val file = resolveDocumentId(documentId)
        if (!mimeTypeOf(file).startsWith("image/")) {
            throw FileNotFoundException("No thumbnail for $documentId")
        }
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return android.content.res.AssetFileDescriptor(
            pfd, 0, android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH
        )
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return try {
            val parent = resolveDocumentId(parentDocumentId).canonicalPath + File.separator
            resolveDocumentId(documentId).canonicalPath.startsWith(parent)
        } catch (e: Exception) {
            false
        }
    }

    // ---- Writing ----

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        val parent = resolveDocumentId(parentDocumentId)
        // Reject separators so a display name cannot write outside the parent folder.
        if (displayName.contains('/') || displayName.contains('\\') || displayName == "..") {
            throw FileNotFoundException("Invalid name: $displayName")
        }
        val target = uniqueChild(parent, displayName)
        val created = if (Document.MIME_TYPE_DIR == mimeType) {
            target.mkdir()
        } else {
            target.createNewFile()
        }
        if (!created) throw FileNotFoundException("Could not create $displayName")
        notifyChildrenChanged(parentDocumentId)
        return toDocumentId(target)
    }

    override fun deleteDocument(documentId: String) {
        val file = resolveDocumentId(documentId)
        val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
        if (!deleted) throw FileNotFoundException("Could not delete $documentId")
        file.parentFile?.let { notifyChildrenChanged(toDocumentId(it)) }
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val file = resolveDocumentId(documentId)
        if (displayName.contains('/') || displayName.contains('\\') || displayName == "..") {
            throw FileNotFoundException("Invalid name: $displayName")
        }
        val parent = file.parentFile ?: throw FileNotFoundException("No parent for $documentId")
        val target = File(parent, displayName)
        if (target.exists()) throw FileNotFoundException("$displayName already exists")
        if (!file.renameTo(target)) throw FileNotFoundException("Could not rename $documentId")
        notifyChildrenChanged(toDocumentId(parent))
        return toDocumentId(target)
    }

    // ---- Helpers ----

    /** Fills one document row for [file]. */
    private fun includeFile(cursor: MatrixCursor, file: File) {
        var flags = 0
        val parentWritable = file.parentFile?.canWrite() == true
        if (file.isDirectory) {
            if (file.canWrite()) flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else if (file.canWrite()) {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }
        if (parentWritable) {
            flags = flags or Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_RENAME
        }
        val mime = mimeTypeOf(file)
        if (mime.startsWith("image/")) flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL

        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, toDocumentId(file))
            add(
                Document.COLUMN_DISPLAY_NAME,
                if (file == storageRoot) appContext.getString(R.string.documents_root_title) else file.name
            )
            add(Document.COLUMN_SIZE, file.length())
            add(Document.COLUMN_MIME_TYPE, mime)
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(Document.COLUMN_FLAGS, flags)
        }
    }

    private fun mimeTypeOf(file: File): String {
        if (file.isDirectory) return Document.MIME_TYPE_DIR
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    /** Document id for [file]: the root id plus its path relative to the storage root. */
    private fun toDocumentId(file: File): String {
        val rootPath = storageRoot.absolutePath
        val path = file.absolutePath
        val relative = when {
            path == rootPath -> ""
            path.startsWith(rootPath + File.separator) -> path.substring(rootPath.length + 1)
            else -> throw IllegalArgumentException("Outside root: $path")
        }
        return "$ROOT_ID:$relative"
    }

    /**
     * Turns a document id back into a real [File], rejecting anything that escapes the storage root
     * or points at protected content.
     */
    private fun resolveDocumentId(documentId: String): File {
        val relative = documentId.substringAfter("$ROOT_ID:", missingDelimiterValue = "!")
        if (relative == "!") throw FileNotFoundException("Unknown document: $documentId")
        val file = if (relative.isEmpty()) storageRoot else File(storageRoot, relative)
        if (!isExposable(file)) throw FileNotFoundException("Unknown document: $documentId")
        if (!file.exists()) throw FileNotFoundException("No such file: $documentId")
        return file
    }

    /**
     * True when [file] may be shown over SAF: it must sit inside the storage root, and must not be
     * the vault or live in (or under) a secured folder.
     */
    private fun isExposable(file: File): Boolean {
        val canonical = try {
            file.canonicalPath
        } catch (e: Exception) {
            return false
        }
        val rootCanonical = try {
            storageRoot.canonicalPath
        } catch (e: Exception) {
            return false
        }
        if (canonical != rootCanonical && !canonical.startsWith(rootCanonical + File.separator)) {
            return false
        }
        val vaultCanonical = try {
            vaultRoot.canonicalPath
        } catch (e: Exception) {
            null
        }
        if (vaultCanonical != null &&
            (canonical == vaultCanonical || canonical.startsWith(vaultCanonical + File.separator))
        ) {
            return false
        }
        return securedFolderPaths().none { secured ->
            canonical == secured || canonical.startsWith(secured + File.separator)
        }
    }

    /** Secured-folder paths, canonicalised and briefly cached. */
    private fun securedFolderPaths(): List<String> {
        val now = System.currentTimeMillis()
        if (now - securedPathsReadAt < SECURED_CACHE_MS) return securedPaths
        securedPaths = try {
            val dao = AppDatabase.getDatabase(appContext).securedFolderDao()
            runBlocking { dao.getAllSecuredFolders() }.map { entry ->
                try {
                    File(entry.path).canonicalPath
                } catch (e: Exception) {
                    entry.path
                }
            }
        } catch (e: Exception) {
            // On any database failure, hide nothing new but keep the last known list.
            securedPaths
        }
        securedPathsReadAt = now
        return securedPaths
    }

    /** Appends " (n)" until the name is free, so a create never silently overwrites. */
    private fun uniqueChild(parent: File, displayName: String): File {
        var candidate = File(parent, displayName)
        if (!candidate.exists()) return candidate
        val base = displayName.substringBeforeLast('.', displayName)
        val ext = if (displayName.contains('.')) "." + displayName.substringAfterLast('.') else ""
        var n = 1
        while (candidate.exists()) {
            candidate = File(parent, "$base ($n)$ext")
            n++
        }
        return candidate
    }

    private fun notifyChildrenChanged(parentDocumentId: String) {
        appContext.contentResolver.notifyChange(
            DocumentsContract.buildChildDocumentsUri(authority, parentDocumentId), null
        )
    }

    companion object {
        private const val ROOT_ID = "vault_files_storage"
        private const val SECURED_CACHE_MS = 2_000L

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )

        /**
         * Tells the system picker that the app changed a folder, so an open picker reloads instead
         * of showing stale contents. Safe to call from any thread; does nothing for paths outside
         * the exposed storage root.
         */
        fun notifyDirectoryChanged(context: Context, directory: File) {
            try {
                val root = File(context.filesDir, "Storage").absolutePath
                val path = directory.absolutePath
                val relative = when {
                    path == root -> ""
                    path.startsWith(root + File.separator) -> path.substring(root.length + 1)
                    else -> return
                }
                val uri = DocumentsContract.buildChildDocumentsUri(
                    "${context.packageName}.documents", "$ROOT_ID:$relative"
                )
                context.contentResolver.notifyChange(uri, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

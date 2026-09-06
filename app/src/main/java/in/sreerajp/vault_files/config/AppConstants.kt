package `in`.sreerajp.vault_files.config

/**
 * Project-wide technical constants.
 *
 * About-screen metadata uses Pattern A (assets/config/app_config.json via AppConfig & ConfigService).
 * This file holds only non-UI technical values shared across layers
 * (database name, sandbox directory names, preview thresholds).
 *
 * Class-private implementation details (crypto aliases, SAF root IDs,
 * chunk sizes) stay in their owning classes.
 */
object AppConstants {

    // ---- Database ----

    /** Room database file name used by [AppDatabase]. */
    const val DATABASE_NAME = "vault_files_database"

    // ---- On-disk directory names (under context.filesDir) ----

    /** User-accessible private sandbox storage directory. */
    const val STORAGE_DIR_NAME = "Storage"

    /** Opaque vault store for byte-renamed secured items. */
    const val VAULT_DIR_NAME = "Vault"

    /** Default sub-directory for files restored from the vault. */
    const val RESTORED_DIR_NAME = "Restored"

    // ---- File preview thresholds ----

    /**
     * Upper bound (bytes) the text previewer reads from a file.
     * Bounds memory and keeps the UI responsive for large logs;
     * anything beyond this limit is shown truncated.
     */
    const val TEXT_PREVIEW_MAX_BYTES = 512 * 1024
}

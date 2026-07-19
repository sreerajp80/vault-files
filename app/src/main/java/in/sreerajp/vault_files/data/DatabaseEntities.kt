package `in`.sreerajp.vault_files.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "secured_folders")
data class SecuredFolder(
    @PrimaryKey val path: String,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_files")
data class VaultFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalName: String,
    val vaultFileName: String, // obfuscated filename stored in actual secure directory
    val fileSize: Long,
    val category: String, // "Image", "Video", "Audio", "Document", "Archive", "Other"
    val dateAdded: Long = System.currentTimeMillis()
)

// --- DAOs ---

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSetting?

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<AppSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSetting)
}

@Dao
interface SecuredFolderDao {
    @Query("SELECT * FROM secured_folders ORDER BY dateAdded DESC")
    fun getAllSecuredFoldersFlow(): Flow<List<SecuredFolder>>

    @Query("SELECT * FROM secured_folders")
    suspend fun getAllSecuredFolders(): List<SecuredFolder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun secureFolder(folder: SecuredFolder)

    @Query("DELETE FROM secured_folders WHERE path = :path")
    suspend fun unsecureFolder(path: String)

    @Query("SELECT EXISTS(SELECT 1 FROM secured_folders WHERE path = :path)")
    suspend fun isFolderSecured(path: String): Boolean
}

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files ORDER BY dateAdded DESC")
    fun getAllVaultFilesFlow(): Flow<List<VaultFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultFile(vaultFile: VaultFile)

    @Delete
    suspend fun deleteVaultFile(vaultFile: VaultFile)

    @Query("SELECT * FROM vault_files WHERE id = :id LIMIT 1")
    suspend fun getVaultFileById(id: Int): VaultFile?
}

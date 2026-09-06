package `in`.sreerajp.vault_files

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import `in`.sreerajp.vault_files.data.FileItem
import `in`.sreerajp.vault_files.data.StorageRepository
import `in`.sreerajp.vault_files.ui.MoveCopyRequest
import `in`.sreerajp.vault_files.ui.StorageViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MoveCopyStateTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `moveCopyRequest stores items and operation type`() {
        val dummyFile = File("/dummy/test.txt")
        val item = FileItem(
            name = "test.txt",
            absolutePath = dummyFile.absolutePath,
            file = dummyFile,
            isDirectory = false,
            size = 123L,
            isSecured = false,
            category = "Document"
        )
        val request = MoveCopyRequest(listOf(item), isMove = true)
        assertEquals(1, request.items.size)
        assertTrue(request.isMove)
        assertEquals("test.txt", request.items.first().name)

        val copyRequest = MoveCopyRequest(listOf(item), isMove = false)
        assertFalse(copyRequest.isMove)
    }

    @Test
    fun `viewModel sets and clears pendingMoveCopy correctly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = StorageViewModel(app)

        assertNull(viewModel.pendingMoveCopy.value)

        val dummyFile = File("/dummy/file.txt")
        val item = FileItem(
            name = "file.txt",
            absolutePath = dummyFile.absolutePath,
            file = dummyFile,
            isDirectory = false,
            size = 100L,
            isSecured = false,
            category = "Document"
        )
        val request = MoveCopyRequest(listOf(item), isMove = true)
        viewModel.setPendingMoveCopy(request)
        assertEquals(request, viewModel.pendingMoveCopy.value)

        viewModel.clearPendingMoveCopy()
        assertNull(viewModel.pendingMoveCopy.value)
    }

    @Test
    fun `viewModel ignores empty items for pendingMoveCopy`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = StorageViewModel(app)

        viewModel.setPendingMoveCopy(MoveCopyRequest(emptyList(), isMove = true))
        assertNull(viewModel.pendingMoveCopy.value)
    }

    @Test
    fun `repository prevents moving folder into itself or its child`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = `in`.sreerajp.vault_files.data.AppDatabase.getDatabase(app)
        val repo = StorageRepository(app, db.settingsDao(), db.securedFolderDao(), db.vaultFileDao())

        val parentDir = tempFolder.newFolder("parent")
        val childDir = File(parentDir, "child").apply { mkdirs() }

        // Attempting to move parent into parent itself
        val resultSelf = repo.moveFileOrFolder(parentDir, parentDir)
        assertFalse("Moving folder into itself must fail", resultSelf)

        // Attempting to move parent into child
        val resultChild = repo.moveFileOrFolder(parentDir, childDir)
        assertFalse("Moving folder into its own child must fail", resultChild)
    }

    @Test
    fun `repository prevents copying folder into itself or its child`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = `in`.sreerajp.vault_files.data.AppDatabase.getDatabase(app)
        val repo = StorageRepository(app, db.settingsDao(), db.securedFolderDao(), db.vaultFileDao())

        val parentDir = tempFolder.newFolder("parent_copy")
        val childDir = File(parentDir, "child_copy").apply { mkdirs() }

        val resultSelf = repo.copyFileOrFolder(parentDir, parentDir)
        assertFalse("Copying folder into itself must fail", resultSelf)

        val resultChild = repo.copyFileOrFolder(parentDir, childDir)
        assertFalse("Copying folder into its own child must fail", resultChild)
    }
}

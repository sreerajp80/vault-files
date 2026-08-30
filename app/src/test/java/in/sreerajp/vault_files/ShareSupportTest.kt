package `in`.sreerajp.vault_files

import `in`.sreerajp.vault_files.ui.mimeTypeForName
import `in`.sreerajp.vault_files.ui.shareTypeFor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Types fed to share/open intents. An APK must resolve to its real type, not the wildcard. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareSupportTest {

    @Test
    fun `apk gets the package-archive type`() {
        assertEquals("application/vnd.android.package-archive", mimeTypeForName("app-release.apk"))
    }

    @Test
    fun `extensions the platform misses come from our own table`() {
        assertEquals("application/x-7z-compressed", mimeTypeForName("backup.7z"))
        assertEquals("application/epub+zip", mimeTypeForName("book.epub"))
    }

    @Test
    fun `unknown and missing extensions fall back to the wildcard`() {
        assertEquals("*/*", mimeTypeForName("notes.xyzzy"))
        assertEquals("*/*", mimeTypeForName("README"))
    }

    @Test
    fun `a single file keeps its own type`() {
        assertEquals("application/vnd.android.package-archive", shareTypeFor(listOf("a.apk")))
    }

    @Test
    fun `files of one family share the family wildcard`() {
        assertEquals("image/*", shareTypeFor(listOf("a.heic", "b.webp")))
    }

    @Test
    fun `identical types are kept exactly`() {
        assertEquals("image/webp", shareTypeFor(listOf("a.webp", "b.webp")))
    }

    @Test
    fun `a mixed batch falls back to the wildcard`() {
        assertEquals("*/*", shareTypeFor(listOf("a.heic", "b.apk")))
        assertEquals("*/*", shareTypeFor(emptyList()))
    }
}

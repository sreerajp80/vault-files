package `in`.sreerajp.vault_files

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import `in`.sreerajp.vault_files.ui.bluetoothInitialIntents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Bluetooth's real send activity accepts only a fixed list of MIME types, so the share sheet hides
 * it for anything else. These tests stand in a fake Bluetooth activity with the same shape and
 * check that we add an explicit entry exactly when one is missing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BluetoothShareIntentTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val bluetooth = ComponentName(
        "com.android.bluetooth",
        "com.android.bluetooth.opp.BluetoothOppLauncherActivity",
    )
    private val uris = listOf(Uri.parse("content://in.sreerajp.vault_files.fileprovider/root/a.apk"))

    @Before
    fun registerFakeBluetooth() {
        val pm = Shadows.shadowOf(context.packageManager)
        pm.addActivityIfNotPresent(bluetooth)
        pm.addIntentFilterForActivity(
            bluetooth,
            IntentFilter(Intent.ACTION_SEND).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataType("image/*")
                addDataType("application/pdf")
            },
        )
    }

    private fun sendIntent(type: String) = Intent(Intent.ACTION_SEND).setType(type)

    @Test
    fun `a type bluetooth does not list gets an explicit entry`() {
        val extra = bluetoothInitialIntents(
            context,
            sendIntent("application/vnd.android.package-archive"),
            uris,
        )
        assertNotNull(extra)
        assertEquals(1, extra!!.size)
        assertEquals(bluetooth, extra[0].component)
        assertEquals("application/vnd.android.package-archive", extra[0].type)
    }

    @Test
    fun `a type bluetooth already lists gets no extra entry`() {
        assertNull(bluetoothInitialIntents(context, sendIntent("image/jpeg"), uris))
        assertNull(bluetoothInitialIntents(context, sendIntent("application/pdf"), uris))
    }
}

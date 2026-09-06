package `in`.sreerajp.vault_files

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import `in`.sreerajp.vault_files.config.AppConfig
import `in`.sreerajp.vault_files.config.ConfigService
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppConfigTest {

    @Test
    fun fallback_hasSensibleDefaults() {
        val fallback = AppConfig.fallback
        assertTrue(fallback.appName.isNotBlank())
        assertTrue(fallback.description.isNotBlank())
        assertEquals("0.0.0", fallback.version)
        assertEquals("0", fallback.build)
        assertTrue(fallback.details.containsKey("License"))
    }

    @Test
    fun fromJson_validJson_parsesCorrectly() {
        val jsonString = """
            {
                "appName": "Test Vault",
                "description": "Test description",
                "version": "1.2.3",
                "build": "42",
                "details": {
                    "Author": "Tester",
                    "License": "Open Source"
                }
            }
        """.trimIndent()
        val json = JSONObject(jsonString)
        val config = AppConfig.fromJson(json)

        assertEquals("Test Vault", config.appName)
        assertEquals("Test description", config.description)
        assertEquals("1.2.3", config.version)
        assertEquals("42", config.build)
        assertEquals("Tester", config.details["Author"])
        assertEquals("Open Source", config.details["License"])
    }

    @Test
    fun fromJson_malformedOrEmptyJson_usesFallbacks() {
        val json = JSONObject("{}")
        val config = AppConfig.fromJson(json)

        assertEquals(AppConfig.fallback.appName, config.appName)
        assertEquals(AppConfig.fallback.description, config.description)
        assertEquals(AppConfig.fallback.version, config.version)
        assertEquals(AppConfig.fallback.build, config.build)
        assertEquals(AppConfig.fallback.details, config.details)
    }

    @Test
    fun configService_loadsAssetConfig() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = ConfigService.load(context)

        assertNotNull(config)
        assertEquals("Vault Files", config.appName)
        assertEquals("17.3", config.version)
        assertEquals("18", config.build)
        assertTrue(config.details.containsKey("Author"))
        assertEquals("Sreeraj P", config.details["Author"])
    }

    @Test
    fun configService_loadAndVerify_succeeds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = ConfigService.loadAndVerify(context)

        assertNotNull(config)
        assertEquals("17.3", config.version)
    }
}

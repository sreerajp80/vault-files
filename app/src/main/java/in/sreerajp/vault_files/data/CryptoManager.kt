package `in`.sreerajp.vault_files.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Real at-rest encryption for secure notes, backed by the Android Keystore.
 *
 * A single AES-256 key lives inside the `AndroidKeyStore` and is never exportable (it is
 * hardware-backed on devices that support it). Content is sealed with AES/GCM/NoPadding, which
 * provides both confidentiality and integrity. The randomly generated IV is prepended to the
 * ciphertext so the same key can decrypt it later.
 *
 * On-disk layout of an encrypted blob: [1 byte: IV length][IV bytes][ciphertext + GCM tag].
 */
class CryptoManager {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getOrCreateSecretKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** Encrypts [plain] and returns a self-contained blob (IV prepended). */
    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        return ByteArray(1 + iv.size + ciphertext.size).also { out ->
            out[0] = iv.size.toByte()
            System.arraycopy(iv, 0, out, 1, iv.size)
            System.arraycopy(ciphertext, 0, out, 1 + iv.size, ciphertext.size)
        }
    }

    /** Reverses [encrypt], returning the original plaintext bytes. */
    fun decrypt(blob: ByteArray): ByteArray {
        val ivSize = blob[0].toInt()
        val iv = blob.copyOfRange(1, 1 + ivSize)
        val ciphertext = blob.copyOfRange(1 + ivSize, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "vault_secure_note_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}

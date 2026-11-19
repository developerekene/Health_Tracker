package com.devekene.healthtracker.data.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DatabaseKeyManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "health_tracker_sqlcipher_master_key"
    private const val PREFS_NAME = "health_tracker_key_vault"
    private const val KEY_WRAPPED_PASSPHRASE = "wrapped_passphrase"
    private const val KEY_IV = "wrapped_passphrase_iv"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PASSPHRASE_LENGTH_BYTES = 32

    @Volatile
    private var cachedPassphrase: ByteArray? = null

    fun isRobolectricEnvironment(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }

    @Synchronized
    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        cachedPassphrase?.let { return it.copyOf() }

        if (isRobolectricEnvironment()) {
            val testKey = ByteArray(PASSPHRASE_LENGTH_BYTES) { index -> (index + 1).toByte() }
            cachedPassphrase = testKey
            return testKey.copyOf()
        }

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encodedCiphertext = prefs.getString(KEY_WRAPPED_PASSPHRASE, null)
        val encodedIv = prefs.getString(KEY_IV, null)

        val secretKey = getOrCreateMasterKey()

        val passphrase = if (encodedCiphertext != null && encodedIv != null) {
            try {
                val ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP)
                val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
                val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
                cipher.doFinal(ciphertext)
            } catch (_: Exception) {
                generateAndPersistPassphrase(prefs, secretKey)
            }
        } else {
            generateAndPersistPassphrase(prefs, secretKey)
        }

        cachedPassphrase = passphrase
        return passphrase.copyOf()
    }

    private fun generateAndPersistPassphrase(
        prefs: android.content.SharedPreferences,
        secretKey: SecretKey
    ): ByteArray {
        val rawPassphrase = ByteArray(PASSPHRASE_LENGTH_BYTES)
        SecureRandom().nextBytes(rawPassphrase)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val ciphertext = cipher.doFinal(rawPassphrase)
        val iv = cipher.iv

        prefs.edit()
            .putString(KEY_WRAPPED_PASSPHRASE, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()

        return rawPassphrase
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}

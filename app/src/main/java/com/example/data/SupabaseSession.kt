
package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SessionStorage {
    fun saveSession(accessToken: String, refreshToken: String, userId: String? = null)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getUserId(): String?
    fun clearSession()
}

class SupabaseSession(context: Context) : SessionStorage {

    private val prefs = context.applicationContext
        .getSharedPreferences("supabase_session", Context.MODE_PRIVATE)

    private val keyAlias = "supabase_session_key"

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val USER_ID = "user_id"
    }

    override fun saveSession(accessToken: String, refreshToken: String, userId: String?) {
        val editor = prefs.edit()
            .putString(ACCESS_TOKEN, encrypt(accessToken))
            .putString(REFRESH_TOKEN, encrypt(refreshToken))
        if (!userId.isNullOrBlank()) {
            editor.putString(USER_ID, encrypt(userId))
        } else {
            editor.remove(USER_ID)
        }
        editor.apply()
    }

    override fun getAccessToken(): String? {
        return readEncrypted(ACCESS_TOKEN)
    }

    override fun getRefreshToken(): String? {
        return readEncrypted(REFRESH_TOKEN)
    }

    override fun getUserId(): String? {
        return readEncrypted(USER_ID)
    }

    private fun readEncrypted(key: String): String? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            decrypt(raw)
        } catch (_: Exception) {
            clearSession()
            null
        }
    }

    override fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val existingKey = keyStore.getKey(keyAlias, null)
        if (existingKey is SecretKey) {
            return existingKey
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(true)
                .build()
        )

        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

        val encrypted = cipher.doFinal(
            value.toByteArray(Charsets.UTF_8)
        )

        val iv = cipher.iv
        val combined = iv + encrypted

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)

        require(combined.size > 12) {
            "Invalid encrypted session data"
        }

        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.DECRYPT_MODE,
            getSecretKey(),
            GCMParameterSpec(128, iv)
        )

        return String(
            cipher.doFinal(encrypted),
            Charsets.UTF_8
        )
    }
}

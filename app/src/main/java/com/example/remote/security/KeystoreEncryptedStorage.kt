package com.example.remote.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreEncryptedStorage(context: Context) {

    private val TAG = "KeystoreEncryptedStorage"
    private val PREFS_NAME = "remote_secure_prefs"
    private val KEY_ALIAS = "RemoteControlMasterKey"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        getOrCreateMasterKey()
    }

    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

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

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            "$ivBase64:$cipherBase64"
        } catch (e: Exception) {
            Log.e(TAG, "Encryption error: ${e.message}", e)
            ""
        }
    }

    fun decrypt(encryptedPayload: String): String {
        if (encryptedPayload.isEmpty() || !encryptedPayload.contains(":")) return ""
        return try {
            val parts = encryptedPayload.split(":")
            if (parts.size != 2) return ""

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val secretKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error: ${e.message}", e)
            ""
        }
    }

    // Clean stable identifier for PC
    private fun cleanKey(identifier: String): String {
        return identifier
            .removePrefix("NexaRemote-")
            .removePrefix("Remote-")
            .removeSuffix("._remote._tcp.local.")
            .removeSuffix("._remote._tcp.local")
            .removeSuffix(".local")
            .trim()
            .uppercase()
    }

    // Get or create unique random device ID (stored safely)
    fun getDeviceId(): String {
        var deviceId = prefs.getString("device_id", null)
        if (deviceId.isNullOrBlank()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", deviceId).apply()
        }
        return deviceId
    }

    // Save encrypted auth token for PC (keyed by stable PC Name and optionally mapped IP)
    fun saveAuthToken(pcName: String, token: String, ipAddress: String? = null) {
        if (token.isBlank()) return
        val encryptedToken = encrypt(token)

        val cleanName = cleanKey(pcName)
        if (cleanName.isNotEmpty()) {
            prefs.edit().putString("token_$cleanName", encryptedToken).apply()
        }

        if (!ipAddress.isNullOrBlank()) {
            val cleanIp = ipAddress.trim()
            prefs.edit().putString("token_$cleanIp", encryptedToken).apply()
            if (cleanName.isNotEmpty()) {
                prefs.edit().putString("ip_$cleanName", cleanIp).apply()
                prefs.edit().putString("name_$cleanIp", cleanName).apply()
            }
        }
    }

    // Get decrypted auth token for PC by Name or IP
    fun getAuthToken(pcNameOrIp: String): String {
        val clean = cleanKey(pcNameOrIp)
        if (clean.isEmpty()) return ""

        // 1. Try directly by clean name
        var encryptedToken = prefs.getString("token_$clean", "") ?: ""
        if (encryptedToken.isNotEmpty()) {
            return decrypt(encryptedToken)
        }

        // 2. Try by original input
        val originalTrimmed = pcNameOrIp.trim()
        encryptedToken = prefs.getString("token_$originalTrimmed", "") ?: ""
        if (encryptedToken.isNotEmpty()) {
            return decrypt(encryptedToken)
        }

        // 3. If input is an IP, check if mapped to a PC name
        val mappedName = prefs.getString("name_$originalTrimmed", "") ?: ""
        if (mappedName.isNotEmpty()) {
            encryptedToken = prefs.getString("token_$mappedName", "") ?: ""
            if (encryptedToken.isNotEmpty()) {
                return decrypt(encryptedToken)
            }
        }

        // 4. If input is a PC name, check mapped IP
        val mappedIp = prefs.getString("ip_$clean", "") ?: ""
        if (mappedIp.isNotEmpty()) {
            encryptedToken = prefs.getString("token_$mappedIp", "") ?: ""
            if (encryptedToken.isNotEmpty()) {
                return decrypt(encryptedToken)
            }
        }

        return ""
    }

    // Remove auth token on unpair
    fun removeAuthToken(pcNameOrIp: String) {
        val clean = cleanKey(pcNameOrIp)
        val originalTrimmed = pcNameOrIp.trim()

        val mappedIp = prefs.getString("ip_$clean", "") ?: ""
        val mappedName = prefs.getString("name_$originalTrimmed", "") ?: ""

        val editor = prefs.edit()
        if (clean.isNotEmpty()) editor.remove("token_$clean").remove("ip_$clean")
        if (originalTrimmed.isNotEmpty()) editor.remove("token_$originalTrimmed").remove("name_$originalTrimmed")
        if (mappedIp.isNotEmpty()) editor.remove("token_$mappedIp").remove("name_$mappedIp")
        if (mappedName.isNotEmpty()) editor.remove("token_$mappedName").remove("ip_$mappedName")
        editor.apply()
    }

    fun isPaired(pcNameOrIp: String): Boolean {
        return getAuthToken(pcNameOrIp).isNotEmpty()
    }
}

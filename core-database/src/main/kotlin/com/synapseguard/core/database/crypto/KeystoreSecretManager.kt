package com.synapseguard.core.database.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.ProviderException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Abstracción del proveedor de cifrado envolvente (Envelope Encryption) respaldado por hardware.
 */
interface MasterKeyCipher {
    fun encrypt(plaintext: ByteArray): EncryptedPayload
    fun decrypt(payload: EncryptedPayload): ByteArray
    val isStrongBoxBacked: Boolean
}

/**
 * Carga útil autenticada con AES-256-GCM (IV de 12 bytes + Ciphertext con etiqueta GCM de 128 bits).
 */
data class EncryptedPayload(
    val iv: ByteArray,
    val ciphertextWithTag: ByteArray
) {
    fun toBinaryBlob(): ByteArray {
        require(iv.size in 12..16) { "Tamaño de IV GCM inválido: ${iv.size}" }
        require(ciphertextWithTag.size >= 16) { "Ciphertext GCM inválido." }
        val buffer = ByteBuffer.allocate(1 + iv.size + ciphertextWithTag.size)
        buffer.put(iv.size.toByte())
        buffer.put(iv)
        buffer.put(ciphertextWithTag)
        return buffer.array()
    }

    companion object {
        fun fromBinaryBlob(blob: ByteArray): EncryptedPayload {
            require(blob.size > 1 + 12 + 16) { "Archivo de clave cifrada corrupto o truncado." }
            val buffer = ByteBuffer.wrap(blob)
            val ivLength = buffer.get().toInt() and 0xFF
            require(ivLength in 12..16 && blob.size >= 1 + ivLength + 16) {
                "Encabezado de IV inválido en blob criptográfico: $ivLength"
            }
            val iv = ByteArray(ivLength)
            buffer.get(iv)
            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)
            return EncryptedPayload(iv = iv, ciphertextWithTag = ciphertext)
        }
    }
}

/**
 * Implementación de producción sobre AndroidKeyStore con soporte prioritario para chip dedicado
 * StrongBox (Keymaster/KeyMint Hardware Security Module) y fallback automático a TEE.
 */
class AndroidKeystoreMasterKeyCipher(
    private val context: Context,
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) : MasterKeyCipher {

    @Volatile
    private var strongBoxActive: Boolean = false

    override val isStrongBoxBacked: Boolean
        get() = strongBoxActive

    override fun encrypt(plaintext: ByteArray): EncryptedPayload {
        val secretKey = getOrCreateHardwareKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(iv = cipher.iv, ciphertextWithTag = ciphertext)
    }

    override fun decrypt(payload: EncryptedPayload): ByteArray {
        val secretKey = getOrCreateHardwareKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(payload.ciphertextWithTag)
    }

    @Synchronized
    private fun getOrCreateHardwareKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingEntry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }

        val hasStrongBoxFeature = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

        if (hasStrongBoxFeature) {
            try {
                val key = generateAesGcmKey(useStrongBox = true)
                strongBoxActive = true
                return key
            } catch (_: StrongBoxUnavailableException) {
                // Fallback transparente a TEE (Trusted Execution Environment)
            } catch (_: ProviderException) {
                // Fallback transparente a TEE en dispositivos con implementación StrongBox parcial
            }
        }

        strongBoxActive = false
        return generateAesGcmKey(useStrongBox = false)
    }

    private fun generateAesGcmKey(useStrongBox: Boolean): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useStrongBox) {
            builder.setIsStrongBoxBacked(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val DEFAULT_KEY_ALIAS = "synapseguard_sqlcipher_master_key_v1"
        private const val AES_KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}

/**
 * Gestor de secretos locales para SQLCipher.
 * Genera una passphrase aleatoria de 32 bytes (256 bits) en el primer arranque, la cifra con
 * la llave maestra del AndroidKeyStore (StrongBox/TEE) y la persiste en almacenamiento privado.
 */
class KeystoreSecretManager(
    private val keyBlobFile: File,
    private val masterKeyCipher: MasterKeyCipher,
    private val secureRandom: SecureRandom = SecureRandom()
) {

    /**
     * Obtiene la passphrase de 32 bytes para abrir SQLCipher, creándola y cifrándola si no existe.
     */
    @Synchronized
    fun getOrCreateDatabasePassphrase(): ByteArray {
        if (keyBlobFile.exists() && keyBlobFile.length() > 0L) {
            val encryptedBlob = keyBlobFile.readBytes()
            val payload = EncryptedPayload.fromBinaryBlob(encryptedBlob)
            val decrypted = masterKeyCipher.decrypt(payload)
            require(decrypted.size == PASSPHRASE_LENGTH_BYTES) {
                "Longitud de passphrase descifrada inválida: ${decrypted.size}"
            }
            return decrypted
        }

        val newPassphrase = ByteArray(PASSPHRASE_LENGTH_BYTES)
        secureRandom.nextBytes(newPassphrase)

        val encryptedPayload = masterKeyCipher.encrypt(newPassphrase)
        keyBlobFile.parentFile?.mkdirs()
        keyBlobFile.writeBytes(encryptedPayload.toBinaryBlob())

        return newPassphrase
    }

    companion object {
        const val PASSPHRASE_LENGTH_BYTES: Int = 32
        const val DEFAULT_KEY_FILE_NAME: String = "synapse_sqlcipher_key.bin"

        /** Limpia de forma segura un arreglo de bytes sensibles en memoria RAM. */
        fun zeroOut(secretBytes: ByteArray?) {
            secretBytes?.fill(0)
        }
    }
}


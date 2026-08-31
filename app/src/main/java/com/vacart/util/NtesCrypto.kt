package com.vacart.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Cryptographic utilities for the NTES (National Train Enquiry System) mobile API.
 *
 * All constants are extracted from `cris.icms.ntes.BuildConfig` (verified against
 * the decompiled NTES APK v27.0):
 *
 *   BuildConfig.A  = "645fbc1e56e23365f2f3c204ae0899f6"  → Salt for payload MD5 signature
 *   BuildConfig.B  = "8EA4DB2CC1EB3DC5"                  → AES-128-CBC Key
 *   BuildConfig.C  = "7DC5EB3BB4DB6EA8"                  → AES-128-CBC IV
 *   BuildConfig.D  = "EA3541BC74345DDA"                  → Salt for meta security header
 */
object NtesCrypto {

    private const val AES_KEY   = "8EA4DB2CC1EB3DC5"
    private const val AES_IV    = "7DC5EB3BB4DB6EA8"
    private const val SALT_PAYLOAD = "645fbc1e56e23365f2f3c204ae0899f6"
    private const val SALT_META    = "EA3541BC74345DDA"

    private val HEX_CHARS = "0123456789ABCDEF"

    // ── Meta header ──────────────────────────────────────────────────────────

    /**
     * Generates the one-time security header key-value pair.
     *
     * Header key  : `"meta" + <16 random uppercase hex chars>`
     * Header value: `MD5(<randomHex> + SALT_META).toUpperCase()`
     */
    fun generateMetaHeader(): Pair<String, String> {
        val random16 = buildString {
            while (length < 16) append(Integer.toHexString(Random.nextInt()))
        }.uppercase().substring(0, 16)

        val headerKey   = "meta$random16"
        val headerValue = md5((random16 + SALT_META)).uppercase()
        return Pair(headerKey, headerValue)
    }

    // ── Request payload ───────────────────────────────────────────────────────

    /**
     * Constructs the `jsonIn` value for the POST body from a raw query string.
     *
     * Format: `MD5(query + SALT_PAYLOAD).toUpperCase() + "#" + hexEncode(base64(aesEncrypt(query)))`
     */
    fun encryptPayload(queryString: String): String {
        val q = queryString.trim()
        val signature = md5(q + SALT_PAYLOAD).uppercase()
        val hexEncrypted = aesEncryptToHex(q)
        return "$signature#$hexEncrypted"
    }

    // ── Response decryption ───────────────────────────────────────────────────

    /**
     * Decrypts the `jsonIn` hex-string from the server response back to a plain JSON string.
     *
     * Steps (mirrors `Encuiry.decrypt` in decompiled source):
     *   1. Hex → Base64 ASCII bytes
     *   2. Base64 decode → AES ciphertext bytes
     *   3. AES-128-CBC decrypt → plaintext UTF-8 JSON
     */
    fun decryptResponse(hexStr: String): String {
        val b64Bytes  = hexStr.fromHex()           // hex → raw bytes (the Base64 chars)
        val cipherBytes = Base64.decode(b64Bytes, Base64.DEFAULT)
        return aesDecrypt(cipherBytes)
    }

    // ── Internal AES helpers ──────────────────────────────────────────────────

    private fun aesEncryptToHex(plaintext: String): String {
        val key  = SecretKeySpec(AES_KEY.toByteArray(Charsets.UTF_8), "AES")
        val iv   = IvParameterSpec(AES_IV.toByteArray(Charsets.UTF_8))
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Base64 encode the cipher bytes → ASCII string
        val b64String = Base64.encodeToString(encrypted, Base64.NO_WRAP)

        // Hex-encode each ASCII char (2 hex digits per char) → uppercase
        return b64String.toByteArray(Charsets.US_ASCII).toHex().uppercase()
    }

    private fun aesDecrypt(cipherBytes: ByteArray): String {
        val key  = SecretKeySpec(AES_KEY.toByteArray(Charsets.UTF_8), "AES")
        val iv   = IvParameterSpec(AES_IV.toByteArray(Charsets.UTF_8))
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }

    // ── Hex / MD5 utilities ───────────────────────────────────────────────────

    private fun ByteArray.toHex(): String = buildString {
        for (b in this@toHex) {
            append(HEX_CHARS[(b.toInt() shr 4) and 0xF])
            append(HEX_CHARS[b.toInt() and 0xF])
        }
    }

    private fun String.fromHex(): ByteArray {
        require(length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(length / 2) { i ->
            ((Character.digit(this[i * 2], 16) shl 4) + Character.digit(this[i * 2 + 1], 16)).toByte()
        }
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes  = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.toHex()
    }
}

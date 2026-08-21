package com.example.util

import java.security.MessageDigest

object SecurityManager {

    /**
     * Compute SHA-256 hash of a string (e.g. for PIN hashing or text verification)
     */
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute SHA-256 hash of a byte array (for media file deduplication)
     */
    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify PIN against stored hash
     */
    fun verifyPin(enteredPin: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrEmpty()) return true
        return sha256(enteredPin) == storedHash
    }
}

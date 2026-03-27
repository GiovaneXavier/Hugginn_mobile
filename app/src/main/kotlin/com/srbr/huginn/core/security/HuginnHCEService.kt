package com.srbr.huginn.core.security

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class HuginnHCEService : HostApduService() {

    companion object {
        private const val TAG = "HuginnHCE"

        private val SW_OK          = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_NOT_ALLOWED = byteArrayOf(0x69.toByte(), 0x00)
        private val SW_UNKNOWN     = byteArrayOf(0x6F.toByte(), 0x00)
        private val SELECT_HEADER  = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)

        @JvmStatic @Volatile var isAuthorized:    Boolean     = false
        @JvmStatic @Volatile var authorizedUntil: Long        = 0L
        @JvmStatic @Volatile var activeCard:      HuginnCard? = null
        @JvmStatic @Volatile var deviceId:        String      = ""
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (!commandApdu.startsWith(SELECT_HEADER)) return SW_UNKNOWN

        val now = System.currentTimeMillis()
        if (!isAuthorized || now > authorizedUntil || activeCard == null || deviceId.isEmpty()) {
            Log.d(TAG, "Not authorized")
            isAuthorized = false
            return SW_NOT_ALLOWED
        }

        return try {
            val token = NfcTokenGenerator().generate(activeCard!!, deviceId)
            Log.d(TAG, "Sending signed token")
            token.toByteArray(Charsets.UTF_8) + SW_OK
        } catch (e: Exception) {
            Log.e(TAG, "Token error: ${e.message}")
            SW_UNKNOWN
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "HCE deactivated: $reason")
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }
}

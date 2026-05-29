package com.srbr.huginn.core.security

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.srbr.huginn.credential.security.HuginnCard

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
        @JvmStatic @Volatile var activeDeviceId:  String      = ""
    }

    private val tokenGenerator = NfcTokenGenerator()

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (!commandApdu.startsWith(SELECT_HEADER)) return SW_UNKNOWN

        val now = System.currentTimeMillis()
        val currentCard = activeCard
        val devId = activeDeviceId

        if (!isAuthorized || now > authorizedUntil || currentCard == null || devId == "") {
            Log.d(TAG, "Not authorized or deviceId empty")
            isAuthorized = false
            return SW_NOT_ALLOWED
        }

        return try {
            val token = tokenGenerator.generate(currentCard, devId)
            Log.d(TAG, "Sending signed token")
            token.toByteArray(Charsets.UTF_8) + SW_OK
        } catch (e: Exception) {
            Log.e(TAG, "Token error: ${e.message}")
            SW_UNKNOWN
        }
    }

    override fun onDeactivated(reason: Int) {
        // reason 0 = DEACTIVATION_LINK_LOSS  (field removed)
        // reason 1 = DEACTIVATION_DESELECTED (reader selected different AID)
        // reason 2 = DEACTIVATION_OBSERVE_MODE (API 34+) — another service taking priority;
        //            re-activation may follow in the same session, keep authorization intact
        Log.d(TAG, "HCE deactivated: reason=$reason${if (reason == 2) " (observe mode)" else ""}")
    }

    override fun onDestroy() {
        isAuthorized = false
        authorizedUntil = 0L
        activeCard = null
        activeDeviceId = ""
        super.onDestroy()
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) {
            if (this[i] != prefix[i]) return false
        }
        return true
    }
}

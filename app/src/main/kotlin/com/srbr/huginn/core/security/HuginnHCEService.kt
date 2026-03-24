package com.srbr.huginn.core.security

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.srbr.huginn.BuildConfig
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HuginnHCEService : HostApduService() {

    companion object {
        private const val TAG = "HuginnHCE"

        private val SW_OK          = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_NOT_ALLOWED = byteArrayOf(0x69.toByte(), 0x00)
        private val SW_UNKNOWN     = byteArrayOf(0x6F.toByte(), 0x00)
        private val SELECT_HEADER  = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)

        @JvmStatic var isAuthorized:    Boolean     = false
        @JvmStatic var authorizedUntil: Long        = 0L
        @JvmStatic var activeCard:      HuginnCard? = null
        @JvmStatic var deviceId:        String      = ""
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
            val token = buildToken(activeCard!!, deviceId)
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

    private fun buildToken(card: HuginnCard, devId: String): String {
        val ts    = System.currentTimeMillis() / 1000L
        val nonce = (Math.random() * 999999).toLong()
        val data  = "$devId|${card.employeeId}|${card.systemId}|$ts|$nonce"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(BuildConfig.TOKEN_HMAC_KEY.toByteArray(), "HmacSHA256"))
        val sig = Base64.encodeToString(
            mac.doFinal(data.toByteArray()),
            Base64.NO_WRAP or Base64.URL_SAFE
        )
        return "$data.$sig"
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }
}

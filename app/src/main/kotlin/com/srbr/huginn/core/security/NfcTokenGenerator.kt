package com.srbr.huginn.core.security

import android.util.Base64
import com.srbr.huginn.BuildConfig
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gera tokens assinados com HMAC-SHA256 para transmissão via NFC/HCE.
 *
 * O formato do token é idêntico ao utilizado pelo QrTokenGenerator no app QR:
 *   deviceId|employeeId|systemId|timestamp|nonce.assinatura
 *
 * Isso garante que o backend Heimdall valide tokens NFC e QR
 * com exatamente a mesma lógica, sem alteração no servidor.
 *
 * A chave é lida diretamente de BuildConfig pois HuginnHCEService não é
 * injetado por Hilt (HostApduService tem limitações com Hilt).
 */
@Singleton
class NfcTokenGenerator @Inject constructor() {

    fun generate(card: HuginnCard, deviceId: String): String {
        val ts    = System.currentTimeMillis() / 1000L
        val nonce = SecureRandom().nextLong() and 0xFFFFFFL
        val data  = "$deviceId|${card.employeeId}|${card.systemId}|$ts|$nonce"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(BuildConfig.TOKEN_HMAC_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val sig = Base64.encodeToString(
            mac.doFinal(data.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
        return "$data.$sig"
    }
}

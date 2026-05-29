package com.srbr.huginn.core.security

import com.srbr.huginn.BuildConfig
import com.srbr.huginn.credential.security.HmacUtils
import com.srbr.huginn.credential.security.HuginnCard
import com.srbr.huginn.credential.security.NonceGenerator
import com.srbr.huginn.credential.security.TokenPayload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gera tokens assinados com HMAC-SHA256 para transmissão via NFC/HCE.
 *
 * Formato (idêntico ao [QrTokenGenerator] do app QR):
 *
 *     deviceId|employeeId|systemId|timestamp|nonce.assinatura
 *
 * A montagem do payload, a geração do nonce e a assinatura HMAC vêm do módulo
 * `:core-credential` ([TokenPayload], [NonceGenerator], [HmacUtils]) — qualquer
 * mudança no contrato passa a ser feita num só lugar para os dois apps.
 *
 * A chave é lida de [BuildConfig] porque [HuginnHCEService] instancia este gerador
 * fora do grafo do Hilt (limitação do `HostApduService`).
 */
@Singleton
class NfcTokenGenerator @Inject constructor() {

    private val nonceGenerator = NonceGenerator()

    fun generate(card: HuginnCard, deviceId: String): String {
        val payload = TokenPayload(
            deviceId     = deviceId,
            employeeId   = card.employeeId,
            systemId     = card.systemId,
            timestampSec = System.currentTimeMillis() / 1000L,
            nonce        = nonceGenerator.generate()
        )
        val canonical = payload.canonical()
        val signature = HmacUtils.sign(canonical, BuildConfig.TOKEN_HMAC_KEY)
        return "$canonical.$signature"
    }
}

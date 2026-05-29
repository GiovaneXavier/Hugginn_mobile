package com.srbr.huginn.core.security

import com.srbr.huginn.credential.security.HuginnCard
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NfcTokenGeneratorTest {

    private val fakeCard = HuginnCard(
        employeeId   = "EMP001",
        employeeName = "Test User",
        employeeArea = "Engineering",
        employeeRole = null,
        systemId     = "SYS001",
        systemName   = "Sistema Teste",
        cardColor    = "#1428A0",
        registeredAt = 1700000000L,
        nonce        = "onboarding-nonce"
    )

    private val deviceId = "SRBR-ABCD-1234"

    @Before
    fun setup() {
        // NfcTokenGenerator usa BuildConfig.TOKEN_HMAC_KEY diretamente.
        // Em testes, o BuildConfig.TOKEN_HMAC_KEY usa o valor padrão do build.
    }

    @Test fun `generate produces correct token format`() {
        val generator = NfcTokenGenerator()
        val token = generator.generate(fakeCard, deviceId)

        // Formato: deviceId|empId|sysId|ts|nonce.signature
        val parts = token.split(".")
        assertEquals("Token must have exactly 2 parts (data.sig)", 2, parts.size)

        val dataParts = parts[0].split("|")
        assertEquals("Data must have 5 pipe-separated fields", 5, dataParts.size)
        assertEquals(deviceId, dataParts[0])
        assertEquals(fakeCard.employeeId, dataParts[1])
        assertEquals(fakeCard.systemId, dataParts[2])
    }

    @Test fun `generate signature is 43 chars (SHA-256 base64url no padding)`() {
        val generator = NfcTokenGenerator()
        val token = generator.generate(fakeCard, deviceId)
        val sig = token.substringAfterLast('.')
        assertEquals(43, sig.length)
        assertFalse("Sig must not contain '='", sig.contains('='))
        assertFalse("Sig must not contain '+'", sig.contains('+'))
        assertFalse("Sig must not contain '/'", sig.contains('/'))
    }

    @Test fun `generate produces different tokens on each call (unique nonce)`() {
        val generator = NfcTokenGenerator()
        val t1 = generator.generate(fakeCard, deviceId)
        val t2 = generator.generate(fakeCard, deviceId)
        assertNotEquals("Each token must be unique due to nonce", t1, t2)
    }

    @Test fun `generate timestamp is close to current time`() {
        val generator = NfcTokenGenerator()
        val before = System.currentTimeMillis() / 1000L
        val token  = generator.generate(fakeCard, deviceId)
        val after  = System.currentTimeMillis() / 1000L

        val ts = token.substringBefore('.').split("|")[3].toLong()
        assertTrue("Timestamp must be >= before", ts >= before)
        assertTrue("Timestamp must be <= after", ts <= after)
    }
}

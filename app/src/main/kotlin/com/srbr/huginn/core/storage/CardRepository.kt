package com.srbr.huginn.core.storage

import com.srbr.huginn.core.security.HuginnCard
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer — sits between ViewModels and CardStorage.
 * This abstraction makes ViewModels fully unit-testable
 * by allowing CardRepository to be mocked in tests.
 */
@Singleton
class CardRepository @Inject constructor(
    private val storage: CardStorage
) {
    fun getCard(): HuginnCard? = storage.loadCard()
    fun hasCard(): Boolean     = storage.hasCard()
    fun saveCard(card: HuginnCard) = storage.saveCard(card)
    fun deleteCard()           = storage.deleteCard()
    fun isNonceUsed(nonce: String)  = storage.isNonceUsed(nonce)
    fun markNonceUsed(nonce: String) = storage.markNonceUsed(nonce)
}

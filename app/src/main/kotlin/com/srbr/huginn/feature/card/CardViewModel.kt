package com.srbr.huginn.feature.card

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.srbr.huginn.core.security.HuginnHCEService
import com.srbr.huginn.credential.card.BaseCardViewModel
import com.srbr.huginn.credential.security.DeviceIdentity
import com.srbr.huginn.credential.security.HuginnCard
import com.srbr.huginn.credential.storage.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Variante NFC: o desbloqueio autoriza o serviço HCE ([HuginnHCEService]) por 30s.
 * Toda a máquina de estado (loadCard, countdown, auto-lock, background) vem de
 * [BaseCardViewModel]; aqui só ficam os ganchos específicos do canal NFC.
 */
@HiltViewModel
class CardViewModel @Inject constructor(
    repository:       CardRepository,
    deviceIdentity:   DeviceIdentity,
    savedStateHandle: SavedStateHandle
) : BaseCardViewModel(repository, deviceIdentity, savedStateHandle) {

    init {
        // EncryptedSharedPreferences não deve bloquear a main thread.
        viewModelScope.launch(Dispatchers.IO) { loadCard() }
    }

    /** Publica o cartão no serviço HCE assim que carregado. */
    override fun onCardLoaded(card: HuginnCard) {
        HuginnHCEService.activeDeviceId = deviceIdentity.getDeviceId()
        HuginnHCEService.activeCard     = card
    }

    /** Autoriza o HCE pela janela de 30s e desbloqueia o card. */
    override suspend fun onUnlocked(card: HuginnCard) {
        HuginnHCEService.isAuthorized    = true
        HuginnHCEService.authorizedUntil = System.currentTimeMillis() + authWindowSecs * 1000L
        setUnlocked()
    }

    /** Revoga o HCE ao bloquear/expirar. */
    override fun onLockedCleanup() {
        HuginnHCEService.isAuthorized = false
    }
}

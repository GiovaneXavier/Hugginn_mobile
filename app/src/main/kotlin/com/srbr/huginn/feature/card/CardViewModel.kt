package com.srbr.huginn.feature.card

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srbr.huginn.core.security.DeviceIdentity
import com.srbr.huginn.core.security.HuginnCard
import com.srbr.huginn.core.security.HuginnHCEService
import com.srbr.huginn.core.storage.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardUiState(
    val card:          HuginnCard? = null,
    val displayId:     String      = "",
    val isUnlocked:    Boolean     = false,
    val countdown:     Int         = 0,       // seconds remaining
    val countdownPct:  Float       = 0f,      // 0..1 for progress bar
    val hasCard:       Boolean     = false,
    val hasLoaded:     Boolean     = false    // true once loadCard() completes
)

@HiltViewModel
class CardViewModel @Inject constructor(
    private val repository:       CardRepository,
    private val deviceIdentity:   DeviceIdentity,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(CardUiState())
    val state: StateFlow<CardUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private val AUTH_WINDOW_SECS = 30

    init {
        viewModelScope.launch(Dispatchers.IO) { loadCard() }
    }

    private suspend fun loadCard() {
        val systemId = savedStateHandle.get<String>("systemId")
        val card = systemId?.let { repository.getCard(it) }
        _state.update {
            it.copy(
                card      = card,
                displayId = deviceIdentity.getDisplayId(),
                hasCard   = card != null,
                hasLoaded = true
            )
        }
        if (card != null) {
            HuginnHCEService.activeDeviceId = deviceIdentity.getDeviceId()
            HuginnHCEService.activeCard     = card
        }
    }

    /**
     * Called by MainActivity after BiometricPrompt succeeds.
     * Authorizes HCE and starts the 30s countdown.
     */
    fun onBiometricSuccess() {
        HuginnHCEService.isAuthorized    = true
        HuginnHCEService.authorizedUntil = System.currentTimeMillis() + AUTH_WINDOW_SECS * 1000L

        _state.update { it.copy(isUnlocked = true, countdown = AUTH_WINDOW_SECS, countdownPct = 1f) }
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in AUTH_WINDOW_SECS downTo 0) {
                _state.update {
                    it.copy(
                        countdown    = remaining,
                        countdownPct = remaining.toFloat() / AUTH_WINDOW_SECS
                    )
                }
                if (remaining == 0) {
                    onExpire()
                    return@launch
                }
                delay(1000)
            }
        }
    }

    fun onExpire() {
        countdownJob?.cancel()
        HuginnHCEService.isAuthorized = false
        _state.update { it.copy(isUnlocked = false, countdown = 0, countdownPct = 0f) }
    }

    fun onAppBackground() {
        if (_state.value.isUnlocked) onExpire()
    }

    override fun onCleared() {
        super.onCleared()
        HuginnHCEService.isAuthorized = false
        countdownJob?.cancel()
    }
}

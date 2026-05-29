package com.srbr.huginn.feature.card_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srbr.huginn.credential.security.HuginnCard
import com.srbr.huginn.credential.storage.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardListViewModel(
    private val repository: CardRepository,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // Construtor usado pelo Hilt; o primário recebe um dispatcher controlável para testes.
    @Inject constructor(repository: CardRepository) : this(repository, Dispatchers.IO)

    private val _cards = MutableStateFlow<List<HuginnCard>>(emptyList())
    val cards: StateFlow<List<HuginnCard>> = _cards.asStateFlow()

    init { load() }

    fun refresh() { load() }

    private fun load() {
        viewModelScope.launch(ioDispatcher) {
            _cards.value = repository.getCards()
        }
    }
}

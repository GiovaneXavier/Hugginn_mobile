package com.srbr.huginn.feature.card_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srbr.huginn.core.security.HuginnCard
import com.srbr.huginn.core.storage.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardListViewModel @Inject constructor(
    private val repository: CardRepository
) : ViewModel() {

    private val _cards = MutableStateFlow<List<HuginnCard>>(emptyList())
    val cards: StateFlow<List<HuginnCard>> = _cards.asStateFlow()

    init { load() }

    fun refresh() { load() }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _cards.value = repository.getCards()
        }
    }
}

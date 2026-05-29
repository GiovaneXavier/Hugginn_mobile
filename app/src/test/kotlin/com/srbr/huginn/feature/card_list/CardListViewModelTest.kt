package com.srbr.huginn.feature.card_list

import app.cash.turbine.test
import com.srbr.huginn.credential.security.HuginnCard
import com.srbr.huginn.credential.storage.CardRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CardListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CardRepository
    private lateinit var viewModel: CardListViewModel

    private val fakeCards = listOf(
        HuginnCard(
            employeeId = "SRBR-0042",
            employeeName = "Ana Lima",
            employeeArea = "Pesquisa",
            employeeRole = "Pesquisadora Sênior",
            systemId = "SRBR_EXIT",
            systemName = "Saída SRBR",
            cardColor = "#1428A0",
            registeredAt = 1710950400,
            nonce = "nonce-1"
        ),
        HuginnCard(
            employeeId = "SRBR-0042",
            employeeName = "Ana Lima",
            employeeArea = "Pesquisa",
            employeeRole = null,
            systemId = "SRBR_ENTRY",
            systemName = "Entrada SRBR",
            cardColor = "#00FF00",
            registeredAt = 1710950400,
            nonce = "nonce-2"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.getCards() } returns fakeCards
        viewModel = CardListViewModel(repository, testDispatcher)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state loads cards from repository`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.cards.test {
            // Skip initial emptyList if IO hasn't completed yet (should not happen with mocks)
            val first = awaitItem()
            val cards = if (first.isEmpty()) awaitItem() else first
            assertEquals(2, cards.size)
            assertEquals(fakeCards, cards)
        }
    }

    @Test
    fun `refresh updates cards from repository`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle() // complete init load

        val updatedCards = fakeCards.take(1)
        every { repository.getCards() } returns updatedCards

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.cards.test {
            val cards = awaitItem()
            assertEquals(1, cards.size)
            assertEquals(updatedCards, cards)
            verify(atLeast = 2) { repository.getCards() } // init + refresh
        }
    }
}

package com.samdori93.yeoksadam.feature.home

import app.cash.turbine.test
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.HeritageSite
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.repository.FigureRepository
import com.samdori93.yeoksadam.core.domain.repository.LocationRepository
import com.samdori93.yeoksadam.core.domain.usecase.GetNearbyFiguresUseCase
import com.samdori93.yeoksadam.feature.home.viewmodel.HomeUiEffect
import com.samdori93.yeoksadam.feature.home.viewmodel.HomeUiEvent
import com.samdori93.yeoksadam.feature.home.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val site = HeritageSite("s1", "수원화성", 37.28, 127.01, "", 300f)
    private val nearby = listOf(
        NearbyFigure(Figure("f2", "세종", "왕", ""), site, distanceM = 800f, bearingDeg = 0f),
        NearbyFigure(Figure("f1", "정조", "왕", ""), site, distanceM = 120f, bearingDeg = 0f),
    )

    private val fakeFigureRepo = object : FigureRepository {
        override fun observeNearbyFigures(origin: LatLng, radiusM: Int) = flowOf(nearby)
        override fun observeFigures(): Flow<List<Figure>> = flowOf(emptyList())
        override suspend fun getFigure(id: String): Result<Figure> =
            Result.Failure(com.samdori93.yeoksadam.core.common.error.AppError.Unknown())
    }
    private val fakeLocationRepo = object : LocationRepository {
        override fun observeLocation() = flowOf(LatLng(37.28, 127.01))
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = HomeViewModel(GetNearbyFiguresUseCase(fakeFigureRepo, fakeLocationRepo))

    @Test
    fun `nearby figures are sorted by distance ascending`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem() // 초기 로딩
            dispatcher.scheduler.advanceUntilIdle()
            val loaded = expectMostRecentItem()
            assertFalse(loaded.isLoading)
            assertEquals(listOf("정조", "세종"), loaded.nearby.map { it.figure.name })
        }
    }

    @Test
    fun `OpenFigureSheet emits NavigateToFigureSheet effect`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.effect.test {
            vm.onEvent(HomeUiEvent.OpenFigureSheet("f1"))
            val effect = awaitItem()
            assertTrue(effect is HomeUiEffect.NavigateToFigureSheet)
            assertEquals("f1", (effect as HomeUiEffect.NavigateToFigureSheet).figureId)
        }
    }
}

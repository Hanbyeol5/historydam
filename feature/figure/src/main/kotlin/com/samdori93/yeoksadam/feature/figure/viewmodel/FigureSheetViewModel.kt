package com.samdori93.yeoksadam.feature.figure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.domain.model.Discovery
import com.samdori93.yeoksadam.core.domain.model.DiscoveryType
import com.samdori93.yeoksadam.core.domain.usecase.RecordDiscoveryUseCase
import com.samdori93.yeoksadam.core.ui.sample.SampleData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 인물 시트를 열면(= 인물을 만나면) 역사의 전당 도감에 기록한다. */
@HiltViewModel
class FigureSheetViewModel @Inject constructor(
    private val recordDiscovery: RecordDiscoveryUseCase,
) : ViewModel() {

    fun onFigureShown(figureId: String) {
        val figure = SampleData.figure(figureId)
        viewModelScope.launch {
            recordDiscovery(
                Discovery(
                    type = DiscoveryType.FIGURE,
                    refId = figure.id,
                    name = figure.name,
                    subtitle = figure.title,
                    description = "${figure.name}(${figure.title}) — ${figure.site}에서 만난 역사 인물입니다.",
                    imageUrl = null,
                    discoveredAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}

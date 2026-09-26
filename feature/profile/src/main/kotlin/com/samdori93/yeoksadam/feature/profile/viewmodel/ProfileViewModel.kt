package com.samdori93.yeoksadam.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.domain.model.Discovery
import com.samdori93.yeoksadam.core.domain.usecase.ObserveDiscoveriesUseCase
import com.samdori93.yeoksadam.core.domain.usecase.RemoveDiscoveryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 역사의 전당 도감 — 발견 목록 구독 + 삭제. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeDiscoveries: ObserveDiscoveriesUseCase,
    private val removeDiscovery: RemoveDiscoveryUseCase,
) : ViewModel() {

    val discoveries: StateFlow<List<Discovery>> = observeDiscoveries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(discovery: Discovery) {
        viewModelScope.launch { removeDiscovery(discovery) }
    }
}

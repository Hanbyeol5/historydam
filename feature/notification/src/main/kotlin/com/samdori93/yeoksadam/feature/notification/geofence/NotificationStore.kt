package com.samdori93.yeoksadam.feature.notification.geofence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 수신한 위치 알림 이벤트를 보관하는 인메모리 스토어(데모용). */
object NotificationStore {

    data class Event(val title: String, val body: String, val timeMillis: Long)

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    fun add(event: Event) {
        _events.update { listOf(event) + it }
    }
}

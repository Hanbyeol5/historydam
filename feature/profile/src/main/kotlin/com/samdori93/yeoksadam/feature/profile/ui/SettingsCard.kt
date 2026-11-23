package com.samdori93.yeoksadam.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.domain.model.ChatSettings
import com.samdori93.yeoksadam.core.domain.repository.ChatRepository
import com.samdori93.yeoksadam.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 설정 — 대화 기록. 지식 경계는 늘 켜져 있어 설정에 두지 않는다. 인물 대화 방식은 화면에서 고르지 않는다(저장된 값, 기본 백엔드 RAG). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {
    val settings: StateFlow<ChatSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatSettings())
    val roomCount: StateFlow<Int> = chatRepository.observeRoomCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun update(transform: (ChatSettings) -> ChatSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun clearAll() {
        viewModelScope.launch { chatRepository.clearHistory(null) }
    }
}

@Composable
fun SettingsCard(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val rooms by viewModel.roomCount.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(DancheongColors.HanjiCard, RoundedCornerShape(16.dp))
            .border(1.dp, DancheongColors.Meok.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⚙ 설정", fontSize = 13.sp, color = DancheongColors.MeokSoft)
            Spacer(Modifier.weight(1f))
            Text(if (open) "▲" else "▼", fontSize = 11.sp, color = DancheongColors.MeokSoft)
        }
        if (!open) return@Column

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    if (rooms > 0) "대화 기록: 인물 ${rooms}명과의 대화가 저장됨" else "대화 기록: 저장된 대화 없음",
                    fontSize = 12.sp,
                    color = DancheongColors.MeokSoft,
                    modifier = Modifier.weight(1f),
                )
                if (rooms > 0) {
                    Text(
                        "모두 지우기",
                        fontSize = 12.sp,
                        color = DancheongColors.Jujak,
                        modifier = Modifier
                            .border(1.dp, DancheongColors.Jujak.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { confirmClear = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            text = { Text("모든 인물과의 대화 기록을 지울까요?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    confirmClear = false
                }) { Text("지우기", color = DancheongColors.Jujak) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("취소") } },
        )
    }
}

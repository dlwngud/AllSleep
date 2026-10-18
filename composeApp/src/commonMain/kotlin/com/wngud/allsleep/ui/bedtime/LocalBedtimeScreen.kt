package com.wngud.allsleep.ui.bedtime

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import allsleep.composeapp.generated.resources.Res
import allsleep.composeapp.generated.resources.character_phone_away
import com.wngud.allsleep.domain.model.bedtime.*
import com.wngud.allsleep.ui.components.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

enum class BedtimeTab(val label: String) { TODAY("오늘"), ROUTINES("루틴"), RECORDS("기록"), MY("마이") }

@Composable
fun LocalBedtimeScreen(
    tab: BedtimeTab,
    contentPadding: PaddingValues,
    onOpenRoutines: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    accountName: String? = null,
    onAccountSettings: () -> Unit = {},
    viewModel: LocalBedtimeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var editKind by rememberSaveable { mutableStateOf<String?>(null) }
    LocalBedtimeContent(tab, state, contentPadding, onOpenRoutines, onLogin, { editKind = it.name }, viewModel::reload, modifier, accountName, onAccountSettings)
    editKind?.let { name ->
        val kind = RoutineKind.valueOf(name)
        state.routines.find { it.kind == kind }?.let { routine ->
            RoutineEditor(routine, state.isSaving, state.error, onDismiss = { if (!state.isSaving) { editKind = null; viewModel.dismissMessage() } }, onSave = { bedtime, release, enabled ->
                viewModel.saveRoutine(kind, bedtime, release, enabled) { editKind = null }
            })
        }
    }
}

@Composable
fun LocalBedtimeContent(
    tab: BedtimeTab,
    state: LocalBedtimeUiState,
    contentPadding: PaddingValues,
    onOpenRoutines: () -> Unit,
    onLogin: () -> Unit,
    onEditRoutine: (RoutineKind) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    accountName: String? = null,
    onAccountSettings: () -> Unit = {},
) {
    BedtimePage(contentPadding, modifier) {
        Text("AllSleep", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
        Text(when (tab) {
            BedtimeTab.TODAY -> "오늘 밤도,\n내 약속대로."
            BedtimeTab.ROUTINES -> "매일 다시\n결심하지 않도록."
            BedtimeTab.RECORDS -> "확인된 기록을\n차근차근."
            BedtimeTab.MY -> "이 기기에서\n약속을 이어가요."
        }, style = MaterialTheme.typography.headlineMedium)
        if (state.isLoading) {
            BedtimeStatus("설정을 불러오는 중", "이 기기에 저장된 약속을 확인하고 있어요.")
        } else {
            state.error?.let { BedtimeStatus("확인이 필요해요", it, action = { BedtimeTextButton(onRetry) { Text("다시 확인") } }) }
            when (tab) {
                BedtimeTab.TODAY -> {
                    Image(painterResource(Res.drawable.character_phone_away), contentDescription = null, modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp))
                    state.routines.firstOrNull { it.kind == state.todayKind }?.let { routine ->
                        BedtimeCard(Modifier.fillMaxWidth()) {
                            Text(if (routine.kind == RoutineKind.WEEKDAY) "평일 취침 약속" else "주말 취침 약속", style = MaterialTheme.typography.labelLarge)
                            Text(routine.bedtime, style = MaterialTheme.typography.displayMedium)
                            Text("해제 ${routine.releaseTime} · ${if (routine.enabled) "사용 설정 켜짐" else "사용 설정 꺼짐"}")
                            Text("설정이 저장됐어요. 자동 제한 준비와 예약은 별도로 확인해야 해요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val active = state.sessions.lastOrNull { !it.isFinished }
                    BedtimeStatus(if (active == null) "제한 실행 준비가 필요해요" else active.phase.label(), if (active == null) "먼저 약속 시간을 확인해 주세요. 실행이 확인되지 않은 상태를 제한 중이라고 표시하지 않아요." else "저장된 회차의 상태예요. 현재 서비스 실행 여부는 별도로 확인해야 해요.")
                    BedtimePrimaryButton(onOpenRoutines, Modifier.fillMaxWidth()) { Text("내 약속 확인") }
                }
                BedtimeTab.ROUTINES -> {
                    Text("평일·주말 약속은 이 기기에 저장돼요. 로그인과 인터넷 연결 없이 편집할 수 있어요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.routines.forEach { routine ->
                        BedtimeCard(Modifier.fillMaxWidth()) {
                            Text(if (routine.kind == RoutineKind.WEEKDAY) "평일" else "주말", style = MaterialTheme.typography.titleLarge)
                            Text("${routine.bedtime} → ${routine.releaseTime}", style = MaterialTheme.typography.headlineSmall)
                            Text(if (routine.enabled) "사용 설정 켜짐" else "사용 설정 꺼짐")
                            BedtimePrimaryButton({ onEditRoutine(routine.kind) }, Modifier.fillMaxWidth()) { Text(if (routine.kind == RoutineKind.WEEKDAY) "평일 약속 편집" else "주말 약속 편집") }
                        }
                    }
                    Text("자동 제한을 사용하려면 실행 준비를 확인해 주세요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BedtimeTab.RECORDS -> {
                    Text("사용 제한 기록이며 실제 수면 시간과 달라요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.sessions.isEmpty()) {
                        BedtimeStatus("첫 약속부터 시작해 볼까요?", "아직 이 기기에서 확인한 제한 기록이 없어요. 기존 계정의 원본 기록은 별도로 보존돼요.")
                        BedtimePrimaryButton(onOpenRoutines, Modifier.fillMaxWidth()) { Text("내 약속 확인") }
                    } else state.sessions.sortedByDescending { it.requestedAt }.forEach { session ->
                        BedtimeCard(Modifier.fillMaxWidth()) {
                            Text(session.occurrence.date, style = MaterialTheme.typography.titleMedium)
                            Text(session.phase.label())
                            Text("약속 ${session.occurrence.routine.bedtime} → ${session.occurrence.routine.releaseTime}")
                            Text(session.startedAt?.let { "실행 확인 ${formatTime(it, session.occurrence.timeZoneId)}" } ?: "실행이 확인되지 않았어요.")
                            Text("완료된 확인 구간 ${session.confirmedDurationMillis / 60_000}분", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            session.endReason?.let { Text("종료 유형 ${it.label()}") }
                        }
                    }
                }
                BedtimeTab.MY -> {
                    BedtimeStatus(if (accountName == null) "계정 없이 사용 중" else accountName, if (accountName == null) "기본 약속과 로컬 기록을 사용할 수 있어요. 앱 삭제나 기기 변경 전에 기록 보관을 확인해 주세요." else "계정 정보와 이 기기의 로컬 기록 소유권은 구분해요.")
                    BedtimeSettingsRow("취침 약속", onOpenRoutines, Modifier.fillMaxWidth(), "평일·주말 시간 확인")
                    if (accountName == null) BedtimeSettingsRow("로그인", onLogin, Modifier.fillMaxWidth(), "계정 기능이 필요할 때 로그인해요.")
                    else BedtimeSettingsRow("계정과 설정", onAccountSettings, Modifier.fillMaxWidth(), "기존 계정·구독 관리")
                    Text("계정 백업과 기기 연동은 아직 사용할 수 없어요. 로그인만으로 게스트 기록을 자동 병합하지 않아요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.savedMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        }
    }
}

@Composable
private fun RoutineEditor(routine: BedtimeRoutine, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit) {
    var bedtime by rememberSaveable(routine.kind) { mutableStateOf(routine.bedtime) }
    var release by rememberSaveable(routine.kind) { mutableStateOf(routine.releaseTime) }
    var enabled by rememberSaveable(routine.kind) { mutableStateOf(routine.enabled) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (routine.kind == RoutineKind.WEEKDAY) "평일 취침 약속" else "주말 취침 약속") }, text = {
        Column(Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BedtimeTextField(bedtime, { bedtime = it }, "사용 제한 시작", Modifier.fillMaxWidth())
            BedtimeTextField(release, { release = it }, "해제 시각", Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("이 약속 사용", Modifier.weight(1f))
                Switch(enabled, { enabled = it }, modifier = Modifier.semantics { contentDescription = "이 약속 사용" }, enabled = !saving)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { BedtimeTextButton({ onSave(bedtime, release, enabled) }, enabled = !saving) { Text(if (saving) "저장 중" else "약속 저장") } }, dismissButton = { BedtimeTextButton(onDismiss, enabled = !saving) { Text("취소") } })
}

private fun RestrictionPhase.label() = when (this) {
    RestrictionPhase.REQUESTED -> "시작 요청 · 실행 미확인"
    RestrictionPhase.RUNNING -> "실행 확인 기록 있음"
    RestrictionPhase.STOP_REQUESTED -> "해제 요청 · 완료 미확인"
    RestrictionPhase.ENDED -> "해제 확인 완료"
    RestrictionPhase.INTERRUPTED -> "기술 중단 확인"
}
private fun RestrictionEndReason.label() = when (this) {
    RestrictionEndReason.SCHEDULED -> "예정 해제"
    RestrictionEndReason.EARLY -> "직접 종료"
    RestrictionEndReason.EMERGENCY -> "긴급 접근"
    RestrictionEndReason.TECHNICAL -> "기술 중단"
}
private fun formatTime(at: Long, zone: String) = Instant.fromEpochMilliseconds(at).toLocalDateTime(TimeZone.of(zone)).time.toString().take(5)

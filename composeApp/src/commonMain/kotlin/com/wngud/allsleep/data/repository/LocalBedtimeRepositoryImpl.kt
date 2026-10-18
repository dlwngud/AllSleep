package com.wngud.allsleep.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.wngud.allsleep.data.source.local.BedtimeJournalCodec
import com.wngud.allsleep.domain.model.bedtime.*
import com.wngud.allsleep.domain.repository.LocalBedtimeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val LOCAL_BEDTIME_JOURNAL_KEY = stringPreferencesKey("local_bedtime_journal_v1")

@OptIn(ExperimentalUuidApi::class)
class LocalBedtimeRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val newOwnerId: () -> String = { "guest:${Uuid.random()}" },
) : LocalBedtimeRepository {
    override val journal = dataStore.data.map { p -> p[LOCAL_BEDTIME_JOURNAL_KEY]?.let(BedtimeJournalCodec::decode) ?: LocalBedtimeJournal() }
    override val routines = dataStore.data.map { p ->
        listOf(
            BedtimeRoutine(RoutineKind.WEEKDAY, p[stringPreferencesKey("bedtime")] ?: "23:00", p[stringPreferencesKey("wake_time")] ?: "07:00", p[booleanPreferencesKey("sleep_alarm_enabled")] ?: true),
            BedtimeRoutine(RoutineKind.WEEKEND, p[stringPreferencesKey("weekend_bedtime")] ?: "00:00", p[stringPreferencesKey("weekend_wake_time")] ?: "09:00", p[booleanPreferencesKey("weekend_sleep_enabled")] ?: true),
        )
    }

    override suspend fun initialize(): Result<LocalOwner> = storageResult {
        val saved = dataStore.edit { p ->
            if (p[LOCAL_BEDTIME_JOURNAL_KEY] == null) {
                val legacyStart = p[longPreferencesKey("active_sleep_start_at")]?.takeIf { it > 0L }
                p[LOCAL_BEDTIME_JOURNAL_KEY] = BedtimeJournalCodec.encode(LocalBedtimeJournal(
                    owner = LocalOwner(newOwnerId()),
                    legacyRecords = legacyStart?.let { listOf(LegacyRestrictionRecord("settings:active_sleep_start_at:$it", activeStartAt = it)) } ?: emptyList(),
                ))
            } else {
                BedtimeJournalCodec.decode(p[LOCAL_BEDTIME_JOURNAL_KEY]!!)
            }
        }
        BedtimeJournalCodec.decode(saved[LOCAL_BEDTIME_JOURNAL_KEY]!!).owner!!
    }

    override suspend fun saveRoutine(routine: BedtimeRoutine): Result<Unit> = storageResult {
        routine.validated()
        dataStore.edit { p ->
            val weekday = routine.kind == RoutineKind.WEEKDAY
            p[stringPreferencesKey(if (weekday) "bedtime" else "weekend_bedtime")] = routine.bedtime
            p[stringPreferencesKey(if (weekday) "wake_time" else "weekend_wake_time")] = routine.releaseTime
            p[booleanPreferencesKey(if (weekday) "sleep_alarm_enabled" else "weekend_sleep_enabled")] = routine.enabled
        }
        Unit
    }

    override suspend fun requestSession(id: String, occurrence: BedtimeOccurrence, requestedAt: Long): Result<BedtimeSession> = storageResult {
        val session = BedtimeSession(id, occurrence, requestedAt)
        val saved = updateJournal { current ->
            val existing = current.sessions.find { it.id == id }
            if (existing != null) {
                require(existing.occurrence == occurrence && existing.requestedAt == requestedAt) { "다른 기록과 ID가 겹칩니다." }
                current
            } else {
                require(current.activeSession(occurrence.owner) == null) { "진행 중인 회차를 먼저 해제해 주세요." }
                current.copy(sessions = current.sessions + session)
            }
        }
        saved.sessions.first { it.id == id }
    }

    override suspend fun transitionSession(owner: LocalOwner, id: String, event: RestrictionEvent): Result<BedtimeSession> = storageResult {
        val saved = updateJournal { current ->
            val previous = current.sessions.firstOrNull { it.id == id } ?: error("회차를 찾을 수 없습니다.")
            require(previous.occurrence.owner == owner) { "다른 소유자의 기록을 변경할 수 없습니다." }
            val next = previous.transition(event)
            current.copy(sessions = current.sessions.map { if (it.id == id) next else it })
        }
        saved.sessions.first { it.id == id }
    }

    override suspend fun preserveLegacyRecord(record: LegacyRestrictionRecord): Result<Unit> = storageResult {
        require(record.sourceId.isNotBlank())
        updateJournal { current ->
            val previous = current.legacyRecords.firstOrNull { it.sourceId == record.sourceId }
            require(previous == null || previous == record) { "보존한 원본을 덮어쓸 수 없습니다." }
            if (previous == null) current.copy(legacyRecords = current.legacyRecords + record) else current
        }
        Unit
    }

    private suspend fun updateJournal(transform: (LocalBedtimeJournal) -> LocalBedtimeJournal): LocalBedtimeJournal {
        val saved = dataStore.edit { p ->
            val current = BedtimeJournalCodec.decode(p[LOCAL_BEDTIME_JOURNAL_KEY] ?: error("로컬 기록을 먼저 준비해 주세요."))
            p[LOCAL_BEDTIME_JOURNAL_KEY] = BedtimeJournalCodec.encode(transform(current))
        }
        return BedtimeJournalCodec.decode(saved[LOCAL_BEDTIME_JOURNAL_KEY]!!)
    }

    private suspend fun <T> storageResult(block: suspend () -> T): Result<T> = try { Result.success(block()) } catch (cancelled: CancellationException) { throw cancelled } catch (error: Exception) { Result.failure(error) }
}

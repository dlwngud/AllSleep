package com.wngud.allsleep.domain.repository

import com.wngud.allsleep.domain.model.bedtime.*
import kotlinx.coroutines.flow.Flow

interface LocalBedtimeRepository {
    val journal: Flow<LocalBedtimeJournal>
    val routines: Flow<List<BedtimeRoutine>>
    suspend fun initialize(): Result<LocalOwner>
    suspend fun saveRoutine(routine: BedtimeRoutine): Result<Unit>
    suspend fun requestSession(id: String, occurrence: BedtimeOccurrence, requestedAt: Long): Result<BedtimeSession>
    suspend fun transitionSession(owner: LocalOwner, id: String, event: RestrictionEvent): Result<BedtimeSession>
    suspend fun preserveLegacyRecord(record: LegacyRestrictionRecord): Result<Unit>
}

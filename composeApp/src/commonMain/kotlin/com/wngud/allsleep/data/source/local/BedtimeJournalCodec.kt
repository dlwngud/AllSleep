package com.wngud.allsleep.data.source.local

import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.model.bedtime.*
import kotlinx.serialization.json.*

/** 명시적인 v1 저장 형식. 알 수 없는 버전이나 손상된 기록을 빈 데이터로 덮어쓰지 않는다. */
internal object BedtimeJournalCodec {
    fun encode(journal: LocalBedtimeJournal): String = buildJsonObject {
        put("version", 1)
        put("owner", journal.owner?.id)
        put("sessions", JsonArray(journal.sessions.map(::sessionJson)))
        put("legacyRecords", JsonArray(journal.legacyRecords.map { legacy -> buildJsonObject {
            put("sourceId", legacy.sourceId)
            put("owner", legacy.owner?.id)
            put("activeStartAt", legacy.activeStartAt)
            put("originalRecord", legacy.originalRecord?.let(::recordJson) ?: JsonNull)
        } }))
    }.toString()

    fun decode(value: String): LocalBedtimeJournal {
        val root = Json.parseToJsonElement(value).jsonObject
        require(root.getValue("version").jsonPrimitive.int == 1) { "지원하지 않는 로컬 기록 버전입니다." }
        val journal = LocalBedtimeJournal(
            owner = root.optionalString("owner")?.let(::LocalOwner),
            sessions = root.getValue("sessions").jsonArray.map { readSession(it.jsonObject) },
            legacyRecords = root.getValue("legacyRecords").jsonArray.map { value ->
                val legacy = value.jsonObject
                LegacyRestrictionRecord(
                    sourceId = legacy.string("sourceId"),
                    owner = legacy.optionalString("owner")?.let(::LocalOwner),
                    activeStartAt = legacy.optionalLong("activeStartAt"),
                    originalRecord = legacy["originalRecord"]?.takeIf { it != JsonNull }?.jsonObject?.let(::readRecord),
                )
            },
        )
        require(journal.owner != null)
        require(journal.sessions.map { it.id }.distinct().size == journal.sessions.size)
        require(journal.legacyRecords.map { it.sourceId }.distinct().size == journal.legacyRecords.size)
        return journal
    }

    private fun sessionJson(s: BedtimeSession) = buildJsonObject {
        put("id", s.id)
        put("occurrence", buildJsonObject {
            put("owner", s.occurrence.owner.id)
            put("kind", s.occurrence.routine.kind.name)
            put("bedtime", s.occurrence.routine.bedtime)
            put("releaseTime", s.occurrence.routine.releaseTime)
            put("enabled", s.occurrence.routine.enabled)
            put("date", s.occurrence.date)
            put("timeZoneId", s.occurrence.timeZoneId)
            put("plannedStartAt", s.occurrence.plannedStartAt)
            put("plannedReleaseAt", s.occurrence.plannedReleaseAt)
        })
        put("requestedAt", s.requestedAt)
        put("phase", s.phase.name)
        put("startedAt", s.startedAt)
        put("stopRequestedAt", s.stopRequestedAt)
        put("releasedAt", s.releasedAt)
        put("interruptedAt", s.interruptedAt)
        put("endReason", s.endReason?.name)
        put("intervals", JsonArray(s.intervals.map { interval -> buildJsonObject {
            put("startedAt", interval.startedAt)
            put("endedAt", interval.endedAt)
        } }))
    }

    private fun readSession(s: JsonObject): BedtimeSession {
        val o = s.getValue("occurrence").jsonObject
        return BedtimeSession(
            id = s.string("id"),
            occurrence = BedtimeOccurrence(
                owner = LocalOwner(o.string("owner")),
                routine = BedtimeRoutine(RoutineKind.valueOf(o.string("kind")), o.string("bedtime"), o.string("releaseTime"), o.getValue("enabled").jsonPrimitive.boolean),
                date = o.string("date"),
                timeZoneId = o.string("timeZoneId"),
                plannedStartAt = o.long("plannedStartAt"),
                plannedReleaseAt = o.long("plannedReleaseAt"),
            ),
            requestedAt = s.long("requestedAt"),
            phase = RestrictionPhase.valueOf(s.string("phase")),
            startedAt = s.optionalLong("startedAt"),
            stopRequestedAt = s.optionalLong("stopRequestedAt"),
            releasedAt = s.optionalLong("releasedAt"),
            interruptedAt = s.optionalLong("interruptedAt"),
            endReason = s.optionalString("endReason")?.let(RestrictionEndReason::valueOf),
            intervals = s.getValue("intervals").jsonArray.map {
                ConfirmedRestrictionInterval(it.jsonObject.long("startedAt"), it.jsonObject.optionalLong("endedAt"))
            },
        )
    }

    private fun recordJson(r: SleepRecord) = buildJsonObject {
        put("id", r.id); put("uid", r.uid); put("date", r.date)
        put("bedtime", r.bedtime); put("wakeTime", r.wakeTime)
        put("targetBedtime", r.targetBedtime); put("targetWakeTime", r.targetWakeTime)
        put("targetMinutes", r.targetMinutes); put("durationMinutes", r.durationMinutes)
        put("sleepEfficiency", r.sleepEfficiency); put("achievementRate", r.achievementRate); put("isLockUsed", r.isLockUsed)
    }

    private fun readRecord(r: JsonObject) = SleepRecord(
        id = r.string("id"), uid = r.string("uid"), date = r.string("date"),
        bedtime = r.long("bedtime"), wakeTime = r.long("wakeTime"),
        targetBedtime = r.string("targetBedtime"), targetWakeTime = r.string("targetWakeTime"),
        targetMinutes = r.getValue("targetMinutes").jsonPrimitive.int,
        durationMinutes = r.getValue("durationMinutes").jsonPrimitive.int,
        sleepEfficiency = r.getValue("sleepEfficiency").jsonPrimitive.float,
        achievementRate = r.getValue("achievementRate").jsonPrimitive.float,
        isLockUsed = r.getValue("isLockUsed").jsonPrimitive.boolean,
    )
    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
    private fun JsonObject.long(key: String) = getValue(key).jsonPrimitive.long
    private fun JsonObject.optionalString(key: String) = getValue(key).jsonPrimitive.contentOrNull
    private fun JsonObject.optionalLong(key: String) = getValue(key).takeIf { it != JsonNull }?.jsonPrimitive?.long
}

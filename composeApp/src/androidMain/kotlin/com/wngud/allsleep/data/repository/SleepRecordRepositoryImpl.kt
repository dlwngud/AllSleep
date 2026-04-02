package com.wngud.allsleep.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.repository.SleepRecordRepository
import kotlinx.coroutines.tasks.await

class SleepRecordRepositoryImpl(
    private val firestore: FirebaseFirestore
) : SleepRecordRepository {

    private fun getRecordsCollection(uid: String) = 
        firestore.collection("users").document(uid).collection("sleep_records")

    override suspend fun saveSleepRecord(record: SleepRecord): Result<Unit> = runCatching {
        println("[SleepDebug] 리포지토리 저장 시작: id=${record.id}, uid=${record.uid}")
        val data = hashMapOf(
            "uid" to record.uid,
            "date" to record.date,
            "bedtime" to record.bedtime,
            "wakeTime" to record.wakeTime,
            "targetBedtime" to record.targetBedtime,
            "targetWakeTime" to record.targetWakeTime,
            "targetMinutes" to record.targetMinutes,
            "durationMinutes" to record.durationMinutes,
            "sleepEfficiency" to record.sleepEfficiency,
            "achievementRate" to record.achievementRate,
            "isLockUsed" to record.isLockUsed
        )
        
        getRecordsCollection(record.uid).document(record.date)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .await()
        println("[SleepDebug] Firestore 저장 성공: ${record.date}")
    }.onFailure {
        println("[SleepDebug] Firestore 저장 실패: ${it.message}")
    }

    override suspend fun getRecordsByMonth(uid: String, yearMonth: String): Result<List<SleepRecord>> = runCatching {
        // yearMonth: "2026-03" -> 2026-03-01 ~ 2026-03-31
        val snapshot = getRecordsCollection(uid)
            .whereGreaterThanOrEqualTo("date", "$yearMonth-01")
            .whereLessThanOrEqualTo("date", "$yearMonth-31")
            .get()
            .await()
            
        snapshot.documents.map { mapDocumentToSleepRecord(it) }
    }

    override suspend fun getRecordsByRange(uid: String, startDate: String, endDate: String): Result<List<SleepRecord>> = runCatching {
        val snapshot = getRecordsCollection(uid)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .get()
            .await()
            
        snapshot.documents.map { mapDocumentToSleepRecord(it) }
    }

    override suspend fun getLatestRecord(uid: String): Result<SleepRecord?> = runCatching {
        val snapshot = getRecordsCollection(uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            
        snapshot.documents.firstOrNull()?.let { mapDocumentToSleepRecord(it) }
    }

    private fun mapDocumentToSleepRecord(doc: com.google.firebase.firestore.DocumentSnapshot): SleepRecord {
        return SleepRecord(
            id = doc.id,
            uid = doc.getString("uid") ?: "",
            date = doc.getString("date") ?: "",
            bedtime = doc.getLong("bedtime") ?: 0L,
            wakeTime = doc.getLong("wakeTime") ?: 0L,
            targetBedtime = doc.getString("targetBedtime") ?: "",
            targetWakeTime = doc.getString("targetWakeTime") ?: "",
            targetMinutes = doc.getLong("targetMinutes")?.toInt() ?: 0,
            durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
            sleepEfficiency = doc.getDouble("sleepEfficiency")?.toFloat() ?: 0f,
            achievementRate = doc.getDouble("achievementRate")?.toFloat() ?: 0f,
            isLockUsed = doc.getBoolean("isLockUsed") ?: false
        )
    }
}

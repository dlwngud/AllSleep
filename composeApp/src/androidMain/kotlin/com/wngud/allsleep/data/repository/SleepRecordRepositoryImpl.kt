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
        getRecordsCollection(record.uid).document(record.date)
            .set(record, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    override suspend fun getRecordsByMonth(uid: String, yearMonth: String): Result<List<SleepRecord>> = runCatching {
        // yearMonth: "2026-03" -> 2026-03-01 ~ 2026-03-31
        val snapshot = getRecordsCollection(uid)
            .whereGreaterThanOrEqualTo("date", "$yearMonth-01")
            .whereLessThanOrEqualTo("date", "$yearMonth-31")
            .get()
            .await()
            
        snapshot.toObjects(SleepRecord::class.java)
    }

    override suspend fun getRecordsByRange(uid: String, startDate: String, endDate: String): Result<List<SleepRecord>> = runCatching {
        val snapshot = getRecordsCollection(uid)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .get()
            .await()
            
        snapshot.toObjects(SleepRecord::class.java)
    }

    override suspend fun getLatestRecord(uid: String): Result<SleepRecord?> = runCatching {
        val snapshot = getRecordsCollection(uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            
        snapshot.toObjects(SleepRecord::class.java).firstOrNull()
    }
}

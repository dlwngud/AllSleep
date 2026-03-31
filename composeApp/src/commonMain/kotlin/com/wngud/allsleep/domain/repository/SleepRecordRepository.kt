package com.wngud.allsleep.domain.repository

import com.wngud.allsleep.domain.model.SleepRecord

/**
 * 수면 기록 저장 및 조회를 위한 데이터 저장소 인터페이스
 */
interface SleepRecordRepository {
    
    /** 수면 기록 저장 (Firestore: users/{uid}/sleep_records/{date}) */
    suspend fun saveSleepRecord(record: SleepRecord): Result<Unit>

    /** 특정 월의 기록 전체 조회 — 월간 캘린더 뷰용 (yearMonth: "2026-03") */
    suspend fun getRecordsByMonth(uid: String, yearMonth: String): Result<List<SleepRecord>>

    /** 기간 내 기록 조회 — 통계 계산용 */
    suspend fun getRecordsByRange(uid: String, startDate: String, endDate: String): Result<List<SleepRecord>>

    /** 가장 최근 단일 기록 조회 — 최신 수면 요약용 */
    suspend fun getLatestRecord(uid: String): Result<SleepRecord?>
}

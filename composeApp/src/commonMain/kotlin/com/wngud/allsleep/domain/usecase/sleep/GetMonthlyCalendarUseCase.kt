package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.repository.SleepRecordRepository

/**
 * 캘린더 뷰를 위한 월별 기록 조회 UseCase
 */
class GetMonthlyCalendarUseCase(
    private val repository: SleepRecordRepository
) {
    /**
     * @param uid 사용자 ID
     * @param yearMonth 연월 ("yyyy-MM")
     * @return 날짜 문자열("yyyy-MM-dd")을 키로 하는 수면 기록 맵
     */
    suspend operator fun invoke(uid: String, yearMonth: String): Map<String, SleepRecord> {
        val result = repository.getRecordsByMonth(uid, yearMonth)
        val records = result.getWithDefault { emptyList() }
        
        return records.associateBy { it.date }
    }

    private fun <T> Result<T>.getWithDefault(default: () -> T): T {
        return if (isSuccess) getOrThrow() else default()
    }
}

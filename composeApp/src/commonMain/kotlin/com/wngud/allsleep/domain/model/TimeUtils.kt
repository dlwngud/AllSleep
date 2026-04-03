package com.wngud.allsleep.domain.model

/**
 * KMP 환경에서 공통적으로 사용할 시간 관련 유틸리티 (expect)
 */
expect fun platformTimeMillis(): Long

/**
 * 타임스탬프(ms)를 "yyyy-MM-dd" 형식의 문자열로 변환 (expect)
 */
expect fun formatTimestampToDate(timestamp: Long): String

/**
 * 타임스탬프(ms)를 "a hh:mm" 형식의 문자열로 변환 (expect)
 */
expect fun formatTimestampToTime(timestamp: Long): String

/**
 * 해당 타임스탬프의 요일이 평일(월-금)인지 확인 (expect)
 */
expect fun isWeekday(timestamp: Long): Boolean

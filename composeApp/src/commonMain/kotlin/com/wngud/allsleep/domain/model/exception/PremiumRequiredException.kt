package com.wngud.allsleep.domain.model.exception

/**
 * 무료 유저가 프리미엄 전용 기능(예: 기기 제한 초과 등록)을 시도할 때 발생하는 예외
 */
class PremiumRequiredException(message: String = "프리미엄 구독이 필요한 기능입니다.") : Exception(message)

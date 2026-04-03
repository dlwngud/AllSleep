package com.wngud.allsleep.domain.usecase.onboarding

import com.wngud.allsleep.domain.repository.SleepSettingsRepository

class CompleteOnboardingUseCase(private val repository: SleepSettingsRepository) {
    suspend operator fun invoke(bedtime: String, wakeTime: String) {
        // 초기 온보딩 시에는 동일한 시간을 평일/주말에 모두 적용
        repository.saveWeekdaySchedule(bedtime, wakeTime)
        repository.saveWeekendSchedule(bedtime, wakeTime)
        repository.saveOnboardingCompleted(true)
    }
}

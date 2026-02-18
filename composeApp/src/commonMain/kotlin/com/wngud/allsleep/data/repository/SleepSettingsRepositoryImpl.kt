package com.wngud.allsleep.data.repository

import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 수면 설정 저장소 구현체
 * (현재는 In-Memory로 구현, 추후 DataStore/Preferences로 교체 가능)
 */
class SleepSettingsRepositoryImpl : SleepSettingsRepository {
    
    private val _bedtime = MutableStateFlow("23:00")
    override val bedtime: Flow<String> = _bedtime.asStateFlow()
    
    private val _wakeTime = MutableStateFlow("07:00")
    override val wakeTime: Flow<String> = _wakeTime.asStateFlow()
    
    override suspend fun saveSleepSchedule(bedtime: String, wakeTime: String) {
        _bedtime.value = bedtime
        _wakeTime.value = wakeTime
        // TODO: Persist to local storage
    }
    
    override suspend fun clear() {
        _bedtime.value = "23:00"
        _wakeTime.value = "07:00"
    }
}

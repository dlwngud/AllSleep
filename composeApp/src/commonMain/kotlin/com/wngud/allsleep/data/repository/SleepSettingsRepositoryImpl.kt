package com.wngud.allsleep.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 수면 설정 저장소 구현체
 * DataStore Preferences를 사용하여 데이터를 영구 저장합니다.
 */
class SleepSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SleepSettingsRepository {

    companion object {
        // 평일 (기존 키 유지하여 마이그레이션)
        private val KEY_WEEKDAY_BEDTIME = stringPreferencesKey("bedtime")
        private val KEY_WEEKDAY_WAKE_TIME = stringPreferencesKey("wake_time")
        private val KEY_WEEKDAY_SLEEP_ENABLED = booleanPreferencesKey("sleep_alarm_enabled")
        private val KEY_WEEKDAY_WAKE_ENABLED = booleanPreferencesKey("wake_alarm_enabled")
        
        // 주말 (새 키 추가)
        private val KEY_WEEKEND_BEDTIME = stringPreferencesKey("weekend_bedtime")
        private val KEY_WEEKEND_WAKE_TIME = stringPreferencesKey("weekend_wake_time")
        private val KEY_WEEKEND_SLEEP_ENABLED = booleanPreferencesKey("weekend_sleep_enabled")
        private val KEY_WEEKEND_WAKE_ENABLED = booleanPreferencesKey("weekend_wake_enabled")
        
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_ACTIVE_SLEEP_START_AT = longPreferencesKey("active_sleep_start_at")
        private val KEY_LAST_APP_OPEN_AD_SHOWN_AT = longPreferencesKey("last_app_open_ad_shown_at")
        
        private const val DEFAULT_WEEKDAY_BEDTIME = "23:00"
        private const val DEFAULT_WEEKDAY_WAKE_TIME = "07:00"
        private const val DEFAULT_WEEKEND_BEDTIME = "00:00"
        private const val DEFAULT_WEEKEND_WAKE_TIME = "09:00"
    }

    // --- 평일 루틴 Flows ---
    override val weekdayBedtime: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_WEEKDAY_BEDTIME] ?: DEFAULT_WEEKDAY_BEDTIME
    }

    override val weekdayWakeTime: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_WEEKDAY_WAKE_TIME] ?: DEFAULT_WEEKDAY_WAKE_TIME
    }

    override val isWeekdaySleepEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_WEEKDAY_SLEEP_ENABLED] ?: true
    }

    override val isWeekdayWakeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_WEEKDAY_WAKE_ENABLED] ?: true
    }

    // --- 주말 루틴 Flows ---
    override val weekendBedtime: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_WEEKEND_BEDTIME] ?: DEFAULT_WEEKEND_BEDTIME
    }

    override val weekendWakeTime: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_WEEKEND_WAKE_TIME] ?: DEFAULT_WEEKEND_WAKE_TIME
    }

    override val isWeekendSleepEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_WEEKEND_SLEEP_ENABLED] ?: true
    }

    override val isWeekendWakeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_WEEKEND_WAKE_ENABLED] ?: true
    }

    // --- 저장 메서드 (평일) ---
    override suspend fun saveWeekdaySchedule(bedtime: String, wakeTime: String) {
        dataStore.edit { preferences ->
            preferences[KEY_WEEKDAY_BEDTIME] = bedtime
            preferences[KEY_WEEKDAY_WAKE_TIME] = wakeTime
        }
    }

    override suspend fun saveWeekdaySleepEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_WEEKDAY_SLEEP_ENABLED] = enabled
        }
    }

    override suspend fun saveWeekdayWakeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_WEEKDAY_WAKE_ENABLED] = enabled
        }
    }

    // --- 저장 메서드 (주말) ---
    override suspend fun saveWeekendSchedule(bedtime: String, wakeTime: String) {
        dataStore.edit { preferences ->
            preferences[KEY_WEEKEND_BEDTIME] = bedtime
            preferences[KEY_WEEKEND_WAKE_TIME] = wakeTime
        }
    }

    override suspend fun saveWeekendSleepEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_WEEKEND_SLEEP_ENABLED] = enabled
        }
    }

    override suspend fun saveWeekendWakeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_WEEKEND_WAKE_ENABLED] = enabled
        }
    }

    // --- 공통 기능 구현 ---
    override val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    override suspend fun saveOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    override val deviceName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_DEVICE_NAME]
    }

    override suspend fun saveDeviceName(name: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DEVICE_NAME] = name
        }
    }

    override val isPremium: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_PREMIUM] ?: false
    }

    override suspend fun savePremiumStatus(isPremium: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_PREMIUM] = isPremium
        }
    }

    override val activeSleepStartAt: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_SLEEP_START_AT] ?: 0L
    }

    override suspend fun saveActiveSleepStartAt(startTime: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_SLEEP_START_AT] = startTime
        }
    }

    override val lastAppOpenAdShownAt: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_APP_OPEN_AD_SHOWN_AT] ?: 0L
    }

    override suspend fun saveLastAppOpenAdShownAt(time: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_APP_OPEN_AD_SHOWN_AT] = time
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}

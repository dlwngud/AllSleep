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
        private val KEY_BEDTIME = stringPreferencesKey("bedtime")
        private val KEY_WAKE_TIME = stringPreferencesKey("wake_time")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_ACTIVE_SLEEP_START_AT = longPreferencesKey("active_sleep_start_at")
        
        private const val DEFAULT_BEDTIME = "23:00"
        private const val DEFAULT_WAKE_TIME = "07:00"
    }

    override val activeSleepStartAt: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_SLEEP_START_AT] ?: 0L
    }

    override suspend fun saveActiveSleepStartAt(startTime: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_SLEEP_START_AT] = startTime
        }
    }
    
    override val deviceName: Flow<String?> = dataStore.data.map { preferences ->
// ... (rest of the file)
        preferences[KEY_DEVICE_NAME]
    }

    override suspend fun saveDeviceName(name: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DEVICE_NAME] = name
        }
    }

    override val bedtime: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_BEDTIME] ?: DEFAULT_BEDTIME
    }

    override val wakeTime: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_WAKE_TIME] ?: DEFAULT_WAKE_TIME
    }

    override val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    override val isPremium: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_PREMIUM] ?: false
    }

    override suspend fun savePremiumStatus(isPremium: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_PREMIUM] = isPremium
        }
    }

    override suspend fun saveSleepSchedule(bedtime: String, wakeTime: String) {
        dataStore.edit { preferences ->
            preferences[KEY_BEDTIME] = bedtime
            preferences[KEY_WAKE_TIME] = wakeTime
        }
    }

    override suspend fun saveOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}

package com.wngud.allsleep.data.source.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Android DataStore 초기화 헬퍼
 * Application.onCreate에서 호출하여 Context를 설정합니다.
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "allsleep_settings")

private var appContext: Context? = null

/**
 * Application Context를 설정합니다. (Application.onCreate에서 호출)
 */
fun initDataStore(context: Context) {
    appContext = context.applicationContext
}

internal fun getAndroidDataStore(): DataStore<Preferences> {
    return appContext?.dataStore
        ?: throw IllegalStateException("DataStore가 초기화되지 않았습니다. initDataStore(context)를 먼저 호출하세요.")
}

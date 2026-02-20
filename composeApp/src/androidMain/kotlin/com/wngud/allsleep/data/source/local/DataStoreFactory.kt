@file:JvmName("DataStoreFactoryAndroid")
package com.wngud.allsleep.data.source.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Android DataStore actual 구현
 * DataStoreInitializer에서 초기화된 인스턴스를 반환합니다.
 */
actual fun createDataStore(): DataStore<Preferences> = getAndroidDataStore()

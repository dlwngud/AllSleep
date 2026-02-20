package com.wngud.allsleep.data.source.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * DataStore 인스턴스를 생성하는 팩토리
 * 플랫폼별 구현 필요 (Android: Context, iOS: NSDocumentDirectory)
 */
internal const val DATA_STORE_FILE_NAME = "allsleep_settings.preferences_pb"

expect fun createDataStore(): DataStore<Preferences>

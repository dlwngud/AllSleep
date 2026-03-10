package com.wngud.allsleep.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.wngud.allsleep.domain.model.AppInfo
import com.wngud.allsleep.domain.repository.AppBlockerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.ByteArrayOutputStream

class AppBlockerRepositoryImpl(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) : AppBlockerRepository {

    private val packageManager: PackageManager = context.packageManager

    companion object {
        private val KEY_BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
    }

    override suspend fun getInstalledApps(): List<AppInfo> {
        val blockedPackages = observeBlockedPackages().first()
        
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
        
        return resolveInfos
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName } // 자기 자신 제외
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 } // 시스템 앱 제외
            .map { appInfo ->
                val label = packageManager.getApplicationLabel(appInfo).toString()
                
                AppInfo(
                    packageName = appInfo.packageName,
                    label = label,
                    iconBytes = getIconBytes(appInfo),
                    isSystemApp = false, // 이제 필터링되므로 무조건 false
                    isBlocked = blockedPackages.contains(appInfo.packageName)
                )
            }
            .sortedBy { it.label }
    }

    override fun observeBlockedPackages(): Flow<Set<String>> {
        return dataStore.data.map { preferences ->
            preferences[KEY_BLOCKED_PACKAGES] ?: emptySet()
        }
    }

    override suspend fun setAppBlocked(packageName: String, isBlocked: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[KEY_BLOCKED_PACKAGES] ?: emptySet()
            val next = if (isBlocked) {
                current + packageName
            } else {
                current - packageName
            }
            preferences[KEY_BLOCKED_PACKAGES] = next
        }
    }

    override suspend fun clearAllBlockedApps() {
        dataStore.edit { preferences ->
            preferences[KEY_BLOCKED_PACKAGES] = emptySet()
        }
    }

    private fun getIconBytes(appInfo: ApplicationInfo): ByteArray? {
        return try {
            val icon = packageManager.getApplicationIcon(appInfo)
            val bitmap = drawableToBitmap(icon)
            val stream = ByteArrayOutputStream()
            // 리스트에서 보여주기만 하므로 용량을 줄이기 위해 압축 및 크기 조절 고려 가능하지만 일단 원본 bitmap 사용
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

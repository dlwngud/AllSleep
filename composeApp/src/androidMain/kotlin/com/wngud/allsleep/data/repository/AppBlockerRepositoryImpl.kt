package com.wngud.allsleep.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.wngud.allsleep.domain.repository.AppBlockerRepository

class AppBlockerRepositoryImpl(
    context: Context
) : AppBlockerRepository {

    private val packageManager: PackageManager = context.packageManager

    override fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            // 패키지를 찾을 수 없는 경우 안전하게 false 반환 (차단 대상)
            false
        }
    }
}

package com.wngud.allsleep.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.Intent
import com.wngud.allsleep.domain.repository.AppBlockerRepository

class AppBlockerRepositoryImpl(
    context: Context
) : AppBlockerRepository {

    private val packageManager: PackageManager = context.packageManager

    override fun isSystemApp(packageName: String): Boolean {
        // 1. 무조건 허용할 필수 패키지 (전화, 알람, UI)
        val whitelist = listOf(
            "com.android.systemui",
            "com.samsung.android.incallui",
            "com.google.android.dialer",
            "com.android.server.telecom",
            "com.android.phone",
            "com.sec.android.app.clockpackage",
            "com.google.android.deskclock",
            "com.lge.clock",
            "com.wngud.allsleep"
        )
        if (whitelist.contains(packageName)) return true

        // 2. 런처(바탕화면) 및 설정 패키지는 무조건 차단 (시스템 앱 판별보다 우선)
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultLauncherPackage = resolveInfo?.activityInfo?.packageName

        if (packageName == defaultLauncherPackage) return false
        if (packageName == "com.android.settings" || packageName == "com.samsung.android.settings") return false

        // 3. 유튜브, 페이스북 등 사전 설치된 시스템 앱도 수면 모드에서는 차단 대상으로 간주
        // 따라서 FLAG_SYSTEM 체크를 제거하고 기본적으로 false를 반환하여 차단 로직이 작동하게 함
        return false
    }
}

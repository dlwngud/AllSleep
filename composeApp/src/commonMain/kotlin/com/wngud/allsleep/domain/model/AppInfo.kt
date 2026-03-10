package com.wngud.allsleep.domain.model

/**
 * 차단 설정 대상 앱 정보전달용 도메인 모델
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val iconBytes: ByteArray? = null,
    val isSystemApp: Boolean = false,
    val isBlocked: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppInfo) return false
        if (packageName != other.packageName) return false
        return true
    }

    override fun hashCode(): Int {
        return packageName.hashCode()
    }
}

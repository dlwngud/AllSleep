package com.wngud.allsleep.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun rememberBitmapFromBytes(bytes: ByteArray?): ImageBitmap? {
    // iOS용 앱 차단 기능은 현재 구현 범위 밖이므로 stub으로 남겨둡니다.
    return null
}

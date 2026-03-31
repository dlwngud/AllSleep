package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.repository.SleepSyncRepository

import com.wngud.allsleep.domain.model.exception.PremiumRequiredException

class RegisterDeviceUseCase(
    private val sleepSyncRepository: SleepSyncRepository
) {
    suspend operator fun invoke(uid: String, deviceState: DeviceState, isPremium: Boolean): Result<Unit> {
        if (!isPremium) {
            val devicesResult = sleepSyncRepository.getRegisteredDevices(uid)
            val currentDevices = devicesResult.getOrNull() ?: emptyList()
            
            // 기존 기기 중 현재 등록하려는 기기가 없다면 새로운 기기를 추가하는 것임
            val isNewDevice = currentDevices.none { it.deviceId == deviceState.deviceId }
            
            // 에버노트(Evernote) 방식의 엄격한 기기 제한 정책 도입
            // 차단 조건 1: 새 기기 추가인데 이미 할당량(1대)이 가득 찬 경우
            // 차단 조건 2: 기존 로그인된 기기이더라도, 유저가 현재 프리미엄이 아닌데 서버에 기기가 2대 이상 등록되어 있는 경우 (구독 만료자)
            if ((isNewDevice && currentDevices.isNotEmpty()) || currentDevices.size > 1) {
                return Result.failure(PremiumRequiredException("무료 버전에서는 1대의 기기만 연결할 수 있습니다."))
            }
        }
        
        return sleepSyncRepository.registerDevice(uid, deviceState)
    }
}

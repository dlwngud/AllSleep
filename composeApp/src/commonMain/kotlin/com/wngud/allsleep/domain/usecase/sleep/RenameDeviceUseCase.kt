package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.repository.SleepSyncRepository

/**
 * 기기 이름을 변경합니다.
 * 기존 registerDevice UseCase를 재활용하여 Firestore의 해당 기기 문서를 업데이트합니다.
 */
class RenameDeviceUseCase(
    private val sleepSyncRepository: com.wngud.allsleep.domain.repository.SleepSyncRepository,
    private val sleepSettingsRepository: com.wngud.allsleep.domain.repository.SleepSettingsRepository
) {
    suspend operator fun invoke(
        uid: String, 
        device: DeviceState, 
        newName: String, 
        allDevices: List<DeviceState>
    ): Result<Unit> {
        val trimmedName = newName.trim()
        
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("기기 이름은 비어 있을 수 없습니다."))
        }
        if (trimmedName.length > 20) {
            return Result.failure(IllegalArgumentException("기기 이름은 20자 이내여야 합니다."))
        }
        
        // [NEW] 중복 이름 체크 (현재 기기 제외)
        val isDuplicate = allDevices.any { 
            it.deviceId != device.deviceId && it.deviceName == trimmedName 
        }
        if (isDuplicate) {
            return Result.failure(IllegalArgumentException("이미 사용 중인 기기 이름입니다."))
        }

        // [NEW] 현재 기기와 이름이 같은 경우 불필요한 업데이트 방지
        if (trimmedName == device.deviceName) {
            return Result.success(Unit)
        }
        
        val updatedDevice = device.copy(deviceName = trimmedName)
        val result = sleepSyncRepository.registerDevice(uid, updatedDevice)
        
        // [NEW] 성공 시 로컬 캐시 업데이트
        if (result.isSuccess) {
            sleepSettingsRepository.saveDeviceName(trimmedName)
        }
        
        return result
    }
}

package com.wngud.allsleep.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.UserSleepState
import com.wngud.allsleep.domain.repository.SleepSyncRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SleepSyncRepositoryImpl(
    private val firestore: FirebaseFirestore
) : SleepSyncRepository {

    private val usersCollection = firestore.collection("users")

    override fun observeUserSleepState(uid: String): Flow<UserSleepState?> = callbackFlow {
        val listenerRegistration = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // 에러 발생 시 Flow 닫기
                    return@addSnapshotListener
                }
                
                // 데이터 변환 후 전송 (문서가 없으면 null)
                val state = snapshot?.toObject(UserSleepState::class.java)
                trySend(state)
            }
        
        // Flow 구독 취소 시 리스너 제거
        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun updateUserSleepState(
        uid: String, 
        isSleeping: Boolean, 
        targetWakeUpTime: Long?
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "isSleeping" to isSleeping,
            "lastUpdatedAt" to System.currentTimeMillis()
        )
        if (targetWakeUpTime != null) {
            updates["targetWakeUpTime"] = targetWakeUpTime
        } else {
            // null인 경우 명시적으로 필드 업데이트 (또는 유지 정책에 따라 분기 가능)
            updates["targetWakeUpTime"] = com.google.firebase.firestore.FieldValue.delete()
        }
        
        // 문서가 없을 수도 있으므로 set(..., SetOptions.merge())를 사용하는 것이 더 안전함
        usersCollection.document(uid)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    override suspend fun updateUserProfile(user: com.wngud.allsleep.domain.model.User): Result<Unit> = runCatching {
        val data = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "provider" to user.provider.name,
            "lastLoginAt" to System.currentTimeMillis()
        )
        user.email?.let { data["email"] = it }
        user.displayName?.let { data["displayName"] = it }
        user.photoUrl?.let { data["photoUrl"] = it }
        
        usersCollection.document(user.uid)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    override suspend fun registerDevice(uid: String, deviceState: DeviceState): Result<Unit> {
        
        return try {
            val data = hashMapOf(
                "deviceId" to deviceState.deviceId,
                "deviceName" to deviceState.deviceName,
                "fcmToken" to deviceState.fcmToken,
                "platform" to deviceState.platform,
                "lastActiveForSleepLocking" to deviceState.lastActiveForSleepLocking,
                "isMainAlarmDevice" to deviceState.isMainAlarmDevice
            )
            
            usersCollection.document(uid).collection("devices").document(deviceState.deviceId)
                .set(data)
                .await()
                
            Result.success(Unit)
        } catch (e: Throwable) {
            android.util.Log.e("SleepSyncRepo", "registerDevice 에러 발생: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getRegisteredDevices(uid: String): Result<List<DeviceState>> = runCatching {
        val snapshot = usersCollection.document(uid).collection("devices").get().await()
        
        snapshot.documents.map { doc ->
            DeviceState(
                deviceId = doc.getString("deviceId") ?: "",
                deviceName = doc.getString("deviceName") ?: "",
                fcmToken = doc.getString("fcmToken") ?: "",
                platform = doc.getString("platform") ?: "Android",
                lastActiveForSleepLocking = doc.getLong("lastActiveForSleepLocking") ?: 0L,
                isMainAlarmDevice = doc.getBoolean("isMainAlarmDevice") ?: false
            )
        }
    }

    override suspend fun setMainAlarmDevice(uid: String, deviceId: String): Result<Unit> = runCatching {
        val devicesRef = usersCollection.document(uid).collection("devices")
        val snapshot = devicesRef.get().await()
        
        // 트랜잭션 대신 batch를 이용해 모든 기기의 상태를 일괄 업데이트
        firestore.runBatch { batch ->
            for (doc in snapshot.documents) {
                val isTarget = doc.id == deviceId
                batch.update(doc.reference, "isMainAlarmDevice", isTarget)
            }
        }.await()
    }

    override suspend fun unregisterDevice(uid: String, deviceId: String): Result<Unit> = runCatching {
        usersCollection.document(uid).collection("devices").document(deviceId)
            .delete()
            .await()
    }

    override fun observeRegisteredDevices(uid: String): Flow<List<DeviceState>> = callbackFlow {
        val listenerRegistration = usersCollection.document(uid).collection("devices")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val devices = snapshot?.documents?.map { doc ->
                    DeviceState(
                        deviceId = doc.getString("deviceId") ?: "",
                        deviceName = doc.getString("deviceName") ?: "",
                        fcmToken = doc.getString("fcmToken") ?: "",
                        platform = doc.getString("platform") ?: "Android",
                        lastActiveForSleepLocking = doc.getLong("lastActiveForSleepLocking") ?: 0L,
                        isMainAlarmDevice = doc.getBoolean("isMainAlarmDevice") ?: false
                    )
                } ?: emptyList()
                
                trySend(devices)
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }
}

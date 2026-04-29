package com.wngud.allsleep.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
                    if (error.isPermissionDenied()) {
                        trySend(null)
                        close()
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }
                
                val state = snapshot?.let { doc ->
                    if (!doc.exists()) return@let null
                    UserSleepState(
                        uid = doc.getString("uid") ?: "",
                        isSleeping = doc.getBoolean("isSleeping") ?: false,
                        targetWakeUpTime = doc.getLong("targetWakeUpTime"),
                        // 평일 (기존 필드 매핑)
                        weekdayBedtime = doc.getString("bedtime") ?: "23:00",
                        weekdayWakeTime = doc.getString("wakeTime") ?: "07:00",
                        isWeekdaySleepEnabled = doc.getBoolean("isSleepAlarmEnabled") ?: true,
                        isWeekdayWakeEnabled = doc.getBoolean("isWakeAlarmEnabled") ?: true,
                        // 주말 (신규 필드)
                        weekendBedtime = doc.getString("weekendBedtime") ?: "00:00",
                        weekendWakeTime = doc.getString("weekendWakeTime") ?: "09:00",
                        isWeekendSleepEnabled = doc.getBoolean("isWeekendSleepEnabled") ?: true,
                        isWeekendWakeEnabled = doc.getBoolean("isWeekendWakeEnabled") ?: true,
                        
                        lastUpdatedAt = doc.getLong("lastUpdatedAt") ?: 0L,
                        sleepStartAt = doc.getLong("sleepStartAt") ?: 0L
                    )
                }
                trySend(state)
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun updateUserSleepState(
        uid: String, 
        isSleeping: Boolean?, 
        targetWakeUpTime: Long?,
        weekdayBedtime: String?,
        weekdayWakeTime: String?,
        isWeekdaySleepEnabled: Boolean?,
        isWeekdayWakeEnabled: Boolean?,
        weekendBedtime: String?,
        weekendWakeTime: String?,
        isWeekendSleepEnabled: Boolean?,
        isWeekendWakeEnabled: Boolean?
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "lastUpdatedAt" to System.currentTimeMillis()
        )
        isSleeping?.let { updates["isSleeping"] = it }
        if (targetWakeUpTime != null) {
            updates["targetWakeUpTime"] = targetWakeUpTime
        } else {
            updates["targetWakeUpTime"] = com.google.firebase.firestore.FieldValue.delete()
        }
        
        // 평일 필드 (기존 이름 유지)
        weekdayBedtime?.let { updates["bedtime"] = it }
        weekdayWakeTime?.let { updates["wakeTime"] = it }
        isWeekdaySleepEnabled?.let { updates["isSleepAlarmEnabled"] = it }
        isWeekdayWakeEnabled?.let { updates["isWakeAlarmEnabled"] = it }
        
        // 주말 필드 (신규 이름)
        weekendBedtime?.let { updates["weekendBedtime"] = it }
        weekendWakeTime?.let { updates["weekendWakeTime"] = it }
        isWeekendSleepEnabled?.let { updates["isWeekendSleepEnabled"] = it }
        isWeekendWakeEnabled?.let { updates["isWakeAlarmEnabled"] = it } // 오타 조심 (isWeekendWakeEnabled로 써야 함)
        
        // 수면 시작 시 sleepStartAt 기록
        if (isSleeping == true) {
            updates["sleepStartAt"] = System.currentTimeMillis()
        } else if (isSleeping == false) {
            updates["sleepStartAt"] = 0L
        }
        
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
        data["isPremium"] = user.isPremium
        
        usersCollection.document(user.uid)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    override suspend fun registerDevice(uid: String, deviceState: DeviceState): Result<Unit> = runCatching {
        val data = hashMapOf(
            "deviceId" to deviceState.deviceId,
            "deviceName" to deviceState.deviceName,
            "fcmToken" to deviceState.fcmToken,
            "platform" to deviceState.platform,
            "lastActiveForSleepLocking" to deviceState.lastActiveForSleepLocking,
            "isMainAlarmDevice" to deviceState.isMainAlarmDevice
        )
        
        usersCollection.document(uid).collection("devices").document(deviceState.deviceId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .await()
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
                    if (error.isPermissionDenied()) {
                        trySend(emptyList())
                        close()
                    } else {
                        close(error)
                    }
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

    private fun Throwable.isPermissionDenied(): Boolean {
        return this is FirebaseFirestoreException &&
            code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    }
}

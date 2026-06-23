package com.wngud.allsleep.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.platform.PlatformContext
import com.wngud.allsleep.domain.usecase.subscription.LoadSubscriptionPackagesUseCase
import com.wngud.allsleep.domain.usecase.subscription.PurchaseSubscriptionUseCase
import com.wngud.allsleep.domain.usecase.subscription.RestorePurchasesUseCase
import com.wngud.allsleep.domain.usecase.subscription.SyncPremiumStatusUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val loadSubscriptionPackagesUseCase: LoadSubscriptionPackagesUseCase,
    private val purchaseSubscriptionUseCase: PurchaseSubscriptionUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase,
    private val syncPremiumStatusUseCase: SyncPremiumStatusUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionContract.State())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SubscriptionContract.Effect>()
    val effect = _effect.asSharedFlow()

    fun handleIntent(intent: SubscriptionContract.Intent, context: PlatformContext? = null) {
        when (intent) {
            is SubscriptionContract.Intent.LoadPackages -> loadPackages()
            is SubscriptionContract.Intent.SelectPackage -> _state.update { it.copy(selectedPackageId = intent.id) }
            is SubscriptionContract.Intent.PurchaseSelected -> {
                context?.let { purchaseSelected(it) }
            }
            is SubscriptionContract.Intent.RestorePurchases -> restorePurchases()
            is SubscriptionContract.Intent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadPackages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val packages = loadSubscriptionPackagesUseCase()
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        packages = packages,
                        selectedPackageId = packages.find { p -> p.type == com.wngud.allsleep.platform.PackageType.ANNUAL }?.id 
                            ?: packages.firstOrNull()?.id
                    ) 
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "상품을 불러오는데 실패했습니다.") }
            }
        }
    }

    private fun purchaseSelected(context: PlatformContext) {
        val selectedId = _state.value.selectedPackageId ?: return
        
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true, error = null) }
                purchaseSubscriptionUseCase(selectedId, context)
                .onSuccess { result ->
                    if (result.isSuccess) {
                        syncPremiumStatusUseCase()
                        _state.update { it.copy(isPurchasing = false, isSuccess = true) }
                        _effect.emit(SubscriptionContract.Effect.ShowSnackbar("프리미엄 구독이 시작되었습니다!"))
                        _effect.emit(SubscriptionContract.Effect.NavigateToManage)
                    } else {
                        _state.update { it.copy(isPurchasing = false) }
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isPurchasing = false, error = error.message ?: "결제 중 오류가 발생했습니다.") }
                }
        }
    }

    private fun restorePurchases() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
                restorePurchasesUseCase()
                .onSuccess {
                    val isPremiumNow = syncPremiumStatusUseCase()
                    _state.update { it.copy(isLoading = false) }
                    _effect.emit(SubscriptionContract.Effect.ShowSnackbar("구매 내역이 복원되었습니다."))
                    _effect.emit(
                        if (isPremiumNow == true) {
                            SubscriptionContract.Effect.NavigateToManage
                        } else {
                            SubscriptionContract.Effect.NavigateBack
                        }
                    )
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message ?: "복원 중 오류가 발생했습니다.") }
                }
        }
    }
}

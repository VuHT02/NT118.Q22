package com.example.citymove.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.citymove.data.model.HomeData
import com.example.citymove.data.model.RewardItem
import com.example.citymove.data.model.Transaction
import com.example.citymove.data.model.UserProfile
import com.example.citymove.data.repository.AccountRepository
import com.example.citymove.data.repository.HomeRepository
import kotlinx.coroutines.launch

sealed class AccountUiState {
    object Loading : AccountUiState()
    data class Success(val profile: UserProfile) : AccountUiState()
    data class Error(val message: String) : AccountUiState()
}

class AccountViewModel(
    private val repository: AccountRepository = AccountRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<AccountUiState>(AccountUiState.Loading)
    val uiState: LiveData<AccountUiState> = _uiState

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _rewards = MutableLiveData<List<RewardItem>>()
    val rewards: LiveData<List<RewardItem>> = _rewards

    fun loadProfile() {
        _uiState.value = AccountUiState.Loading
        viewModelScope.launch {
            repository.getUserProfile()
                .onSuccess { _uiState.value = AccountUiState.Success(it) }
                .onFailure { _uiState.value = AccountUiState.Error(it.message ?: "Lỗi tải thông tin") }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            repository.getTransactions()
                .onSuccess { _transactions.value = it }
        }
    }

    fun loadRewards() {
        viewModelScope.launch {
            repository.getAvailableRewards()
                .onSuccess { _rewards.value = it }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val data: HomeData) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: HomeRepository = HomeRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<HomeUiState>(HomeUiState.Loading)
    val uiState: LiveData<HomeUiState> = _uiState

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            repository.getHomeData()
                .onSuccess { _uiState.value = HomeUiState.Success(it) }
                .onFailure { _uiState.value = HomeUiState.Error(it.message ?: "Lỗi không xác định") }
        }
    }
}
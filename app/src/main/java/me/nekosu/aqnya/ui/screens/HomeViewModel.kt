package me.nekosu.aqnya.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.nekosu.aqnya.ncore
import me.nekosu.aqnya.util.RootDbHelper
import me.nekosu.aqnya.util.RuleDbHelper

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val _installStatus = MutableStateFlow(InstallStatus.CHECKING)
    val installStatus: StateFlow<InstallStatus> = _installStatus

    private val _suCount = MutableStateFlow(0)
    val suCount: StateFlow<Int> = _suCount

    private val _ruleCount = MutableStateFlow(0)
    val ruleCount: StateFlow<Int> = _ruleCount

    init {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { ncore().ctl(1) }
            if (result == 0) {
                withContext(Dispatchers.IO) { ncore().ctl(3) }
                withContext(Dispatchers.IO) {
                    val ctx = app.applicationContext
                    _suCount.value = RootDbHelper(ctx).getAllowedCount()
                    _ruleCount.value = RuleDbHelper(ctx).getCount()
                }
                _installStatus.value = InstallStatus.INSTALLED
            } else {
                _installStatus.value = InstallStatus.NOT_INSTALLED
            }
        }
    }
}

class HomeViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(app) as T
}

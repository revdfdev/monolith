package com.rehan.monolith.ui.home

import androidx.lifecycle.ViewModel
import com.rehan.monolith.data.AppEntry
import com.rehan.monolith.data.AppRepository
import com.rehan.monolith.data.LauncherPrefs
import com.rehan.monolith.data.MonolithSettings
import com.rehan.monolith.shizuku.ShizukuManager
import com.rehan.monolith.shizuku.ShizukuStatus
import kotlinx.coroutines.flow.StateFlow

class LauncherViewModel(
    prefs: LauncherPrefs,
    appRepository: AppRepository,
    shizuku: ShizukuManager
) : ViewModel() {
    val settings: StateFlow<MonolithSettings> = prefs.settings
    val apps: StateFlow<List<AppEntry>> = appRepository.apps
    val shizukuStatus: StateFlow<ShizukuStatus> = shizuku.status
}

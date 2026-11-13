package com.rehan.monolith.shizuku

import kotlinx.coroutines.flow.StateFlow

interface ShizukuOps {
    val status: StateFlow<ShizukuState>
    fun requestPermission()
    suspend fun isGrayscaleEnabled(): Boolean
    suspend fun getStayAwake(): Boolean
    suspend fun isDozeAlwaysOn(): Boolean
    suspend fun getLockscreenNotificationMode(): String
    suspend fun setGrayscale(enabled: Boolean): ShellResult
    suspend fun setStayAwake(enabled: Boolean): ShellResult
    suspend fun setDozeAlwaysOn(enabled: Boolean): ShellResult
    suspend fun setLockscreenNotificationMode(mode: String): ShellResult
    suspend fun setAnimationScale(scale: Float): ShellResult
    suspend fun setEnabled(pkgs: List<String>, enabled: Boolean): ShellResult
    suspend fun freezePackage(pkg: String): ShellResult
    suspend fun unfreezePackage(pkg: String): ShellResult
    suspend fun forceStop(pkg: String): ShellResult
    suspend fun trimAllCaches(): ShellResult
    suspend fun setDnd(on: Boolean): ShellResult
    suspend fun allowUsageStats(): ShellResult
}

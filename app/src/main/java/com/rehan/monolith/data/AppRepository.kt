package com.rehan.monolith.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppEntry(
    val packageName: String,
    val label: String,
    val isFrozen: Boolean,
    val isSystem: Boolean,
    val icon: ImageBitmap?
)

/** Single source of the installed-app list; reloads on package changes (Olauncher pattern). */
class AppRepository(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val frozenPackages: suspend () -> Set<String>,
    private val loader: suspend (frozen: Set<String>, emit: (List<AppEntry>) -> Unit) -> Unit
) {
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private var pending: Job? = null

    fun refresh() = schedule(0)

    fun onPackagesChanged() = schedule(DEBOUNCE_MS)

    private fun schedule(delayMs: Long) {
        pending?.cancel()
        pending = scope.launch(dispatcher) {
            delay(delayMs)
            val frozen = frozenPackages()
            loader(frozen) { _apps.value = it }
        }
    }

    fun registerPackageCallback(context: Context) {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return
        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) = onPackagesChanged()
            override fun onPackageRemoved(packageName: String, user: UserHandle) = onPackagesChanged()
            override fun onPackageChanged(packageName: String, user: UserHandle) = onPackagesChanged()
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onPackagesChanged()
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onPackagesChanged()
        })
    }

    companion object {
        private const val DEBOUNCE_MS = 300L
    }
}

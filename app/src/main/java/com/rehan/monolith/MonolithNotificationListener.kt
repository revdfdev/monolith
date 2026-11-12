package com.rehan.monolith

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationSummary(
    val count: Int = 0,
    val packages: Set<String> = emptySet()
)

class MonolithNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateSummary()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateSummary()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateSummary()
    }

    private fun updateSummary() {
        try {
            val active = activeNotifications ?: emptyArray()
            val filtered = active.filter { sbn ->
                !sbn.isOngoing && sbn.packageName != packageName
            }
            val pkgs = filtered.map { it.packageName }.toSet()
            _summaryFlow.value = NotificationSummary(
                count = filtered.size,
                packages = pkgs
            )
        } catch (_: Exception) {
            // Service disconnected or query issue
        }
    }

    companion object {
        private val _summaryFlow = MutableStateFlow(NotificationSummary())
        val summaryFlow: StateFlow<NotificationSummary> = _summaryFlow.asStateFlow()
    }
}

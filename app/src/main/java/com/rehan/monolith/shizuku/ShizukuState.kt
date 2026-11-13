package com.rehan.monolith.shizuku

sealed interface ShizukuState {
    data object Unavailable : ShizukuState
    data object NoPermission : ShizukuState
    data object Binding : ShizukuState
    data class Ready(val uid: Int) : ShizukuState

    val isReady: Boolean get() = this is Ready
}

sealed interface ShizukuEvent {
    data class Refresh(val binderAlive: Boolean, val granted: Boolean) : ShizukuEvent
    data class PermissionResult(val granted: Boolean) : ShizukuEvent
    data class ServiceConnected(val uid: Int) : ShizukuEvent
    data class ServiceDisconnected(val binderAlive: Boolean, val granted: Boolean) : ShizukuEvent
    data object BinderDead : ShizukuEvent
}

data class ShizukuTransition(val state: ShizukuState, val bind: Boolean = false)

private fun fromBinder(current: ShizukuState, alive: Boolean, granted: Boolean): ShizukuTransition = when {
    !alive -> ShizukuTransition(ShizukuState.Unavailable)
    !granted -> ShizukuTransition(ShizukuState.NoPermission)
    current is ShizukuState.Ready || current is ShizukuState.Binding -> ShizukuTransition(current)
    else -> ShizukuTransition(ShizukuState.Binding, bind = true)
}

fun reduceShizuku(state: ShizukuState, event: ShizukuEvent): ShizukuTransition = when (event) {
    is ShizukuEvent.Refresh -> fromBinder(state, event.binderAlive, event.granted)
    is ShizukuEvent.PermissionResult ->
        if (state == ShizukuState.Unavailable) ShizukuTransition(state)
        else fromBinder(state, alive = true, granted = event.granted)
    is ShizukuEvent.ServiceConnected ->
        if (state == ShizukuState.Binding) ShizukuTransition(ShizukuState.Ready(event.uid))
        else ShizukuTransition(state)
    is ShizukuEvent.ServiceDisconnected ->
        fromBinder(ShizukuState.Unavailable, event.binderAlive, event.granted)
    ShizukuEvent.BinderDead -> ShizukuTransition(ShizukuState.Unavailable)
}

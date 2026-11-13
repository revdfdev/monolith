package com.rehan.monolith

import com.rehan.monolith.shizuku.ShizukuEvent
import com.rehan.monolith.shizuku.ShizukuState
import com.rehan.monolith.shizuku.reduceShizuku
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuStateMachineTest {

    private fun step(state: ShizukuState, event: ShizukuEvent) = reduceShizuku(state, event)

    @Test
    fun `GIVEN binder arrives without permission THEN NoPermission and no bind`() {
        val t = step(ShizukuState.Unavailable, ShizukuEvent.Refresh(binderAlive = true, granted = false))
        assertEquals(ShizukuState.NoPermission, t.state)
        assertFalse(t.bind)
    }

    @Test
    fun `GIVEN binder arrives with permission THEN Binding and bind requested`() {
        val t = step(ShizukuState.Unavailable, ShizukuEvent.Refresh(binderAlive = true, granted = true))
        assertEquals(ShizukuState.Binding, t.state)
        assertTrue(t.bind)
    }

    @Test
    fun `GIVEN no binder THEN Unavailable`() {
        val t = step(ShizukuState.Ready(2000), ShizukuEvent.Refresh(binderAlive = false, granted = false))
        assertEquals(ShizukuState.Unavailable, t.state)
        assertFalse(t.bind)
    }

    @Test
    fun `GIVEN NoPermission WHEN permission granted THEN Binding and bind`() {
        val t = step(ShizukuState.NoPermission, ShizukuEvent.PermissionResult(granted = true))
        assertEquals(ShizukuState.Binding, t.state)
        assertTrue(t.bind)
    }

    @Test
    fun `GIVEN NoPermission WHEN permission denied THEN stays NoPermission`() {
        val t = step(ShizukuState.NoPermission, ShizukuEvent.PermissionResult(granted = false))
        assertEquals(ShizukuState.NoPermission, t.state)
        assertFalse(t.bind)
    }

    @Test
    fun `GIVEN Binding WHEN service connects THEN Ready with uid`() {
        val t = step(ShizukuState.Binding, ShizukuEvent.ServiceConnected(uid = 2000))
        assertEquals(ShizukuState.Ready(2000), t.state)
        assertTrue(t.state.isReady)
    }

    @Test
    fun `GIVEN Ready WHEN binder dies THEN Unavailable`() {
        val t = step(ShizukuState.Ready(2000), ShizukuEvent.BinderDead)
        assertEquals(ShizukuState.Unavailable, t.state)
        assertFalse(t.state.isReady)
    }

    @Test
    fun `GIVEN Ready WHEN user service dies but binder alive THEN rebinds`() {
        val t = step(ShizukuState.Ready(2000), ShizukuEvent.ServiceDisconnected(binderAlive = true, granted = true))
        assertEquals(ShizukuState.Binding, t.state)
        assertTrue(t.bind)
    }

    @Test
    fun `GIVEN Ready WHEN user service dies and binder gone THEN Unavailable without bind`() {
        val t = step(ShizukuState.Ready(2000), ShizukuEvent.ServiceDisconnected(binderAlive = false, granted = false))
        assertEquals(ShizukuState.Unavailable, t.state)
        assertFalse(t.bind)
    }

    @Test
    fun `GIVEN Ready or Binding WHEN refreshed THEN no duplicate bind`() {
        val ready = step(ShizukuState.Ready(2000), ShizukuEvent.Refresh(binderAlive = true, granted = true))
        assertEquals(ShizukuState.Ready(2000), ready.state)
        assertFalse(ready.bind)
        val binding = step(ShizukuState.Binding, ShizukuEvent.Refresh(binderAlive = true, granted = true))
        assertEquals(ShizukuState.Binding, binding.state)
        assertFalse(binding.bind)
    }

    @Test
    fun `GIVEN Ready WHEN permission revoked on refresh THEN NoPermission`() {
        val t = step(ShizukuState.Ready(2000), ShizukuEvent.Refresh(binderAlive = true, granted = false))
        assertEquals(ShizukuState.NoPermission, t.state)
        assertFalse(t.bind)
    }

    @Test
    fun `GIVEN Unavailable WHEN a stale service connection arrives THEN ignored`() {
        val t = step(ShizukuState.Unavailable, ShizukuEvent.ServiceConnected(uid = 2000))
        assertEquals(ShizukuState.Unavailable, t.state)
    }
}

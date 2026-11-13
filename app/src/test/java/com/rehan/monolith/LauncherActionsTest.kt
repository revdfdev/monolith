package com.rehan.monolith

import androidx.test.core.app.ApplicationProvider
import com.rehan.monolith.data.AppRepository
import com.rehan.monolith.data.InstalledCheck
import com.rehan.monolith.data.LauncherPrefs
import com.rehan.monolith.shizuku.ShellResult
import com.rehan.monolith.shizuku.ShizukuOps
import com.rehan.monolith.shizuku.ShizukuState
import com.rehan.monolith.ui.home.LauncherViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val OK = ShellResult(0, "")
private fun fail(msg: String) = ShellResult(1, msg)

class FakeShizuku : ShizukuOps {
    override val status = MutableStateFlow<ShizukuState>(ShizukuState.Ready(2000))
    val calls = mutableListOf<String>()
    var result: ShellResult = OK
    var stayAwakeOn = false
    var permissionRequested = false

    private fun rec(c: String): ShellResult { calls += c; return result }
    override fun requestPermission() { permissionRequested = true }
    override suspend fun isGrayscaleEnabled() = false
    override suspend fun getStayAwake() = stayAwakeOn
    override suspend fun isDozeAlwaysOn() = false
    override suspend fun getLockscreenNotificationMode() = "FULL"
    override suspend fun setGrayscale(enabled: Boolean) = rec("grayscale $enabled")
    override suspend fun setStayAwake(enabled: Boolean) = rec("stayAwake $enabled")
    override suspend fun setDozeAlwaysOn(enabled: Boolean) = rec("doze $enabled")
    override suspend fun setLockscreenNotificationMode(mode: String) = rec("lock $mode")
    override suspend fun setAnimationScale(scale: Float) = rec("anim $scale")
    override suspend fun setEnabled(pkgs: List<String>, enabled: Boolean) = rec("enabled $enabled $pkgs")
    override suspend fun freezePackage(pkg: String) = rec("freeze $pkg")
    override suspend fun unfreezePackage(pkg: String) = rec("unfreeze $pkg")
    override suspend fun forceStop(pkg: String) = rec("stop $pkg")
    override suspend fun trimAllCaches() = rec("trim")
    override suspend fun setDnd(on: Boolean) = rec("dnd $on")
    override suspend fun allowUsageStats() = rec("usage")
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherActionsTest {

    private lateinit var fake: FakeShizuku
    private lateinit var vm: LauncherViewModel
    private val messages = mutableListOf<String>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fake = FakeShizuku()
    }

    private fun build(installed: Set<String> = emptySet()): LauncherViewModel {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = AppRepository(CoroutineScope(Dispatchers.Main), Dispatchers.Main, { emptySet() }, { _, _ -> })
        vm = LauncherViewModel(LauncherPrefs(ctx), repo, fake, InstalledCheck { it in installed })
        CoroutineScope(UnconfinedTestDispatcher()).launch { vm.messages.collect { messages += it } }
        return vm
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `GIVEN Shizuku Ready WHEN view model starts THEN toggles show the real device values`() {
        fake.stayAwakeOn = true
        build()
        assertTrue(vm.tweaks.value.stayAwake)
    }

    @Test
    fun `GIVEN Shizuku connects later THEN toggles are read on connect`() {
        fake.status.value = ShizukuState.Binding
        fake.stayAwakeOn = true
        build()
        assertFalse(vm.tweaks.value.stayAwake)
        fake.status.value = ShizukuState.Ready(2000)
        assertTrue(vm.tweaks.value.stayAwake)
    }

    @Test
    fun `GIVEN the service fails WHEN toggling stay awake THEN the error is shown and the toggle stays off`() {
        build()
        fake.result = fail("Permission denial")
        vm.setStayAwake(true)
        assertFalse(vm.tweaks.value.stayAwake)
        assertTrue(messages.last(), messages.last().contains("Permission denial"))
    }

    @Test
    fun `GIVEN the service succeeds WHEN toggling stay awake THEN the toggle turns on`() {
        build()
        vm.setStayAwake(true)
        assertTrue(vm.tweaks.value.stayAwake)
        assertEquals(listOf("stayAwake true"), fake.calls)
    }

    @Test
    fun `GIVEN only some installers are installed WHEN locking THEN only those are disabled`() {
        build(installed = setOf("com.android.vending"))
        vm.setInstallersLocked(true)
        assertEquals(listOf("enabled false [com.android.vending]"), fake.calls)
        assertTrue(vm.tweaks.value.installersLocked)
    }

    @Test
    fun `GIVEN locking fails THEN the error is shown and the lock stays off`() {
        build(installed = setOf("com.android.vending"))
        fake.result = fail("Unknown package")
        vm.setInstallersLocked(true)
        assertFalse(vm.tweaks.value.installersLocked)
        assertTrue(messages.last().contains("Unknown package"))
    }

    @Test
    fun `GIVEN Shizuku not connected WHEN an action runs THEN nothing is called and permission is requested`() {
        fake.status.value = ShizukuState.NoPermission
        build()
        vm.setGrayscale(true)
        assertTrue(fake.calls.isEmpty())
        assertTrue(fake.permissionRequested)
        assertTrue(messages.last().contains("Shizuku"))
    }

    @Test
    fun `GIVEN freezing fails WHEN freezing an app THEN the error is shown not success`() {
        build()
        fake.result = fail("Cannot disable")
        vm.setFrozen("a.b", true)
        assertTrue(messages.last().contains("Cannot disable"))
    }

    @Test
    fun `GIVEN trim caches fails THEN the error is shown`() {
        build()
        fake.result = fail("boom")
        vm.trimCaches()
        assertTrue(messages.last().contains("boom"))
    }
}

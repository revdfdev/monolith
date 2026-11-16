package com.rehan.monolith

import androidx.test.core.app.ApplicationProvider
import com.rehan.monolith.data.AppRepository
import com.rehan.monolith.data.InstalledCheck
import com.rehan.monolith.data.LauncherPrefs
import com.rehan.monolith.shizuku.ShellResult
import com.rehan.monolith.shizuku.ShizukuOps
import com.rehan.monolith.shizuku.ShizukuState
import com.rehan.monolith.ui.home.LauncherViewModel
import com.rehan.monolith.ui.home.PackageInstallState
import com.rehan.monolith.ui.install.PackageInstallTracker
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
    override suspend fun installApk(apkPath: String, flags: List<String>) = rec("install $apkPath $flags")
    override suspend fun installSplitApks(apkPaths: List<String>, flags: List<String>) = rec("installSplit $apkPaths $flags")
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherActionsTest {

    private lateinit var fake: FakeShizuku
    private lateinit var vm: LauncherViewModel
    private lateinit var prefs: LauncherPrefs
    private val messages = mutableListOf<String>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fake = FakeShizuku()
        PackageInstallTracker.reset()
    }

    private fun build(installed: Set<String> = emptySet()): LauncherViewModel {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = AppRepository(CoroutineScope(Dispatchers.Main), Dispatchers.Main, { emptySet() }, { _, _ -> })
        val testDispatcher = UnconfinedTestDispatcher()
        prefs = LauncherPrefs(ctx)
        vm = LauncherViewModel(prefs, repo, fake, InstalledCheck { it in installed }, testDispatcher)
        CoroutineScope(testDispatcher).launch { vm.messages.collect { messages += it } }
        return vm
    }

    @After
    fun tearDown() {
        PackageInstallTracker.reset()
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

    @Test
    fun `GIVEN ready Shizuku WHEN installPackage called THEN runs install and state transitions to Success`() {
        build()
        val tempApk = java.io.File.createTempFile("test_apk", ".apk")
        tempApk.writeText("dummy content")

        vm.installPackage(tempApk.absolutePath, "Test App", "com.test.app", listOf("-r", "-d"))

        assertEquals("install ${tempApk.absolutePath} [-r, -d]", fake.calls.last())
        assertTrue(vm.packageInstallState.value is PackageInstallState.Success)
        val success = vm.packageInstallState.value as PackageInstallState.Success
        assertEquals("Test App", success.appLabel)
        assertEquals("com.test.app", success.packageName)
        assertTrue(messages.last().contains("Installed Test App"))
        assertFalse(tempApk.exists())
    }

    @Test
    fun `GIVEN install fails with downgrade error THEN error state has human readable reason`() {
        build()
        val tempApk = java.io.File.createTempFile("test_apk", ".apk")
        fake.result = fail("Failure [INSTALL_FAILED_VERSION_DOWNGRADE]")

        vm.installPackage(tempApk.absolutePath, "Test App", "com.test.app", listOf("-r"))

        assertTrue(vm.packageInstallState.value is PackageInstallState.Error)
        val err = vm.packageInstallState.value as PackageInstallState.Error
        assertEquals("Test App", err.appLabel)
        assertTrue(err.reason.contains("Existing version is newer"))
    }

    @Test
    fun `GIVEN Shizuku not connected WHEN installPackage called THEN requests permission without calling install`() {
        fake.status.value = ShizukuState.NoPermission
        build()

        vm.installPackage("/tmp/fake.apk", "Test App", "com.test.app", listOf("-r"))

        assertTrue(fake.calls.isEmpty())
        assertTrue(fake.permissionRequested)
        assertEquals(PackageInstallState.Idle, vm.packageInstallState.value)
    }

    @Test
    fun `GIVEN focus active WHEN Shizuku offline THEN turning focus off succeeds locally`() {
        build()
        vm.toggleFocus() // turn on while ready
        assertTrue(vm.settings.value.focusModeActive)

        // Shizuku goes offline
        fake.status.value = ShizukuState.NoPermission
        vm.toggleFocus() // turn off
        assertFalse(vm.settings.value.focusModeActive)
        assertFalse(vm.tweaks.value.grayscale)
    }

    @Test
    fun `GIVEN mindful app cooldown started WHEN checked within window THEN cooldown is active`() {
        build()
        assertFalse(vm.isMindfulCooldownActive("com.distraction.app"))
        vm.startMindfulCooldown("com.distraction.app", durationMinutes = 15)
        assertTrue(vm.isMindfulCooldownActive("com.distraction.app"))
    }

    @Test
    fun `GIVEN mindful app launched WHEN Shizuku ready THEN temporary monochrome is enabled and restored on resume`() {
        build()
        prefs.toggleMindfulPause("com.social.app")

        // Mindful app launch triggers temporary grayscale
        vm.onAppLaunchRequested("com.social.app")
        assertEquals(listOf("grayscale true"), fake.calls)

        // Returning to launcher restores color
        vm.onLauncherResumed()
        assertEquals(listOf("grayscale true", "grayscale false"), fake.calls)
    }
}

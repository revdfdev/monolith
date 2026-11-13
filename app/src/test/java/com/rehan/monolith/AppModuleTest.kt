package com.rehan.monolith

import com.rehan.monolith.data.LauncherPrefs
import com.rehan.monolith.shizuku.ShizukuManager
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = MonolithApp::class)
class AppModuleTest {

    @After
    fun tearDown() = stopKoin()

    @Test
    fun `GIVEN app started WHEN resolving singletons twice THEN the same app-scoped instances are returned`() {
        val koin = GlobalContext.get()
        assertSame(koin.get<LauncherPrefs>(), koin.get<LauncherPrefs>())
        assertSame(koin.get<ShizukuManager>(), koin.get<ShizukuManager>())
    }
}

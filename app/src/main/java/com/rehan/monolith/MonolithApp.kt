package com.rehan.monolith

import android.app.Application
import com.rehan.monolith.data.AppRepository
import com.rehan.monolith.data.LauncherPrefs
import com.rehan.monolith.data.loadInstalledApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.rehan.monolith.ui.home.LauncherViewModel
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModelOf
import com.rehan.monolith.shizuku.ShizukuManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    single { LauncherPrefs(get()) }
    single(createdAtStart = true) { ShizukuManager().apply { init() } }
    single {
        val shizuku = get<ShizukuManager>()
        val context = androidContext()
        AppRepository(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
            dispatcher = Dispatchers.Default,
            frozenPackages = { if (shizuku.status.value.isReady) shizuku.getFrozenPackages() else emptySet() },
            loader = { frozen, emit -> loadInstalledApps(context, frozen, emit) }
        ).apply { registerPackageCallback(context) }
    }
    viewModelOf(::LauncherViewModel)
}

class MonolithApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MonolithApp)
            modules(appModule)
        }
    }
}

package com.rehan.monolith

import android.app.Application
import com.rehan.monolith.data.LauncherPrefs
import com.rehan.monolith.shizuku.ShizukuManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    single { LauncherPrefs(get()) }
    single { ShizukuManager().apply { init() } }
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

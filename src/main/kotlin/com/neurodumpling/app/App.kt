package com.neurodumpling.app


import com.neurodumpling.app.model.AppArgs
import com.neurodumpling.app.ui.feature.MainActivity
import com.theapache64.cyclone.core.Application
import com.toxicbakery.logging.Arbor
import com.toxicbakery.logging.Seedling


class App(
    appArgs: AppArgs,
) : Application() {

    companion object {
        lateinit var appArgs: AppArgs
    }

    init {
        App.appArgs = appArgs
    }

    override fun onCreate() {
        super.onCreate()
        Arbor.sow(Seedling())

        Arbor.d("Starting app...")

        val splashIntent = MainActivity.getStartIntent()
        startActivity(splashIntent)
    }
}

/**
 * The magic begins here
 */
fun main() {

    val appArgs = AppArgs(
        appName = "MindDumplingDesktop", // To show on title bar
        version = "v1.0.0", // To show on title inside brackets
        versionCode = 100 // To compare with latest version code (in case if you want to prompt update)
    )

    App(appArgs).onCreate()
}
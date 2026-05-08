package com.neurodumpling.app.test

import com.neurodumpling.app.data.di.module.MyModule
import com.neurodumpling.app.data.repo.MyRepo
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        MyModule::class
        // Add your modules here
    ]
)
interface TestComponent {
    fun myRepo(): MyRepo
}
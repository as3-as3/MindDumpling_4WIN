package com.neurodumpling.app.test

import com.neurodumpling.app.data.di.module.MyModule
import it.cosenonjaviste.daggermock.DaggerMockRule

class MyDaggerMockRule : DaggerMockRule<TestComponent>(
    TestComponent::class.java
    // TODO : Add your modules here
) {
    init {
        customizeBuilder<DaggerTestComponent.Builder> {
            it
        }
    }
}

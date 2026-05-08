pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
        id("org.jetbrains.kotlin.plugin.compose").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }

}
rootProject.name = "minddumplingdesktop"
include("data")

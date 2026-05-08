import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

group = "com.neurodumpling.app"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

val daggerVersion by extra("2.56.1")

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(compose.runtime)
    implementation("org.json:json:20240303")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    implementation("org.apache.pdfbox:pdfbox:3.0.1")
    implementation("org.jfree:jfreesvg:3.4.4")
}

compose.desktop {
    application {
        mainClass = "com.neurodumpling.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MindDumplingDesktop"
            packageVersion = "1.0.0"

            val iconsRoot = project.file("src/main/resources/drawables")

            linux {
                iconFile.set(iconsRoot.resolve("launcher_icons/linux.png"))
            }

            windows {
                iconFile.set(iconsRoot.resolve("launcher_icons/windows.ico"))
            }

            macOS {
                iconFile.set(iconsRoot.resolve("launcher_icons/macos.icns"))
            }
        }
    }
}

// Global bypass for jpackage tasks to use the host JDK
tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask> {
    val jdkPath = System.getProperty("org.gradle.java.home") ?: System.getenv("JAVA_HOME")
    if (jdkPath != null) {
        runtimeImage.set(file(jdkPath))
    }
}

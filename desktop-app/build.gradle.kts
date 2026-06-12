import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":desktop-ui"))
    implementation(project(":desktop-core"))

    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.20")

    implementation("io.github.kdroidfilter:composenativetray:0.6.3")
    implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
    implementation("cafe.adriel.voyager:voyager-navigator:1.1.0-beta03")
    implementation("cafe.adriel.voyager:voyager-screenmodel:1.1.0-beta03")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "HonkaiGameLauncher"
            packageVersion = "1.0.0"
        }

        buildTypes.release.proguard {
            isEnabled.set(false)
            configurationFiles.from(project.file("compose-desktop.pro"))
        }

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs(
                "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
                "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
                "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
            )
        } else {
            jvmArgs(
                "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED"
            )
        }
    }
}

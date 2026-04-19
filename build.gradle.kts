import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")

    kotlin("plugin.serialization") version "2.2.0"
}

group = "com.honkai_rts"
version = "1.0-SNAPSHOT"

val voyagerVersion = "1.1.0-beta03"
val nativeTrayVersion = "0.6.3"
val filekitVersion = "0.10.0-beta04"
val settingsVersion = "1.3.0"

val webviewVersion = "1.9.40"

val ktorVersion = "3.3.1"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jogamp.org/deployment/maven")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.20")

    implementation("br.com.devsrsouza.compose.icons:eva-icons:1.1.1")
    implementation("br.com.devsrsouza.compose.icons:feather:1.1.1")
    implementation("br.com.devsrsouza.compose.icons:line-awesome:1.1.1")

    implementation("io.github.kdroidfilter:composenativetray:${nativeTrayVersion}")

    // voyager导航
    implementation("cafe.adriel.voyager:voyager-navigator:${voyagerVersion}")
    implementation("cafe.adriel.voyager:voyager-screenmodel:${voyagerVersion}")
    implementation("cafe.adriel.voyager:voyager-transitions:${voyagerVersion}")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    // filekit文件管理
    implementation("io.github.vinceglb:filekit-dialogs-compose:${filekitVersion}")

    // 设置
    implementation("com.russhwolf:multiplatform-settings-no-arg:${settingsVersion}")

    //ktor
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty:${ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")

    // json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // WebView
    api("io.github.kevinnzou:compose-webview-multiplatform:1.9.40")
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
        }

        if(System.getProperty("os.name").contains("Mac"))
        {
            jvmArgs(
                "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
                "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
                "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
            )
        }
        else
        {
            jvmArgs(
                "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED",
            )
        }
    }
}
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.honkai_rts"
version = "1.0-SNAPSHOT"

val voyagerVersion = "1.1.0-beta03"
val nativeTrayVersion = "0.6.3"
val filekitVersion = "0.10.0-beta04"
val settingsVersion = "1.3.0"

val koinversion = "4.0.4"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
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

    // filekit文件管理
    implementation("io.github.vinceglb:filekit-dialogs-compose:${filekitVersion}")

    // 设置
    implementation("com.russhwolf:multiplatform-settings-no-arg:${settingsVersion}")

}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "HonkaiGameLauncher"
            packageVersion = "1.0.0"
        }
    }
}
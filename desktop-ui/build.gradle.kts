plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

compose.resources {
    publicResClass = true
}

dependencies {
    implementation(project(":desktop-core"))

    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)
    implementation("io.github.compose-fluent:fluent:v0.1.0")
    implementation("io.github.compose-fluent:fluent-icons-extended:v0.1.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.20")

    implementation("br.com.devsrsouza.compose.icons:eva-icons:1.1.1")
    implementation("br.com.devsrsouza.compose.icons:feather:1.1.1")
    implementation("br.com.devsrsouza.compose.icons:line-awesome:1.1.1")

    implementation("cafe.adriel.voyager:voyager-navigator:1.1.0-beta03")
    implementation("cafe.adriel.voyager:voyager-screenmodel:1.1.0-beta03")
    implementation("cafe.adriel.voyager:voyager-transitions:1.1.0-beta03")
    implementation("org.jetbrains:markdown:0.7.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    implementation("io.github.vinceglb:filekit-dialogs-compose:0.10.0-beta04")
    implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    api("io.github.kevinnzou:compose-webview-multiplatform:1.9.40")
    implementation("dev.datlag:kcef:2024.04.20.3")
}

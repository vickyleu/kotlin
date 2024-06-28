plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    js {
        binaries.executable()
        nodejs()
    }
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExtension>().version = "22.1.0"
}
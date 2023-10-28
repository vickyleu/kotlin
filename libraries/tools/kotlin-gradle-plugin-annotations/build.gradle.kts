plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("gradle-compat-convention")
}

publish()
standardPublicJars()

dependencies {
    api(platform(project(":kotlin-gradle-plugins-bom")))
}

apiValidation {
    nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
}

tasks {
    apiBuild.configure {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}
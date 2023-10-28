plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    google()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(11)

    compilerOptions {
        //  TODO: set to true before review
        allWarningsAsErrors.set(false)
    }
}

val bootstrapKotlinVersion = project.bootstrapKotlinVersion

dependencies {
    api(project(":buildsrc-compat"))
    api(libs.gradle.pluginPublish.gradlePlugin)

    // TODO: remove it
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-annotations") {
        version {
            strictly(project.bootstrapKotlinVersion)
        }
    }

    compileOnly(gradleApi())
}

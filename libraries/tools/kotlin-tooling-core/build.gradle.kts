plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("gradle-compat-convention")
}

publish()
sourcesJar()
javadocJar()

dependencies {
    api(platform(project(":kotlin-gradle-plugins-bom")))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
}

tasks {
    apiBuild.configure {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}
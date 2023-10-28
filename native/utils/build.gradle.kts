plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-compat-convention")
}

description = "Kotlin/Native utils"

dependencies {
    api(project(":kotlin-util-io"))
    api(project(":kotlin-util-klib"))
    api(platform(project(":kotlin-gradle-plugins-bom")))

    testImplementation(libs.junit4)
    testImplementation(kotlinStdlib())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

publish()

standardPublicJars()

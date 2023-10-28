plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-compat-convention")
}

description = "Common klib reader and writer"


dependencies {
    api(project(":kotlin-util-io"))
    testImplementation(libs.junit4)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

standardPublicJars()
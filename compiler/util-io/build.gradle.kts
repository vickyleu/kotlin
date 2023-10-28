plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-compat-convention")
}

dependencies {
    testImplementation(libs.junit4)
    testImplementation(kotlin("test"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

standardPublicJars()

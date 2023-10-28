plugins {
    id("org.jetbrains.kotlin.jvm")
    id("jps-compatible")
    id("gradle-compat-convention")
}

sourceSets {
    "main" {
        kotlin.srcDir("src/generated/kotlin")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("jps-compatible")
    id("gradle-compat-convention")
}

kotlin {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
}

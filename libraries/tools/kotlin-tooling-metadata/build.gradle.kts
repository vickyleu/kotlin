plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-compat-convention")
}

publish()
sourcesJar()
javadocJar()

dependencies {
    implementation(commonDependency("com.google.code.gson:gson"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
}

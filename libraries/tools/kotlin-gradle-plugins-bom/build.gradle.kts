plugins {
    id("java-platform")
    id("maven-publish")
    id("gradle-compat-convention")
}

gradleCompat {
    configureCommonPublicationSettingsForGradle(singingRequired = true, configureSbom = false)
}

dependencies {
    constraints {
        // kotlin-gradle-plugin-api
        api(project(":kotlin-gradle-plugin-api"))
        api(project(":kotlin-gradle-plugin-annotations"))
        api(project(":kotlin-gradle-plugin-model"))
        api(project(":native:kotlin-native-utils"))
        api(project(":kotlin-tooling-core"))

        // plugins
        api(project(":kotlin-gradle-plugin"))
        api(project(":atomicfu"))
        api(project(":kotlin-allopen"))
        api(project(":kotlin-lombok"))
        api(project(":kotlin-noarg"))
        api(project(":kotlin-sam-with-receiver"))
        api(project(":kotlin-serialization"))
        api(project(":kotlin-assignment"))
    }
}

publishing {
    publications {
        create<MavenPublication>("myPlatform") {
            from(components["javaPlatform"])
            pom {
                packaging = "pom"
            }
        }
    }
}

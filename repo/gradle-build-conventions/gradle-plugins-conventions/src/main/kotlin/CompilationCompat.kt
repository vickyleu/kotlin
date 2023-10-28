import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes
import javax.inject.Inject

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class GradleCompatExtension @Inject constructor(
    private val project: Project
) {
    private val internalPlugins = setOf(
        "android-test-fixes",
        "gradle-warnings-detector",
        "kotlin-compiler-args-properties",
    )

    private val testPlugins = internalPlugins + setOf(
        "kotlin-gradle-plugin-api",
        "kotlin-gradle-plugin",
    )

    /**
     * Configures common pom configuration parameters
     */
    fun configureCommonPublicationSettingsForGradle(
        signingRequired: Boolean,
        configureSBom: Boolean = true,
    ) = with(project) {
        plugins.withId("maven-publish") {
            extensions.configure<PublishingExtension> {
                publications
                    .withType<MavenPublication>()
                    .configureEach {
                        configureKotlinPomAttributes(project)
                        if (configureSBom && project.name !in internalPlugins) {
                            if (name == "pluginMaven") {
                                val sbomTask = configureSbom(target = "PluginMaven")
                                artifact("$buildDir/spdx/PluginMaven/PluginMaven.spdx.json") {
                                    extension = "spdx.json"
                                    builtBy(sbomTask)
                                }
                            } else if (name == "Main") {
                                val sbomTask = configureSbom()
                                artifact("$buildDir/spdx/MainPublication/MainPublication.spdx.json") {
                                    extension = "spdx.json"
                                    builtBy(sbomTask)
                                }
                            }
                        }
                    }
            }
        }
        configureDefaultPublishing(signingRequired)
    }
}

// check https://docs.gradle.org/current/userguide/compatibility.html#kotlin for Kotlin-Gradle versions matrix
internal fun Project.configureKotlinCompileTasksGradleCompatibility() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            // we can't use a language version greater than 1.5 as minimal supported Gradle embeds Kotlin 1.4
            @Suppress("DEPRECATION")
            languageVersion.set(KotlinVersion.KOTLIN_1_5)

            // we can't use an api version greater than 1.4 as the minimal supported Gradle version uses kotlin-stdlib 1.4
            @Suppress("DEPRECATION")
            apiVersion.set(KotlinVersion.KOTLIN_1_4)

            freeCompilerArgs.addAll(
                listOf(
                    "-Xskip-prerelease-check",
                    "-Xsuppress-version-warnings",
                    // We have to override the default value for `-Xsam-conversions` to `class`
                    // otherwise the compiler would compile lambdas using `invokedynamic`,
                    // such lambdas are not serializable so are not compatible with Gradle configuration cache.
                    // It doesn't lead to a significant difference in binaries' sizes,
                    // and previously (before LV 1.5) the `class` value was set by default.
                    "-Xsam-conversions=class",
                )
            )
        }
    }
}

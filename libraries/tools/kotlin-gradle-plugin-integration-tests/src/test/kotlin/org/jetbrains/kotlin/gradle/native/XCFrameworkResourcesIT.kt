/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@NativeGradlePluginTests
@DisplayName("Test multiplatform resources publication embedded in XCFramework")
class XCFrameworkResourcesIT : KGPBaseTest() {

    /**
     * Tests the publication of multiplatform resources for multiple targets embedded
     * within an XCFramework.
     *
     * @param gradleVersion the Gradle version to be used for the test execution.
     */
    @DisplayName("Multiplatform resources publication for multiple targets embedded in XCFramework")
    @GradleTest
    fun testXCFrameworkMultipleTargetsResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        project("resourcesXCFramework", gradleVersion) {
            configureForResources {
                listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64(),
                )
            }

            build("assembleSharedDebugXCFramework") {
                assertTasksExecuted(":assembleSharedDebugXCFramework")
                assertTasksExecuted(":assembleSharedDebugResourcesForXCFramework")

                val xcframeworkPath = projectPath.resolve("build/XCFrameworks/debug/Shared.xcframework")
                val iosArm64FrameworkPath = xcframeworkPath.resolve("ios-arm64/Shared.framework")
                val iosSimulatorArm64FrameworkPath = xcframeworkPath.resolve("ios-arm64_x86_64-simulator/Shared.framework")

                // Assert that the XCFramework and the frameworks for the iOS targets are created
                assertDirectoryExists(iosArm64FrameworkPath, "ios-arm64/Shared.framework not found in $xcframeworkPath")
                assertDirectoryExists(
                    iosSimulatorArm64FrameworkPath,
                    "ios-arm64_x86_64-simulator/Shared.framework not found in $xcframeworkPath"
                )

                val iosArm64ResourcesPath = iosArm64FrameworkPath.resolve("embedResources")
                val iosSimulatorArm64ResourcesPath = iosSimulatorArm64FrameworkPath.resolve("embedResources")

                // Assert that the resources are published for the iOS targets
                assertDirectoryExists(iosArm64ResourcesPath, "embedResources not found in $iosArm64FrameworkPath")
                assertDirectoryExists(iosSimulatorArm64ResourcesPath, "embedResources not found in $iosSimulatorArm64FrameworkPath")

                // Assert that the resources are the same for both iOS targets
                assertEqualDirectories(
                    iosArm64ResourcesPath.toFile(),
                    iosSimulatorArm64ResourcesPath.toFile(),
                    forgiveExtraFiles = false
                )
            }
        }
    }

    /**
     * Tests the publication of multiplatform resources for a single iOS target embedded
     * within an XCFramework.
     *
     * @param gradleVersion the Gradle version to be used for the test execution.
     */
    @DisplayName("Multiplatform resources publication for single target embedded in XCFramework")
    @GradleTest
    fun testXCFrameworkSingleTargetsResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        project("resourcesXCFramework", gradleVersion) {
            configureForResources()

            build("assembleSharedDebugXCFramework") {
                assertTasksExecuted(":assembleSharedDebugXCFramework")
                assertTasksExecuted(":assembleSharedDebugResourcesForXCFramework")

                val xcframeworkPath = projectPath.resolve("build/XCFrameworks/debug/Shared.xcframework")
                val iosArm64FrameworkPath = xcframeworkPath.resolve("ios-arm64/Shared.framework")
                val iosSimulatorArm64FrameworkPath = xcframeworkPath.resolve("ios-arm64_x86_64-simulator/Shared.framework")

                // Assert that the XCFramework and the frameworks for the iOS targets are created
                assertDirectoryExists(iosArm64FrameworkPath, "ios-arm64/Shared.framework not found in $xcframeworkPath")
                assertDirectoryDoesNotExist(iosSimulatorArm64FrameworkPath)

                val iosArm64ResourcesPath = iosArm64FrameworkPath.resolve("embedResources")
                val iosSimulatorArm64ResourcesPath = iosSimulatorArm64FrameworkPath.resolve("embedResources")

                // Assert that the resources are published for the iOS targets
                assertDirectoryExists(iosArm64ResourcesPath, "embedResources not found in $iosArm64FrameworkPath")
                assertDirectoryDoesNotExist(iosSimulatorArm64ResourcesPath)
            }
        }
    }

    /**
     * Tests the mutation of multiplatform resources embedded within an XCFramework, ensuring proper publication and resource handling.
     *
     * @param gradleVersion the Gradle version to be used for the test execution.
     */
    @DisplayName("Multiplatform resources mutation in XCFramework")
    @GradleTest
    fun testXCFrameworkMutationResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        project("resourcesXCFramework", gradleVersion) {
            configureForResources()

            val xcframeworkPath = projectPath.resolve("build/XCFrameworks/debug/Shared.xcframework")
            val iosArm64ResourcesPath = xcframeworkPath.resolve("ios-arm64/Shared.framework/embedResources")

            val drawables = iosArm64ResourcesPath.resolve("drawable")
            val files = iosArm64ResourcesPath.resolve("files")
            val font = iosArm64ResourcesPath.resolve("font")
            val values = iosArm64ResourcesPath.resolve("values")

            build("assembleSharedDebugXCFramework") {
                assertTasksExecuted(":assembleSharedDebugXCFramework")
                assertTasksExecuted(":assembleSharedDebugResourcesForXCFramework")

                assertDirectoriesExist(
                    drawables, files, font, values,
                )

                assertFileExists(drawables.resolve("compose-multiplatform.xml"))
                assertFileExists(files.resolve("commonResource"))
                assertFileExists(font.resolve("IndieFlower-Regular.ttf"))
                assertFileExists(values.resolve("strings.xml"))
            }

            projectPath.resolve("src/commonMain/appResources/drawable").deleteRecursively()
            projectPath.resolve("src/commonMain/appResources/font").deleteRecursively()

            projectPath.resolve("src/commonMain/appResources/files").resolve("commonResource").deleteExisting()
            projectPath.resolve("src/commonMain/appResources/files").resolve("newCommonResource").createFile()
                .writeText("new commonResource")

            build("assembleSharedDebugXCFramework") {
                assertTasksExecuted(":assembleSharedDebugXCFramework")
                assertTasksExecuted(":assembleSharedDebugResourcesForXCFramework")

                assertDirectoriesExist(
                    files, values,
                )

                assertDirectoryDoesNotExist(drawables)
                assertDirectoryDoesNotExist(font)

                assertFileNotExists(files.resolve("commonResource"))
                assertFileExists(files.resolve("newCommonResource"))
                assertFileExists(values.resolve("strings.xml"))
            }
        }
    }

    @DisplayName("run XCTests for testing xcframework with resources")
    @GradleTest
    fun testXcframeworkResourcesXCTests(
        gradleVersion: GradleVersion,
    ) {
        XCTestHelpers().use {
            val simulator = it.createSimulator().apply {
                boot()
            }

            project("resourcesXCFramework", gradleVersion) {
                configureForResources {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64(),
                    )
                }

                build("assembleSharedDebugXCFramework") {
                    assertTasksExecuted(":assembleSharedDebugXCFramework")
                    assertTasksExecuted(":assembleSharedDebugResourcesForXCFramework")
                }

                val xcframeworkPath = projectPath.resolve("build/XCFrameworks/debug/Shared.xcframework")
                assertDirectoryExists(xcframeworkPath, "xcframework not found in $projectPath")

                xcframeworkPath.copyToRecursively(projectPath.resolve("iosApp/Shared.xcframework"), followLinks = false, overwrite = true)

                buildXcodeProject(
                    xcodeproj = projectPath.resolve("iosApp/XCTestApp.xcodeproj"),
                    scheme = "XCTestAppTests",
                    destination = "platform=iOS Simulator,id=${simulator.udid}",
                    action = XcodeBuildAction.Test
                )
            }
        }
    }
}

private fun TestProject.configureForResources(
    multiplatform: KotlinMultiplatformExtension.() -> List<KotlinNativeTarget> = {
        listOf(
            iosArm64()
        )
    },
) {
    addKgpToBuildScriptCompilationClasspath()
    buildScriptInjection {
        project.applyMultiplatform {
            val xcf = project.XCFramework("Shared")

            val publication = project.extraProperties.get(
                KotlinTargetResourcesPublication.EXTENSION_NAME
            ) as KotlinTargetResourcesPublication

            multiplatform().forEach { target ->
                target.binaries.framework {
                    baseName = "Shared"
                    isStatic = true
                    xcf.add(this)
                }

                publication.publishResourcesAsKotlinComponent(
                    target = target,
                    resourcePathForSourceSet = { sourceSet ->
                        KotlinTargetResourcesPublication.ResourceRoot(
                            resourcesBaseDirectory = project.provider { project.file("src/${sourceSet.name}/appResources") },
                            includes = emptyList(),
                            excludes = emptyList(),
                        )
                    },
                    relativeResourcePlacement = project.provider { File("embedResources") },
                )
            }
        }
    }
}
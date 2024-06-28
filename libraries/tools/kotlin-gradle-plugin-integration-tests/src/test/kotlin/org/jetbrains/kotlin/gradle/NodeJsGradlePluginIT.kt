/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.PACKAGE_LOCK_MISMATCH_MESSAGE
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.RESTORE_PACKAGE_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.STORE_PACKAGE_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.UPGRADE_PACKAGE_LOCK
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.TestVersions.Gradle.G_7_6
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.test.assertEquals


@MppGradlePluginTests
class NodeJsGradlePluginIT : KGPBaseTest() {
    @DisplayName("Set different Node.js versions in different subprojects")
    @GradleTest
    fun testDifferentVersionInSubprojects(gradleVersion: GradleVersion) {
        project(
            "subprojects-nodejs-setup",
            gradleVersion
        ) {
            build(":app1:jsNodeDevelopmentRun") {
                assertOutputContains("v22.2.0")
            }

            build(":app2:jsNodeDevelopmentRun") {
                assertOutputContains("v22.1.0")
            }
        }
    }
}

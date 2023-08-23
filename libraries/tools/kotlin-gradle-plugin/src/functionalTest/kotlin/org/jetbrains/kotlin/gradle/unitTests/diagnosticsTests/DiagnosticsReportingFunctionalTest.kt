/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.applyKotlinJvmPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DiagnosticsReportingFunctionalTest {

    @Test
    fun testNonDuplicatedReporting() {
        buildProjectWithMockedCheckers {
            applyKotlinJvmPlugin()
            evaluate()
            reportTestDiagnostic()
            reportTestDiagnostic()
            checkDiagnostics("nonDuplicatedReporting")
        }
    }

    @Test
    fun testOncePerBuildReporting() {
        val root = buildProjectWithMockedCheckers()

        root.applyKotlinJvmPlugin()
        root.evaluate()

        buildProjectWithMockedCheckers("subproject-a", root) {
            applyKotlinJvmPlugin()
            evaluate()
            reportOnePerBuildTestDiagnostic()
            reportOnePerBuildTestDiagnostic()
        }

        buildProjectWithMockedCheckers("subproject-b", root) {
            applyKotlinJvmPlugin()
            evaluate()
            reportOnePerBuildTestDiagnostic()
            reportOnePerBuildTestDiagnostic()
        }

        root.checkDiagnostics("oncePerBuildReporting")
    }

    // Known quirk: deduplicated diagnostics use internalId as a default key of deduplication,
    // meaning that subsequent reported diagnostics with the same ID will be dropped even if
    // they have different message/severity
    @Test
    fun testOncePerBuildWithDifferentSeverities() {
        val root = buildProject()

        root.applyKotlinJvmPlugin()
        root.evaluate()

        buildProject(
            {
                withName("subproject")
                withParent(root)
            }
        ).run {
            applyKotlinJvmPlugin()
            evaluate()
            reportOnePerBuildTestDiagnostic()
            reportOnePerBuildTestDiagnostic(severity = ERROR) // NB: will be lost!
        }

        root.checkDiagnostics("deduplicationWithDifferentSeverities", compactRendering = false)
    }

    @Test
    fun testSuppressedWarnings() {
        buildProject().run {
            applyKotlinJvmPlugin()
            extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS, "TEST_DIAGNOSTIC")
            reportTestDiagnostic()
            evaluate()
            checkDiagnostics("suppressedWarnings")
        }
    }

    @Test
    fun testSuppressedErrors() {
        buildProject().run {
            applyKotlinJvmPlugin()
            extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_SUPPRESS_GRADLE_PLUGIN_ERRORS, "TEST_DIAGNOSTIC")
            reportTestDiagnostic(severity = ERROR)
            evaluate()
            checkDiagnostics("suppressedErrors")
        }
    }

    @Test
    fun testSuppressForWarningsDoesntWorkForErrors() {
        buildProject().run {
            applyKotlinJvmPlugin()
            extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS, "TEST_DIAGNOSTIC")
            reportTestDiagnostic(severity = ERROR)
            evaluate()
            checkDiagnostics("suppressForWarningsDoesntWorkForErrors")
        }
    }

    @Test
    fun testLocationsAttaching() {
        buildProject().run {
            applyKotlinJvmPlugin()
            reportTestDiagnostic()
            evaluate()

            val diagnostics = project!!.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(rootProject)

            val projectDiagnostic = diagnostics.single()
            assertNotNull(projectDiagnostic, "Project diagnostic hasn't been reported")
            assertEquals(project!!.toLocation(), projectDiagnostic.location)
        }
    }
}

private fun buildProjectWithMockedCheckers(
    name: String? = null,
    parent: ProjectInternal? = null,
    block: ProjectInternal.() -> Unit = { },
): ProjectInternal {
    val project = buildProject(
        {
            if (name != null) withName(name)
            if (parent != null) withParent(parent)
        }
    )

    project.allprojects {
        project.extensions.extraProperties.set(
            KOTLIN_GRADLE_PROJECT_CHECKERS_OVERRIDE,
            listOf(MockChecker, MockPerBuildChecker)
        )
    }

    project.block()
    return project
}

private val testDiagnosticId = "TEST_DIAGNOSTIC"
private fun Project.reportTestDiagnostic(severity: Severity = WARNING) {
    val TEST_DIAGNOSTIC = ToolingDiagnostic(testDiagnosticId, "This is a test diagnostic\n\nIt has multiple lines of text", severity)
    kotlinToolingDiagnosticsCollector.report(
        project,
        TEST_DIAGNOSTIC
    )
}

private fun Project.reportOnePerBuildTestDiagnostic(severity: Severity = WARNING) {
    kotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(
        project,
        ToolingDiagnostic(
            "TEST_DIAGNOSTIC_ONE_PER_BUILD",
            "This is a test diagnostics that should be reported once per build\n\nIt has multiple lines of text",
            severity
        )
    )
}

internal object MockChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        project.reportTestDiagnostic()
    }
}

internal object MockPerBuildChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        project.reportOnePerBuildTestDiagnostic()
    }
}

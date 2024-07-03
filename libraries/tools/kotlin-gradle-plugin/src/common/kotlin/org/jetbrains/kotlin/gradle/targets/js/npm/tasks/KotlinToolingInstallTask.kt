/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NpmToolingEnv
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.asNodeJsEnvironment
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property

@DisableCachingByDefault
abstract class KotlinToolingInstallTask :
    DefaultTask() {


    // Only in configuration phase
    // Not part of configuration caching

    private val nodeJs: NodeJsRootExtension
        get() = project.rootProject.kotlinNodeJsExtension

    // -----

    private val nodeJsEnvironment by lazy {
        nodeJs.requireConfigured().asNodeJsEnvironment
    }

    private val packageManagerEnv by lazy {
        nodeJs.packageManagerExtension.get().environment
    }

    @get:Internal
    internal val npmTooling: Property<NpmToolingEnv> = project.objects.property()

    @get:Input
    internal val versionsHash: Provider<String> = npmTooling.map { it.version }

    private val tools = nodeJs.versions.allDeps

    @get:OutputDirectory
    val destination: Provider<Directory> = project.objects.directoryProperty().fileProvider(
        npmTooling.map { it.dir }
    )

    @Input
    val args: MutableList<String> = mutableListOf()

    @get:Internal
    val nodeModules: Provider<Directory> = destination.map { it.dir("node_modules") }

    @TaskAction
    fun install() {
        val toolingPackageJson = PackageJson(
            "kotlin-npm-tooling",
            versionsHash.get()
        ).apply {
            private = true
            dependencies.putAll(
                tools.map { it.name to it.version }
            )
        }

        toolingPackageJson.saveTo(destination.getFile().resolve(NpmProject.PACKAGE_JSON))

        nodeJsEnvironment.packageManager.packageManagerExec(
            services = services,
            logger = logger,
            nodeJs = nodeJsEnvironment,
            environment = packageManagerEnv,
            dir = destination.getFile(),
            description = "Installation of tooling install",
            args = args,
        )
    }

    companion object {
        const val NAME = "kotlinToolingInstall"
    }
}
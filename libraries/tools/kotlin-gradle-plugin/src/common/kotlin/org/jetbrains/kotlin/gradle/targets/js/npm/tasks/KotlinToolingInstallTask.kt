/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import com.intellij.openapi.util.io.findOrCreateFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NpmToolingEnv
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.asNodeJsEnvironment
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption


@DisableCachingByDefault
abstract class KotlinToolingInstallTask :
    DefaultTask() {


    // Only in configuration phase
    // Not part of configuration caching

    private val nodeJsRoot: NodeJsRootExtension
        get() = project.rootProject.kotlinNodeJsRootExtension

    private val nodeJs: NodeJsExtension
        get() = project.rootProject.kotlinNodeJsExtension

    // -----

    private val nodeJsEnvironment by lazy {
        asNodeJsEnvironment(nodeJsRoot, nodeJs.requireConfigured())
    }

    private val packageManagerEnv by lazy {
        nodeJsRoot.packageManagerExtension.get().environment
    }

    @get:Internal
    internal val npmTooling: Property<NpmToolingEnv> = project.objects.property()

    @get:Input
    internal val versionsHash: Provider<String> = npmTooling.map { it.version }

    private val tools = nodeJsRoot.versions.allDeps

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
        val lockFile = destination.getFile().resolve("lock")
        FileChannel.open(
            lockFile.toPath(),
            StandardOpenOption.CREATE, StandardOpenOption.WRITE
        ).use { channel ->
            channel.lock().use { _ ->
                val packageJsonFile = destination.getFile().resolve(NpmProject.PACKAGE_JSON)
                if (packageJsonFile.exists()) return // return from install
                val toolingPackageJson = PackageJson(
                    "kotlin-npm-tooling",
                    versionsHash.get()
                ).apply {
                    private = true
                    dependencies.putAll(
                        tools.map { it.name to it.version }
                    )
                }

                toolingPackageJson.saveTo(packageJsonFile)

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
        }
    }

    companion object {
        const val NAME = "kotlinToolingInstall"
    }
}
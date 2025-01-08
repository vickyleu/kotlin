/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.TASKS_GROUP_NAME
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NpmTooling
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinToolingInstallTask
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.web.nodejs.CommonNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NodeJsRootPluginApplier
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import org.jetbrains.kotlin.gradle.utils.getFile

open class WasmNodeJsRootPlugin : CommonNodeJsRootPlugin {

    override fun apply(target: Project) {
        val rootDirectoryName = WasmPlatformDisambiguate.platformDisambiguate
        val nodeJsRootPluginApplier = NodeJsRootPluginApplier(
            platformDisambiguate = WasmPlatformDisambiguate,
            nodeJsRootKlass = WasmNodeJsRootExtension::class,
            nodeJsRootName = WasmNodeJsRootExtension.EXTENSION_NAME,
            npmKlass = WasmNpmExtension::class,
            npmName = WasmNpmExtension.Companion.EXTENSION_NAME,
            rootDirectoryName = rootDirectoryName,
            lockFileDirectory = { it.dir(LockCopyTask.Companion.KOTLIN_JS_STORE).dir(rootDirectoryName) },
            singleNodeJsPluginApply = { WasmNodeJsPlugin.apply(it) },
            yarnPlugin = WasmYarnPlugin::class,
            platformType = KotlinPlatformType.wasm,
        )

        nodeJsRootPluginApplier.apply(target)

        val nodeJsRoot = target.extensions.getByName(WasmNodeJsRootExtension.EXTENSION_NAME) as WasmNodeJsRootExtension
        val nodeJs = target.extensions.getByName(WasmNodeJsEnvSpec.EXTENSION_NAME) as WasmNodeJsEnvSpec

        val packageManagerName = nodeJsRoot.packageManagerExtension.map { it.name }

        val allDeps = nodeJsRoot.versions.allDeps

        val npmTooling = NpmTooling(
            target.objects.directoryProperty()
                .fileValue(target.gradle.gradleUserHomeDir.resolve("kotlin-npm-tooling"))
                .zip(packageManagerName) { toolingDir, name ->
                    toolingDir.dir(name)
                },
            allDeps
        ).produceEnv()

        target.registerTask<KotlinToolingInstallTask>(KotlinToolingInstallTask.NAME) { toolingInstall ->
            toolingInstall
                .versionsHash
                .value(npmTooling.map { it.version })
                .disallowChanges()

            toolingInstall
                .tools
                .value(allDeps)
                .disallowChanges()

            toolingInstall
                .destination
                .fileProvider(npmTooling.map { it.dir })
                .disallowChanges()

            toolingInstall
                .nodeModules
                .fileProvider(npmTooling.map { it.dir.resolve("node_modules") })
                .disallowChanges()

            with(nodeJsRootPluginApplier) {
                toolingInstall.configureNodeJsEnvironmentTasks(
                    nodeJsRoot,
                    nodeJs
                )
            }

            with(nodeJs) {
                toolingInstall.dependsOn(target.nodeJsSetupTaskProvider)
            }
            toolingInstall.group = TASKS_GROUP_NAME
            toolingInstall.description = "Find, download and link NPM dependencies and projects"

            toolingInstall.outputs.upToDateWhen {
                toolingInstall.nodeModules.getFile().exists()
            }
        }

        nodeJsRoot.npmTooling
            .value(npmTooling)
    }

    companion object : HasPlatformDisambiguate by WasmPlatformDisambiguate {
        fun apply(rootProject: Project): WasmNodeJsRootExtension {
            check(rootProject == rootProject.rootProject)
            rootProject.plugins.apply(WasmNodeJsRootPlugin::class.java)
            return rootProject.extensions.getByName(WasmNodeJsRootExtension.EXTENSION_NAME) as WasmNodeJsRootExtension
        }

        val Project.kotlinNodeJsRootExtension: WasmNodeJsRootExtension
            get() = extensions.getByName(WasmNodeJsRootExtension.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

        val Project.kotlinNpmResolutionManager: Provider<KotlinNpmResolutionManager>
            get() {
                return project.gradle.sharedServices.registerIfAbsent(
                    extensionName(KotlinNpmResolutionManager::class.java.name),
                    KotlinNpmResolutionManager::class.java
                ) {
                    error("Must be already registered")
                }
            }
    }
}
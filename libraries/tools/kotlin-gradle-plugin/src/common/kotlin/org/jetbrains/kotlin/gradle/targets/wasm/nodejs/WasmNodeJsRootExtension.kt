/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NpmToolingEnv
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinToolingInstallTask
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.web.nodejs.AbstractNodeJsRootExtension
import org.jetbrains.kotlin.gradle.utils.property

open class WasmNodeJsRootExtension(
    project: Project,
    nodeJs: () -> NodeJsEnvSpec,
    rootDir: String,
) : AbstractNodeJsRootExtension(
    project,
    nodeJs,
    rootDir
), HasPlatformDisambiguate by WasmPlatformDisambiguate {

    val npmTooling: Property<NpmToolingEnv> = project.objects.property()

    val toolingInstallTaskProvider: TaskProvider<out KotlinToolingInstallTask>
        get() = project.tasks.withType(KotlinToolingInstallTask::class.java)
            .named(KotlinToolingInstallTask.NAME)

    companion object : HasPlatformDisambiguate by WasmPlatformDisambiguate {
        val EXTENSION_NAME: String
            get() = extensionName("kotlinNodeJs")
    }
}
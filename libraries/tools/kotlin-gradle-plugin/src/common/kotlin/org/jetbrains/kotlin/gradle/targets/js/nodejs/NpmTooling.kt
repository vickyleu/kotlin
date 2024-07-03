/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.toHexString
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

open class NpmTooling(
    val installationDir: Provider<Directory>,
    val nodeJs: NodeJsRootExtension,
) : ConfigurationPhaseAware<NpmToolingEnv>() {

    override fun finalizeConfiguration(): NpmToolingEnv {
        val md = MessageDigest.getInstance("MD5")
        nodeJs.versions.allDeps.forEach { (name, version) ->
            md.update(name.toByteArray(StandardCharsets.UTF_8))
            md.update(version.toByteArray(StandardCharsets.UTF_8))
        }

        val hashVersion = md.digest().toHexString()

        val cleanableStore = CleanableStore[installationDir.getFile().absolutePath]

        val nodeDir = cleanableStore[hashVersion].use()

        return NpmToolingEnv(
            cleanableStore = cleanableStore,
            version = hashVersion,
            dir = nodeDir,
        )
    }
}

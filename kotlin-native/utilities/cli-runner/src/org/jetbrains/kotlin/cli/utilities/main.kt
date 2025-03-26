/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.native.interop.gen.defFileDependencies
import org.jetbrains.kotlin.cli.bc.main as konancMain
import org.jetbrains.kotlin.cli.klib.main as klibMain
import org.jetbrains.kotlin.cli.bc.mainNoExitWithGradleRenderer as konancMainWithGradleRenderer
import org.jetbrains.kotlin.cli.bc.mainNoExitWithXcodeRenderer as konancMainWithXcodeRenderer
import org.jetbrains.kotlin.cli.bc.mainWithPerformance as konancMainWithPerformance
import org.jetbrains.kotlin.backend.konan.env.setEnv
import org.jetbrains.kotlin.utils.usingNativeMemoryAllocator

private fun mainImpl(args: Array<String>, runFromDaemon: Boolean, konancMain: (Array<String>) -> Any) {
    val utilityName = args[0]
    val utilityArgs = args.drop(1).toTypedArray()
    when (utilityName) {
        "konanc" ->
            konancMain(utilityArgs)

        "cinterop" -> {
            val konancArgs = invokeInterop("native", utilityArgs, runFromDaemon)
            konancArgs?.let { konancMain(it) }
        }
        "jsinterop" -> {
            error("wasm target in Kotlin/Native is removed. See https://kotl.in/native-targets-tiers")
        }
        "klib" ->
            klibMain(utilityArgs)
        "defFileDependencies" ->
            defFileDependencies(utilityArgs, runFromDaemon)
        "generatePlatformLibraries" ->
            generatePlatformLibraries(utilityArgs)

        "llvm" -> runLlvmTool(utilityArgs)
        "clang" -> runLlvmClangToolWithTarget(utilityArgs)

        else -> error("Unexpected utility name: $utilityName")
    }
}

fun main(args: Array<String>) = if (args[0] == "--report-file") {
    val reportFile = args[1]
    mainImpl(args.drop(2).toTypedArray(), false) { reportFile.runKonancMainAndStorePerformanceMetrics(it) }
} else {
    mainImpl(args, false, ::konancMain)
}

private fun String.runKonancMainAndStorePerformanceMetrics(args: Array<String>) = konancMainWithPerformance(this, args)

private fun setupClangEnv() {
    setEnv("LIBCLANG_DISABLE_CRASH_RECOVERY", "1")
}

fun daemonMain(args: Array<String>) = inProcessMain(args, ::konancMainWithGradleRenderer)
fun daemonMainWithPerformance(args: Array<String>, reportFilePath: String) =
        inProcessMain(args) { args -> konancMainWithPerformance(reportFilePath, args) }


fun daemonMainWithXcodeRenderer(args: Array<String>) = inProcessMain(args, ::konancMainWithXcodeRenderer)

private fun inProcessMain(args: Array<String>, konancMain: (Array<String>) -> Any): Any? =
    usingNativeMemoryAllocator {
        setupClangEnv() // For in-process invocation have to setup proper environment manually.
        mainImpl(args, true, konancMain)
    }


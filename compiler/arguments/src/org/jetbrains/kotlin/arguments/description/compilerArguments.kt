/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.arguments.*
import org.jetbrains.kotlin.arguments.dsl.compilerArguments
import java.io.File

val kotlinCompilerArguments = compilerArguments {
    topLevel(Levels.commonToolArguments, mergeWith = setOf(actualCommonToolsArguments)) {
        subLevel(Levels.commonCompilerArguments, mergeWith = setOf(actualCommonCompilerArguments)) {
            subLevel(Levels.jvmCompilerArguments, mergeWith = setOf(actualJvmCompilerArguments)) {}
            subLevel(Levels.commonKlibBasedArguments, mergeWith = setOf(actualCommonKlibBasedArguments)) {
                subLevel(Levels.wasmArguments, mergeWith = setOf(actualWasmArguments)) {
                    subLevel(Levels.jsArguments, mergeWith = setOf(actualJsArguments)) {}
                }
                subLevel(Levels.nativeArguments, mergeWith = setOf(actualNativeArguments)) {}
            }
            subLevel(Levels.metadataArguments, mergeWith = setOf(actualMetadataArguments)) {}
        }
    }
}

fun main() {
    val format = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    val jsonArguments = format.encodeToString(kotlinCompilerArguments)
    println("=== arguments in JSON ===")
    println(jsonArguments)
    println("=== end of JSON ===")
    val jsonFile = File("./compiler/arguments/arguments.json")
    jsonFile.writeText(jsonArguments)

    val decodedCompilerArguments = format.decodeFromString<CompilerArguments>(jsonArguments)
    println("Decoded arguments: $decodedCompilerArguments")
}

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.WasmImportDescriptor
import org.jetbrains.kotlin.wasm.ir.WasmSymbol
import kotlin.collections.contains

abstract class ModuleImportControllerBase {
    abstract var enableImport: Boolean
    abstract fun importIfNeededOrFalse(declaration: IrDeclaration, prefix: String, body: (WasmImportDescriptor) -> Unit): Boolean
    abstract fun addFileFragmentImports(wasmCompiledFileFragment: WasmCompiledFileFragment)
}

class ModuleImportController(private val moduleName: String, private val signatureRetriever: IdSignatureRetriever) : ModuleImportControllerBase() {
    private val importDeclarations = mutableSetOf<IdSignature>()

    override var enableImport = true

    override fun addFileFragmentImports(wasmCompiledFileFragment: WasmCompiledFileFragment) {
        importDeclarations.addAll(wasmCompiledFileFragment.functions.unbound.keys)
        importDeclarations.addAll(wasmCompiledFileFragment.globalFields.unbound.keys)
        importDeclarations.addAll(wasmCompiledFileFragment.globalVTables.unbound.keys)
        importDeclarations.addAll(wasmCompiledFileFragment.globalClassITables.unbound.keys)
        wasmCompiledFileFragment.rttiElements?.let {
            importDeclarations.addAll(it.globalReferences.unbound.keys)
        }
    }

    override fun importIfNeededOrFalse(declaration: IrDeclaration, prefix: String, body: (WasmImportDescriptor) -> Unit): Boolean {
        if (!enableImport) return false
        val signature = signatureRetriever.declarationSignature(declaration)
        if (signature !in importDeclarations) return true
        body(WasmImportDescriptor(moduleName, WasmSymbol(prefix + signature.toString())))
        return true
    }
}

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.WasmGlobal

class WasmFileCodegenContextWithExport(
    wasmFileFragment: WasmCompiledFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
) : WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever) {
    override fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        val owner = irFunction.owner
        if (owner.isEffectivelyPrivate()) return
        val signature = idSignatureRetriever.declarationSignature(owner)
        addExport(
            WasmExport.Function(
                field = wasmFunction,
                name = "func_$signature"
            )
        )
    }

    override fun defineGlobalField(irField: IrFieldSymbol, wasmGlobal: WasmGlobal, doNotExportWithIEC: Boolean) {
        if (!doNotExportWithIEC) {
            exportDeclarationGlobal(irField.owner, "field", wasmGlobal)
        }
        super.defineGlobalField(irField, wasmGlobal, doNotExportWithIEC)
    }

    override fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        exportDeclarationGlobal(irClass.owner, "rtti", wasmGlobal)
        super.defineGlobalVTable(irClass, wasmGlobal)
    }

    override fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        exportDeclarationGlobal(irClass.owner, "itable", wasmGlobal)
        super.defineGlobalClassITable(irClass, wasmGlobal)
    }

    override fun defineRttiGlobal(global: WasmGlobal, irClass: IrClassSymbol, irSuperClass: IrClassSymbol?) {
        exportDeclarationGlobal(irClass.owner, "rtti", global)
        super.defineRttiGlobal(global, irClass, irSuperClass)
    }

    private fun exportDeclarationGlobal(declaration: IrDeclarationWithVisibility, prefix: String, global: WasmGlobal) {
        if (declaration.isEffectivelyPrivate()) return
        val signature = idSignatureRetriever.declarationSignature(declaration)
        addExport(
            WasmExport.Global(
                name = "${prefix}_$signature",
                field = global
            )
        )
    }
}
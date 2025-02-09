/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")  // Used by compiler

package kotlin.wasm.internal

internal class TypeInfoData(val typeId: Long, val packageName: String, val typeName: String)

@Suppress("UNUSED_PARAMETER")
@WasmArrayOf(Long::class, isNullable = false)
internal class WasmLongImmutableArray(size: Int) {
    @WasmOp(WasmOp.ARRAY_GET)
    fun get(index: Int): Long =
        implementedAsIntrinsic

    @WasmOp(WasmOp.ARRAY_LEN)
    fun len(): Int =
        implementedAsIntrinsic
}

// This is a very special class which NOT effectively derived from Any.
internal class Rtti @WasmPrimitiveConstructor constructor(
    val supportedIFaces: WasmLongImmutableArray?,
    val superClassRtti: Rtti?,
    val packageNameAddress: Int,
    val packageNameLength: Int,
    val packageNamePoolId: Int,
    val simpleNameAddress: Int,
    val simpleNameLength: Int,
    val simpleNamePoolId: Int,
)

internal fun getInterfaceSlot(obj: Any, interfaceId: Long): Int {
    val rtti = obj.rtti ?: return -1
    val interfaceArray = rtti.supportedIFaces ?: return -1
    val interfaceArraySize = interfaceArray.len()

    var interfaceSlot = 0
    while (interfaceSlot < interfaceArraySize) {
        val supportedInterface = interfaceArray.get(interfaceSlot)
        if (supportedInterface == interfaceId) {
            return interfaceSlot
        }
        interfaceSlot++
    }
    return -1
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <T> wasmIsInterface(obj: Any): Boolean =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmGetInterfaceId(): Long =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T> wasmGetTypeRtti(): Rtti =
    implementedAsIntrinsic

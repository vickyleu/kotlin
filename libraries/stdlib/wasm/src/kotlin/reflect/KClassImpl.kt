/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.wasm.internal

import kotlin.reflect.KClass

internal class KClassImpl<T : Any> @WasmPrimitiveConstructor constructor(internal val objRtti: Rtti) : KClass<T> {
    override val simpleName: String get() = getSimpleName()
    override val qualifiedName: String
        get() {
            val typeName = getSimpleName()
            val packageName = getPackageName()
            return if (packageName.isEmpty()) typeName else "$packageName.$typeName"
        }

    override fun isInstance(value: Any?): Boolean {
        if (value !is Any) return false

        val rtti = objRtti
        var current: Rtti? = value.rtti
        while (current != null) {
            if (wasm_ref_eq(rtti, current)) return true
            current = current.superClassRtti
        }
        return false
    }

    override fun equals(other: Any?): Boolean =
        (other !== null) && ((this === other) || (other is KClassImpl<*> && wasm_ref_eq(objRtti, other.objRtti)))

    override fun hashCode(): Int = qualifiedName.hashCode()

    override fun toString(): String = "class $qualifiedName"

    private fun getPackageName(): String = stringLiteral(
        poolId = objRtti.packageNamePoolId,
        startAddress = objRtti.packageNameAddress,
        length = objRtti.packageNameLength,
    )

    private fun getSimpleName(): String = stringLiteral(
        poolId = objRtti.simpleNamePoolId,
        startAddress = objRtti.simpleNameAddress,
        length = objRtti.simpleNameLength,
    )
}
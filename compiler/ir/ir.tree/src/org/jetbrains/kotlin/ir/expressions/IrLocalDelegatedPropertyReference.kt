/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol

abstract class IrLocalDelegatedPropertyReference : IrCallableReference<IrLocalDelegatedPropertySymbol>(0) {
    abstract val delegate: IrVariableSymbol
    abstract val getter: IrSimpleFunctionSymbol
    abstract val setter: IrSimpleFunctionSymbol?

    override val valueArgumentsCount: Int
        get() = 0
}

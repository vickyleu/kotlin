/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.try]
 */
abstract class IrTry(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    val catches: MutableList<IrCatch>,
) : IrExpression(
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
) {
    lateinit var tryResult: IrExpression

    var finallyExpression: IrExpression? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitTry(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        tryResult.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyExpression?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        tryResult = tryResult.transform(transformer, data)
        catches.transformInPlace(transformer, data)
        finallyExpression = finallyExpression?.transform(transformer, data)
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

class FirSingleExpressionBlock(
    var statement: FirStatement
) : FirBlock() {
    override val source: KtSourceElement?
        get() = statement.source?.fakeElement(KtFakeSourceElementKind.SingleExpressionBlock)
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override val statements: List<FirStatement> get() = listOf(statement)
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.acceptAllElements(visitor, data)
        statement.accept(visitor, data)
        typeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSingleExpressionBlock {
        transformStatements(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun <D> transformStatements(transformer: FirTransformer<D>, data: D): FirBlock {
        statement = statement.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirBlock {
        transformAnnotations(transformer, data)
        typeRef = typeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirBlock {
        annotations.transformInplace(transformer, data)
        return this
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun buildSingleExpressionBlock(statement: FirStatement): FirBlock {
    return FirSingleExpressionBlock(statement)
}

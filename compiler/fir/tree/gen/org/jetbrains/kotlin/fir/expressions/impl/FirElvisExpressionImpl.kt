/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirElvisExpressionImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override var calleeReference: FirReference,
    override var lhs: FirExpression,
    override var rhs: FirExpression,
) : FirElvisExpression() {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.acceptAllElements(visitor, data)
        calleeReference.accept(visitor, data)
        lhs.accept(visitor, data)
        rhs.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElvisExpressionImpl {
        typeRef = typeRef.transform(transformer, data)
        transformAnnotations(transformer, data)
        transformCalleeReference(transformer, data)
        transformLhs(transformer, data)
        transformRhs(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirElvisExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirElvisExpressionImpl {
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    override fun <D> transformLhs(transformer: FirTransformer<D>, data: D): FirElvisExpressionImpl {
        lhs = lhs.transform(transformer, data)
        return this
    }

    override fun <D> transformRhs(transformer: FirTransformer<D>, data: D): FirElvisExpressionImpl {
        rhs = rhs.transform(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        calleeReference = newCalleeReference
    }
}

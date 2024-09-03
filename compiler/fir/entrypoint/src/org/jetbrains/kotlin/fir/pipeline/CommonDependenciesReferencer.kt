/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid

fun referenceAllCommonDependencies(outputs: List<ModuleCompilerAnalyzedOutput>) {
    val (platformSession, scopeSession, _) = outputs.last()
    val dependantFragments = outputs.dropLast(1)
    val visitor = Visitor(platformSession)
    for ((_, _, files) in dependantFragments) {
        for (file in files) {
            file.accept(visitor)
        }
    }
}

private class Visitor(val session: FirSession) : FirDefaultVisitorVoid() {
    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        val symbol = resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*> ?: return
        val id = symbol.callableId.takeUnless { it.isLocal || it.classId != null } ?: return
        session.symbolProvider.getTopLevelCallableSymbols(id.packageName, id.callableName)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        lookupType(resolvedQualifier.resolvedType)
        visitElement(resolvedQualifier)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        val type = resolvedTypeRef.coneType
        type.forEachType l@{
            lookupType(it)
        }
    }

    private fun lookupType(type: ConeKotlinType) {
        val lookupTag = type.classLikeLookupTagIfAny ?: return
        if (lookupTag is ConeClassLookupTagWithFixedSymbol) return
        session.symbolProvider.getClassLikeSymbolByClassId(lookupTag.classId)
    }
}

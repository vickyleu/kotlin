/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.types.AbstractTypeChecker

fun createExpectActualTypeParameterSubstitutor(
    expectedTypeParameters: List<FirTypeParameterSymbol>,
    actualTypeParameters: List<FirTypeParameterSymbol>,
    useSiteSession: FirSession,
    parentSubstitutor: ConeSubstitutor? = null
): ConeSubstitutor {
    val substitution = expectedTypeParameters.zip(actualTypeParameters).associate { (expectedParameterSymbol, actualParameterSymbol) ->
        actualParameterSymbol to expectedParameterSymbol.toLookupTag().constructType(emptyArray(), isNullable = false)
    }
    val substitutor = ConeSubstitutorByMap(
        substitution,
        useSiteSession
    )
    if (parentSubstitutor == null) {
        return substitutor
    }
    return substitutor.chain(parentSubstitutor)
}

fun createActualTypeAliasBasedSubstitutor(
    actualTypeAlias: FirTypeAliasSymbol,
    useSiteSession: FirSession,
    parentSubstitutor: ConeSubstitutor? = null,
): ConeSubstitutor {
    val rhsTypeParameterSymbols = (actualTypeAlias.resolvedExpandedTypeRef
        .coneType
        .toSymbol(useSiteSession) as? FirClassLikeSymbol)
        ?.typeParameterSymbols ?: error("Unexpected rhs side for $actualTypeAlias type alias")
    val rhsTypeArguments = actualTypeAlias.resolvedExpandedTypeRef
        .type.typeArguments

    val substitution = rhsTypeParameterSymbols.zip(rhsTypeArguments)
        .associate { (rhsTypeParameterSymbol, rhsTypeArgument) ->
            // TODO: consider declaration site variance and upper bounds for star projections
            val rhsType = rhsTypeArgument.type ?: useSiteSession.builtinTypes.nullableAnyType.type
            val subbed = parentSubstitutor?.substituteOrSelf(rhsType) ?: rhsType
            rhsTypeParameterSymbol to subbed
        }
    val substitutor = ConeSubstitutorByMap(
        substitution,
        useSiteSession
    )
    if (parentSubstitutor == null) {
        return substitutor
    }
    return substitutor.chain(parentSubstitutor)
}

fun areCompatibleExpectActualTypes(
    expectedType: ConeKotlinType?,
    actualType: ConeKotlinType?,
    actualSession: FirSession
): Boolean {
    if (expectedType == null) return actualType == null
    if (actualType == null) return false

    return AbstractTypeChecker.equalTypes(
        actualSession.typeContext,
        expectedType,
        actualType
    )
}
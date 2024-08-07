/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

/**
 * A [FirSymbolProvider] which provides symbols from Kotlin sources via [KotlinDeclarationProvider].
 */
internal abstract class LLFirKotlinSymbolProvider(session: FirSession) : FirSymbolProvider(session) {
    abstract val declarationProvider: KotlinDeclarationProvider

    abstract val packageProvider: KotlinPackageProvider

    /**
     * Whether the [LLFirKotlinSymbolProvider] should be able to find symbols defined in `kotlin` packages. This is usually not the case for
     * source sessions, unless the `allowKotlinPackage` flag is enabled in the session's `languageVersionSettings`.
     */
    abstract val allowKotlinPackage: Boolean

    /**
     * This function is optimized for a known [classLikeDeclaration].
     */
    @FirSymbolProviderInternals
    abstract fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: KtClassLikeDeclaration): FirClassLikeSymbol<*>?

    /**
     * This function is optimized for known [callables].
     */
    @FirSymbolProviderInternals
    abstract fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<KtCallableDeclaration>,
    )

    /**
     * This function is optimized for known [functions].
     */
    @FirSymbolProviderInternals
    abstract fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<KtNamedFunction>,
    )

    /**
     * This function is optimized for known [properties].
     */
    @FirSymbolProviderInternals
    abstract fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<KtProperty>,
    )

    override fun toString(): String {
        return "${this::class.simpleName} for $session"
    }
}

/**
 * An [LLFirKotlinSymbolProvider] which represents the main Kotlin symbol provider for its session, covering all symbols directly declared
 * in Kotlin sources / deserialized stubs. It is related to the notion of a "main" [KotlinDeclarationProvider]. Only a single Kotlin symbol
 * provider per session may be a main provider.
 *
 * Main Kotlin symbol providers of different kinds may be combined into an [LLFirCombinedKotlinSymbolProvider]. The combined symbol provider
 * requires each symbol provider to have a unique [KaModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaModule], and since there
 * may be only a single main Kotlin symbol provider per session, we can combine even symbol providers of different kinds.
 */
internal abstract class LLFirMainKotlinSymbolProvider(session: FirSession) : LLFirKotlinSymbolProvider(session)

/**
 * An [LLFirKotlinSymbolProvider] which provides additional symbols as an extension to the [LLFirMainKotlinSymbolProvider]. Auxiliary symbol
 * providers usually provide symbols for generated declarations.
 *
 * In contrast to [LLFirMainKotlinSymbolProvider]s, auxiliary providers of different kinds may not be combined into an
 * [LLFirCombinedKotlinSymbolProvider]. It is however possible to combine auxiliary providers of a *specific kind* into such a provider,
 * since each session should only have a single symbol provider of the same kind.
 */
internal abstract class LLFirAuxiliaryKotlinSymbolProvider(session: FirSession) : LLFirKotlinSymbolProvider(session)

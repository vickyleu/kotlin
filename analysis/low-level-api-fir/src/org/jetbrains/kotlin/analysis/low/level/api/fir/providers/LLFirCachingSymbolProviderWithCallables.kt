/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.github.benmanes.caffeine.cache.Caffeine
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * An [LLFirCachingSymbolProvider] that additionally caches the following elements in fixed-size Caffeine caches:
 *
 * - Functions: Results from [computeTopLevelFunctionSymbols], including empty lists, are cached in a Caffeine cache.
 * - Properties: Results from [computeTopLevelPropertySymbols], including empty lists, are cached in a Caffeine cache.
 *
 * Note that `callableCacheSize` is the individual size of the function and property caches, not their combined size.
 */
internal abstract class LLFirCachingSymbolProviderWithCallables(
    session: FirSession,
    classifierCacheSize: Long,
    packageCacheSize: Long,
    callableCacheSize: Long,
) : LLFirCachingSymbolProvider(session, classifierCacheSize, packageCacheSize) {
    private val functionCache = Caffeine.newBuilder().maximumSize(callableCacheSize).build<CallableId, List<FirNamedFunctionSymbol>>()
    private val propertyCache = Caffeine.newBuilder().maximumSize(callableCacheSize).build<CallableId, List<FirPropertySymbol>>()

    protected abstract fun computeTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol>
    protected abstract fun computeTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol>

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val callableId = CallableId(packageFqName, name)
        destination += getTopLevelFunctionSymbols(callableId)
        destination += getTopLevelPropertySymbols(callableId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += getTopLevelFunctionSymbols(packageFqName, name)
    }

    override fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> =
        getTopLevelFunctionSymbols(CallableId(packageFqName, name))

    private fun getTopLevelFunctionSymbols(callableId: CallableId): List<FirNamedFunctionSymbol> =
        functionCache.get(callableId) { computeTopLevelFunctionSymbols(it.packageName, it.callableName) }
            ?: error("`computeTopLevelFunctionSymbols` should always compute a result.")

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += getTopLevelPropertySymbols(packageFqName, name)
    }

    override fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> =
        getTopLevelPropertySymbols(CallableId(packageFqName, name))

    private fun getTopLevelPropertySymbols(callableId: CallableId): List<FirPropertySymbol> =
        propertyCache.get(callableId) { computeTopLevelPropertySymbols(it.packageName, it.callableName) }
            ?: error("`computeTopLevelPropertySymbols` should always compute a result.")
}

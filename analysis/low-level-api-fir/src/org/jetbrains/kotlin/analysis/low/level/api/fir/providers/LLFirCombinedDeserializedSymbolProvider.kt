/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.github.benmanes.caffeine.cache.Caffeine
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.NegativeCaffeineCache
import org.jetbrains.kotlin.analysis.utils.collections.buildSmartList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.AbstractFirDeserializedSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.utils.SmartSet

/**
 * TODO (marco): Document.
 */
class LLFirCombinedDeserializedSymbolProvider(
    session: FirSession,
    classifierCacheSize: Long,
    callableCacheSize: Long,
    private val providers: List<AbstractFirDeserializedSymbolProvider>,
) : FirSymbolProvider(session) {
    private val classifierCache = NegativeCaffeineCache<ClassId, FirClassLikeSymbol<*>> { it.maximumSize(classifierCacheSize) }

    /**
     * [callableCache] is separate from [functionCache] and [propertyCache] because of JVM facade duplicate symbol handling, which requires
     * going through symbols provider by provider. Combining [functionCache] and [propertyCache] into some callables result is therefore not
     * possible.
     */
    private val callableCache = Caffeine.newBuilder().maximumSize(callableCacheSize).build<CallableId, List<FirCallableSymbol<*>>>()

    private val functionCache = Caffeine.newBuilder().maximumSize(callableCacheSize).build<CallableId, List<FirNamedFunctionSymbol>>()
    private val propertyCache = Caffeine.newBuilder().maximumSize(callableCacheSize).build<CallableId, List<FirPropertySymbol>>()

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        classifierCache.getOrCompute(classId) {
            providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }
        }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += callableCache.get(CallableId(packageFqName, name)) { callableId ->
            collectSymbolsConsideringJvmFacades { getTopLevelCallableSymbolsTo(it, callableId) }
        }!!
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += functionCache.get(CallableId(packageFqName, name)) { callableId ->
            collectSymbolsConsideringJvmFacades { getTopLevelFunctionSymbolsTo(it, callableId) }
        }!!
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += propertyCache.get(CallableId(packageFqName, name)) { callableId ->
            collectSymbolsConsideringJvmFacades { getTopLevelPropertySymbolsTo(it, callableId) }
        }!!
    }

    private inline fun <S : FirCallableSymbol<*>> collectSymbolsConsideringJvmFacades(
        getSymbols: AbstractFirDeserializedSymbolProvider.(MutableList<S>) -> Unit,
    ): List<S> {
        val symbols = mutableListOf<S>()
        val facades = SmartSet.create<JvmClassName>()
        for (provider in providers) {
            val newSymbols = buildSmartList {
                provider.getSymbols(this)
            }
            addNewSymbolsConsideringJvmFacades(symbols, newSymbols, facades)
        }
        return symbols
    }

    /**
     * Adds all [newSymbols] to [destination] whose [jvmClassName] isn't already contained in [facades]. [newSymbols] must be considered as
     * a unit produced by a single symbol provider, because symbols in [newSymbols] may have the same facade if it's new. If this function
     * is called with results from multiple symbol providers, duplicate callables might slip into [destination].
     */
    private fun <S : FirCallableSymbol<*>> addNewSymbolsConsideringJvmFacades(
        destination: MutableList<S>,
        newSymbols: List<S>,
        facades: MutableSet<JvmClassName>,
    ) {
        if (newSymbols.isEmpty()) return
        val newFacades = SmartSet.create<JvmClassName>()
        for (symbol in newSymbols) {
            val facade = symbol.jvmClassName()
            if (facade != null) {
                newFacades += facade
                if (facade !in facades) {
                    destination += symbol
                }
            } else {
                destination += symbol
            }
        }
        facades += newFacades
    }

    private fun FirCallableSymbol<*>.jvmClassName(): JvmClassName? {
        val jvmPackagePartSource = fir.containerSource as? JvmPackagePartSource ?: return null
        return jvmPackagePartSource.facadeClassName ?: jvmPackagePartSource.className
    }

    override fun getPackage(fqName: FqName): FqName? = providers.firstNotNullOfOrNull { it.getPackage(fqName) }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null
    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? = null
    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null

    companion object {
        fun merge(session: FirSession, providers: List<AbstractFirDeserializedSymbolProvider>): FirSymbolProvider? =
            if (providers.size > 1) LLFirCombinedDeserializedSymbolProvider(session, 1000, 250, providers)
            else providers.singleOrNull()
    }
}

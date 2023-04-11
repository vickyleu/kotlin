/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.utils.collections.buildSmartList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.utils.SmartSet

@OptIn(FirSymbolProviderInternals::class)
internal class LLFirModuleWithDependenciesSymbolProvider(
    session: FirSession,
    val providers: List<FirSymbolProvider>,
    val dependencyProvider: LLFirDependenciesSymbolProvider,
) : LLFirCachingSymbolProviderWithCallables(
    session,
    classifierCacheSize = 1000,
    packageCacheSize = 200,
    callableCacheSize = 500,
) {
    override fun computeClassLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>? =
        getClassLikeSymbolByClassIdWithoutDependencies(classId)
            ?: dependencyProvider.getClassLikeSymbolByClassIdWithoutCaching(classId)

    fun getClassLikeSymbolByClassIdWithoutDependencies(classId: ClassId): FirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        // We cannot combine function and property symbols here because `dependencyProvider` might deduplicate symbols based on JVM facades.
        // This is not critical for performance, because callable symbols are requested rarely.
        getTopLevelCallableSymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
    }

    @FirSymbolProviderInternals
    fun getTopLevelCallableSymbolsToWithoutDependencies(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        providers.forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
    }

    override fun computeTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> =
        buildList {
            getTopLevelFunctionSymbolsToWithoutDependencies(this, packageFqName, name)
            dependencyProvider.getTopLevelFunctionSymbolsToWithoutCaching(this, packageFqName, name)
        }

    @FirSymbolProviderInternals
    fun getTopLevelFunctionSymbolsToWithoutDependencies(
        destination: MutableList<FirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name
    ) {
        providers.forEach { it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }
    }

    override fun computeTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> =
        buildList {
            getTopLevelPropertySymbolsToWithoutDependencies(this, packageFqName, name)
            dependencyProvider.getTopLevelPropertySymbolsToWithoutCaching(this, packageFqName, name)
        }

    @FirSymbolProviderInternals
    fun getTopLevelPropertySymbolsToWithoutDependencies(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        providers.forEach { it.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }
    }

    override fun computePackage(packageName: FqName): FqName? =
        getPackageWithoutDependencies(packageName) ?: dependencyProvider.getPackageWithoutCaching(packageName)

    fun getPackageWithoutDependencies(fqName: FqName): FqName? =
        providers.firstNotNullOfOrNull { it.getPackage(fqName) }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null
    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? = null
    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null
}

/**
 * Most symbols found by [LLFirDependenciesSymbolProvider] are cached in [LLFirModuleWithDependenciesSymbolProvider], because it is called
 * from that symbol provider. Sometimes though, [LLFirDependenciesSymbolProvider] is called directly, which is why it is also a caching
 * symbol provider.
 *
 * [LLFirModuleWithDependenciesSymbolProvider] calls the non-caching functions of [LLFirDependenciesSymbolProvider] to avoid two cache
 * accesses in sequence. This also allows [LLFirDependenciesSymbolProvider]'s cache to optimize for the use cases where the symbol provider
 * is called directly, instead of through [LLFirModuleWithDependenciesSymbolProvider].
 */
@OptIn(FirSymbolProviderInternals::class)
internal class LLFirDependenciesSymbolProvider(
    session: FirSession,
    val providers: List<FirSymbolProvider>,
) : LLFirCachingSymbolProviderWithCallables(
    session,
    classifierCacheSize = 500,
    packageCacheSize = 100,
    callableCacheSize = 250,
) {
    init {
        require(providers.all { it !is LLFirModuleWithDependenciesSymbolProvider }) {
            "${LLFirDependenciesSymbolProvider::class.simpleName} may not contain ${LLFirModuleWithDependenciesSymbolProvider::class.simpleName}:" +
                    " dependency providers must be flattened during session creation."
        }
    }

    override fun computeClassLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>? =
        getClassLikeSymbolByClassIdWithoutCaching(classId)

    @FirSymbolProviderInternals
    fun getClassLikeSymbolByClassIdWithoutCaching(classId: ClassId): FirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in providers) {
            val newSymbols = buildSmartList {
                provider.getTopLevelCallableSymbolsTo(this, packageFqName, name)
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    override fun computeTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> =
        buildList { getTopLevelFunctionSymbolsToWithoutCaching(this, packageFqName, name) }

    @FirSymbolProviderInternals
    fun getTopLevelFunctionSymbolsToWithoutCaching(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in providers) {
            val newSymbols = buildSmartList {
                provider.getTopLevelFunctionSymbolsTo(this, packageFqName, name)
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    override fun computeTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> =
        buildList { getTopLevelPropertySymbolsToWithoutCaching(this, packageFqName, name) }

    @FirSymbolProviderInternals
    fun getTopLevelPropertySymbolsToWithoutCaching(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in providers) {
            val newSymbols = buildSmartList {
                provider.getTopLevelPropertySymbolsTo(this, packageFqName, name)
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    override fun computePackage(packageName: FqName): FqName? = getPackageWithoutCaching(packageName)

    @FirSymbolProviderInternals
    fun getPackageWithoutCaching(fqName: FqName): FqName? = providers.firstNotNullOfOrNull { it.getPackage(fqName) }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null
    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? = null
    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null

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
}

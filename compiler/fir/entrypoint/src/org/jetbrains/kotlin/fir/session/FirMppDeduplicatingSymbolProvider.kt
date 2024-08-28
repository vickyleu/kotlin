/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(SymbolInternals::class)
class FirMppDeduplicatingSymbolProvider(
    session: FirSession,
    val commonSymbolProvider: FirSymbolProvider,
    val platformSymbolProvider: FirSymbolProvider,
) : FirSymbolProvider(session) {
    val providers: List<FirSymbolProvider> = listOf(commonSymbolProvider, platformSymbolProvider)

    data class ClassPair(val commonClass: FirClassLikeSymbol<*>, val platformClass: FirClassLikeSymbol<*>)

    val classMapping: MutableMap<ClassId, ClassPair> = mutableMapOf()

    val processedCallables: MutableMap<CallableId, List<FirCallableSymbol<*>>> = mutableMapOf()
    val commonCallableToPlatformCallableMap: MutableMap<FirCallableSymbol<*>, FirCallableSymbol<*>> = mutableMapOf()
    val platformCallableToCommonCallableMap: MutableMap<FirCallableSymbol<*>, FirCallableSymbol<*>> = mutableMapOf()

    override val symbolNamesProvider: FirSymbolNamesProvider = FirCompositeSymbolNamesProvider.fromSymbolProviders(providers)

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId.shortClassName.asString() == "Bar") {
            Unit
        }
        val commonSymbol = commonSymbolProvider.getClassLikeSymbolByClassId(classId)
        val platformSymbol = platformSymbolProvider.getClassLikeSymbolByClassId(classId)

        return when {
            commonSymbol == null -> platformSymbol
            platformSymbol == null -> commonSymbol
            commonSymbol == platformSymbol -> commonSymbol
            else -> {
                classMapping[classId] = ClassPair(commonSymbol, platformSymbol)
                commonSymbol
            }
        }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        val callableId = CallableId(packageFqName, name)
        processedCallables[callableId]?.let { return it }

        val commonDeclarations = commonSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        val platformDeclarations = platformSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        val resultingDeclarations = preferCommonDeclarations(commonDeclarations, platformDeclarations)
        processedCallables[callableId] = resultingDeclarations
        return resultingDeclarations
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += getTopLevelCallableSymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getTopLevelCallableSymbols(packageFqName, name).filterIsInstanceTo<FirNamedFunctionSymbol, _>(destination)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        getTopLevelCallableSymbols(packageFqName, name).filterIsInstanceTo<FirPropertySymbol, _>(destination)
    }

    override fun getPackage(fqName: FqName): FqName? {
        return providers.firstNotNullOfOrNull { it.getPackage(fqName) }
    }

    private fun <D : FirCallableDeclaration, S : FirCallableSymbol<D>> preferCommonDeclarations(
        commonDeclarations: List<S>,
        platformDeclarations: List<S>,
    ): List<S> {
        val result = mutableListOf<S>()
        val notProcessedPlatformDeclarations = platformDeclarations.toMutableSet()

        outerLoop@ for (commonSymbol in commonDeclarations) {
            val platformIterator = notProcessedPlatformDeclarations.iterator()
            while (platformIterator.hasNext()) {
                val platformSymbol = platformIterator.next()
                if (areEquivalentTopLevelCallables(commonSymbol.fir, platformSymbol.fir)) {
                    result.add(commonSymbol)
                    commonCallableToPlatformCallableMap[commonSymbol] = platformSymbol
                    platformCallableToCommonCallableMap[platformSymbol] = commonSymbol
                    platformIterator.remove()
                    continue@outerLoop
                }
            }
            result += commonSymbol
        }
        result += notProcessedPlatformDeclarations
        return result
    }

    private fun areEquivalentTopLevelCallables(
        first: FirCallableDeclaration,
        second: FirCallableDeclaration,
    ): Boolean {
        // Emulate behavior from K1 where declarations from the same source module are never equivalent.
        // We expect REDECLARATION or CONFLICTING_OVERLOADS to be reported in those cases.
        // See a.containingDeclaration == b.containingDeclaration check in
        // org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides.areCallableDescriptorsEquivalent.
        // We can't rely on the fact that library declarations will have different moduleData, e.g. in Native metadata compilation,
        // multiple stdlib declarations with the same moduleData can be present, see KT-61461.
        if (first.moduleData == second.moduleData && first.moduleData.session.kind == FirSession.Kind.Source) return false
        if (first is FirVariable != second is FirVariable) {
            return false
        }
        if (!first.symbol.mappedArgumentsOrderRepresentation.contentEquals(second.symbol.mappedArgumentsOrderRepresentation)) {
            return false
        }

        val overrideChecker = FirStandardOverrideChecker(session)
        return when {
            first is FirProperty && second is FirProperty -> {
                overrideChecker.isOverriddenProperty(first, second, ignoreVisibility = true) &&
                        overrideChecker.isOverriddenProperty(second, first, ignoreVisibility = true)
            }
            first is FirSimpleFunction && second is FirSimpleFunction -> {
                overrideChecker.isOverriddenFunction(first, second, ignoreVisibility = true) &&
                        overrideChecker.isOverriddenFunction(second, first, ignoreVisibility = true)
            }
            else -> false
        }
    }

    private val FirCallableSymbol<*>.mappedArgumentsOrderRepresentation: IntArray?
        get() {
            val function = fir as? FirFunction ?: return null
            val parametersToIndices = function.valueParameters.mapIndexed { index, it -> it to index }.toMap()
            val mapping = function.valueParameters
            val result = IntArray(mapping.size + 1) { function.valueParameters.size }
            for ((index, parameter) in mapping.withIndex()) {
                result[index + 1] = parametersToIndices[parameter] ?: error("Unmapped argument in arguments mapping")
            }
            return result
        }
}
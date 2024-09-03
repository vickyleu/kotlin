/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.actualizer.IrExpectActualMapPreFiller
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrClassifierStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrDeclarationStorage
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.session.FirMppDeduplicatingSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled


class IrCommonToPlatformDependencyExpectActualMapPreFiller(
    val deduplicatingProvider: FirMppDeduplicatingSymbolProvider,
    private val classifierStorage: Fir2IrClassifierStorage,
    private val declarationStorage: Fir2IrDeclarationStorage,
) : IrExpectActualMapPreFiller() {
    companion object {
        fun create(
            platformSession: FirSession,
            classifierStorage: Fir2IrClassifierStorage,
            declarationStorage: Fir2IrDeclarationStorage
        ): IrCommonToPlatformDependencyExpectActualMapPreFiller? {
            val deduplicatingProvider = (platformSession.symbolProvider as FirCachingCompositeSymbolProvider)
                .providers
                .firstIsInstanceOrNull<FirMppDeduplicatingSymbolProvider>()
            if (deduplicatingProvider == null) return null
            return IrCommonToPlatformDependencyExpectActualMapPreFiller(
                deduplicatingProvider, classifierStorage, declarationStorage
            )
        }
    }

    override fun collectClassesMap(): ActualClassInfo {
        val classMapping = mutableMapOf<IrClassSymbol, IrClassSymbol>()
        val actualTypeAliases = mutableMapOf<ClassId, IrTypeAliasSymbol>()
        for ((commonFirClassSymbol, platformFirClassSymbol) in deduplicatingProvider.classMapping.values) {
            val commonIrClassSymbol = commonFirClassSymbol.toIrSymbol() as IrClassSymbol
            val platformClassSymbol = when (val platformSymbol = platformFirClassSymbol.toIrSymbol()) {
                is IrClassSymbol -> platformSymbol
                is IrTypeAliasSymbol -> {
                    actualTypeAliases[platformFirClassSymbol.classId] = platformSymbol
                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    platformSymbol.owner.expandedType.type.classOrFail
                }
                else -> error("Unexpected symbol: $commonIrClassSymbol")
            }
            classMapping[commonIrClassSymbol] = platformClassSymbol
        }
        return ActualClassInfo(classMapping, actualTypeAliases)
    }

    override fun collectTopLevelCallablesMap(): Map<IrSymbol, IrSymbol> {
        return deduplicatingProvider.commonCallableToPlatformCallableMap.entries.associate { (commonFirSymbol, platformFirSymbol) ->
            val commonIrSymbol = commonFirSymbol.toIrSymbol()
            val platformIrSymbol = platformFirSymbol.toIrSymbol()
            commonIrSymbol to platformIrSymbol
        }
    }

    private fun FirClassLikeSymbol<*>.toIrSymbol(): IrSymbol {
        return when (this) {
            is FirClassSymbol -> classifierStorage.getIrClassSymbol(this)
            is FirTypeAliasSymbol -> classifierStorage.getIrTypeAliasSymbol(this)
        }
    }

    private fun FirCallableSymbol<*>.toIrSymbol(): IrSymbol {
        return when (this) {
            is FirNamedFunctionSymbol -> declarationStorage.getIrFunctionSymbol(this)
            is FirPropertySymbol -> declarationStorage.getIrPropertySymbol(this)
            else -> shouldNotBeCalled()
        }
    }
}

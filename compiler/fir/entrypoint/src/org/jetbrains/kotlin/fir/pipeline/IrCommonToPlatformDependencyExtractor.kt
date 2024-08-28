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

    override fun collectClassesMap(): Map<IrClassSymbol, IrClassSymbol> {
        return deduplicatingProvider.classMapping.values.associate { (commonFirClassSymbol, platformFirClassSymbol) ->
            val commonIrClassSymbol = commonFirClassSymbol.toIrSymbol()
            val platformIrClassSymbol = platformFirClassSymbol.toIrSymbol()
            commonIrClassSymbol to platformIrClassSymbol
        }
    }

    override fun collectTopLevelCallablesMap(): Map<IrSymbol, IrSymbol> {
        return deduplicatingProvider.commonCallableToPlatformCallableMap.entries.associate { (commonFirSymbol, platformFirSymbol) ->
            val commonIrSymbol = commonFirSymbol.toIrSymbol()
            val platformIrSymbol = platformFirSymbol.toIrSymbol()
            commonIrSymbol to platformIrSymbol
        }
    }

    private fun FirClassLikeSymbol<*>.toIrSymbol(): IrClassSymbol {
        return classifierStorage.getIrClassSymbol(this as FirClassSymbol)
    }

    private fun FirCallableSymbol<*>.toIrSymbol(): IrSymbol {
        return when (this) {
            is FirNamedFunctionSymbol -> declarationStorage.getIrFunctionSymbol(this)
            is FirPropertySymbol -> declarationStorage.getIrPropertySymbol(this)
            else -> shouldNotBeCalled()
        }
    }
}
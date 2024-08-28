/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.extensions.FirSwitchableExtensionDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider

class SessionProviderStructure(
    val sourceProviders: SourceProviders,
    val icProviders: List<FirSymbolProvider>,
    val dependencyProviders: List<FirSymbolProvider>,
    val builtinProviders: List<FirSymbolProvider>,
) {
    class SourceProviders(
        val sourceProvider: FirSymbolProvider,
        val pluginDeclarationsProvider: FirSwitchableExtensionDeclarationsSymbolProvider?
    )
}
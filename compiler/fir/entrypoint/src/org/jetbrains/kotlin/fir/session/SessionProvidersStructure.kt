/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider

class SessionProvidersStructure(
    val sourceProviders: List<FirSymbolProvider>,
    val libraryProviders: List<FirSymbolProvider>,
) : FirSessionComponent

val FirSession.structuredSymbolProviders: SessionProvidersStructure by FirSession.sessionComponentAccessor()
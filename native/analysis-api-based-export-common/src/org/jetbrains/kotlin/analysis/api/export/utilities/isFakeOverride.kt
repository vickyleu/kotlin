/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.export.utilities

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin

/**
 * @return `true` when the declaration is considered a `fake override`.
 * K2 will differentiate fake-overrides into 'intersection override' and 'substitution override'
 * `false` if the symbol is not a fake override
 *
 * See:
 * - [org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.isReal]
 * - [KtSymbolOrigin.INTERSECTION_OVERRIDE]
 * - [KtSymbolOrigin.SUBSTITUTION_OVERRIDE]
 */
public val KaSymbol.isFakeOverride: Boolean
    get() {
        val origin = this.origin
        if (origin == KaSymbolOrigin.INTERSECTION_OVERRIDE) return true
        if (origin == KaSymbolOrigin.SUBSTITUTION_OVERRIDE) return true
        return false
    }
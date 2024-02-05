/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.name.StandardClassIds

object FirDataSealedArgumentClassChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.hasAnnotation(StandardClassIds.Annotations.DataArgument, context.session)) {
            if (!checkDataArgument(declaration, context.session)) {
                reporter.reportOn(declaration.source, FirErrors.INCORRECT_DATAARG_CLASS, context)
            }
        }
        if (declaration.hasAnnotation(StandardClassIds.Annotations.SealedArgument, context.session)) {
            if (!checkSealedArgument(declaration, context.session)) {
                reporter.reportOn(declaration.source, FirErrors.INCORRECT_SEALEDARG_CLASS, context)
            }
        }
    }

    private fun checkDataArgument(declaration: FirClass, session: FirSession): Boolean {
        val constructor = declaration.primaryConstructorIfAny(session) ?: return false
        for (parameter in constructor.valueParameterSymbols) {
            if (parameter.isVararg) return false
            if (parameter.isDataArgument) return false
            if (parameter.isSealedArgument) return false
            if (!parameter.hasDefaultValue) return false
        }
        return true
    }

    private fun checkSealedArgument(declaration: FirClass, session: FirSession): Boolean  {
        if (declaration !is FirRegularClass) return false
        if (!declaration.isSealed) return false

        val knownTypes = mutableSetOf<ConeKotlinType>()
        for (subclassId in declaration.getSealedClassInheritors(session)) {
            val subclass = session.getRegularClassSymbolByClassId(subclassId) ?: return false
            val constructor = subclass.primaryConstructorSymbol(session) ?: return false
            if (constructor.typeParameterSymbols.isNotEmpty()) return false
            if (constructor.valueParameterSymbols.size != 1) return false
            val singleParameter = constructor.valueParameterSymbols.single()
            if (singleParameter.isVararg) return false
            if (singleParameter.isDataArgument) return false
            if (singleParameter.isSealedArgument) return false
            val type = singleParameter.resolvedReturnType
            if (knownTypes.any { type.isSubtypeOf(it, session) || it.isSubtypeOf(type, session) }) return false
            knownTypes.add(type)
        }
        return true
    }
}
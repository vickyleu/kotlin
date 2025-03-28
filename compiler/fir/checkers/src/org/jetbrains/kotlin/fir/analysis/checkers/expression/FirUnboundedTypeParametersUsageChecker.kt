/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.resolvedType

object FirUnboundedTypeParametersUsageChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    lateinit var report: (String) -> Unit

    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        report = {
            reporter.reportOn(
                expression.source,
                FirErrors.INVALID_USAGE_OF_UNBOUNDED_TYPE_PARAMETER,
                "ROMANV; $it;ROMANV",
                context,
            )
        }

        when (expression) {
            is FirSafeCallExpression -> {
                expression.receiver.reportIfUnboundedTypeParameter { "" }
            }
            is FirEqualityOperatorCall -> {
                expression.argumentList.arguments.forEach {
                    it.reportIfUnboundedTypeParameter { "" }
                }
            }
        }
    }

    fun <T> test(t: T) {
        t?.let { println("Kitty") }
    }

    inline fun FirExpression.reportIfUnboundedTypeParameter(message: () -> String) {
        if (resolvedType.isUnboundedTypeParameter) {
            report(message())
        }
    }

    val ConeKotlinType?.isUnboundedTypeParameter
        get() = this is ConeTypeParameterType && lookupTag.typeParameterSymbol.resolvedBounds.isEmpty()
}
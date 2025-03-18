/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.ErrorNodeDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferType
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirDesugaredAssignmentValueReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.forEachExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.renderForDebugging
import org.jetbrains.kotlin.fir.types.resolvedType

object FirResultAsTypeArgumentChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        // Pure statements have no type
        if (expression !is FirExpression) return
        // Filter non-error types from the beginning
        val type = expression.resolvedType
        type.classId?.let {
            if (it.shortClassName.toString() == "Result" ||
                it.shortClassName.toString() == "Either" ||
                it.asString() == "kotlin/Result"
            ) {
                return
            }
        }
        var contains: String? = null
        type.forEachExpandedType(context.session) { type ->
            type.classId?.let {
                if (it.shortClassName.toString() == "Result") {
                    contains = "Result"
                }
                if (it.shortClassName.toString() == "Either") {
                    contains = "Either"
                }
                if (it.asString() == "kotlin/Result") {
                    contains = "KResult"
                }
            }
        }
        if (contains == null) return
        val content = buildString {
            append(" CONTAINED_TYPE: $contains;")
            append(" TYPE: ${type.renderForDebugging()};")
        }
        reporter.reportOn(
            expression.source,
            FirErrors.RESULT_AS_TYPE_PARAMETER,
            "ROMANV;${content}ROMANV",
            context
        )
    }
}

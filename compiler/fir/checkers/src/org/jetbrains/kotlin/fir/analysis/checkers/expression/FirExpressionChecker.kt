/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.FirCheckerWithMppKind
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirStatement

abstract class FirExpressionChecker<in E : FirStatement>(final override val mppKind: MppCheckerKind) : FirCheckerWithMppKind {
    abstract fun check(expression: E, context: CheckerContext, reporter: DiagnosticReporter)
}

abstract class FirExpressionCheckerContextual<in E : FirStatement>(mppKind: MppCheckerKind) : FirExpressionChecker<E>(mppKind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(expression: E)

    final override fun check(expression: E, context: CheckerContext, reporter: DiagnosticReporter) {
        with(context) {
            with(reporter) {
                check(expression)
            }
        }
    }
}


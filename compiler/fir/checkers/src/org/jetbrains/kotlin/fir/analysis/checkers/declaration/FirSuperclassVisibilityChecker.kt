/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.typeContext

object FirSuperclassVisibilityChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val selfEffVis = declaration.effectiveVisibility
        declaration.superTypeRefs.forEach {
            check(it, selfEffVis, context) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.MY_ERROR,
                    "ROMANV; " + it,
                    context
                )
            }
        }
    }

    fun check2(typeProj: ConeTypeProjection, selfEffVis: EffectiveVisibility, context: CheckerContext, report: (String) -> Unit) {
        typeProj.type?.typeArguments?.forEach { check2(it, selfEffVis, context, report) }
        val effVis = typeProj.type?.toClassLikeSymbol(context.session)?.effectiveVisibility ?: return
        when (effVis.relation(selfEffVis, context.session.typeContext)) {
            EffectiveVisibility.Permissiveness.LESS -> {
                report("")
            }
            else -> return
        }
    }

    fun check(typeRef: FirTypeRef, selfEffVis: EffectiveVisibility, context: CheckerContext, report: (String) -> Unit) {
        typeRef.coneTypeOrNull?.typeArguments?.forEach { check2(it, selfEffVis, context, report) }
        val effVis = typeRef.toClassLikeSymbol(context.session)?.effectiveVisibility ?: return
        when (effVis.relation(selfEffVis, context.session.typeContext)) {
            EffectiveVisibility.Permissiveness.LESS -> {
                report("")
            }
            else -> return
        }
    }
}

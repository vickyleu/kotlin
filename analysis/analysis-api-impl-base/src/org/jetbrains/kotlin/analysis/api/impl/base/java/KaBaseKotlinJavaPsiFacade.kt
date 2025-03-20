/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.java

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.restrictedAnalysis.KotlinRestrictedAnalysisService
import org.jetbrains.kotlin.analysis.api.platform.restrictedAnalysis.withRestrictedDataAccess
import org.jetbrains.kotlin.analysis.utils.isKotlinPackage
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade

/**
 * A [KotlinJavaPsiFacade] which additionally supports restricted analysis according to [KotlinRestrictedAnalysisService].
 */
internal class KaBaseKotlinJavaPsiFacade(project: Project) : KotlinJavaPsiFacade(project) {
    @KaCachedService
    private val restrictedAnalysisService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinRestrictedAnalysisService.getInstance(project)
    }

    override fun findClass(request: JavaClassFinder.Request, scope: GlobalSearchScope): JavaClass? {
        if (request.classId.isKotlinOnly()) return null
        return restrictedAnalysisService.withRestrictedDataAccess { super.findClass(request, scope) }
    }

    override fun findClasses(request: JavaClassFinder.Request, scope: GlobalSearchScope): List<JavaClass?> {
        if (request.classId.isKotlinOnly()) return emptyList()
        return restrictedAnalysisService.withRestrictedDataAccess { super.findClasses(request, scope) }
    }

    override fun knownClassNamesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Set<String?>? =
        restrictedAnalysisService.withRestrictedDataAccess { super.knownClassNamesInPackage(packageFqName, scope) }

    override fun findPackage(qualifiedName: String, searchScope: GlobalSearchScope): PsiPackage? =
        restrictedAnalysisService.withRestrictedDataAccess { super.findPackage(qualifiedName, searchScope) }

    /**
     * Whether the given [ClassId] is Kotlin-only, meaning the Java PSI facade won't find a corresponding result.
     *
     * Class IDs which *cannot* represent a Java class are those limited to Kotlin itself, i.e. `kotlin.*` classes, except for some internal
     * packages such as `kotlin.jvm.internal`. This filters out a huge number of requests to Java symbol providers (see KT-76157).
     */
    private fun ClassId.isKotlinOnly(): Boolean {
        val packageString = packageFqName.asString()
        return packageString.isKotlinPackage() && !packageString.contains("internal")
    }

    override fun shouldUseSlowResolve(): Boolean {
        // The Analysis API has a slightly different notion of whether slow resolve should be used based on restricted analysis concepts.
        // When restricted analysis is allowed, we should use index accesses instead of "slow resolve" in the form of alternative resolution
        // mode.
        return super.shouldUseSlowResolve() && restrictedAnalysisService?.isRestrictedAnalysisAllowed == false
    }
}

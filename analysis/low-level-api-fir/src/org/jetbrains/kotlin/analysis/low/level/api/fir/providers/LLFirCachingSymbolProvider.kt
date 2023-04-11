/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.NullableCaffeineCache
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * A symbol provider which caches the following elements in limited-size Caffeine caches:
 *
 * - Classifiers: Results from [computeClassLikeSymbol] are cached in a nullable cache.
 * - Packages: Results from [computePackage] are cached in a nullable cache.
 */
internal abstract class LLFirCachingSymbolProvider(
    session: FirSession,
    classifierCacheSize: Long,
    packageCacheSize: Long,
) : FirSymbolProvider(session) {
    private val classifierCache = NullableCaffeineCache<ClassId, FirClassLikeSymbol<*>> { it.maximumSize(classifierCacheSize) }

    private val packageCache = NullableCaffeineCache<FqName, FqName> { it.maximumSize(packageCacheSize) }

    protected abstract fun computeClassLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>?

    protected abstract fun computePackage(packageName: FqName): FqName?

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        classifierCache.get(classId, ::computeClassLikeSymbol)

    override fun getPackage(fqName: FqName): FqName? = packageCache.get(fqName, ::computePackage)
}

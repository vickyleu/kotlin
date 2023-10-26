/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.isDefinitelyEmpty
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

internal fun createStubBasedFirSymbolProviderForClassFiles(
    project: Project,
    baseScope: GlobalSearchScope,
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
): FirSymbolProvider? = createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    project, baseScope, session, moduleDataProvider, kotlinScopeProvider,
    fileFilter = { file ->
        val extension = file.extension
        extension == JavaClassFileType.INSTANCE.defaultExtension || extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION
    },
)

internal fun createStubBasedFirSymbolProviderForCommonMetadataFiles(
    project: Project,
    baseScope: GlobalSearchScope,
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
): FirSymbolProvider? = createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    project, baseScope, session, moduleDataProvider, kotlinScopeProvider,
    fileFilter = { file ->
        val extension = file.extension
        extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION ||
                extension == MetadataPackageFragment.METADATA_FILE_EXTENSION ||
                // klib metadata symbol provider
                extension == KLIB_METADATA_FILE_EXTENSION
    },
)

internal fun createStubBasedFirSymbolProviderForKotlinNativeMetadataFiles(
    project: Project,
    baseScope: GlobalSearchScope,
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
): FirSymbolProvider? = createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    project, baseScope, session, moduleDataProvider, kotlinScopeProvider,
    fileFilter = { file -> file.extension == KLIB_METADATA_FILE_EXTENSION },
)

/**
 * Creates a [StubBasedFirDeserializedSymbolProvider] for the given parameters. If the symbol provider cannot provide any symbols, for
 * example when the given scope doesn't contain any Kotlin-compiled classes, this function returns `null`.
 */
internal fun createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    project: Project,
    baseScope: GlobalSearchScope,
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    fileFilter: (VirtualFile) -> Boolean,
): StubBasedFirDeserializedSymbolProvider? {
    val scopeWithFileFiltering = object : DelegatingGlobalSearchScope(project, baseScope) {
        override fun contains(file: VirtualFile): Boolean {
            if (!fileFilter(file)) {
                return false
            }
            return super.contains(file)
        }
    }

    val symbolProvider = StubBasedFirDeserializedSymbolProvider(
        session,
        moduleDataProvider,
        kotlinScopeProvider,
        project,
        scopeWithFileFiltering,
        FirDeclarationOrigin.Library,
    )

    // This might compute package name sets, which can be quite heavy, but they will be cached and used later.
    if (symbolProvider.symbolNamesProvider.isDefinitelyEmpty()) {
        return null
    }

    return symbolProvider
}

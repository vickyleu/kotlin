/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.FirIdentityLessPlatformDeterminer
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsIdentityLessPlatformDeterminer
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsModuleKind
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.checkers.registerJsCheckers
import org.jetbrains.kotlin.fir.declarations.FirTypeSpecificityComparatorProvider
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirExtensionSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportProviderHolder
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.new.NewFirAbstractSessionFactory
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.js.resolve.JsTypeSpecificityComparatorWithoutDelegate
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.serialization.js.ModuleKind

@OptIn(SessionConfiguration::class)
object NewFirJsSessionFactory : NewFirAbstractSessionFactory<NewFirJsSessionFactory.LibraryContext, NewFirJsSessionFactory.SourceContext>() {

    // ==================================== Shared dependency session ====================================

    fun createSharedDependencySession(
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings
    ): FirSession {
        val context = LibraryContext(configuration = CompilerConfiguration.EMPTY, resolvedLibraries = emptyList())
        return createSharedDependencySession(
            moduleDataProvider,
            context,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings
        )
    }

    override fun setupSharedDependencySymbolProviders(
        session: FirSession,
        moduleData: FirModuleData,
        moduleDataProvider: ModuleDataProvider,
        kotlinScopeProvider: FirKotlinScopeProvider,
        syntheticFunctionInterfaceProvider: FirExtensionSyntheticFunctionInterfaceProvider?,
        c: LibraryContext,
    ): List<FirSymbolProvider> {
        return listOfNotNull(
            FirBuiltinSyntheticFunctionInterfaceProvider.initialize(session, moduleData, kotlinScopeProvider),
            syntheticFunctionInterfaceProvider
        )
    }

    // ==================================== Library session ====================================

    override fun setupDependencySymbolProviders(
        session: FirSession,
        moduleDataProvider: ModuleDataProvider,
        kotlinScopeProvider: FirKotlinScopeProvider,
        c: LibraryContext,
    ): List<FirSymbolProvider> {
        return listOfNotNull(
            KlibBasedSymbolProvider(
                session, moduleDataProvider, kotlinScopeProvider, c.resolvedLibraries,
                flexibleTypeFactory = JsFlexibleTypeFactory(session),
            )
        )
    }

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider()
    }

    override fun FirSession.registerLibrarySessionComponents(c: LibraryContext) {
        registerComponents(c.configuration)
    }

    // ==================================== Platform session ====================================

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        sharedDependencySession: FirSession,
        moduleDataProvider: ModuleDataProvider,
        compilerConfiguration: CompilerConfiguration,
        resolvedLibraries: List<KotlinLibrary>,
        extensionRegistrars: List<FirExtensionRegistrar>,
        lookupTracker: LookupTracker?,
        enumWhenTracker: EnumWhenTracker?,
        importTracker: ImportTracker?,
        init: FirSessionConfigurator.() -> Unit,
    ): FirSession {
        val libraryContext = LibraryContext(compilerConfiguration, resolvedLibraries)
        val sourceContext = SourceContext(compilerConfiguration)
        return createModuleBasedSession(
            moduleData,
            sharedDependencySession,
            moduleDataProvider,
            libraryContext,
            sourceContext,
            sessionProvider,
            extensionRegistrars,
            compilerConfiguration.languageVersionSettings,
            lookupTracker,
            enumWhenTracker = enumWhenTracker,
            importTracker = importTracker,
            init,
        )
    }

    override fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): FirKotlinScopeProvider {
        return FirKotlinScopeProvider()
    }

    override fun FirSessionConfigurator.registerPlatformCheckers(c: SourceContext) {
        registerJsCheckers()
    }

    override fun FirSession.registerSourceSessionComponents(c: SourceContext) {
        registerComponents(c.configuration)
    }

    override fun setupPlatformSpecificSourceSessionProviders(
        session: FirSession,
        moduleData: FirModuleData,
        c: SourceContext,
    ): List<FirSymbolProvider> {
        return emptyList()
    }

    // ==================================== Common parts ====================================

    private fun FirSession.registerComponents(compilerConfiguration: CompilerConfiguration) {
        val moduleKind = compilerConfiguration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        registerDefaultComponents()
        registerJsComponents(moduleKind)
    }

    fun FirSession.registerJsComponents(moduleKind: ModuleKind?) {
        register(ConeCallConflictResolverFactory::class, JsCallConflictResolverFactory)
        register(
            FirTypeSpecificityComparatorProvider::class,
            FirTypeSpecificityComparatorProvider(JsTypeSpecificityComparatorWithoutDelegate(typeContext))
        )
        register(FirPlatformDiagnosticSuppressor::class, FirJsPlatformDiagnosticSuppressor())
        register(FirIdentityLessPlatformDeterminer::class, FirJsIdentityLessPlatformDeterminer)

        if (moduleKind != null) {
            register(FirJsModuleKind::class, FirJsModuleKind(moduleKind))
        }
        register(FirDefaultImportProviderHolder::class, FirDefaultImportProviderHolder(JsPlatformAnalyzerServices))
    }

    // ==================================== Utilities ====================================

    class LibraryContext(
        val configuration: CompilerConfiguration,
        val resolvedLibraries: List<KotlinLibrary>,
    )

    class SourceContext(val configuration: CompilerConfiguration)
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session.new

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerCommonCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSwitchableExtensionDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.java.FirCliSession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirExtensionSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirLibrarySessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
abstract class NewFirAbstractSessionFactory<LIBRARY_CONTEXT, SOURCE_CONTEXT> {

    // ==================================== Shared dependency session ====================================

    protected fun createSharedDependencySession(
        moduleDataProvider: ModuleDataProvider,
        context: LIBRARY_CONTEXT,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings
    ): FirSession {
        return FirCliSession(sessionProvider, FirSession.Kind.Library).apply {
            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)
            registerLibrarySessionComponents(context)

            val kotlinScopeProvider = createKotlinScopeProviderForLibrarySession()
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val builtinsModuleData = BinaryModuleData.createDependencyModuleData(
                Name.special("<shared dependencies>"),
                moduleDataProvider.platform,
            )
            builtinsModuleData.bindSession(this)

            FirSessionConfigurator(this).apply {
                for (extensionRegistrar in extensionRegistrars) {
                    registerExtensions(extensionRegistrar.configure())
                }
            }.configure()
            registerCommonComponentsAfterExtensionsAreConfigured()

            val syntheticFunctionInterfaceProvider = FirExtensionSyntheticFunctionInterfaceProvider.createIfNeeded(
                this,
                builtinsModuleData,
                kotlinScopeProvider
            )

            val providers = setupSharedDependencySymbolProviders(
                this,
                builtinsModuleData,
                moduleDataProvider,
                kotlinScopeProvider,
                syntheticFunctionInterfaceProvider,
                context
            )
            val symbolProvider = FirCachingCompositeSymbolProvider.create(this, providers)
            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
        }
    }

    protected abstract fun setupSharedDependencySymbolProviders(
        session: FirSession,
        moduleData: FirModuleData,
        moduleDataProvider: ModuleDataProvider,
        kotlinScopeProvider: FirKotlinScopeProvider,
        syntheticFunctionInterfaceProvider: FirExtensionSyntheticFunctionInterfaceProvider?,
        c: LIBRARY_CONTEXT
    ): List<FirSymbolProvider>

    // ==================================== Library session ====================================

    protected fun createLibrarySession(
        context: LIBRARY_CONTEXT,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        languageVersionSettings: LanguageVersionSettings,
        sharedDependencySession: FirSession
    ): FirSession {
        return FirCliSession(sessionProvider, FirSession.Kind.Library).apply session@{
            moduleDataProvider.allModuleData.forEach {
                sessionProvider.registerSession(it, this)
                it.bindSession(this)
            }

            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)
            registerLibrarySessionComponents(context)

            val kotlinScopeProvider = createKotlinScopeProviderForLibrarySession()
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)
            registerCommonComponentsAfterExtensionsAreConfigured()

            val realLibraryProviders = setupDependencySymbolProviders(
                this,
                moduleDataProvider,
                kotlinScopeProvider,
                context
            )
            register(LibrarySessionRealProviders::class, LibrarySessionRealProviders(realLibraryProviders))

            val allProviders = realLibraryProviders + sharedDependencySession.symbolProvider.decompose()

            val symbolProvider = FirCachingCompositeSymbolProvider.create(this, allProviders)
            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
        }
    }

    protected abstract fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider
    protected abstract fun FirSession.registerLibrarySessionComponents(c: LIBRARY_CONTEXT)
    protected abstract fun setupDependencySymbolProviders(
        session: FirSession,
        moduleDataProvider: ModuleDataProvider,
        kotlinScopeProvider: FirKotlinScopeProvider,
        c: LIBRARY_CONTEXT
    ): List<FirSymbolProvider>

    // ==================================== Platform session ====================================

    protected fun createModuleBasedSession(
        moduleData: FirModuleData,
        sharedDependencySession: FirSession,
        moduleDataProvider: ModuleDataProvider,
        libraryContext: LIBRARY_CONTEXT,
        sourceContext: SOURCE_CONTEXT,
        sessionProvider: FirProjectSessionProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        lookupTracker: LookupTracker?,
        enumWhenTracker: EnumWhenTracker?,
        importTracker: ImportTracker?,
        init: FirSessionConfigurator.() -> Unit,
    ): FirSession {
        val librarySession = createLibrarySession(
            libraryContext,
            sessionProvider,
            moduleDataProvider,
            languageVersionSettings,
            sharedDependencySession
        )

        return FirCliSession(sessionProvider, FirSession.Kind.Source).apply session@{
            moduleData.bindSession(this@session)
            sessionProvider.registerSession(moduleData, this@session)
            registerModuleData(moduleData)
            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)
            registerResolveComponents(lookupTracker, enumWhenTracker, importTracker)
            registerSourceSessionComponents(sourceContext)

            val kotlinScopeProvider = createKotlinScopeProviderForSourceSession(moduleData, languageVersionSettings)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val firProvider = FirProviderImpl(this, kotlinScopeProvider)
            register(FirProvider::class, firProvider)

            FirSessionConfigurator(this).apply {
                registerCommonCheckers()
                registerPlatformCheckers(sourceContext)

                for (extensionRegistrar in extensionRegistrars) {
                    registerExtensions(extensionRegistrar.configure())
                }
                init()
            }.configure()
            registerCommonComponentsAfterExtensionsAreConfigured()

            setupSymbolProviders(this, firProvider, moduleData, sourceContext, librarySession, sharedDependencySession)
        }
    }

    private fun setupSymbolProviders(
        session: FirCliSession,
        firProvider: FirProviderImpl,
        moduleData: FirModuleData,
        sourceContext: SOURCE_CONTEXT,
        librarySession: FirSession,
        sharedDependencySession: FirSession,
    ) {
        val generatedSymbolsProvider = FirSwitchableExtensionDeclarationsSymbolProvider.createIfNeeded(session)

        val sourceProviders = buildList {
            add(firProvider.symbolProvider)
            addIfNotNull(generatedSymbolsProvider)
            addAll(setupPlatformSpecificSourceSessionProviders(session, moduleData, sourceContext))
        }

        val dependsOnSessions = session.findDependsOnSessions(moduleData)
        val dependsOnSourceProviders = dependsOnSessions.flatMap {
            it.structuredSymbolProviders.sourceProviders
        }

        val libraryProviders = librarySession.realLibraryProviders.providers

        val sharedProviders = sharedDependencySession.symbolProvider.decompose()

        // TODO: should we use deduplicating providers from intermediate sessions here?
        val libraryProvidersFromDependsOnSessions = dependsOnSessions.flatMap {
            it.structuredSymbolProviders.libraryProviders
        }

        val deduplicatingProviderForLibraries = FirMppDeduplicatingSymbolProvider(
            session,
            commonSymbolProvider = FirCompositeSymbolProvider.create(session, libraryProvidersFromDependsOnSessions),
            platformSymbolProvider = FirCompositeSymbolProvider.create(session, libraryProviders)
        )

        val allProviders = buildList {
            addAll(sourceProviders)
            addAll(dependsOnSourceProviders)
            add(deduplicatingProviderForLibraries)
            addAll(sharedProviders)
        }

        session.register(
            FirSymbolProvider::class,
            FirCachingCompositeSymbolProvider.create(
                session, allProviders,
                expectedCachesToBeCleanedOnce = generatedSymbolsProvider != null
            )
        )

        generatedSymbolsProvider?.let { session.register(FirSwitchableExtensionDeclarationsSymbolProvider::class, it) }

        session.register(
            SessionProvidersStructure::class,
            SessionProvidersStructure(sourceProviders, libraryProviders)
        )

        val dependencyProviders = buildList {
            addAll(dependsOnSourceProviders)
            addAll(libraryProviders)
            addAll(sharedProviders)
        }

        session.register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, FirCachingCompositeSymbolProvider.create(session, dependencyProviders))
    }

    protected abstract fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData, languageVersionSettings: LanguageVersionSettings
    ): FirKotlinScopeProvider

    protected abstract fun FirSessionConfigurator.registerPlatformCheckers(c: SOURCE_CONTEXT)
    protected abstract fun FirSession.registerSourceSessionComponents(c: SOURCE_CONTEXT)

    protected abstract fun setupPlatformSpecificSourceSessionProviders(
        session: FirSession,
        moduleData: FirModuleData,
        c: SOURCE_CONTEXT
    ): List<FirSymbolProvider>

    // ==================================== Common parts ====================================

    // ==================================== Utilities ====================================

    private fun FirSession.findDependsOnSessions(moduleData: FirModuleData): List<FirSession> {
        return (moduleData.allDependsOnDependencies)
            .mapNotNull { sessionProvider?.getSession(it) }
            .onEach { check(it.kind == FirSession.Kind.Source) }
    }

    private fun FirSymbolProvider.decompose(): List<FirSymbolProvider> {
        return when (this) {
            is FirCompositeSymbolProvider -> providers
            is FirCachingCompositeSymbolProvider -> providers
            else -> listOf(this)
        }
    }
}

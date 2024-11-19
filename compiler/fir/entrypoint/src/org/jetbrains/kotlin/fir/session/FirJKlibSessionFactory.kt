/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerJvmCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.FirJvmActualizingBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.*
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

@OptIn(SessionConfiguration::class)
object FirJKlibSessionFactory : FirAbstractSessionFactory<FirJKlibSessionFactory.Context, FirJKlibSessionFactory.Context>() {

    // ==================================== Library session ====================================

    fun createLibrarySession(
        mainModuleName: Name,
        resolvedLibraries: List<KotlinResolvedLibrary>,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        projectEnvironment: AbstractProjectEnvironment,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        predefinedJavaComponents: FirSharableJavaComponents,
    ): FirSession {
        return createLibrarySession(
            mainModuleName,
            context = Context(projectEnvironment, predefinedJavaComponents),
            sessionProvider,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            createProviders = { session, builtinsModuleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider ->
                val resolvedKotlinLibraries = resolvedLibraries.map { it.library }
                val scope = projectEnvironment.getSearchScopeForProjectLibraries()
                val packagePartProvider = projectEnvironment.getPackagePartProvider(scope)
                val kotlinClassFinder = projectEnvironment.getKotlinClassFinder(scope)

                listOfNotNull(
                    JvmClassFileBasedSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        packagePartProvider,
                        kotlinClassFinder,
                        projectEnvironment.getFirJavaFacade(session, moduleDataProvider.allModuleData.last(), scope)
                    ),
                    runUnless(languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
                        initializeBuiltinsProvider(session, builtinsModuleData, kotlinScopeProvider, kotlinClassFinder)
                    },
                    KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, resolvedKotlinLibraries),
                    FirBuiltinSyntheticFunctionInterfaceProvider.initialize(session, builtinsModuleData, kotlinScopeProvider),
                    syntheticFunctionInterfaceProvider,
                    FirCloneableSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                    OptionalAnnotationClassesProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        packagePartProvider
                    )
                )
            })
    }

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
    }

    override fun FirSession.registerLibrarySessionComponents(c: Context) {
        registerComponents()
        registerJavaComponents(c.projectEnvironment.getJavaModuleResolver(), c.predefinedJavaComponents)
    }

    // ==================================== Platform session ====================================

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        projectEnvironment: AbstractProjectEnvironment,
        createIncrementalCompilationSymbolProviders: (FirSession) -> FirJvmIncrementalCompilationSymbolProviders?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        init: FirSessionConfigurator.() -> Unit,
        predefinedJavaComponents: FirSharableJavaComponents
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            context = Context(projectEnvironment, predefinedJavaComponents),
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker = null,
            enumWhenTracker = null,
            importTracker = null,
            init,
            createProviders = { session, kotlinScopeProvider, symbolProvider, generatedSymbolsProvider, dependencies ->
                val scope = projectEnvironment.getSearchScopeForProjectLibraries()
                val javaSymbolProvider =
                    JavaSymbolProvider(session, projectEnvironment.getFirJavaFacade(session, moduleData, scope))
                session.register(JavaSymbolProvider::class, javaSymbolProvider)

                val incrementalCompilationSymbolProviders = createIncrementalCompilationSymbolProviders(session)

                listOfNotNull(
                    javaSymbolProvider,
                    *(incrementalCompilationSymbolProviders?.previousFirSessionsSymbolProviders?.toTypedArray() ?: emptyArray()),
                    incrementalCompilationSymbolProviders?.symbolProviderForBinariesFromIncrementalCompilation,
                    initializeForStdlibIfNeeded(projectEnvironment, session, kotlinScopeProvider, dependencies),
                    symbolProvider,
                    generatedSymbolsProvider,
                    *dependencies.toTypedArray(),
                    incrementalCompilationSymbolProviders?.optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation,
                )
            }
        ).also {
            projectEnvironment.registerAsJavaElementFinder(it)
        }
    }

    override fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): FirKotlinScopeProvider {
        return FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
    }

    override fun FirSessionConfigurator.registerPlatformCheckers(c: Context) {
        registerJvmCheckers()
    }

    override fun FirSession.registerSourceSessionComponents(c: Context) {
        registerComponents()
        registerJavaComponents(c.projectEnvironment.getJavaModuleResolver(), c.predefinedJavaComponents)
    }

    // ==================================== Common parts ====================================

    private fun FirSession.registerComponents() {
        registerDefaultComponents()
    }

    // ==================================== Utilities ====================================

    class Context(
        val projectEnvironment: AbstractProjectEnvironment,
        val predefinedJavaComponents: FirSharableJavaComponents
    )

    private fun initializeForStdlibIfNeeded(
        projectEnvironment: AbstractProjectEnvironment,
        session: FirSession,
        kotlinScopeProvider: FirKotlinScopeProvider,
        dependencies: List<FirSymbolProvider>,
    ): FirSymbolProvider? {
        return runIf(session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) && !session.moduleData.isCommon) {
            val kotlinClassFinder = projectEnvironment.getKotlinClassFinder(projectEnvironment.getSearchScopeForProjectLibraries())
            val builtinsSymbolProvider = initializeBuiltinsProvider(session, session.moduleData, kotlinScopeProvider, kotlinClassFinder)
            if (session.moduleData.dependsOnDependencies.isNotEmpty()) {
                val refinedSourceSymbolProviders = dependencies.filter { it.session.kind == FirSession.Kind.Source }
                FirJvmActualizingBuiltinSymbolProvider(builtinsSymbolProvider, refinedSourceSymbolProviders)
            } else {
                // `FirBuiltinsSymbolProvider` is needed anyway for jvm-only modules that don't have common dependencies (jdk7, jdk8)
                builtinsSymbolProvider
            }
        }
    }

    private fun initializeBuiltinsProvider(
        session: FirSession,
        builtinsModuleData: FirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        kotlinClassFinder: KotlinClassFinder,
    ): FirBuiltinsSymbolProvider = FirBuiltinsSymbolProvider(
        session, FirClasspathBuiltinSymbolProvider(
            session,
            builtinsModuleData,
            kotlinScopeProvider
        ) { kotlinClassFinder.findBuiltInsData(it) },
        FirFallbackBuiltinSymbolProvider(session, builtinsModuleData, kotlinScopeProvider)
    )
}

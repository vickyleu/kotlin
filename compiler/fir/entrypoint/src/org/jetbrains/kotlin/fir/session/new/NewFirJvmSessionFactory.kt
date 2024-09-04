/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session.new

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerJvmCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirJvmTargetProvider
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirJvmActualizingBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.*
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirJvmIncrementalCompilationSymbolProviders
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.fir.session.FirSharableJavaComponents
import org.jetbrains.kotlin.fir.session.KlibBasedSymbolProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.session.registerDefaultComponents
import org.jetbrains.kotlin.fir.session.registerJavaComponents
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

@OptIn(SessionConfiguration::class)
object NewFirJvmSessionFactory : NewFirAbstractSessionFactory<NewFirJvmSessionFactory.LibraryContext, NewFirJvmSessionFactory.SourceContext>() {

    // ==================================== Shared dependency session ====================================

    fun createSharedDependencySession(
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        projectEnvironment: AbstractProjectEnvironment,
        extensionRegistrars: List<FirExtensionRegistrar>,
        scope: AbstractProjectFileSearchScope,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings,
        predefinedJavaComponents: FirSharableJavaComponents?,
    ): FirSession {
        val context = LibraryContext(
            predefinedJavaComponents,
            projectEnvironment,
            languageVersionSettings,
            scope,
            projectEnvironment.getKotlinClassFinder(scope),
            packagePartProvider,
            resolvedLibraries = emptyList(),
            isLeafModule = true,
        )
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
        c: LibraryContext
    ): List<FirSymbolProvider> {
        return listOfNotNull(
            runUnless(c.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
                initializeBuiltinsProvider(session, moduleData, kotlinScopeProvider, c.kotlinClassFinder)
            },
            FirBuiltinSyntheticFunctionInterfaceProvider.initialize(session, moduleData, kotlinScopeProvider),
            syntheticFunctionInterfaceProvider,
            FirCloneableSymbolProvider(session, moduleData, kotlinScopeProvider),
        )
    }

    // ==================================== Library session ====================================

    override fun setupDependencySymbolProviders(
        session: FirSession,
        moduleDataProvider: ModuleDataProvider,
        kotlinScopeProvider: FirKotlinScopeProvider,
        c: LibraryContext
    ): List<FirSymbolProvider> {
        return buildList {
            if (c.resolvedLibraries.isNotEmpty()) {
                this += KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, c.resolvedLibraries)
            }

            val regularDependencyModuleData = moduleDataProvider.allModuleData.first()
            if (!c.scope.isEmpty) {
                this += JvmClassFileBasedSymbolProvider(
                    session,
                    moduleDataProvider,
                    kotlinScopeProvider,
                    c.packagePartProvider,
                    c.kotlinClassFinder,
                    c.projectEnvironment.getFirJavaFacade(session, regularDependencyModuleData, c.scope)
                )
            }
            if (c.isLeafModule) {
                this += FirBuiltinsSymbolProvider(
                    session, FirClasspathBuiltinSymbolProvider(
                        session,
                        regularDependencyModuleData,
                        kotlinScopeProvider
                    ) { c.kotlinClassFinder.findBuiltInsData(it) },
                    FirFallbackBuiltinSymbolProvider(session, regularDependencyModuleData, kotlinScopeProvider)
                )
            }
        }
    }

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
    }

    override fun FirSession.registerLibrarySessionComponents(c: LibraryContext) {
        registerDefaultComponents()
        registerJavaComponents(c.projectEnvironment.getJavaModuleResolver(), c.predefinedJavaComponents, c.isLeafModule)
    }

    // ==================================== Platform session ====================================

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        sharedDependencySession: FirSession,
        moduleDataProvider: ModuleDataProvider,
        javaSourcesScope: AbstractProjectFileSearchScope,
        libraryScope: AbstractProjectFileSearchScope,
        packagePartProvider: PackagePartProvider,
        resolvedLibraries: List<KotlinLibrary>,
        projectEnvironment: AbstractProjectEnvironment,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        jvmTarget: JvmTarget,
        lookupTracker: LookupTracker?,
        enumWhenTracker: EnumWhenTracker?,
        importTracker: ImportTracker?,
        predefinedJavaComponents: FirSharableJavaComponents?,
        needRegisterJavaElementFinder: Boolean,
        isLeafModule: Boolean = true,
        init: FirSessionConfigurator.() -> Unit,
    ): FirSession {
        val libraryContext = LibraryContext(
            predefinedJavaComponents,
            projectEnvironment,
            languageVersionSettings,
            libraryScope,
            projectEnvironment.getKotlinClassFinder(libraryScope),
            packagePartProvider,
            resolvedLibraries,
            isLeafModule
        )
        val sourceContext = SourceContext(jvmTarget, predefinedJavaComponents, projectEnvironment, javaSourcesScope, isLeafModule)
        return createModuleBasedSession(
            moduleData,
            sharedDependencySession,
            moduleDataProvider,
            libraryContext,
            sourceContext,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker,
            enumWhenTracker,
            importTracker,
            init,
        ).also {
            if (needRegisterJavaElementFinder) {
                projectEnvironment.registerAsJavaElementFinder(it)
            }
        }
    }

    override fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): FirKotlinScopeProvider {
        return if (languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) && moduleData.isCommon) {
            FirKotlinScopeProvider()
        } else {
            FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        }
    }

    override fun FirSessionConfigurator.registerPlatformCheckers(c: SourceContext) {
        registerJvmCheckers()
    }

    override fun FirSession.registerSourceSessionComponents(c: SourceContext) {
        registerDefaultComponents()
        registerJavaComponents(c.projectEnvironment.getJavaModuleResolver(), c.predefinedJavaComponents, c.isLeafModule)
        register(FirJvmTargetProvider::class, FirJvmTargetProvider(c.jvmTarget))
    }

    override fun setupPlatformSpecificSourceSessionProviders(
        session: FirSession,
        moduleData: FirModuleData,
        c: SourceContext
    ): List<FirSymbolProvider> {
        val javaSymbolProvider = JavaSymbolProvider(session, c.projectEnvironment.getFirJavaFacade(session, moduleData, c.javaSourcesScope))
        session.register(JavaSymbolProvider::class, javaSymbolProvider)
        return listOf(javaSymbolProvider)
    }

    // ==================================== Common parts ====================================

    // ==================================== Utilities ====================================

    class LibraryContext(
        val predefinedJavaComponents: FirSharableJavaComponents?,
        val projectEnvironment: AbstractProjectEnvironment,
        val languageVersionSettings: LanguageVersionSettings,
        val scope: AbstractProjectFileSearchScope,
        val kotlinClassFinder: KotlinClassFinder,
        val packagePartProvider: PackagePartProvider,
        val resolvedLibraries: List<KotlinLibrary>,
        val isLeafModule: Boolean,
    )

    class SourceContext(
        val jvmTarget: JvmTarget,
        val predefinedJavaComponents: FirSharableJavaComponents?,
        val projectEnvironment: AbstractProjectEnvironment,
        val javaSourcesScope: AbstractProjectFileSearchScope,
        val isLeafModule: Boolean,
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

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.unregisterFinders
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.container.topologicalSort
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerExtraCommonCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.session.new.NewFirJvmSessionFactory
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.lightTreeSyntaxDiagnosticsReporterHolder
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import org.jetbrains.kotlin.konan.file.File as KFile

open class FirFrontendFacade(
    testServices: TestServices,
    private val additionalSessionConfiguration: SessionConfiguration?
) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    // TODO: there might be more than one root
    // Ideally need to use leaf instead of root
    private val sharedDependencySessionsByRootModule: MutableMap<TestModule, FirSession> = mutableMapOf()

    // Separate constructor is needed for creating callable references to it
    constructor(testServices: TestServices) : this(testServices, additionalSessionConfiguration = null)

    fun interface SessionConfiguration : (FirSessionConfigurator) -> Unit

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirModuleInfoProvider))

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    private fun registerExtraComponents(session: FirSession) {
        testServices.firSessionComponentRegistrar?.registerAdditionalComponent(session)
    }

    override fun analyze(module: TestModule): FirOutputArtifact {
        val sortedModules = listOf(module)

        val (moduleDataMap, moduleDataProvider) = initializeModuleData(sortedModules)

        val project = testServices.compilerConfigurationProvider.getProject(module)
        val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
        val projectEnvironment = createProjectEnvironment(module, project)

        val moduleInfoProvider = testServices.firModuleInfoProvider
        val sessionProvider = moduleInfoProvider.firSessionProvider
        val dependencySearchScope = PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))
        val packagePartProvider = projectEnvironment?.getPackagePartProvider(dependencySearchScope)

        val targetPlatform = module.targetPlatform
        val sharedDependencySession = createSharedDependenciesSession(
            module,
            targetPlatform,
            sessionProvider,
            moduleDataProvider,
            projectEnvironment,
            extensionRegistrars,
            dependencySearchScope,
            packagePartProvider
        )

        val predefinedJavaComponents = runIf(targetPlatform.isJvm()) {
            FirSharableJavaComponents(firCachesFactoryForCliMode)
        }

        val firOutput = analyze(
            module,
            moduleDataMap[module]!!,
            targetPlatform,
            projectEnvironment,
            extensionRegistrars,
            predefinedJavaComponents,
            sharedDependencySession,
            libraryScope = dependencySearchScope,
            packagePartProvider,
            moduleDataProvider
        )

        return FirOutputArtifactImpl(listOf(firOutput))
    }

    private fun createSharedDependenciesSession(
        module: TestModule,
        targetPlatform: TargetPlatform,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        projectEnvironment: AbstractProjectEnvironment?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        dependencySearchScope: PsiBasedProjectFileSearchScope,
        packagePartProvider: PackagePartProvider?,
    ): FirSession = sharedDependencySessionsByRootModule.getOrPut(module.findLeaf(testServices)) {
        when {
            targetPlatform.isCommon() || targetPlatform.isJvm() -> {
                NewFirJvmSessionFactory.createSharedDependencySession(
                    sessionProvider,
                    moduleDataProvider,
                    projectEnvironment!!,
                    extensionRegistrars,
                    dependencySearchScope,
                    packagePartProvider!!,
                    module.languageVersionSettings,
                    FirSharableJavaComponents(firCachesFactoryForCliMode)
                )
            }
            targetPlatform.isJs() -> {
                NewFirJsSessionFactory.createSharedDependencySession(
                    sessionProvider,
                    moduleDataProvider,
                    extensionRegistrars,
                    module.languageVersionSettings
                )
            }
            else -> TODO()
        }
    }

    protected fun sortDependsOnTopologically(module: TestModule): List<TestModule> {
        return topologicalSort(listOf(module), reverseOrder = true) { item ->
            item.dependsOnDependencies.map { testServices.dependencyProvider.getTestModule(it.moduleName) }
        }
    }

    private fun initializeModuleData(modules: List<TestModule>): Pair<Map<TestModule, FirModuleData>, ModuleDataProvider> {
        val mainModule = modules.last()

        val targetPlatform = mainModule.targetPlatform

        // the special name is required for `KlibMetadataModuleDescriptorFactoryImpl.createDescriptorOptionalBuiltIns`
        // it doesn't seem convincingly legitimate, probably should be refactored
        val moduleName = Name.special("<${mainModule.name}>")
        val binaryModuleData = BinaryModuleData.initialize(moduleName, targetPlatform)

        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(mainModule)

        val libraryList = initializeLibraryList(mainModule, binaryModuleData, targetPlatform, configuration, testServices)

        val moduleInfoProvider = testServices.firModuleInfoProvider
        val moduleDataMap = mutableMapOf<TestModule, FirModuleData>()

        val module = modules.last()

        val regularModules = libraryList.regularDependencies + moduleInfoProvider.getRegularDependentSourceModules(module)
        val friendModules = libraryList.friendsDependencies + moduleInfoProvider.getDependentFriendSourceModules(module)
        val dependsOnModules = libraryList.dependsOnDependencies + moduleInfoProvider.getDependentDependsOnSourceModules(module)

        val moduleData = FirModuleDataImpl(
            Name.special("<${module.name}>"),
            regularModules,
            dependsOnModules,
            friendModules,
            mainModule.targetPlatform,
            isCommon = module.targetPlatform.isCommon(),
        )

        moduleInfoProvider.registerModuleData(module, moduleData)

        moduleDataMap[module] = moduleData

        return moduleDataMap to libraryList.moduleDataProvider
    }

    private fun createProjectEnvironment(module: TestModule, project: Project): AbstractProjectEnvironment? {
        return when {
            module.targetPlatform.isCommon() || module.targetPlatform.isJvm() -> {
                val compilerConfigurationProvider = testServices.compilerConfigurationProvider
                val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)
                VfsBasedProjectEnvironment(
                    project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                ) { packagePartProviderFactory.invoke(it) }
            }
            else -> null
        }
    }

    private fun analyze(
        module: TestModule,
        moduleData: FirModuleData,
        targetPlatform: TargetPlatform,
        projectEnvironment: AbstractProjectEnvironment?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        predefinedJavaComponents: FirSharableJavaComponents?,
        sharedDependencySession: FirSession,
        libraryScope: AbstractProjectFileSearchScope,
        packagePartProvider: PackagePartProvider?,
        moduleDataProvider: ModuleDataProvider
    ): FirOutputPartForDependsOnModule {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val moduleInfoProvider = testServices.firModuleInfoProvider
        val sessionProvider = moduleInfoProvider.firSessionProvider

        val project = compilerConfigurationProvider.getProject(module)

        PsiElementFinder.EP.getPoint(project).unregisterFinders<JavaElementFinder>()

        val parser = module.directives.singleValue(FirDiagnosticsDirectives.FIR_PARSER)

        val (ktFiles, lightTreeFiles) = when (parser) {
            FirParser.LightTree -> {
                emptyMap<TestFile, KtFile>() to testServices.sourceFileProvider.getKtSourceFilesForSourceFiles(module.files)
            }
            FirParser.Psi -> testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project) to emptyMap()
        }

        val sessionConfigurator: FirSessionConfigurator.() -> Unit = {
            if (FirDiagnosticsDirectives.WITH_EXTRA_CHECKERS in module.directives) {
                registerExtraCommonCheckers()
            }
            additionalSessionConfiguration?.invoke(this)
        }

        val moduleBasedSession = createModuleBasedSession(
            module,
            moduleData,
            targetPlatform,
            sessionProvider,
            projectEnvironment,
            extensionRegistrars,
            sessionConfigurator,
            predefinedJavaComponents,
            project,
            ktFiles.values,
            sharedDependencySession,
            libraryScope,
            packagePartProvider,
            moduleDataProvider
        )

        val firAnalyzerFacade = FirAnalyzerFacade(
            moduleBasedSession,
            ktFiles.values,
            lightTreeFiles.values,
            parser,
            testServices.lightTreeSyntaxDiagnosticsReporterHolder?.reporter,
        )
        val firFiles = firAnalyzerFacade.runResolution()

        val usedFilesMap = when (parser) {
            FirParser.LightTree -> lightTreeFiles
            FirParser.Psi -> ktFiles
        }

        val filesMap = usedFilesMap.keys
            .zip(firFiles)
            .onEach { assert(it.first.name == it.second.name) }
            .toMap()

        return FirOutputPartForDependsOnModule(module, moduleBasedSession, firAnalyzerFacade, filesMap)
    }

    private fun createModuleBasedSession(
        module: TestModule,
        moduleData: FirModuleData,
        targetPlatform: TargetPlatform,
        sessionProvider: FirProjectSessionProvider,
        projectEnvironment: AbstractProjectEnvironment?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
        predefinedJavaComponents: FirSharableJavaComponents?,
        project: Project,
        ktFiles: Collection<KtFile>,
        sharedDependencySession: FirSession,
        libraryScope: AbstractProjectFileSearchScope,
        packagePartProvider: PackagePartProvider?,
        moduleDataProvider: ModuleDataProvider
    ): FirSession {
        val languageVersionSettings = module.languageVersionSettings
        return when {
//            targetPlatform.isCommon() -> {
//                TODO()
//                FirCommonSessionFactory.createModuleBasedSession(
//                    moduleData = moduleData,
//                    sessionProvider = sessionProvider,
//                    projectEnvironment = projectEnvironment!!,
//                    incrementalCompilationContext = null,
//                    extensionRegistrars = extensionRegistrars,
//                    languageVersionSettings = languageVersionSettings,
//                    init = sessionConfigurator,
//                ).also(::registerExtraComponents)
//            }
            targetPlatform.isCommon() || targetPlatform.isJvm() -> {
                val isLeafModule = module.isLeaf(testServices)
                val (libraryScope, resolvedLibraries) = when {
                    !isLeafModule -> {
                        val kLibs = commonKlibDependencies(module, testServices).map { resolveSingleFileKlib(KFile(it)) }
                        AbstractProjectFileSearchScope.EMPTY to kLibs
                    }
                    else -> libraryScope to emptyList()
                }

                NewFirJvmSessionFactory.createModuleBasedSession(
                    moduleData,
                    sessionProvider,
                    sharedDependencySession,
                    moduleDataProvider,
                    javaSourcesScope = PsiBasedProjectFileSearchScope(TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)),
                    libraryScope = libraryScope,
                    packagePartProvider = packagePartProvider!!,
                    resolvedLibraries,
                    projectEnvironment = projectEnvironment!!,
                    extensionRegistrars,
                    languageVersionSettings,
                    jvmTarget = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                        .get(JVMConfigurationKeys.JVM_TARGET, JvmTarget.DEFAULT),
                    lookupTracker = null,
                    enumWhenTracker = null,
                    importTracker = null,
                    predefinedJavaComponents,
                    needRegisterJavaElementFinder = true,
                    isLeafModule,
                    init = sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isJs() -> {
                val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                val resolvedLibraries = resolveLibraries(
                    compilerConfiguration,
                    getAllJsDependenciesPaths(module, testServices)
                ).map { it.library }
                NewFirJsSessionFactory.createModuleBasedSession(
                    moduleData,
                    sessionProvider,
                    sharedDependencySession,
                    moduleDataProvider,
                    compilerConfiguration,
                    resolvedLibraries,
                    extensionRegistrars,
                    lookupTracker = null,
                    enumWhenTracker = null,
                    importTracker = null,
                    sessionConfigurator,
                ).also(::registerExtraComponents)
            }
            targetPlatform.isNative() -> {
                TODO()
//                FirNativeSessionFactory.createModuleBasedSession(
//                    moduleData,
//                    sessionProvider,
//                    extensionRegistrars,
//                    languageVersionSettings,
//                    init = sessionConfigurator
//                ).also(::registerExtraComponents)
            }
            targetPlatform.isWasm() -> {
                TODO()
//                TestFirWasmSessionFactory.createModuleBasedSession(
//                    moduleData,
//                    sessionProvider,
//                    extensionRegistrars,
//                    languageVersionSettings,
//                    testServices.compilerConfigurationProvider.getCompilerConfiguration(module).wasmTarget,
//                    lookupTracker = null,
//                    sessionConfigurator,
//                ).also(::registerExtraComponents)
            }
            else -> error("Unsupported")
        }
    }


    companion object {
        fun initializeLibraryList(
            mainModule: TestModule,
            binaryModuleData: BinaryModuleData,
            targetPlatform: TargetPlatform,
            configuration: CompilerConfiguration,
            testServices: TestServices
        ): DependencyListForCliModule {
            return DependencyListForCliModule.build(binaryModuleData) {
                when {
                    targetPlatform.isCommon() || targetPlatform.isJvm() -> {
                        if (mainModule.isLeaf(testServices)) {
                            dependencies(configuration.jvmModularRoots.map { it.toPath() })
                            dependencies(configuration.jvmClasspathRoots.map { it.toPath() })
                            friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
                        } else {
                            dependencies(commonKlibDependencies(mainModule, testServices))
                        }
                    }
                    targetPlatform.isJs() -> {
                        val runtimeKlibsPaths = JsEnvironmentConfigurator.getRuntimePathsForModule(mainModule, testServices)
                        val (transitiveLibraries, friendLibraries) = getTransitivesAndFriends(mainModule, testServices)
                        dependencies(runtimeKlibsPaths.map { Paths.get(it).toAbsolutePath() })
                        dependencies(transitiveLibraries.map { it.toPath().toAbsolutePath() })
                        friendDependencies(friendLibraries.map { it.toPath().toAbsolutePath() })
                    }
                    targetPlatform.isNative() -> {
                        val runtimeKlibsPaths = NativeEnvironmentConfigurator.getRuntimePathsForModule(mainModule, testServices)
                        val (transitiveLibraries, friendLibraries) = getTransitivesAndFriends(mainModule, testServices)
                        dependencies(runtimeKlibsPaths.map { Paths.get(it).toAbsolutePath() })
                        dependencies(transitiveLibraries.map { it.toPath().toAbsolutePath() })
                        friendDependencies(friendLibraries.map { it.toPath().toAbsolutePath() })
                    }
                    targetPlatform.isWasm() -> {
                        val runtimeKlibsPaths = WasmEnvironmentConfigurator.getRuntimePathsForModule(
                            configuration.get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS)
                        )
                        val (transitiveLibraries, friendLibraries) = getTransitivesAndFriends(mainModule, testServices)
                        dependencies(runtimeKlibsPaths.map { Paths.get(it).toAbsolutePath() })
                        dependencies(transitiveLibraries.map { it.toPath().toAbsolutePath() })
                        friendDependencies(friendLibraries.map { it.toPath().toAbsolutePath() })
                    }
                    else -> error("Unsupported")
                }
            }
        }

        private fun commonKlibDependencies(module: TestModule, testServices: TestServices): List<Path> {
            val commonStdlib = File("libraries/stdlib/build/libs/kotlin-stdlib-metadata-2.1.255-SNAPSHOT.klib").toPath()
            val dependentModules = module.regularDependencies.map { testServices.dependencyProvider.getTestModule(it.moduleName) }
            val artifacts = dependentModules.mapNotNull { testServices.dependencyProvider.getArtifactSafe(it, ArtifactKinds.KLib) }
            val kLibs = buildList {
                artifacts.mapTo(this) { it.outputFile.toPath() }
                add(commonStdlib)
            }
            return kLibs
        }
    }
}

fun TestModule.isLeaf(testServices: TestServices): Boolean {
    return testServices.moduleStructure.modules.none { module ->
        if (module === this) return@none false
        module.dependsOnDependencies.any { it.moduleName == this.name }
    }
}

fun TestModule.findLeaf(testServices: TestServices): TestModule {
    var result = this
    var counter = 0
    while (true) {
        result = testServices.moduleStructure.modules.firstOrNull { module ->
            if (module === result) return@firstOrNull false
            module.dependsOnDependencies.any { it.moduleName == result.name }
        } ?: break
        if (counter++ > 100) {
            error("Infinite loop")
        }
    }
    return result
}

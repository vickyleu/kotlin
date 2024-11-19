/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.arguments.K2JKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.messages.toLogger
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createIncrementalCompilationScope
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.configureJdkHomeFromSystemProperty
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinResolvedLibraryImpl
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

/**
 * This class is the entry-point for compiling Kotlin code into a Klib with references to jars.
 *
 */
class K2JKlibCompiler : CLICompiler<K2JKlibCompilerArguments>() {

    override val defaultPerformanceManager: CommonCompilerPerformanceManager = K2MJKlibPerformanceManager()

    override fun createArguments() = K2JKlibCompilerArguments()

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration, arguments: K2JKlibCompilerArguments, services: Services
    ) {
        // No specific arguments yet
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2JKlibCompilerArguments) {}

    public override fun doExecute(
        arguments: K2JKlibCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val collector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val performanceManager = configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)

        val pluginLoadResult = loadPlugins(paths, arguments, configuration)
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        val commonSources = arguments.commonSources?.toSet() ?: emptySet()
        val hmppCliModuleStructure = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
        if (hmppCliModuleStructure != null) {
            collector.report(ERROR, "HMPP module structure should not be passed during metadata compilation. Please remove `-Xfragments` and related flags")
            return COMPILATION_ERROR
        }

        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, isCommon = arg in commonSources, hmppModuleName = null)
        }

        with(configuration) {
            if (arguments.noJdk) {
                put(JVMConfigurationKeys.NO_JDK, true)
            } else {
                configureJdkHomeFromSystemProperty()
            }
            configuration.configureJdkClasspathRoots()
            if (!arguments.noStdlib) {
                getLibraryFromHome(
                    paths,
                    KotlinPaths::stdlibPath,
                    PathUtil.KOTLIN_JAVA_STDLIB_JAR,
                    messageCollector,
                    "'-no-stdlib'"
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.stdlib")
                }
                getLibraryFromHome(
                    paths,
                    KotlinPaths::scriptRuntimePath,
                    PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR,
                    messageCollector,
                    "'-no-stdlib'"
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.script.runtime")
                }
            }
            // "-no-stdlib" implies "-no-reflect": otherwise we would be able to transitively read stdlib classes through kotlin-reflect,
            // which is likely not what user wants since s/he manually provided "-no-stdlib"`
            if (!arguments.noReflect && !arguments.noStdlib) {
                getLibraryFromHome(
                    paths,
                    KotlinPaths::reflectPath,
                    PathUtil.KOTLIN_JAVA_REFLECT_JAR,
                    messageCollector,
                    "'-no-reflect' or '-no-stdlib'"
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.reflect")
                }
            }
            arguments.klibLibraries?.let { libraries ->
                put(JVMConfigurationKeys.KLIB_PATHS, libraries.split(File.pathSeparator.toRegex()).filterNot(String::isEmpty))
            }
            for (path in arguments.classpath?.split(File.pathSeparatorChar).orEmpty()) {
                add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(File(path)))
            }
        }

        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configuration.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)

        val projectEnvironment = createProjectEnvironment(
            configuration,
            rootDisposable,
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
            collector
        )
        val groupedSources = collectSources(configuration, projectEnvironment, collector)

        performanceManager.notifyCompilerInitialized(0, 0, "JKlib mode for $moduleName module")

        if (groupedSources.isEmpty()) {
            if (arguments.version) {
                return ExitCode.OK
            }
            collector.report(ERROR, "No source files")
            return COMPILATION_ERROR
        }

        val destination = arguments.destination
        if (arguments.destination.isNullOrEmpty()) {
            collector.report(
                STRONG_WARNING,
                "No destination given. Using current working directory"
            )
        }
        val destDir = File(destination ?: "")

        try {
            if (destination == null) {
                val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                messageCollector.report(ERROR, "Specify destination via -d")
                // return null
                TODO()
            }
            val rootModuleNameAsString = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)
            val rootModuleName = Name.special("<${rootModuleNameAsString}>")

            val module = ModuleBuilder(
                configuration[CommonConfigurationKeys.MODULE_NAME] ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME,
                File(destination).path ?: ".", "java-production"
            )
            with(module) {
                arguments.friendPaths?.forEach { addFriendDir(it) }
                arguments.classpath?.split(File.pathSeparator)?.forEach { addClasspathEntry(it) }
            }

            val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(collector)

            val klibFiles = configuration.getList(JVMConfigurationKeys.KLIB_PATHS)

            val logger = collector.toLogger()

            val resolvedLibraries = klibFiles.map {
                KotlinResolvedLibraryImpl(
                    resolveSingleFileKlib(
                        org.jetbrains.kotlin.konan.file.File(it),
                        logger
                    )
                )
            }
            val extensionRegistrars = FirExtensionRegistrar.getInstances(projectEnvironment.project)
            val ltFiles = groupedSources.let { it.commonSources + it.platformSources }.toList()

            val libraryList = createLibraryListForJvm(rootModuleNameAsString, configuration, module.getFriendPaths())

            var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
            val incrementalCompilationScope = createIncrementalCompilationScope(
                configuration,
                projectEnvironment,
                null,
            )?.also { librariesScope -= it }

            val sessionsWithSources = prepareJKlibSessions(
                projectEnvironment,
                ltFiles,
                configuration,
                rootModuleName,
                resolvedLibraries,
                libraryList,
                extensionRegistrars,
                metadataCompilationMode = false,
                isCommonSource = groupedSources.isCommonSourceForLt,
                fileBelongsToModule = groupedSources.fileBelongsToModuleForLt,
                createProviderAndScopeForIncrementalCompilation = { files ->
                    val scope = projectEnvironment.getSearchScopeBySourceFiles(files)
                    org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createContextForIncrementalCompilation(
                        configuration,
                        projectEnvironment,
                        scope,
                        emptyList(),
                        incrementalCompilationScope
                    )
                }
            )
            val outputs = sessionsWithSources.map { (session, files) ->
                val firFiles = session.buildFirViaLightTree(files, diagnosticsReporter, performanceManager::addSourcesStats)
                resolveAndCheckFir(session, firFiles, diagnosticsReporter)
            }

            outputs.runPlatformCheckers(diagnosticsReporter)

            val firFiles = outputs.flatMap { it.fir }
            checkKotlinPackageUsageForLightTree(configuration, firFiles)

            val renderDiagnosticName = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
            if (diagnosticsReporter.hasErrors) {
                diagnosticsReporter.reportToMessageCollector(collector, renderDiagnosticName)
                return COMPILATION_ERROR
            }

            val firResult = FirResult(outputs)

            performanceManager.notifyAnalysisFinished()

            val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl())
            val irGenerationExtensions = IrGenerationExtension.getInstances(projectEnvironment.project)
            val fir2IrResult =
                firResult.convertToIrAndActualizeForJvm(fir2IrExtensions, configuration, diagnosticsReporter, irGenerationExtensions)
            //val (factory, input) = fir2IrAndIrActualizerResult.codegenFactoryWithJvmIrBackendInput(configuration)

            performanceManager.notifyGenerationStarted()

            val produceHeaderKlib = true // TODO: make CLI argument instead

            val serializerOutput = serializeModuleIntoKlib(
                moduleName = fir2IrResult.irModuleFragment.name.asString(),
                irModuleFragment = fir2IrResult.irModuleFragment,
                configuration = configuration,
                diagnosticReporter = diagnosticsReporter,
                compatibilityMode = CompatibilityMode.CURRENT,
                cleanFiles = emptyList(),
                dependencies = resolvedLibraries.map { it.library },
                createModuleSerializer = {
                        irDiagnosticReporter: IrDiagnosticReporter, irBuiltIns: IrBuiltIns, compatibilityMode: CompatibilityMode,
                        normalizeAbsolutePaths: Boolean, sourceBaseDirs: Collection<String>, languageVersionSettings: LanguageVersionSettings,
                        shouldCheckSignaturesOnUniqueness: Boolean
                    ->
                    JKlibModuleSerializer(
                        compatibilityMode = compatibilityMode,
                        normalizeAbsolutePaths = normalizeAbsolutePaths,
                        sourceBaseDirs = sourceBaseDirs,
                        languageVersionSettings = languageVersionSettings,
                        bodiesOnlyForInlines = produceHeaderKlib,
                        publicAbiOnly = produceHeaderKlib,
                        shouldCheckSignaturesOnUniqueness = shouldCheckSignaturesOnUniqueness,
                        diagnosticReporter = irDiagnosticReporter,
                    )
                },
                metadataSerializer = Fir2KlibMetadataSerializer(
                    configuration,
                    firResult.outputs,
                    produceHeaderKlib = produceHeaderKlib,
                    fir2IrActualizedResult = fir2IrResult,
                    exportKDoc = false,
                ),
            ).also {
                performanceManager.notifyGenerationFinished()
            }

            val versions = KotlinLibraryVersioning(
                abiVersion = KotlinAbiVersion.CURRENT,
                compilerVersion = KotlinCompilerVersion.getVersion(),
                metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
            )

            buildKotlinLibrary(
                linkDependencies = serializerOutput.neededLibraries,
                ir = serializerOutput.serializedIr,
                metadata = serializerOutput.serializedMetadata ?: error("expected serialized metadata"),
                versions = versions,
                output = destDir.absolutePath,
                moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!,
                nopack = false,
                perFile = false,
                manifestProperties = null,
                builtInsPlatform = BuiltInsPlatform.COMMON,
                nativeTargets = emptyList()
            )
        } catch (e: CompilationException) {
            collector.report(EXCEPTION, OutputMessageUtil.renderException(e), MessageUtil.psiElementToMessageLocation(e.element))
            return ExitCode.INTERNAL_ERROR
        }

        return ExitCode.OK
    }

    override fun executableScriptFileName(): String = "kotlinc"

    public override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = BuiltInsBinaryVersion(*versionArray)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JKlibCompiler(), args)
        }
    }

    protected class K2MJKlibPerformanceManager : CommonCompilerPerformanceManager("Kotlin to JKlib compiler")
}
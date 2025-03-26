/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.buildtools.internal.KotlinBuildToolsInternalJdkUtils
import org.jetbrains.kotlin.buildtools.internal.getJdkClassesClassLoader
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerArgumentsLogLevel
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.ParentClassLoaderProvider
import org.jetbrains.kotlin.gradle.logging.gradleLogLevel
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.plugin.statistics.NativeArgumentMetrics
import org.jetbrains.kotlin.gradle.utils.escapeStringCharacters
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.phaseDescription
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.inject.Inject
import kotlin.io.path.absolutePathString

internal abstract class KotlinNativeToolRunner @Inject constructor(
    private val metricsReporterProvider: Provider<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>>,
    private val classLoadersCachingBuildServiceProvider: Provider<ClassLoadersCachingBuildService>,
    private val toolSpec: ToolSpec,
    private val fusMetricsConsumer: Provider<out BuildFusService<out BuildFusService.Parameters>>,
    private val execOperations: ExecOperations,
) {
    private val logger = Logging.getLogger(toolSpec.displayName.get())
    private val classLoadersCachingBuildService: ClassLoadersCachingBuildService
        get() = classLoadersCachingBuildServiceProvider.get()
    private val metricsReporter get() = metricsReporterProvider.get()

    fun runTool(args: ToolArguments) {
        metricsReporter.measure(GradleBuildTime.RUN_COMPILATION_IN_WORKER) {
            fusMetricsConsumer.orNull?.let { metricsConsumer ->
                NativeArgumentMetrics.collectMetrics(args.arguments, metricsConsumer.getFusMetricsConsumer())
            }
            if (args.shouldRunInProcessMode) {
                runInProcess(args)
            } else {
                runViaExec(args)
            }
        }
    }

    private fun runViaExec(args: ToolArguments) {
        metricsReporter.measure(GradleBuildTime.NATIVE_IN_EXECUTOR) {
            val systemProperties = System.getProperties()
                /* Capture 'System.getProperties()' current state to avoid potential 'ConcurrentModificationException' */
                .snapshot()
                .asSequence()
                .map { (k, v) -> k.toString() to v.toString() }
                .filter { (k, _) -> k !in toolSpec.systemPropertiesBlacklist }
                .escapeQuotesForWindows()
                .toMap() + toolSpec.systemProperties

            val reportFile = File.createTempFile("native_compiler_report", "txt")
            val reportArguments = if (toolSpec.collectNativeCompilerMetrics.get()) {
                listOf("--report-file", reportFile.absolutePath)
            } else emptyList()

            val toolArgsPair = if (toolSpec.shouldPassArgumentsViaArgFile.get()) {
                val argFile = args.toArgFile(/*reportFile*/)
                argFile to listOfNotNull(
                    toolSpec.optionalToolName.orNull,
                    "@${argFile.toFile().absolutePath}"
                )
            } else {
                null to reportArguments + listOfNotNull(toolSpec.optionalToolName.orNull) + args.arguments
            }

            try {
                logger.log(
                    args.compilerArgumentsLogLevel.gradleLogLevel,
                    """
                |Run "${toolSpec.displayName.get()}" tool in a separate JVM process
                |Main class = ${toolSpec.mainClass.get()}
                |Arguments = ${args.arguments.toPrettyString()}
                |Transformed arguments = ${toolArgsPair.second.toPrettyString()}
                |Classpath = ${toolSpec.classpath.files.map { it.absolutePath }.toPrettyString()}
                |JVM options = ${toolSpec.jvmArgs.get().toPrettyString()}
                |Java system properties = ${systemProperties.toPrettyString()}
                |Suppressed ENV variables = ${toolSpec.environmentBlacklist.toPrettyString()}
                |Custom ENV variables = ${toolSpec.environment.toPrettyString()}
                """.trimMargin()
                )

                execOperations.javaexec { spec ->
                    spec.mainClass.set(toolSpec.mainClass)
                    spec.classpath = toolSpec.classpath
                    spec.jvmArgs(toolSpec.jvmArgs.get())
                    spec.systemProperties(systemProperties)
                    spec.environment(toolSpec.environment)
                    toolSpec.environmentBlacklist.forEach { spec.environment.remove(it) }
                    spec.args(toolArgsPair.second)
                }
                metricsReporter.parseCompilerMetricsFromFile(reportFile)
            } finally {
                toolArgsPair.first?.let {
                    try {
                        Files.deleteIfExists(it)
                    } catch (_: IOException) {
                    }
                }
            }
        }
    }

    private fun BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>.parseCompilerMetricsFromFile(file: File) {
        if (!file.isFile()) return
        file.forEachLine { line ->
            val matchResult = pattern.matchEntire(line)
            if (matchResult != null) {
                val metricName = matchResult.groupValues.getOrNull(1).let { phases[it] }
                val value = matchResult.groupValues.getOrNull(2)?.toLong()
                if (metricName != null && value != null) {
                    metricsReporter.addTimeMetricMs(metricName.toGradleBuildTime(), value)
                }
            }
        }
    }

    private fun runInProcess(args: ToolArguments) {
        metricsReporter.measure(GradleBuildTime.NATIVE_IN_PROCESS) {
            val isolatedClassLoader = classLoadersCachingBuildService.getClassLoader(
                toolSpec.classpath.files.toList(),
                // Required for KotlinNativePaths to properly detect konan home directory
                object : ParentClassLoaderProvider {
                    @OptIn(KotlinBuildToolsInternalJdkUtils::class)
                    private val jdkClassesClassLoader = getJdkClassesClassLoader()

                    override fun getClassLoader(): ClassLoader? = jdkClassesClassLoader
                    override fun hashCode(): Int = jdkClassesClassLoader.hashCode()
                    override fun equals(other: Any?): Boolean =
                        other is ParentClassLoaderProvider && other.getClassLoader() == jdkClassesClassLoader
                }
            )
            if (toolSpec.jvmArgs.get().contains("-ea")) isolatedClassLoader.setDefaultAssertionStatus(true)

            logger.log(
                args.compilerArgumentsLogLevel.gradleLogLevel,
                """
                |Run in-process tool "${toolSpec.displayName.get()}"
                |Entry point method = ${toolSpec.mainClass.get()}.${toolSpec.daemonEntryPoint.get()}
                |Classpath = ${toolSpec.classpath.files.map { it.absolutePath }.toPrettyString()}
                |Arguments = ${args.arguments.toPrettyString()}
                """.trimMargin()
            )

            val toolArgs = listOf(toolSpec.displayName.get()) + args.arguments
            try {
                val mainClass = isolatedClassLoader.loadClass(toolSpec.mainClass.get())
                val entryPoint = mainClass
                    .methods
                    .singleOrNull { it.name == toolSpec.daemonEntryPoint.get() }
                    ?: error("Couldn't find daemon entry point '${toolSpec.daemonEntryPoint.get()}'")

                metricsReporter.measure(GradleBuildTime.RUN_ENTRY_POINT) {
                    if (toolSpec.collectNativeCompilerMetrics.get()) {
                        val reportFile = Files.createTempFile("compiler-native-report", ".txt")
                        val result = entryPoint.invoke(null, toolArgs.toTypedArray(), reportFile.absolutePathString())
                        println(result)
                        metricsReporter.parseCompilerMetricsFromFile(reportFile.toFile())
                    } else {
                        entryPoint.invoke(null, toolArgs.toTypedArray())
                    }
                }
            } catch (t: InvocationTargetException) {
                throw t.targetException
            }
        }
    }

    private fun Properties.snapshot(): Properties = clone() as Properties

    private fun Sequence<Pair<String, String>>.escapeQuotesForWindows() =
        if (HostManager.hostIsMingw) map { (key, value) -> key.escapeQuotes() to value.escapeQuotes() } else this

    private fun String.escapeQuotes() = replace("\"", "\\\"")

    private fun Map<String, String>.toPrettyString(): String = buildString {
        append('[')
        if (this@toPrettyString.isNotEmpty()) append('\n')
        this@toPrettyString.entries.forEach { (key, value) ->
            append('\t').append(key).append(" = ").append(value.toPrettyString()).append('\n')
        }
        append(']')
    }

    private fun Collection<String>.toPrettyString(): String = buildString {
        append('[')
        if (this@toPrettyString.isNotEmpty()) append('\n')
        this@toPrettyString.forEach { append('\t').append(it.toPrettyString()).append('\n') }
        append(']')
    }

    private fun String.toPrettyString(): String =
        when {
            isEmpty() -> "\"\""
            any { it == '"' || it.isWhitespace() } -> '"' + escapeStringCharacters(this) + '"'
            else -> this
        }

    private fun ToolArguments.toArgFile(/*reportFile: Path? = null*/): Path {
        val argFile = Files.createTempFile(
            "kotlinc-native-args",
            ".lst"
        )

        argFile.toFile().printWriter().use { w ->
            arguments.forEach { arg ->
                val escapedArg = arg
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                w.println("\"$escapedArg\"")
            }
        }

        return argFile
    }

    class ToolSpec(
        val displayName: Provider<String>,
        val optionalToolName: Provider<String>,
        val mainClass: Provider<String>,
        val daemonEntryPoint: Provider<String>,
        val classpath: FileCollection,
        val jvmArgs: ListProperty<String>,
        val shouldPassArgumentsViaArgFile: Provider<Boolean>,
        val collectNativeCompilerMetrics: Provider<Boolean>,
        val systemProperties: Map<String, String> = emptyMap(),
        val environment: Map<String, String> = emptyMap(),
        val environmentBlacklist: Set<String> = emptySet(),
    ) {
        val systemPropertiesBlacklist: Set<String> = setOf(
            "java.endorsed.dirs",       // Fix for KT-25887
            "user.dir",                 // Don't propagate the working dir of the current Gradle process
            "java.system.class.loader"  // Don't use custom class loaders
        )

        /**
         * Disable C2 compiler for HotSpot VM to improve compilation speed.
         */
        fun disableC2(): ToolSpec {
            System.getProperty("java.vm.name")?.let { vmName ->
                if (vmName.contains("HotSpot", true)) jvmArgs.add("-XX:TieredStopAtLevel=1")
            }

            return this
        }

        fun enableAssertions(): ToolSpec {
            jvmArgs.add("-ea")

            return this
        }

        fun configureDefaultMaxHeapSize(): ToolSpec {
            if (jvmArgs.get().none { it.startsWith("-Xmx") }) {
                jvmArgs.add("-Xmx3g")
            }

            return this
        }
    }

    data class ToolArguments(
        val shouldRunInProcessMode: Boolean,
        val compilerArgumentsLogLevel: KotlinCompilerArgumentsLogLevel,
        val arguments: List<String>,
    )

    companion object {
        private val phases = PhaseType.values().associateBy { it.phaseDescription() }
        private val pattern = "\\s*(\\w*)\\s*(\\d*) ms\\b*(\\d*)\\b*[loc/s]*".toRegex()
    }
}

private fun PhaseType.toGradleBuildTime() = when (this) {
    PhaseType.Initialization -> GradleBuildTime.COMPILER_INITIALIZATION
    PhaseType.Analysis -> GradleBuildTime.CODE_ANALYSIS
    PhaseType.TranslationToIr -> GradleBuildTime.TRANSLATION_TO_IR
    PhaseType.IrLowering -> GradleBuildTime.IR_LOWERING
    PhaseType.Backend -> GradleBuildTime.BACKEND
}
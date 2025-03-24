/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginPublicDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.existsCompat
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.io.Serializable
import javax.inject.Inject

@Suppress("unused") // used through .values() call
internal enum class AppleTarget(
    val targetName: String,
    val targets: List<KonanTarget>,
) : Serializable {
    MACOS_DEVICE("macos", listOf(KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64)),
    IPHONE_DEVICE("ios", listOf(KonanTarget.IOS_ARM64)),
    IPHONE_SIMULATOR("iosSimulator", listOf(KonanTarget.IOS_X64, KonanTarget.IOS_SIMULATOR_ARM64)),
    WATCHOS_DEVICE("watchos", listOf(KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_DEVICE_ARM64)),
    WATCHOS_SIMULATOR("watchosSimulator", listOf(KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_SIMULATOR_ARM64)),
    TVOS_DEVICE("tvos", listOf(KonanTarget.TVOS_ARM64)),
    TVOS_SIMULATOR("tvosSimulator", listOf(KonanTarget.TVOS_X64, KonanTarget.TVOS_SIMULATOR_ARM64))
}

internal class XCFrameworkTaskHolder(
    val buildType: NativeBuildType,
    val task: TaskProvider<XCFrameworkTask>,
    val resTask: TaskProvider<FrameworkResourcesTask>,
    val fatTasks: Map<AppleTarget, TaskProvider<FatFrameworkTask>>,
) {
    companion object {
        fun create(project: Project, xcFrameworkName: String, buildType: NativeBuildType): XCFrameworkTaskHolder {
            require(xcFrameworkName.isNotBlank())
            val task = project.registerAssembleXCFrameworkTask(xcFrameworkName, buildType)
            val resTask = project.registerAssembleResourcesForXCFrameworkTask(xcFrameworkName, buildType)

            task.dependsOn(resTask)

            val fatTasks = AppleTarget.values().associate { fatTarget ->
                val fatTask = project.registerAssembleFatForXCFrameworkTask(xcFrameworkName, buildType, fatTarget)

                resTask.configure {
                    it.inputs.files(fatTask.map { it.fatFramework })
                }

                task.configure {
                    it.inputs.files(fatTask.map { it.fatFramework })
                }

                fatTarget to fatTask
            }

            return XCFrameworkTaskHolder(buildType, task, resTask, fatTasks)
        }
    }
}

@KotlinGradlePluginPublicDsl
class XCFrameworkConfig {
    private val taskHolders: List<XCFrameworkTaskHolder>
    private val resourcesExtension: KotlinTargetResourcesPublication?

    constructor(project: Project, xcFrameworkName: String, buildTypes: Set<NativeBuildType>) {
        val parentTask = project.parentAssembleXCFrameworkTask(xcFrameworkName)
        resourcesExtension = project.multiplatformExtension.resourcesPublicationExtension
        taskHolders = buildTypes.map { buildType ->
            XCFrameworkTaskHolder.create(project, xcFrameworkName, buildType).also {
                parentTask.dependsOn(it.task)
            }
        }
    }

    constructor(project: Project) : this(project, project.name)
    constructor(project: Project, xcFrameworkName: String) : this(project, xcFrameworkName, NativeBuildType.values().toSet())

    /**
     * Adds the specified frameworks in this XCFramework.
     */
    fun add(framework: Framework) {
        val resources = if (resourcesExtension?.canResolveResources(framework.target) == true) {
            resourcesExtension.resolveResources(framework.target)
        } else {
            null
        }

        taskHolders.forEach { holder ->
            if (framework.buildType == holder.buildType) {
                val xcTask = holder.task
                val resTask = holder.resTask

                xcTask.configure { task ->
                    task.from(framework)
                }

                resTask.configure { task ->
                    resources?.let {
                        task.from(framework, resources)
                    }
                }

                AppleTarget.values()
                    .firstOrNull { it.targets.contains(framework.konanTarget) }
                    ?.also { appleTarget ->
                        val fTask = holder.fatTasks[appleTarget] ?: return

                        resTask.configure { task ->
                            task.from(
                                appleTarget,
                                fTask.map { it.fatFramework },
                                fTask.map { it.frameworks.size > 1 }
                            )
                        }

                        fTask.configure { fatTask ->
                            fatTask.baseName = framework.baseName //all frameworks should have same names
                            fatTask.from(framework)
                        }
                    }
            }
        }
    }
}

@KotlinGradlePluginPublicDsl
fun Project.XCFramework(xcFrameworkName: String = name) = XCFrameworkConfig(this, xcFrameworkName)

private fun Project.eraseIfDefault(xcFrameworkName: String) =
    if (name == xcFrameworkName) "" else xcFrameworkName

private fun Project.parentAssembleXCFrameworkTask(xcFrameworkName: String): TaskProvider<Task> =
    locateOrRegisterTask(lowerCamelCaseName("assemble", eraseIfDefault(xcFrameworkName), "XCFramework")) {
        it.group = "build"
        it.description = "Assemble all types of registered '$xcFrameworkName' XCFramework"
    }

private fun Project.registerAssembleResourcesForXCFrameworkTask(
    xcFrameworkName: String,
    buildType: NativeBuildType,
): TaskProvider<FrameworkResourcesTask> {
    val taskName = lowerCamelCaseName(
        "assemble",
        xcFrameworkName,
        buildType.getName(),
        "ResourcesFor",
        "XCFramework"
    )
    return registerTask(taskName)
}

private fun Project.registerAssembleXCFrameworkTask(
    xcFrameworkName: String,
    buildType: NativeBuildType,
): TaskProvider<XCFrameworkTask> {
    val taskName = lowerCamelCaseName(
        "assemble",
        xcFrameworkName,
        buildType.getName(),
        "XCFramework"
    )
    return registerTask(taskName) { task ->
        task.baseName = provider { xcFrameworkName }
        task.buildType = buildType
    }
}

//see: https://developer.apple.com/forums/thread/666335
private fun Project.registerAssembleFatForXCFrameworkTask(
    xcFrameworkName: String,
    buildType: NativeBuildType,
    appleTarget: AppleTarget,
): TaskProvider<FatFrameworkTask> {
    val taskName = lowerCamelCaseName(
        "assemble",
        buildType.getName(),
        appleTarget.targetName,
        "FatFrameworkFor",
        xcFrameworkName,
        "XCFramework"
    )

    return registerTask(taskName) { task ->
        task.destinationDirProperty.set(XCFrameworkTask.fatFrameworkDir(project, xcFrameworkName, buildType, appleTarget))
        task.onlyIf {
            task.frameworks.size > 1
        }
    }
}

@DisableCachingByDefault
internal abstract class FrameworkResourcesTask
@Inject
internal constructor(
    objectFactory: ObjectFactory,
    private val fileOperations: FileSystemOperations,
) : DefaultTask() {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:SkipWhenEmpty
    @get:InputFiles
    internal val inputResourceFiles: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:OutputDirectories
    internal val resourceOutputs: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Internal
    internal val resourcesMap: MapProperty<KonanTarget, File> = objectFactory.mapProperty(
        KonanTarget::class.java,
        File::class.java
    )

    @get:Internal
    internal val copyKonanTargetMap: MapProperty<KonanTarget, File> = objectFactory.mapProperty(
        KonanTarget::class.java,
        File::class.java
    )

    @get:Internal
    internal val copyAppleTargetMap: MapProperty<AppleTarget, File> = objectFactory.mapProperty(
        AppleTarget::class.java,
        File::class.java
    )

    @get:Internal
    internal val shouldCopyAppleTargetMap: MapProperty<AppleTarget, Boolean> = objectFactory.mapProperty(
        AppleTarget::class.java,
        Boolean::class.java
    )

    /**
     * Configures the resources and corresponding linkage tasks for the specified framework.
     *
     * @param framework The framework instance containing information about the target platform,
     *                  linkage tasks, and other attributes.
     * @param resources A provider for the file representing the framework's resources.
     */
    internal fun from(framework: Framework, resources: Provider<File>) {
        resourcesMap.put(
            framework.target.konanTarget,
            resources
        )

        copyKonanTargetMap.put(
            framework.target.konanTarget,
            framework.linkTaskProvider.map { it.outputFile.get() }
        )

        inputResourceFiles.from(resources)
        resourceOutputs.from(framework.linkTaskProvider.map { it.outputFile.get() })
    }

    /**
     * Configures the Apple target, its corresponding fat framework, and the copy behavior.
     *
     * @param appleTarget The Apple target representing the platform and its configurations.
     * @param fatFramework A provider for the fat framework file associated with the Apple target.
     * @param copy A provider indicating whether the framework resources should be copied.
     */
    internal fun from(appleTarget: AppleTarget, fatFramework: Provider<File>, copy: Provider<Boolean>) {
        copyAppleTargetMap.put(appleTarget, fatFramework)
        shouldCopyAppleTargetMap.put(appleTarget, copy)

        resourceOutputs.from(fatFramework)
    }

    @TaskAction
    protected fun copyResources() {
        copyKonanTargetMap.get().forEach { (target, destination) ->
            val from = resourcesMap.get().getValue(target)
            deleteExistingResources(from, destination)
            fileOperations.copy {
                it.from(from)
                it.into(destination)
            }
        }

        shouldCopyAppleTargetMap.get().forEach { (appleTarget, shouldCopy) ->
            if (!shouldCopy) return
            val froms = appleTarget.targets.map { resourcesMap.get().getValue(it) }
            val destination = copyAppleTargetMap.get().getValue(appleTarget)
            froms.forEach { from ->
                deleteExistingResources(from, destination)
            }

            fileOperations.copy {
                it.from(froms)
                it.into(destination)
                /**
                * Exclude duplicates to prevent copying the same file multiple times.
                * It's required because we are combining frameworks to a single fat-framework.
                */
                it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }
    }

    private fun deleteExistingResources(from: File, destination: File) {
        from.listFiles()?.forEach { file ->
            val existing = destination.resolve(file.name)
            if (existing.exists()) {
                existing.deleteRecursively()
            }
        }
    }
}

@DisableCachingByDefault
abstract class XCFrameworkTask
@Inject
internal constructor(
    private val execOperations: ExecOperations,
    private val projectLayout: ProjectLayout,
) : DefaultTask(), UsesKotlinToolingDiagnostics {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    private val projectBuildDir: File get() = projectLayout.buildDirectory.asFile.get()

    /**
     * A base name for the XCFramework.
     */
    @Input
    var baseName: Provider<String> = project.provider { project.name }

    @get:Internal
    internal val xcFrameworkName: Provider<String>
        get() = baseName.map { it.asValidFrameworkName() }

    /**
     * A build type of the XCFramework.
     */
    @Input
    var buildType: NativeBuildType = NativeBuildType.RELEASE

    private val groupedFrameworkFiles: MutableMap<AppleTarget, MutableList<FrameworkDescriptor>> = mutableMapOf()

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:SkipWhenEmpty
    val inputFrameworkFiles: Collection<File>
        get() = groupedFrameworkFiles.values.flatten().map { it.file }.filter {
            it.existsCompat()
        }

    /**
     * A parent directory for the XCFramework.
     */
    @get:Internal  // We take it into account as an output in the outputXCFrameworkFile property.
    var outputDir: File = projectBuildDir.resolve("XCFrameworks")

    /**
     * A parent directory for the fat frameworks.
     */
    @get:Internal  // We take it into account as an input in the buildType and baseName properties.
    protected val fatFrameworksDir: File
        get() = fatFrameworkDir(projectLayout.buildDirectory, xcFrameworkName.get(), buildType).getFile()

    @get:OutputDirectory
    internal val outputXCFrameworkFile: File
        get() = outputDir.resolve(buildType.getName()).resolve("${xcFrameworkName.get()}.xcframework")

    /**
     * Adds the specified frameworks in this XCFramework.
     */
    fun from(vararg frameworks: Framework) {
        frameworks.forEach { framework ->
            require(framework.konanTarget.family.isAppleFamily) {
                "XCFramework supports Apple frameworks only"
            }

            inputs.files(framework.linkTaskProvider.map { it.outputFile.get() })
        }
        fromFrameworkDescriptors(frameworks.map { FrameworkDescriptor(it) })
    }

    fun fromFrameworkDescriptors(vararg frameworks: FrameworkDescriptor) = fromFrameworkDescriptors(frameworks.toList())

    fun fromFrameworkDescriptors(frameworks: Iterable<FrameworkDescriptor>) {
        val frameworkName = groupedFrameworkFiles.values.flatten().firstOrNull()?.name

        frameworks.forEach { framework ->
            if (frameworkName != null && framework.name != frameworkName) {
                error(
                    "All inner frameworks in XCFramework '${baseName.get()}' should have same names. " +
                            "But there are two with '$frameworkName' and '${framework.name}' names"
                )
            }
            val group = AppleTarget.values().first { it.targets.contains(framework.target) }
            groupedFrameworkFiles.getOrPut(group) { mutableListOf() }.add(framework)
        }
    }

    @TaskAction
    fun assemble() {
        val xcfName = xcFrameworkName.get()

        val frameworksForXCFramework = xcframeworkSlices(
            frameworkName = singleFrameworkName(xcfName)
        )

        createXCFramework(frameworksForXCFramework, outputXCFrameworkFile)
    }

    internal fun singleFrameworkName(xcfName: String): String {
        val frameworks = groupedFrameworkFiles.values.flatten()
        if (frameworks.isEmpty()) error("XCFramework $xcfName is empty")

        val rawXcfName = baseName.get()
        val name = frameworks.first().name
        if (frameworks.any { it.name != name }) {
            error(
                "All inner frameworks in XCFramework '$rawXcfName' should have same names!" +
                        frameworks.joinToString("\n") { it.file.path })
        }
        if (name != xcfName) {
            toolingDiagnosticsCollector.get().report(
                this, KotlinToolingDiagnostics.XCFrameworkDifferentInnerFrameworksName(
                    xcFramework = rawXcfName,
                    innerFrameworks = name,
                )
            )
        }
        return name
    }

    internal fun xcframeworkSlices(frameworkName: String) = groupedFrameworkFiles.entries.mapNotNull { (group, files) ->
        when {
            files.size == 1 -> files.first()
            files.size > 1 -> FrameworkDescriptor(
                fatFrameworksDir.resolve(group.targetName).resolve("${frameworkName}.framework"),
                files.all { it.isStatic },
                group.targets.first() //will be not used
            )
            else -> null
        }
    }

    internal fun xcodebuildArguments(
        frameworkFiles: List<FrameworkDescriptor>,
        output: File,
        fileExists: (File) -> Boolean = { it.exists() },
    ): List<String> {
        val cmdArgs = mutableListOf("xcodebuild", "-create-xcframework")
        frameworkFiles.forEach { frameworkFile ->
            cmdArgs.add("-framework")
            cmdArgs.add(frameworkFile.file.path)
            if (!frameworkFile.isStatic) {
                val dsymFile = File(frameworkFile.file.path + ".dSYM")
                if (fileExists(dsymFile)) {
                    cmdArgs.add("-debug-symbols")
                    cmdArgs.add(dsymFile.path)
                }
            }
        }
        cmdArgs.add("-output")
        cmdArgs.add(output.path)
        return cmdArgs
    }

    private fun createXCFramework(frameworkFiles: List<FrameworkDescriptor>, output: File) {
        if (output.exists()) output.deleteRecursively()

        val cmdArgs = xcodebuildArguments(frameworkFiles, output)
        execOperations.exec { it.commandLine(cmdArgs) }
    }

    internal companion object {
        fun fatFrameworkDir(
            project: Project,
            xcFrameworkName: String,
            buildType: NativeBuildType,
            appleTarget: AppleTarget? = null,
        ): Provider<Directory> = fatFrameworkDir(project.layout.buildDirectory, xcFrameworkName, buildType, appleTarget)

        fun fatFrameworkDir(
            buildDir: DirectoryProperty,
            xcFrameworkName: String,
            buildType: NativeBuildType,
            appleTarget: AppleTarget? = null,
        ): Provider<Directory> = buildDir.map {
            it.dir(xcFrameworkName.asValidFrameworkName() + "XCFrameworkTemp")
                .dir("fatframework")
                .dir(buildType.getName())
                .dirIfNotNull(appleTarget?.targetName)
        }

        private fun Directory.dirIfNotNull(relative: String?): Directory = if (relative == null) this else this.dir(relative)
    }
}
